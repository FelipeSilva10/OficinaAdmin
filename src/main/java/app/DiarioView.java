package app;

import core.DiarioAula;
import core.Turma;
import dao.DiarioDAO;
import dao.TurmaDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

/**
 * Tela do Diário de Aulas — exclusiva para o professor.
 *
 * Permite registrar, para cada aula, o título da aula, o conteúdo trabalhado
 * e observações gerais sobre a turma ou sobre alunos individuais.
 *
 * Layout: tabela de entradas à esquerda | painel de edição à direita.
 */
public class DiarioView {

    private BorderPane view;
    private MainFX     mainApp;

    private final DiarioDAO diarioDAO = new DiarioDAO();
    private final TurmaDAO  turmaDAO  = new TurmaDAO();

    private final ObservableList<DiarioAula> todos = FXCollections.observableArrayList();
    private FilteredList<DiarioAula> filtrado;

    // Tabela principal
    private TableView<DiarioAula> tabela;

    // Painel de edição lateral
    private VBox       painelEditor;
    private Label      lblEditorTitulo;
    private ComboBox<Turma> cbTurma;
    private DatePicker dpData;
    private TextField  txtTitulo;
    private TextArea   txtConteudo;
    private TextArea   txtObservacoes;
    private Button     btnSalvar;

    private DiarioAula editando;

    // Filtros do cabeçalho
    private ComboBox<Turma> cbFiltroTurma;
    private TextField       txtBusca;

    public DiarioView(MainFX mainApp) {
        this.mainApp = mainApp;
        construirInterface();
        carregar();
    }

    // =========================================================================
    // CONSTRUÇÃO
    // =========================================================================

    private void construirInterface() {
        view = new BorderPane();

        // ── Cabeçalho ──────────────────────────────────────────────────────
        Label lblTitulo = new Label("Diário de Aulas");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Label lblSub = new Label("Registre o conteúdo de cada aula e suas observações.");
        lblSub.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");

        cbFiltroTurma = new ComboBox<>();
        cbFiltroTurma.setPromptText("Todas as turmas");
        cbFiltroTurma.setPrefWidth(230);
        cbFiltroTurma.getItems().add(null);
        cbFiltroTurma.getItems().addAll(turmaDAO.listarPorProfessor(mainApp.getSessao().getId()));
        cbFiltroTurma.setCellFactory(lv -> celulaTurma());
        cbFiltroTurma.setButtonCell(celulaTurmaBtn("Todas as turmas"));
        cbFiltroTurma.setOnAction(e -> aplicarFiltro());

        txtBusca = new TextField();
        txtBusca.setPromptText("Buscar por título ou conteúdo...");
        txtBusca.setPrefWidth(260);
        txtBusca.textProperty().addListener((obs, ov, nv) -> aplicarFiltroTexto(nv));

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregar());

        Button btnNova = new Button("+ Nova Entrada");
        btnNova.setStyle("-fx-background-color: #6b46c1; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNova.setOnAction(e -> abrirFormNovo());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        VBox tituloBox = new VBox(2, lblTitulo, lblSub);
        HBox header = new HBox(14, tituloBox, sp,
                new Label("Turma:"), cbFiltroTurma,
                txtBusca, btnAtualizar, btnNova);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 14, 20));
        header.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                "-fx-border-width: 0 0 1 0;");

        // ── Tabela ─────────────────────────────────────────────────────────
        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhuma entrada no diário. Clique em \"+ Nova Entrada\" para começar."));
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<DiarioAula, String> cData = new TableColumn<>("Data");
        cData.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDataFormatada()));
        cData.setMaxWidth(110);
        cData.setMinWidth(90);

        TableColumn<DiarioAula, String> cTurma = new TableColumn<>("Turma");
        cTurma.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTurmaNome()));
        cTurma.setMaxWidth(180);

        TableColumn<DiarioAula, String> cTitulo = new TableColumn<>("Título da Aula");
        cTitulo.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTitulo().isBlank() ? "—" : cd.getValue().getTitulo()));
        cTitulo.setMinWidth(140);

        TableColumn<DiarioAula, String> cPreview = new TableColumn<>("Conteúdo");
        cPreview.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getConteudoPreview()));

        tabela.getColumns().addAll(cData, cTurma, cTitulo, cPreview);

        // Colorir linhas com observações preenchidas
        tabela.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(DiarioAula item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle(""); setContextMenu(null); return;
                }
                boolean temObs = !item.getObservacoes().isBlank();
                setStyle(temObs ? "-fx-background-color: #fffaf0;" : "");

                ContextMenu cm = new ContextMenu();
                MenuItem miEdit = new MenuItem("Editar");
                MenuItem miDel  = new MenuItem("Excluir");
                miDel.setStyle("-fx-text-fill: red;");
                miEdit.setOnAction(e -> abrirFormEditar(item));
                miDel.setOnAction(e  -> excluir(item));
                cm.getItems().addAll(miEdit, miDel);
                setContextMenu(cm);
            }
        });

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (nv != null) abrirFormEditar(nv);
        });

        filtrado = new FilteredList<>(todos, a -> true);
        tabela.setItems(filtrado);

        // ── Painel editor lateral ───────────────────────────────────────────
        construirPainelEditor();

        // ── Montagem ───────────────────────────────────────────────────────
        VBox centroEsq = new VBox(tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        HBox main = new HBox(centroEsq, painelEditor);
        HBox.setHgrow(centroEsq, Priority.ALWAYS);
        VBox corpo = new VBox(header, main);
        VBox.setVgrow(main, Priority.ALWAYS);
        view.setCenter(corpo);
    }

    private void construirPainelEditor() {
        painelEditor = new VBox(0);
        painelEditor.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
        painelEditor.setMinWidth(360);
        painelEditor.setMaxWidth(420);
        painelEditor.setVisible(false);
        painelEditor.setManaged(false);

        // Header do painel
        lblEditorTitulo = new Label("Nova Entrada");
        lblEditorTitulo.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-cursor: hand;");
        btnFechar.setOnAction(e -> fecharEditor());

        Region spH = new Region();
        HBox.setHgrow(spH, Priority.ALWAYS);
        HBox hdrE = new HBox(12, lblEditorTitulo, spH, btnFechar);
        hdrE.setAlignment(Pos.CENTER_LEFT);
        hdrE.setPadding(new Insets(16, 16, 12, 20));
        hdrE.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        // Formulário
        VBox form = new VBox(10);
        form.setPadding(new Insets(16, 20, 16, 20));

        // Turma
        Label lblT = campo("Turma");
        cbTurma = new ComboBox<>();
        cbTurma.setPromptText("Selecione a turma...");
        cbTurma.setMaxWidth(Double.MAX_VALUE);
        cbTurma.getItems().addAll(turmaDAO.listarPorProfessor(mainApp.getSessao().getId()));
        cbTurma.setCellFactory(lv -> celulaTurma());
        cbTurma.setButtonCell(celulaTurmaBtn("Selecione a turma..."));

        // Data
        Label lblD = campo("Data da Aula");
        dpData = new DatePicker(LocalDate.now());
        dpData.setMaxWidth(Double.MAX_VALUE);

        // Título
        Label lblTit = campo("Título / Tema da Aula");
        txtTitulo = new TextField();
        txtTitulo.setPromptText("Ex: Introdução a variáveis e tipos de dados");
        txtTitulo.setStyle(estiloInput());

        // Conteúdo
        Label lblC = campo("Conteúdo Trabalhado");
        Label lblCHint = new Label("O que foi abordado, exercícios realizados, recursos utilizados...");
        lblCHint.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 11px;");
        lblCHint.setWrapText(true);
        txtConteudo = new TextArea();
        txtConteudo.setPromptText("Descreva o conteúdo da aula...");
        txtConteudo.setWrapText(true);
        txtConteudo.setPrefRowCount(6);
        txtConteudo.setStyle(estiloInput());

        // Observações
        Label lblO = campo("Observações");
        Label lblOHint = new Label("Comportamento da turma, dificuldades, destaques, próximas ações...");
        lblOHint.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 11px;");
        lblOHint.setWrapText(true);
        txtObservacoes = new TextArea();
        txtObservacoes.setPromptText("Anotações livres sobre a aula ou alunos...");
        txtObservacoes.setWrapText(true);
        txtObservacoes.setPrefRowCount(4);
        txtObservacoes.setStyle(estiloInput());

        // Dica de linha amarela = entrada com observações
        Label lblLegenda = new Label("⚠ Linha em destaque = entrada com observações");
        lblLegenda.setStyle("-fx-text-fill: #b7791f; -fx-font-size: 11px; " +
                "-fx-background-color: #fefcbf; -fx-padding: 6 10; -fx-background-radius: 6;");

        // Botão salvar
        btnSalvar = new Button("Salvar Entrada");
        btnSalvar.setStyle("-fx-background-color: #6b46c1; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 10 20; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> salvar());

        form.getChildren().addAll(
                lblT, cbTurma,
                lblD, dpData,
                lblTit, txtTitulo,
                lblC, lblCHint, txtConteudo,
                lblO, lblOHint, txtObservacoes,
                lblLegenda,
                btnSalvar
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: white; -fx-background-color: white;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        painelEditor.getChildren().addAll(hdrE, scroll);
    }

    // =========================================================================
    // AÇÕES
    // =========================================================================

    private void abrirFormNovo() {
        editando = null;
        lblEditorTitulo.setText("Nova Entrada");
        btnSalvar.setText("Salvar Entrada");
        cbTurma.getSelectionModel().clearSelection();
        dpData.setValue(LocalDate.now());
        txtTitulo.clear();
        txtConteudo.clear();
        txtObservacoes.clear();
        mostrarEditor();
        tabela.getSelectionModel().clearSelection();
    }

    private void abrirFormEditar(DiarioAula entrada) {
        editando = entrada;
        lblEditorTitulo.setText("Editar Entrada — " + entrada.getDataFormatada());
        btnSalvar.setText("Salvar Alterações");

        // Seleciona a turma correspondente
        cbTurma.getItems().stream()
                .filter(t -> t.getId().equals(entrada.getTurmaId()))
                .findFirst()
                .ifPresentOrElse(cbTurma::setValue,
                        () -> cbTurma.getSelectionModel().clearSelection());

        dpData.setValue(entrada.getDataAula());
        txtTitulo.setText(entrada.getTitulo());
        txtConteudo.setText(entrada.getConteudo());
        txtObservacoes.setText(entrada.getObservacoes());
        mostrarEditor();
    }

    private void salvar() {
        Turma turma  = cbTurma.getValue();
        LocalDate dt = dpData.getValue();
        String titulo = txtTitulo.getText().trim();
        String cont   = txtConteudo.getText().trim();
        String obs    = txtObservacoes.getText().trim();

        if (turma == null || dt == null) {
            mainApp.mostrarAviso("Selecione a turma e a data.", true);
            return;
        }
        if (titulo.isBlank() && cont.isBlank()) {
            mainApp.mostrarAviso("Preencha ao menos o título ou o conteúdo.", true);
            return;
        }

        if (editando == null) {
            String id = diarioDAO.inserir(
                    mainApp.getSessao().getId(), turma.getId(), dt, titulo, cont, obs);
            if (id != null) {
                mainApp.mostrarAviso("Entrada registrada no diário!", false);
                fecharEditor();
                carregar();
            } else {
                mainApp.mostrarAviso("Erro ao salvar. Tente novamente.", true);
            }
        } else {
            if (diarioDAO.atualizar(editando.getId(), dt, titulo, cont, obs)) {
                mainApp.mostrarAviso("Entrada atualizada!", false);
                fecharEditor();
                carregar();
            } else {
                mainApp.mostrarAviso("Erro ao atualizar entrada.", true);
            }
        }
    }

    private void excluir(DiarioAula entrada) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir entrada de " + entrada.getDataFormatada() + " — " +
                        entrada.getTurmaNome() + "?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar exclusão");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                if (diarioDAO.excluir(entrada.getId())) {
                    mainApp.mostrarAviso("Entrada excluída.", false);
                    if (editando != null && editando.getId().equals(entrada.getId()))
                        fecharEditor();
                    carregar();
                } else {
                    mainApp.mostrarAviso("Erro ao excluir entrada.", true);
                }
            }
        });
    }

    // =========================================================================
    // FILTROS
    // =========================================================================

    private void aplicarFiltro() {
        Turma selecionada = cbFiltroTurma.getValue();
        String termo = txtBusca.getText() == null ? "" : txtBusca.getText().trim().toLowerCase();

        filtrado.setPredicate(d -> {
            boolean passaTurma = selecionada == null || d.getTurmaId().equals(selecionada.getId());
            boolean passaBusca = termo.isBlank()
                    || d.getTitulo().toLowerCase().contains(termo)
                    || d.getConteudo().toLowerCase().contains(termo)
                    || d.getObservacoes().toLowerCase().contains(termo)
                    || d.getTurmaNome().toLowerCase().contains(termo);
            return passaTurma && passaBusca;
        });
    }

    private void aplicarFiltroTexto(String texto) {
        aplicarFiltro();
    }

    // =========================================================================
    // DADOS
    // =========================================================================

    private void carregar() {
        todos.setAll(diarioDAO.listarPorProfessor(mainApp.getSessao().getId()));
        aplicarFiltro();
    }

    // =========================================================================
    // HELPERS DE UI
    // =========================================================================

    private void mostrarEditor() {
        painelEditor.setVisible(true);
        painelEditor.setManaged(true);
    }

    private void fecharEditor() {
        painelEditor.setVisible(false);
        painelEditor.setManaged(false);
        tabela.getSelectionModel().clearSelection();
        editando = null;
    }

    private Label campo(String texto) {
        Label l = new Label(texto + ":");
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 12px;");
        return l;
    }

    private String estiloInput() {
        return "-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #e2e8f0; -fx-padding: 6 10; -fx-font-size: 13px;";
    }

    private ListCell<Turma> celulaTurma() {
        return new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getNome() + " (" + t.getEscolaNome() + ")");
            }
        };
    }

    private ListCell<Turma> celulaTurmaBtn(String placeholder) {
        return new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? placeholder : t.getNome() + " (" + t.getEscolaNome() + ")");
            }
        };
    }

    public BorderPane getView() { return view; }
}
