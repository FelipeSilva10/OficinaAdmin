package app;

import core.CronogramaAula;
import core.Turma;
import dao.CronogramaDAO;
import dao.TurmaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

import java.util.List;

public class CronogramaView {

    private static final String[] DIAS = {
            "SEGUNDA","TERÇA","QUARTA","QUINTA","SEXTA","SÁBADO"
    };

    private BorderPane view;
    private MainFX mainApp;
    private CronogramaDAO cronogramaDAO;
    private TurmaDAO turmaDAO;

    private TableView<CronogramaAula> tabela;
    private ObservableList<CronogramaAula> dados = FXCollections.observableArrayList();

    // Painel de formulário
    private VBox painelForm;
    private Label lblFormTitulo;
    private ComboBox<Turma>   cbTurma;
    private ComboBox<String>  cbDia;
    private TextField         txtInicio, txtFim;
    private Button            btnSalvar;
    private CronogramaAula    slotEditando;

    // Grid semanal (visual)
    private GridPane gridSemanal;

    public CronogramaView(MainFX mainApp) {
        this.mainApp       = mainApp;
        this.cronogramaDAO = new CronogramaDAO();
        this.turmaDAO      = new TurmaDAO();
        construirInterface();
        carregar();
    }

    private void construirInterface() {
        view = new BorderPane();

        // ── Cabeçalho ─────────────────────────────────────────────────────────
        Label lblTitulo = new Label("Cronograma Semanal");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregar());

        Button btnNovo = new Button("+ Adicionar Aula");
        btnNovo.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNovo.setOnAction(e -> abrirFormNovo());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, lblTitulo, spacer, btnAtualizar, btnNovo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 12, 20));

        // ── Grid Semanal ──────────────────────────────────────────────────────
        gridSemanal = new GridPane();
        gridSemanal.setHgap(8);
        gridSemanal.setVgap(0);
        gridSemanal.setPadding(new Insets(0, 20, 12, 20));

        for (int i = 0; i < DIAS.length; i++) {
            Label lblDia = new Label(DIAS[i]);
            lblDia.setMaxWidth(Double.MAX_VALUE);
            lblDia.setAlignment(Pos.CENTER);
            lblDia.setStyle("-fx-background-color: #2d3748; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 4; -fx-background-radius: 6 6 0 0;");
            gridSemanal.add(lblDia, i, 0);
            GridPane.setHgrow(lblDia, Priority.ALWAYS);
        }

        // ── Tabela ────────────────────────────────────────────────────────────
        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhuma aula no cronograma. Clique em '+ Adicionar Aula'."));
        tabela.setItems(dados);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<CronogramaAula, String> colDia = new TableColumn<>("Dia");
        colDia.setCellValueFactory(new PropertyValueFactory<>("diaSemana"));
        colDia.setMaxWidth(100);

        TableColumn<CronogramaAula, String> colTurma = new TableColumn<>("Turma");
        colTurma.setCellValueFactory(new PropertyValueFactory<>("turmaNome"));

        TableColumn<CronogramaAula, String> colInicio = new TableColumn<>("Início");
        colInicio.setCellValueFactory(new PropertyValueFactory<>("horarioInicio"));
        colInicio.setMaxWidth(90);

        TableColumn<CronogramaAula, String> colFim = new TableColumn<>("Fim");
        colFim.setCellValueFactory(new PropertyValueFactory<>("horarioFim"));
        colFim.setMaxWidth(90);

        tabela.getColumns().addAll(colDia, colTurma, colInicio, colFim);

        tabela.setRowFactory(tv -> {
            TableRow<CronogramaAula> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem miEditar = new MenuItem("Editar");
            MenuItem miExcluir = new MenuItem("Excluir");
            miExcluir.setStyle("-fx-text-fill: red;");
            miEditar.setOnAction(e -> { if (row.getItem() != null) abrirFormEditar(row.getItem()); });
            miExcluir.setOnAction(e -> excluir(row.getItem()));
            cm.getItems().addAll(miEditar, miExcluir);
            row.emptyProperty().addListener((obs, w, n) -> row.setContextMenu(n ? null : cm));
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) abrirFormEditar(row.getItem());
            });
            return row;
        });

        // ── Painel de formulário ───────────────────────────────────────────────
        painelForm = new VBox(12);
        painelForm.setPadding(new Insets(24));
        painelForm.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
        painelForm.setMinWidth(280);
        painelForm.setMaxWidth(320);
        painelForm.setVisible(false);
        painelForm.setManaged(false);

        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-cursor: hand;");
        btnFechar.setOnAction(e -> fecharForm());
        HBox hdrForm = new HBox(btnFechar);
        hdrForm.setAlignment(Pos.TOP_RIGHT);

        lblFormTitulo = new Label("Nova Aula");
        lblFormTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        cbTurma = new ComboBox<>();
        cbTurma.setPromptText("Selecione a turma...");
        cbTurma.setMaxWidth(Double.MAX_VALUE);
        cbTurma.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getNome());
            }
        });
        cbTurma.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "Selecione a turma..." : t.getNome());
            }
        });

        cbDia = new ComboBox<>(FXCollections.observableArrayList(DIAS));
        cbDia.setPromptText("Dia da semana...");
        cbDia.setMaxWidth(Double.MAX_VALUE);

        txtInicio = new TextField();
        txtInicio.setPromptText("Início: 08:00");

        txtFim = new TextField();
        txtFim.setPromptText("Fim: 09:30");

        Label lblDica = new Label("Formato de hora: HH:mm\nEx: 08:00 e 09:30");
        lblDica.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 11px;");
        lblDica.setTextAlignment(TextAlignment.LEFT);

        btnSalvar = new Button("Salvar");
        btnSalvar.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> salvar());

        painelForm.getChildren().addAll(
                hdrForm, lblFormTitulo, new Separator(),
                new Label("Turma:"), cbTurma,
                new Label("Dia da Semana:"), cbDia,
                new Label("Horário de Início:"), txtInicio,
                new Label("Horário de Fim:"), txtFim,
                lblDica, btnSalvar
        );

        // ── Layout ────────────────────────────────────────────────────────────
        VBox centro = new VBox(header, gridSemanal, tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        HBox mainLayout = new HBox(centro, painelForm);
        HBox.setHgrow(centro, Priority.ALWAYS);
        view.setCenter(mainLayout);
    }

    private void carregar() {
        String profId = mainApp.getSessao().getId();
        List<CronogramaAula> slots = cronogramaDAO.listarPorProfessor(profId);
        dados.setAll(slots);
        atualizarGrid(slots);
    }

    private void atualizarGrid(List<CronogramaAula> slots) {
        // Remove células antigas (linha 1 em diante)
        gridSemanal.getChildren().removeIf(n -> GridPane.getRowIndex(n) != null && GridPane.getRowIndex(n) > 0);

        int[] linhas = new int[DIAS.length];

        for (CronogramaAula slot : slots) {
            int col = diaParaColuna(slot.getDiaSemana());
            if (col < 0) continue;
            int row = ++linhas[col];

            VBox card = new VBox(3);
            card.setPadding(new Insets(6));
            card.setStyle("-fx-background-color: #ebf8ff; -fx-border-color: #bee3f8; -fx-border-radius: 4; -fx-background-radius: 4;");
            card.setMaxWidth(Double.MAX_VALUE);

            Label lblNome = new Label(slot.getTurmaNome());
            lblNome.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #2b6cb0;");
            lblNome.setWrapText(true);

            Label lblHora = new Label(slot.getHorarioFormatado());
            lblHora.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a5568;");

            card.getChildren().addAll(lblNome, lblHora);
            gridSemanal.add(card, col, row);
        }

        // Linha vazia se coluna vazia
        for (int col = 0; col < DIAS.length; col++) {
            if (linhas[col] == 0) {
                Label vazia = new Label("—");
                vazia.setStyle("-fx-text-fill: #cbd5e0; -fx-font-size: 12px; -fx-padding: 8 4;");
                vazia.setMaxWidth(Double.MAX_VALUE);
                vazia.setAlignment(Pos.CENTER);
                gridSemanal.add(vazia, col, 1);
            }
        }
    }

    private int diaParaColuna(String dia) {
        for (int i = 0; i < DIAS.length; i++) if (DIAS[i].equals(dia)) return i;
        return -1;
    }

    private void abrirFormNovo() {
        slotEditando = null;
        lblFormTitulo.setText("Nova Aula no Cronograma");
        btnSalvar.setText("Adicionar");
        cbTurma.getItems().setAll(turmaDAO.listarPorProfessor(mainApp.getSessao().getId()));
        cbTurma.getSelectionModel().clearSelection();
        cbDia.getSelectionModel().clearSelection();
        txtInicio.clear(); txtFim.clear();
        mostrarForm();
    }

    private void abrirFormEditar(CronogramaAula slot) {
        slotEditando = slot;
        lblFormTitulo.setText("Editar Aula");
        btnSalvar.setText("Salvar Alterações");
        cbTurma.getItems().setAll(turmaDAO.listarPorProfessor(mainApp.getSessao().getId()));
        // Pré-seleciona a turma
        cbTurma.getItems().stream()
                .filter(t -> t.getId().equals(slot.getTurmaId()))
                .findFirst()
                .ifPresent(cbTurma::setValue);
        cbTurma.setDisable(true); // turma não pode mudar na edição
        cbDia.setValue(slot.getDiaSemana());
        txtInicio.setText(slot.getHorarioInicio());
        txtFim.setText(slot.getHorarioFim());
        mostrarForm();
    }

    private void salvar() {
        Turma turma    = cbTurma.getValue();
        String dia     = cbDia.getValue();
        String inicio  = txtInicio.getText().trim();
        String fim     = txtFim.getText().trim();

        if ((turma == null && slotEditando == null) || dia == null
                || inicio.isBlank() || fim.isBlank()) {
            mainApp.mostrarAviso("Preencha todos os campos.", true);
            return;
        }

        if (!inicio.matches("\\d{2}:\\d{2}") || !fim.matches("\\d{2}:\\d{2}")) {
            mainApp.mostrarAviso("Horário inválido. Use o formato HH:mm (ex: 08:00).", true);
            return;
        }

        boolean ok;
        if (slotEditando == null) {
            ok = cronogramaDAO.inserir(mainApp.getSessao().getId(),
                    turma.getId(), dia, inicio, fim);
        } else {
            ok = cronogramaDAO.atualizar(slotEditando.getId(), dia, inicio, fim);
        }

        if (ok) {
            mainApp.mostrarAviso(slotEditando == null ? "Aula adicionada ao cronograma!" : "Cronograma atualizado!", false);
            cbTurma.setDisable(false);
            fecharForm();
            carregar();
        } else {
            mainApp.mostrarAviso("Erro ao salvar. Verifique se já existe aula neste dia para esta turma.", true);
        }
    }

    private void excluir(CronogramaAula slot) {
        if (slot == null) return;
        if (cronogramaDAO.excluir(slot.getId())) {
            mainApp.mostrarAviso("Aula removida do cronograma.", false);
            carregar();
            fecharForm();
        } else {
            mainApp.mostrarAviso("Erro ao remover aula.", true);
        }
    }

    private void mostrarForm() { painelForm.setVisible(true); painelForm.setManaged(true); }
    private void fecharForm()  {
        painelForm.setVisible(false); painelForm.setManaged(false);
        cbTurma.setDisable(false);
        tabela.getSelectionModel().clearSelection();
        slotEditando = null;
    }

    public BorderPane getView() { return view; }
}