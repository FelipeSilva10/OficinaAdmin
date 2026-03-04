package app;

import core.Aluno;
import core.ChamadaPresenca;
import core.CronogramaAula;
import core.Turma;
import dao.AlunoDAO;
import dao.ChamadaDAO;
import dao.CronogramaDAO;
import dao.TurmaDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChamadaView {

    private BorderPane view;
    private MainFX mainApp;
    private ChamadaDAO chamadaDAO;
    private CronogramaDAO cronogramaDAO;
    private TurmaDAO turmaDAO;
    private AlunoDAO alunoDAO;

    // Seleção de contexto
    private ComboBox<Turma>          cbTurma;
    private DatePicker               dpData;
    private Label                    lblCronogramaInfo;
    private CronogramaAula           slotAtual;

    // Tabela de presença
    private TableView<ChamadaPresenca> tabelaPresenca;
    private ObservableList<ChamadaPresenca> presencas = FXCollections.observableArrayList();

    // Rodapé
    private Label lblResumo;
    private Button btnSalvarChamada;

    public ChamadaView(MainFX mainApp) {
        this.mainApp        = mainApp;
        this.chamadaDAO     = new ChamadaDAO();
        this.cronogramaDAO  = new CronogramaDAO();
        this.turmaDAO       = new TurmaDAO();
        this.alunoDAO       = new AlunoDAO();
        construirInterface();
        carregarTurmas();
    }

    private void construirInterface() {
        view = new BorderPane();

        // ── Cabeçalho ─────────────────────────────────────────────────────────
        Label lblTitulo = new Label("Chamada");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        HBox header = new HBox(lblTitulo);
        header.setPadding(new Insets(20, 20, 12, 20));
        header.setAlignment(Pos.CENTER_LEFT);

        // ── Painel de seleção (turma + data) ──────────────────────────────────
        cbTurma = new ComboBox<>();
        cbTurma.setPromptText("1. Selecione a turma...");
        cbTurma.setPrefWidth(240);
        cbTurma.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getNome());
            }
        });
        cbTurma.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "1. Selecione a turma..." : t.getNome());
            }
        });

        dpData = new DatePicker(LocalDate.now());
        dpData.setPromptText("2. Data da aula");
        dpData.setPrefWidth(160);

        lblCronogramaInfo = new Label();
        lblCronogramaInfo.setStyle("-fx-text-fill: #2b6cb0; -fx-font-size: 13px; -fx-background-color: #ebf8ff; -fx-padding: 6 12; -fx-background-radius: 6;");
        lblCronogramaInfo.setVisible(false);
        lblCronogramaInfo.setManaged(false);

        Button btnCarregar = new Button("Carregar Alunos");
        btnCarregar.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnCarregar.setOnAction(e -> carregarAlunos());

        HBox selecao = new HBox(12, cbTurma, dpData, btnCarregar, lblCronogramaInfo);
        selecao.setAlignment(Pos.CENTER_LEFT);
        selecao.setPadding(new Insets(0, 20, 12, 20));

        // ── Tabela de presença ────────────────────────────────────────────────
        tabelaPresenca = new TableView<>();
        tabelaPresenca.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelaPresenca.setPlaceholder(new Label("Selecione uma turma e data para carregar a chamada."));
        tabelaPresenca.setEditable(true);
        tabelaPresenca.setItems(presencas);
        VBox.setVgrow(tabelaPresenca, Priority.ALWAYS);

        TableColumn<ChamadaPresenca, String> colNome = new TableColumn<>("Aluno");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAlunoNome()));
        colNome.setSortable(true);

        TableColumn<ChamadaPresenca, Boolean> colPresente = new TableColumn<>("Presente");
        colPresente.setCellValueFactory(c -> c.getValue().presenteProperty());
        colPresente.setCellFactory(CheckBoxTableCell.forTableColumn(colPresente));
        colPresente.setEditable(true);
        colPresente.setMaxWidth(100);
        colPresente.setMinWidth(100);

        tabelaPresenca.getColumns().addAll(colNome, colPresente);

        // Atalho: marcar todos / desmarcar todos
        Button btnTodos = new Button("✔ Marcar Todos");
        btnTodos.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 6; -fx-padding: 6 12;");
        btnTodos.setOnAction(e -> presencas.forEach(p -> p.setPresente(true)));

        Button btnNenhum = new Button("✖ Desmarcar Todos");
        btnNenhum.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 6; -fx-padding: 6 12;");
        btnNenhum.setOnAction(e -> presencas.forEach(p -> p.setPresente(false)));

        HBox atalhos = new HBox(8, btnTodos, btnNenhum);
        atalhos.setPadding(new Insets(0, 20, 8, 20));
        atalhos.setAlignment(Pos.CENTER_LEFT);
        atalhos.setVisible(false);
        atalhos.setManaged(false);

        // Atualiza visibilidade dos atalhos quando há alunos
        presencas.addListener((javafx.collections.ListChangeListener<ChamadaPresenca>) c -> {
            boolean temAlunos = !presencas.isEmpty();
            atalhos.setVisible(temAlunos);
            atalhos.setManaged(temAlunos);
        });

        // ── Rodapé ────────────────────────────────────────────────────────────
        lblResumo = new Label();
        lblResumo.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 13px;");

        btnSalvarChamada = new Button("Salvar Chamada");
        btnSalvarChamada.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20; -fx-font-weight: bold;");
        btnSalvarChamada.setDisable(true);
        btnSalvarChamada.setOnAction(e -> salvarChamada());

        Region spacerRodape = new Region();
        HBox.setHgrow(spacerRodape, Priority.ALWAYS);

        HBox rodape = new HBox(12, lblResumo, spacerRodape, btnSalvarChamada);
        rodape.setAlignment(Pos.CENTER_LEFT);
        rodape.setPadding(new Insets(12, 20, 16, 20));
        rodape.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

        // ── Montagem ──────────────────────────────────────────────────────────
        VBox centro = new VBox(header, selecao, atalhos, tabelaPresenca, rodape);
        VBox.setVgrow(tabelaPresenca, Priority.ALWAYS);
        view.setCenter(centro);
    }

    private void carregarTurmas() {
        cbTurma.getItems().setAll(
                turmaDAO.listarPorProfessor(mainApp.getSessao().getId())
        );
    }

    private void carregarAlunos() {
        Turma turma = cbTurma.getValue();
        LocalDate data = dpData.getValue();

        if (turma == null || data == null) {
            mainApp.mostrarAviso("Selecione a turma e a data da aula.", true);
            return;
        }

        // Verifica se já existe chamada para esta turma/data
        if (chamadaDAO.chamadaJaExiste(mainApp.getSessao().getId(), turma.getId(), data)) {
            mainApp.mostrarAviso("Chamada já registrada para " + turma.getNome()
                    + " em " + data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".", true);
            presencas.clear();
            btnSalvarChamada.setDisable(true);
            lblResumo.setText("");
            lblCronogramaInfo.setVisible(false);
            lblCronogramaInfo.setManaged(false);
            return;
        }

        // Busca slot do cronograma para este dia
        String diaSemana = diaSemanaPortugues(data.getDayOfWeek());
        List<CronogramaAula> slots = cronogramaDAO.listarPorProfessorEDia(
                mainApp.getSessao().getId(), diaSemana);

        slotAtual = slots.stream()
                .filter(s -> s.getTurmaId().equals(turma.getId()))
                .findFirst()
                .orElse(null);

        if (slotAtual != null) {
            lblCronogramaInfo.setText("📅 Aula de " + slotAtual.getHorarioFormatado()
                    + " conforme cronograma");
            lblCronogramaInfo.setVisible(true);
            lblCronogramaInfo.setManaged(true);
        } else {
            lblCronogramaInfo.setText("⚠ Esta turma não tem aula prevista em " + diaSemana
                    + ". A chamada será registrada assim mesmo.");
            lblCronogramaInfo.setVisible(true);
            lblCronogramaInfo.setManaged(true);
        }

        // Carrega alunos da turma
        List<Aluno> alunos = alunoDAO.listarPorTurma(turma.getId());
        if (alunos.isEmpty()) {
            mainApp.mostrarAviso("Nenhum aluno matriculado nesta turma.", true);
            presencas.clear();
            btnSalvarChamada.setDisable(true);
            return;
        }

        presencas.setAll(alunos.stream()
                .map(a -> new ChamadaPresenca(null, null, a.getId(), a.getNome(), true))
                .toList());

        atualizarResumo();
        btnSalvarChamada.setDisable(false);

        // Atualiza resumo ao mudar checkboxes
        presencas.forEach(p -> p.presenteProperty().addListener((obs, ov, nv) -> atualizarResumo()));
    }

    private void atualizarResumo() {
        long presentes = presencas.stream().filter(ChamadaPresenca::isPresente).count();
        long total     = presencas.size();
        lblResumo.setText("Presentes: " + presentes + " / " + total
                + "   Ausentes: " + (total - presentes));
    }

    private void salvarChamada() {
        Turma turma = cbTurma.getValue();
        LocalDate data = dpData.getValue();
        if (turma == null || data == null || presencas.isEmpty()) return;

        // Determina horário: do cronograma se existir, senão pede confirmação
        String inicio, fim;
        if (slotAtual != null) {
            inicio = slotAtual.getHorarioInicio();
            fim    = slotAtual.getHorarioFim();
        } else {
            // Aula fora do cronograma: usa horário genérico (pode ser melhorado)
            inicio = "08:00";
            fim    = "09:00";
        }

        String chamadaId = chamadaDAO.abrirChamada(
                mainApp.getSessao().getId(),
                turma.getId(),
                slotAtual != null ? slotAtual.getId() : null,
                data, inicio, fim,
                new ArrayList<>(presencas)
        );

        if (chamadaId != null) {
            long presentes = presencas.stream().filter(ChamadaPresenca::isPresente).count();
            mainApp.mostrarAviso(
                    "Chamada salva! " + presentes + "/" + presencas.size() + " presentes.", false);
            presencas.clear();
            btnSalvarChamada.setDisable(true);
            lblResumo.setText("");
            lblCronogramaInfo.setVisible(false);
            lblCronogramaInfo.setManaged(false);
            dpData.setValue(LocalDate.now());
        } else {
            mainApp.mostrarAviso("Erro ao salvar a chamada.", true);
        }
    }

    /** Converte DayOfWeek para o texto usado no banco. */
    private String diaSemanaPortugues(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY    -> "SEGUNDA";
            case TUESDAY   -> "TERÇA";
            case WEDNESDAY -> "QUARTA";
            case THURSDAY  -> "QUINTA";
            case FRIDAY    -> "SEXTA";
            case SATURDAY  -> "SÁBADO";
            default        -> "DOMINGO";
        };
    }

    public BorderPane getView() { return view; }
}