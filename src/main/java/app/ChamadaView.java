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
 * Tela de Chamada — máquina de estados explícita.
 *
 *  PREVIEW     → cards de turmas + banner aula ativa (se houver)
 *  FORM        → seletor turma/data + tabela de presença (nova chamada)
 *  HISTORICO   → lista de chamadas + painel lateral de detalhe e edição
 */
public class ChamadaView {

    private enum Tela { PREVIEW, FORM, HISTORICO }
    private Tela telaAtual = Tela.PREVIEW;

    private BorderPane view;
    private MainFX     mainApp;

    private final ChamadaDAO    chamadaDAO    = new ChamadaDAO();
    private final CronogramaDAO cronogramaDAO = new CronogramaDAO();
    private final TurmaDAO      turmaDAO      = new TurmaDAO();
    private final AlunoDAO      alunoDAO      = new AlunoDAO();

    private static final DateTimeFormatter FMT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Painéis de estado
    private VBox      painelPreview;
    private VBox      painelForm;
    private VBox      painelHistorico;
    private StackPane conteudo;

    // ── Preview ───────────────────────────────────────────────────────────────
    private VBox  bannerAulaAtiva;
    private Label lblAulaAtiva;
    private CronogramaAula slotAtual;

    // ── Form ──────────────────────────────────────────────────────────────────
    private ComboBox<Turma> cbTurma;
    private DatePicker      dpData;
    private Label           lblCronInfo;
    private TableView<ChamadaPresenca> tabelaPresenca;
    private ObservableList<ChamadaPresenca> presencas = FXCollections.observableArrayList();
    private Label  lblResumo;
    private Button btnSalvar;
    private Label  lblTituloForm;

    // ── Histórico ─────────────────────────────────────────────────────────────
    private TableView<Chamada>             tabelaHistorico;
    private ObservableList<Chamada>        historico = FXCollections.observableArrayList();

    /** Painel lateral de detalhe/edição de chamada histórica */
    private VBox   painelDetalhe;
    private Label  lblDetalheTitulo;
    private TableView<ChamadaPresenca> tabelaDetalhe;
    private ObservableList<ChamadaPresenca> presencasDetalhe = FXCollections.observableArrayList();
    private Label  lblResumoDetalhe;
    private Chamada chamadaDetalhada;

    // Tabs
    private Button tabBtnPreview, tabBtnHistorico;

    public ChamadaView(MainFX mainApp) {
        this.mainApp = mainApp;
        construirInterface();
        ir(Tela.PREVIEW);
    }

    // =========================================================================
    // CONSTRUÇÃO
    // =========================================================================

    private void construirInterface() {
        view = new BorderPane();

        Label lblTitulo = new Label("Chamada");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        tabBtnPreview   = tabBtn("Turmas");
        tabBtnHistorico = tabBtn("Histórico");
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

    // ── HISTÓRICO ─────────────────────────────────────────────────────────────

    private void construirPainelHistorico() {
        painelHistorico = new VBox(0);
        painelHistorico.setVisible(false);
        VBox.setVgrow(painelHistorico, Priority.ALWAYS);

        Label lblSub = new Label("Clique em uma chamada para ver as presenças e editar.");
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

        TableColumn<Chamada, String> cHor = new TableColumn<>("Horário");
        cHor.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getHorarioInicio() + " – " + cd.getValue().getHorarioFim()));
        cHor.setMaxWidth(130);

        TableColumn<Chamada, String> cPres = new TableColumn<>("Presença");
        cPres.setCellValueFactory(cd -> {
            Chamada c = cd.getValue();
            return new SimpleStringProperty(
                    c.getTotalPresentes() + "/" + c.getTotalAlunos() + "  (" +
                            (c.getTotalAlunos() > 0
                                    ? String.format("%.0f%%", (double)c.getTotalPresentes()/c.getTotalAlunos()*100)
                                    : "—") + ")");
        });
        cPres.setMaxWidth(140);

        TableColumn<Chamada, Void> cAcoes = new TableColumn<>("Ações");
        cAcoes.setMaxWidth(90); cAcoes.setMinWidth(90);
        cAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnDel = new Button("Apagar");
            {
                btnDel.setStyle("-fx-background-color: transparent; -fx-text-fill: #e53e3e; " +
                        "-fx-font-size: 11px; -fx-cursor: hand;");
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

        // Clicar abre painel de detalhe
        tabelaHistorico.getSelectionModel().selectedItemProperty().addListener(
                (obs, ov, nv) -> {
                    if (nv != null) abrirDetalhe(nv);
                }
        );

        // ── Painel de detalhe lateral ──────────────────────────────────────
        painelDetalhe = new VBox(0);
        painelDetalhe.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
        painelDetalhe.setMinWidth(320);
        painelDetalhe.setMaxWidth(380);
        painelDetalhe.setVisible(false);
        painelDetalhe.setManaged(false);

        // Header do painel
        lblDetalheTitulo = new Label("Detalhes da Chamada");
        lblDetalheTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");
        lblDetalheTitulo.setWrapText(true);

        Button btnFecharDet = new Button("Fechar");
        btnFecharDet.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-cursor: hand;");
        btnFecharDet.setOnAction(e -> fecharDetalhe());

        Region spDet = new Region(); HBox.setHgrow(spDet, Priority.ALWAYS);
        HBox hdrDet = new HBox(10, lblDetalheTitulo, spDet, btnFecharDet);
        hdrDet.setAlignment(Pos.CENTER_LEFT);
        hdrDet.setPadding(new Insets(14, 14, 10, 16));
        hdrDet.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        // Tabela de presenças
        tabelaDetalhe = new TableView<>();
        tabelaDetalhe.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabelaDetalhe.setEditable(true);
        tabelaDetalhe.setItems(presencasDetalhe);
        tabelaDetalhe.setPlaceholder(new Label("Nenhum aluno."));
        VBox.setVgrow(tabelaDetalhe, Priority.ALWAYS);

        TableColumn<ChamadaPresenca, String> cdNome = new TableColumn<>("Aluno");
        cdNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAlunoNome()));

        TableColumn<ChamadaPresenca, Boolean> cdPres = new TableColumn<>("Presente");
        cdPres.setCellValueFactory(c -> c.getValue().presenteProperty());
        cdPres.setCellFactory(CheckBoxTableCell.forTableColumn(cdPres));
        cdPres.setEditable(true);
        cdPres.setMaxWidth(95); cdPres.setMinWidth(95);
        tabelaDetalhe.getColumns().addAll(cdNome, cdPres);

        // Atalhos de edição
        Button btnTodosDet  = atalhoBtn("Todos ✓");
        Button btnNenhumDet = atalhoBtn("Todos ✗");
        btnTodosDet.setOnAction(e  -> presencasDetalhe.forEach(p -> p.setPresente(true)));
        btnNenhumDet.setOnAction(e -> presencasDetalhe.forEach(p -> p.setPresente(false)));

        lblResumoDetalhe = new Label();
        lblResumoDetalhe.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 12px;");
        presencasDetalhe.addListener(
                (javafx.collections.ListChangeListener<ChamadaPresenca>) c -> atualizarResumoDetalhe());

        Region spAta = new Region(); HBox.setHgrow(spAta, Priority.ALWAYS);
        HBox atalhosD = new HBox(6, btnTodosDet, btnNenhumDet, spAta, lblResumoDetalhe);
        atalhosD.setPadding(new Insets(8, 12, 6, 12));
        atalhosD.setAlignment(Pos.CENTER_LEFT);

        // Botão de salvar edições
        Button btnSalvarDet = new Button("Salvar Alterações");
        btnSalvarDet.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvarDet.setMaxWidth(Double.MAX_VALUE);
        btnSalvarDet.setOnAction(e -> salvarEdicaoPresencas());

        VBox rodapeDet = new VBox(8, btnSalvarDet);
        rodapeDet.setPadding(new Insets(10, 14, 14, 14));
        rodapeDet.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

        VBox corpoDet = new VBox(atalhosD, tabelaDetalhe, rodapeDet);
        VBox.setVgrow(tabelaDetalhe, Priority.ALWAYS);

        painelDetalhe.getChildren().addAll(hdrDet, corpoDet);
        VBox.setVgrow(corpoDet, Priority.ALWAYS);

        // Montar
        VBox listCol = new VBox(subH, tabelaHistorico);
        VBox.setVgrow(tabelaHistorico, Priority.ALWAYS);
        HBox splitH = new HBox(listCol, painelDetalhe);
        HBox.setHgrow(listCol, Priority.ALWAYS);
        VBox.setVgrow(splitH, Priority.ALWAYS);

        painelHistorico.getChildren().add(splitH);
    }

    // =========================================================================
    // MÁQUINA DE ESTADOS
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
                    ? "Nenhuma turma atribuída."
                    : "Visão geral das turmas:");
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

        Label lChamadas = new Label(r.getTotalChamadas() + " chamadas registradas");
        lChamadas.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a5568;");

        Label lUltima = new Label("Última: " + r.getUltimaChamadaFormatada());
        lUltima.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a5568;");

        double pct = r.getMediaPresenca();
        Label lPct = new Label("Presença média: " + r.getMediaPresencaFormatada());
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

        Button btnHistorico = new Button("Ver Histórico");
        btnHistorico.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #2d3748; " +
                "-fx-background-radius: 6; -fx-padding: 6 12; -fx-font-size: 12px;");
        btnHistorico.setMaxWidth(Double.MAX_VALUE);
        btnHistorico.setOnAction(e -> ir(Tela.HISTORICO));

        card.getChildren().addAll(lNome, lEscola, new Separator(),
                lChamadas, lUltima, lPct, bar, btnChamar, btnHistorico);
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
            mainApp.mostrarAviso("Chamada já registrada para " + turma.getNome()
                    + " em " + data.format(FMT_BR) + ". Use o Histórico para gerenciar.", true);
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
        lblResumo.setText("Presentes: " + pres + " / " + total + "   Ausentes: " + (total - pres));
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
    // HISTÓRICO — detalhe e edição
    // =========================================================================

    private void carregarHistorico() {
        historico.setAll(chamadaDAO.listarPorProfessor(mainApp.getSessao().getId()));
        fecharDetalhe();
    }

    private void abrirDetalhe(Chamada chamada) {
        chamadaDetalhada = chamada;

        lblDetalheTitulo.setText(
                chamada.getDataAula().format(FMT_BR) + "  —  " + chamada.getTurmaNome() +
                        "\n" + chamada.getHorarioInicio() + " – " + chamada.getHorarioFim());

        List<ChamadaPresenca> lista = chamadaDAO.listarPresencas(chamada.getId());
        presencasDetalhe.setAll(lista);
        presencasDetalhe.forEach(p -> p.presenteProperty()
                .addListener((o, ov, nv) -> atualizarResumoDetalhe()));
        atualizarResumoDetalhe();

        painelDetalhe.setVisible(true);
        painelDetalhe.setManaged(true);
    }

    private void fecharDetalhe() {
        painelDetalhe.setVisible(false);
        painelDetalhe.setManaged(false);
        presencasDetalhe.clear();
        chamadaDetalhada = null;
        tabelaHistorico.getSelectionModel().clearSelection();
    }

    private void atualizarResumoDetalhe() {
        long pres  = presencasDetalhe.stream().filter(ChamadaPresenca::isPresente).count();
        long total = presencasDetalhe.size();
        lblResumoDetalhe.setText("Presentes: " + pres + "/" + total);
    }

    private void salvarEdicaoPresencas() {
        if (chamadaDetalhada == null || presencasDetalhe.isEmpty()) return;

        boolean ok = chamadaDAO.atualizarPresencas(new ArrayList<>(presencasDetalhe));
        if (ok) {
            mainApp.mostrarAviso("Presenças atualizadas!", false);
            carregarHistorico(); // recarrega totais na tabela
        } else {
            mainApp.mostrarAviso("Erro ao atualizar presenças.", true);
        }
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