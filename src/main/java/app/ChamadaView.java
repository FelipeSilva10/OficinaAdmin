package app;

import core.*;
import dao.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela de Chamada — maquina de estados explicita.
 *
 *  PREVIEW     → cards de turmas + banner aula ativa (se houver)
 *  FORM        → seletor turma/data + tabela de presenca (nova chamada)
 *  HISTORICO   → lista de chamadas ja realizadas, com opcao de excluir
 */
public class ChamadaView {

    private enum Tela { PREVIEW, FORM, HISTORICO }
    private Tela telaAtual = Tela.PREVIEW;

    private BorderPane view;
    private MainFX mainApp;

    private final ChamadaDAO    chamadaDAO    = new ChamadaDAO();
    private final CronogramaDAO cronogramaDAO = new CronogramaDAO();
    private final TurmaDAO      turmaDAO      = new TurmaDAO();
    private final AlunoDAO      alunoDAO      = new AlunoDAO();

    private static final DateTimeFormatter FMT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private VBox painelPreview;
    private VBox painelForm;
    private VBox painelHistorico;
    private StackPane conteudo;

    private VBox   bannerAulaAtiva;
    private Label  lblAulaAtiva;
    private CronogramaAula slotAtual;

    private ComboBox<Turma> cbTurma;
    private DatePicker      dpData;
    private Label           lblCronInfo;
    private TableView<ChamadaPresenca> tabelaPresenca;
    private ObservableList<ChamadaPresenca> presencas = FXCollections.observableArrayList();
    private Label  lblResumo;
    private Button btnSalvar;
    private Label  lblTituloForm;

    private TableView<Chamada> tabelaHistorico;
    private ObservableList<Chamada> historico = FXCollections.observableArrayList();

    private Button tabBtnPreview, tabBtnHistorico;

    public ChamadaView(MainFX mainApp) {
        this.mainApp = mainApp;
        construirInterface();
        ir(Tela.PREVIEW);
    }

    // =========================================================================
    // CONSTRUCAO
    // =========================================================================

    private void construirInterface() {
        view = new BorderPane();

        Label lblTitulo = new Label("Chamada");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        tabBtnPreview   = tabBtn("Turmas");
        tabBtnHistorico = tabBtn("Historico");
        tabBtnPreview.setOnAction(e -> ir(Tela.PREVIEW));
        tabBtnHistorico.setOnAction(e -> ir(Tela.HISTORICO));

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> recarregarTelaAtual());

        Button btnNovaChamada = new Button("+ Nova Chamada");
        btnNovaChamada.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 14; -fx-font-weight: bold;");
        btnNovaChamada.setOnAction(e -> abrirFormManual(null, null));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox header = new HBox(12, lblTitulo, tabBtnPreview, tabBtnHistorico,
                sp, btnAtualizar, btnNovaChamada);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        construirPainelPreview();
        construirPainelForm();
        construirPainelHistorico();

        conteudo = new StackPane(painelPreview, painelForm, painelHistorico);
        view.setTop(header);
        view.setCenter(conteudo);
    }

    // ── PREVIEW ───────────────────────────────────────────────────────────────

    private void construirPainelPreview() {
        painelPreview = new VBox(0);
        VBox.setVgrow(painelPreview, Priority.ALWAYS);

        bannerAulaAtiva = new VBox(8);
        bannerAulaAtiva.setPadding(new Insets(12, 20, 8, 20));
        bannerAulaAtiva.setVisible(false); bannerAulaAtiva.setManaged(false);

        lblAulaAtiva = new Label();
        lblAulaAtiva.setStyle("-fx-background-color: #c6f6d5; -fx-text-fill: #22543d; " +
                "-fx-padding: 12 20; -fx-background-radius: 10; -fx-font-size: 14px; " +
                "-fx-font-weight: bold;");
        lblAulaAtiva.setMaxWidth(Double.MAX_VALUE);

        Button btnIniciar = new Button("Iniciar Chamada Agora");
        btnIniciar.setStyle("-fx-background-color: #38a169; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 10 20; -fx-font-weight: bold;");
        btnIniciar.setOnAction(e -> {
            if (slotAtual != null) {
                Turma t = encontrarTurmaObj(slotAtual.getTurmaId());
                abrirFormComSlot(t, LocalDate.now(), slotAtual);
            }
        });
        bannerAulaAtiva.getChildren().addAll(lblAulaAtiva, btnIniciar);

        VBox areaCards = new VBox(12);
        areaCards.setPadding(new Insets(8, 20, 16, 20));
        VBox.setVgrow(areaCards, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(areaCards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        painelPreview.getChildren().addAll(bannerAulaAtiva, scroll);
        painelPreview.setUserData(areaCards);
    }

    // ── FORM ──────────────────────────────────────────────────────────────────

    private void construirPainelForm() {
        painelForm = new VBox(0);
        painelForm.setVisible(false);
        VBox.setVgrow(painelForm, Priority.ALWAYS);

        Button btnVoltar = new Button("← Voltar");
        btnVoltar.setStyle("-fx-background-color: transparent; -fx-text-fill: #3182ce; " +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;");
        // Sem confirmacao: volta direto descartando
        btnVoltar.setOnAction(e -> { presencas.clear(); ir(Tela.PREVIEW); });

        lblTituloForm = new Label("Nova Chamada");
        lblTituloForm.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Region spF = new Region(); HBox.setHgrow(spF, Priority.ALWAYS);
        HBox subHeader = new HBox(12, btnVoltar, lblTituloForm, spF);
        subHeader.setAlignment(Pos.CENTER_LEFT);
        subHeader.setPadding(new Insets(12, 20, 8, 20));
        subHeader.setStyle("-fx-background-color: #f7fafc; -fx-border-color: #e2e8f0; " +
                "-fx-border-width: 0 0 1 0;");

        cbTurma = new ComboBox<>();
        cbTurma.setPromptText("Turma..."); cbTurma.setPrefWidth(240);
        cbTurma.setCellFactory(lv -> celulaTurma()); cbTurma.setButtonCell(celulaTurma());

        dpData = new DatePicker(LocalDate.now());
        dpData.setPrefWidth(150);

        Button btnCarregar = new Button("Carregar Alunos");
        btnCarregar.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnCarregar.setOnAction(e -> carregarAlunos(cbTurma.getValue(), dpData.getValue()));

        lblCronInfo = new Label();
        lblCronInfo.setStyle("-fx-padding: 6 12; -fx-background-radius: 6; -fx-font-size: 12px;");
        lblCronInfo.setVisible(false); lblCronInfo.setManaged(false);

        HBox barSeletor = new HBox(12, cbTurma, dpData, btnCarregar, lblCronInfo);
        barSeletor.setAlignment(Pos.CENTER_LEFT);
        barSeletor.setPadding(new Insets(10, 20, 10, 20));
        barSeletor.setStyle("-fx-background-color: #f7fafc; -fx-border-color: #e2e8f0; " +
                "-fx-border-width: 0 0 1 0;");

        tabelaPresenca = new TableView<>();
        tabelaPresenca.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabelaPresenca.setEditable(true);
        tabelaPresenca.setItems(presencas);
        tabelaPresenca.setPlaceholder(new Label("Selecione turma e data e clique em Carregar Alunos."));
        VBox.setVgrow(tabelaPresenca, Priority.ALWAYS);

        TableColumn<ChamadaPresenca, String> colNome = new TableColumn<>("Aluno");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAlunoNome()));

        TableColumn<ChamadaPresenca, Boolean> colPres = new TableColumn<>("Presente");
        colPres.setCellValueFactory(c -> c.getValue().presenteProperty());
        colPres.setCellFactory(CheckBoxTableCell.forTableColumn(colPres));
        colPres.setEditable(true); colPres.setMaxWidth(110); colPres.setMinWidth(110);
        tabelaPresenca.getColumns().addAll(colNome, colPres);

        Button btnTodos  = atalhoBtn("Todos presentes");
        Button btnNenhum = atalhoBtn("Todos ausentes");
        btnTodos.setOnAction(e  -> presencas.forEach(p -> p.setPresente(true)));
        btnNenhum.setOnAction(e -> presencas.forEach(p -> p.setPresente(false)));

        lblResumo = new Label();
        lblResumo.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 13px;");
        presencas.addListener(
                (javafx.collections.ListChangeListener<ChamadaPresenca>) c -> atualizarResumo());

        Region spAtalho = new Region(); HBox.setHgrow(spAtalho, Priority.ALWAYS);
        HBox atalhos = new HBox(8, btnTodos, btnNenhum, spAtalho, lblResumo);
        atalhos.setPadding(new Insets(8, 20, 8, 20));
        atalhos.setAlignment(Pos.CENTER_LEFT);

        btnSalvar = new Button("Salvar Chamada");
        btnSalvar.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 10 20; -fx-font-weight: bold;");
        btnSalvar.setDisable(true);
        btnSalvar.setOnAction(e -> salvar());

        HBox rodape = new HBox(btnSalvar);
        rodape.setPadding(new Insets(12, 20, 16, 20));
        rodape.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

        VBox corpoForm = new VBox(atalhos, tabelaPresenca, rodape);
        VBox.setVgrow(tabelaPresenca, Priority.ALWAYS);

        painelForm.getChildren().addAll(subHeader, barSeletor, corpoForm);
        VBox.setVgrow(corpoForm, Priority.ALWAYS);
    }

    // ── HISTORICO ─────────────────────────────────────────────────────────────

    private void construirPainelHistorico() {
        painelHistorico = new VBox(0);
        painelHistorico.setVisible(false);
        VBox.setVgrow(painelHistorico, Priority.ALWAYS);

        Label lblSub = new Label("Chamadas realizadas (mais recentes primeiro)");
        lblSub.setStyle("-fx-text-fill: #718096; -fx-font-size: 13px;");
        HBox subH = new HBox(lblSub);
        subH.setPadding(new Insets(12, 20, 8, 20));

        tabelaHistorico = new TableView<>();
        tabelaHistorico.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabelaHistorico.setItems(historico);
        tabelaHistorico.setPlaceholder(new Label("Nenhuma chamada registrada ainda."));
        VBox.setVgrow(tabelaHistorico, Priority.ALWAYS);

        TableColumn<Chamada, String> cData = new TableColumn<>("Data");
        cData.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDataAula().format(FMT_BR)));
        cData.setMaxWidth(110);

        TableColumn<Chamada, String> cTurma = new TableColumn<>("Turma");
        cTurma.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTurmaNome()));

        TableColumn<Chamada, String> cHor = new TableColumn<>("Horario");
        cHor.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getHorarioInicio() + " – " + cd.getValue().getHorarioFim()));
        cHor.setMaxWidth(130);

        TableColumn<Chamada, String> cPres = new TableColumn<>("Presenca");
        cPres.setCellValueFactory(cd -> {
            Chamada c = cd.getValue();
            return new SimpleStringProperty(
                    c.getTotalPresentes() + "/" + c.getTotalAlunos() + "  (" +
                            (c.getTotalAlunos() > 0
                                    ? String.format("%.0f%%", (double)c.getTotalPresentes()/c.getTotalAlunos()*100)
                                    : "—") + ")");
        });
        cPres.setMaxWidth(140);

        TableColumn<Chamada, Void> cAcoes = new TableColumn<>("Acoes");
        cAcoes.setMaxWidth(90); cAcoes.setMinWidth(90);
        cAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnDel = new Button("Apagar");
            {
                btnDel.setStyle("-fx-background-color: transparent; -fx-text-fill: #e53e3e; " +
                        "-fx-font-size: 11px; -fx-cursor: hand;");
                // Sem confirmacao: apaga direto com feedback via toast
                btnDel.setOnAction(e -> {
                    Chamada c = getTableView().getItems().get(getIndex());
                    excluirChamada(c);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btnDel);
            }
        });

        tabelaHistorico.getColumns().addAll(cData, cTurma, cHor, cPres, cAcoes);
        painelHistorico.getChildren().addAll(subH, tabelaHistorico);
    }

    // =========================================================================
    // MAQUINA DE ESTADOS
    // =========================================================================

    private void ir(Tela tela) {
        telaAtual = tela;
        painelPreview.setVisible(false);
        painelForm.setVisible(false);
        painelHistorico.setVisible(false);

        ativarTab(tabBtnPreview,   tela == Tela.PREVIEW);
        ativarTab(tabBtnHistorico, tela == Tela.HISTORICO);

        switch (tela) {
            case PREVIEW   -> { painelPreview.setVisible(true);   carregarPreview(); }
            case FORM      -> painelForm.setVisible(true);
            case HISTORICO -> { painelHistorico.setVisible(true); carregarHistorico(); }
        }
    }

    private void recarregarTelaAtual() { ir(telaAtual); }

    // =========================================================================
    // PREVIEW
    // =========================================================================

    private void carregarPreview() {
        String profId = mainApp.getSessao().getId();

        LocalDate hoje  = LocalDate.now();
        LocalTime agora = LocalTime.now();
        String diaSem   = diaSemanaPortugues(hoje.getDayOfWeek());

        slotAtual = cronogramaDAO.listarAtivosParaDia(profId, diaSem, hoje).stream()
                .filter(s -> {
                    try {
                        LocalTime ini = LocalTime.parse(s.getHorarioInicio());
                        LocalTime fim = LocalTime.parse(s.getHorarioFim());
                        return !agora.isBefore(ini) && agora.isBefore(fim);
                    } catch (Exception ex) { return false; }
                })
                .findFirst().orElse(null);

        boolean aulaAtiva = slotAtual != null
                && !chamadaDAO.chamadaJaExiste(profId, slotAtual.getTurmaId(), hoje);

        if (aulaAtiva) {
            lblAulaAtiva.setText("Aula em andamento: " + slotAtual.getTurmaNome()
                    + "   |   " + slotAtual.getHorarioFormatado()
                    + "   |   " + slotAtual.getTipoLabel());
            bannerAulaAtiva.setVisible(true); bannerAulaAtiva.setManaged(true);
        } else {
            bannerAulaAtiva.setVisible(false); bannerAulaAtiva.setManaged(false);
        }

        VBox areaCards = (VBox) painelPreview.getUserData();
        areaCards.getChildren().clear();

        List<ResumoTurma> resumos = chamadaDAO.resumoPorTurma(profId);

        if (!aulaAtiva) {
            Label lblSub = new Label(resumos.isEmpty()
                    ? "Nenhuma turma atribuida."
                    : "Visao geral das turmas:");
            lblSub.setStyle("-fx-text-fill: #718096; -fx-font-size: 13px;");
            areaCards.getChildren().add(lblSub);
        }

        FlowPane grade = new FlowPane(16, 12);
        grade.setPrefWrapLength(900);
        for (ResumoTurma r : resumos) grade.getChildren().add(criarCard(r));
        areaCards.getChildren().add(grade);
    }

    private VBox criarCard(ResumoTurma r) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 2);");

        Label lNome = new Label(r.getTurmaNome());
        lNome.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1a202c;");
        lNome.setWrapText(true);

        Label lEscola = new Label(r.getEscolaNome());
        lEscola.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");

        Separator sep = new Separator();

        Label lChamadas = new Label(r.getTotalChamadas() + " chamadas registradas");
        lChamadas.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a5568;");

        Label lUltima = new Label("Ultima: " + r.getUltimaChamadaFormatada());
        lUltima.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a5568;");

        double pct = r.getMediaPresenca();
        Label lPct = new Label("Presenca media: " + r.getMediaPresencaFormatada());
        lPct.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a5568;");

        ProgressBar bar = new ProgressBar(pct / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-accent: " + (pct >= 75 ? "#38a169" : pct >= 50 ? "#d69e2e" : "#e53e3e") + ";");

        Button btnChamar = new Button("Iniciar Chamada");
        btnChamar.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-padding: 6 12; -fx-font-size: 12px;");
        btnChamar.setMaxWidth(Double.MAX_VALUE);
        btnChamar.setOnAction(e -> {
            Turma turma = encontrarTurmaObj(r.getTurmaId());
            abrirFormManual(turma, LocalDate.now());
        });

        card.getChildren().addAll(lNome, lEscola, sep, lChamadas, lUltima, lPct, bar, btnChamar);
        return card;
    }

    // =========================================================================
    // FORM
    // =========================================================================

    private void abrirFormManual(Turma turma, LocalDate data) {
        String profId = mainApp.getSessao().getId();
        cbTurma.getItems().setAll(turmaDAO.listarPorProfessor(profId));
        presencas.clear();
        btnSalvar.setDisable(true);
        lblCronInfo.setVisible(false); lblCronInfo.setManaged(false);
        slotAtual = null;

        if (turma != null) {
            cbTurma.getItems().stream()
                    .filter(t -> t.getId().equals(turma.getId()))
                    .findFirst().ifPresent(cbTurma::setValue);
        } else {
            cbTurma.getSelectionModel().clearSelection();
        }
        dpData.setValue(data != null ? data : LocalDate.now());
        lblTituloForm.setText("Nova Chamada");
        ir(Tela.FORM);

        if (turma != null) carregarAlunos(cbTurma.getValue(), dpData.getValue());
    }

    private void abrirFormComSlot(Turma turma, LocalDate data, CronogramaAula slot) {
        slotAtual = slot;
        abrirFormManual(turma, data);
    }

    private void carregarAlunos(Turma turma, LocalDate data) {
        presencas.clear();
        btnSalvar.setDisable(true);
        lblCronInfo.setVisible(false); lblCronInfo.setManaged(false);

        if (turma == null || data == null) {
            mainApp.mostrarAviso("Selecione turma e data.", true); return;
        }

        String profId = mainApp.getSessao().getId();

        if (chamadaDAO.chamadaJaExiste(profId, turma.getId(), data)) {
            mainApp.mostrarAviso("Chamada ja registrada para " + turma.getNome()
                    + " em " + data.format(FMT_BR) + ". Use o Historico para gerenciar.", true);
            return;
        }

        String dia = diaSemanaPortugues(data.getDayOfWeek());
        slotAtual = cronogramaDAO.listarAtivosParaDia(profId, dia, data).stream()
                .filter(s -> s.getTurmaId().equals(turma.getId()))
                .findFirst().orElse(slotAtual);

        if (slotAtual != null) {
            lblCronInfo.setText(slotAtual.getTipoLabel() + "  " + slotAtual.getHorarioFormatado());
            lblCronInfo.setStyle("-fx-text-fill: #22543d; -fx-background-color: #c6f6d5; " +
                    "-fx-padding: 6 12; -fx-background-radius: 6; -fx-font-size: 12px;");
        } else {
            lblCronInfo.setText("Sem aula prevista para " + dia + " — chamada livre.");
            lblCronInfo.setStyle("-fx-text-fill: #744210; -fx-background-color: #fefcbf; " +
                    "-fx-padding: 6 12; -fx-background-radius: 6; -fx-font-size: 12px;");
        }
        lblCronInfo.setVisible(true); lblCronInfo.setManaged(true);

        List<Aluno> alunos = alunoDAO.listarPorTurma(turma.getId());
        if (alunos.isEmpty()) {
            mainApp.mostrarAviso("Nenhum aluno cadastrado nesta turma.", true); return;
        }

        presencas.setAll(alunos.stream()
                .map(a -> new ChamadaPresenca(null, null, a.getId(), a.getNome(), true))
                .toList());
        presencas.forEach(p -> p.presenteProperty().addListener((o, ov, nv) -> atualizarResumo()));
        atualizarResumo();
        btnSalvar.setDisable(false);
    }

    private void atualizarResumo() {
        long pres  = presencas.stream().filter(ChamadaPresenca::isPresente).count();
        long total = presencas.size();
        lblResumo.setText("Presentes: " + pres + " / " + total
                + "   Ausentes: " + (total - pres));
    }

    private void salvar() {
        Turma turma = cbTurma.getValue();
        LocalDate data = dpData.getValue();
        if (turma == null || data == null || presencas.isEmpty()) return;

        String ini = slotAtual != null ? slotAtual.getHorarioInicio() : "08:00";
        String fim = slotAtual != null ? slotAtual.getHorarioFim()    : "09:00";

        String id = chamadaDAO.abrirChamada(
                mainApp.getSessao().getId(), turma.getId(),
                slotAtual != null ? slotAtual.getId() : null,
                data, ini, fim, new ArrayList<>(presencas));

        if (id != null) {
            long pres = presencas.stream().filter(ChamadaPresenca::isPresente).count();
            mainApp.mostrarAviso("Chamada salva! " + pres + "/" + presencas.size() + " presentes.", false);
            presencas.clear();
            ir(Tela.PREVIEW);
        } else {
            mainApp.mostrarAviso("Erro ao salvar chamada.", true);
        }
    }

    // =========================================================================
    // HISTORICO
    // =========================================================================

    private void carregarHistorico() {
        historico.setAll(chamadaDAO.listarPorProfessor(mainApp.getSessao().getId()));
    }

    private void excluirChamada(Chamada c) {
        if (chamadaDAO.excluirChamada(c.getId())) {
            mainApp.mostrarAviso("Chamada apagada.", false);
            carregarHistorico();
        } else {
            mainApp.mostrarAviso("Erro ao apagar chamada.", true);
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Turma encontrarTurmaObj(String turmaId) {
        return turmaDAO.listarPorProfessor(mainApp.getSessao().getId()).stream()
                .filter(t -> t.getId().equals(turmaId)).findFirst().orElse(null);
    }

    private void ativarTab(Button btn, boolean ativo) {
        btn.setStyle(ativo
                ? "-fx-background-color: #3182ce; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 13px;"
                : "-fx-background-color: #edf2f7; -fx-text-fill: #4a5568; " +
                "-fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 13px;");
    }

    private Button tabBtn(String texto) {
        Button b = new Button(texto);
        b.setCursor(javafx.scene.Cursor.HAND);
        return b;
    }

    private Button atalhoBtn(String texto) {
        Button b = new Button(texto);
        b.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 6; -fx-padding: 5 12;");
        return b;
    }

    private ListCell<Turma> celulaTurma() {
        return new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "Turma..." : t.getNome());
            }
        };
    }

    private String diaSemanaPortugues(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY    -> "SEGUNDA"; case TUESDAY   -> "TERCA";
            case WEDNESDAY -> "QUARTA";  case THURSDAY  -> "QUINTA";
            case FRIDAY    -> "SEXTA";   case SATURDAY  -> "SABADO";
            default        -> "DOMINGO";
        };
    }

    public BorderPane getView() { return view; }
}