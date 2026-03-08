package app;

import core.Escola;
import dao.EscolasDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class EscolasView {

    private BorderPane view;
    private TableView<Escola> tabela;
    private EscolasDAO dao;
    private Escola escolaSelecionada;
    private MainFX mainApp;
    private final ObservableList<Escola> dados = FXCollections.observableArrayList();

    private VBox painelDetalhe;
    private Label lblAcaoEscola;
    private TextField txtNome;
    private Button btnSalvar;

    public EscolasView(MainFX mainApp) {
        this.mainApp = mainApp;
        this.dao = new EscolasDAO();
        construirInterface();
        carregarDados();
    }

    private void construirInterface() {
        view = new BorderPane();

        Label lblTitulo = new Label("Gestão de Escolas");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        // PATCH: subtítulo de escopo para professor
        Label lblEscopo = new Label("Exibindo apenas as escolas das suas turmas");
        lblEscopo.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");
        lblEscopo.setVisible(!mainApp.isAdmin());
        lblEscopo.setManaged(!mainApp.isAdmin());

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("Buscar por nome/status...");
        txtBusca.setPrefWidth(280);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregarDados());

        // PATCH: cadastrar escola apenas para admin
        Button btnNova = new Button("+ Cadastrar Escola");
        btnNova.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNova.setOnAction(e -> abrirFormNovo());
        btnNova.setVisible(mainApp.isAdmin());
        btnNova.setManaged(mainApp.isAdmin());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox tituloBox = new VBox(2, lblTitulo, lblEscopo);
        HBox header = new HBox(12, tituloBox, spacer, txtBusca, btnAtualizar, btnNova);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 16, 20));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhuma escola encontrada."));
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<Escola, String> colNome = new TableColumn<>("Nome da Escola");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Escola, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tabela.getColumns().addAll(colNome, colStatus);

        FilteredList<Escola> filtrado = new FilteredList<>(dados, escola -> true);
        txtBusca.textProperty().addListener((obs, old, term) -> {
            String filtro = term == null ? "" : term.trim().toLowerCase();
            filtrado.setPredicate(escola -> filtro.isBlank()
                    || escola.getNome().toLowerCase().contains(filtro)
                    || escola.getStatus().toLowerCase().contains(filtro));
        });

        SortedList<Escola> ordenado = new SortedList<>(filtrado);
        ordenado.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenado);

        tabela.setRowFactory(tv -> {
            TableRow<Escola> row = new TableRow<>();

            // PATCH: menu de excluir apenas para admin
            if (mainApp.isAdmin()) {
                ContextMenu contextMenu = new ContextMenu();
                MenuItem deleteItem = new MenuItem("Excluir Escola");
                deleteItem.setStyle("-fx-text-fill: red;");
                deleteItem.setOnAction(event -> {
                    Escola e = row.getItem();
                    if (e != null) {
                        if (dao.excluir(e.getId())) {
                            mainApp.mostrarAviso("Escola removida com sucesso!", false);
                            carregarDados();
                            fecharDetalhe();
                        } else {
                            mainApp.mostrarAviso("Erro ao excluir. Verifique vínculos.", true);
                        }
                    }
                });
                contextMenu.getItems().add(deleteItem);
                row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) ->
                        row.setContextMenu(isNowEmpty ? null : contextMenu));
            }

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    mainApp.abrirDashboardEscola(row.getItem());
                } else if (event.getClickCount() == 1 && !row.isEmpty()) {
                    abrirDetalheEscola(row.getItem());
                }
            });
            return row;
        });

        // ── Painel de detalhe ──────────────────────────────────────────────────
        painelDetalhe = new VBox(14);
        painelDetalhe.setPadding(new Insets(24));
        painelDetalhe.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
        painelDetalhe.setMinWidth(300);
        painelDetalhe.setMaxWidth(360);
        painelDetalhe.setVisible(false);
        painelDetalhe.setManaged(false);

        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-cursor: hand;");
        btnFechar.setOnAction(e -> fecharDetalhe());
        HBox hdrD = new HBox(btnFechar);
        hdrD.setAlignment(Pos.TOP_RIGHT);

        lblAcaoEscola = new Label("Escola");
        lblAcaoEscola.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Label lblFormTitle = new Label("Dados da Escola");
        lblFormTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        txtNome = new TextField();
        txtNome.setPromptText("Nome da Escola");
        // PATCH: professor não edita
        txtNome.setEditable(mainApp.isAdmin());

        // PATCH: botão salvar apenas para admin
        btnSalvar = new Button("Cadastrar Escola");
        btnSalvar.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> cadastrar());
        btnSalvar.setVisible(mainApp.isAdmin());
        btnSalvar.setManaged(mainApp.isAdmin());

        painelDetalhe.getChildren().addAll(
                hdrD, lblAcaoEscola, new Separator(),
                lblFormTitle,
                new Label("Nome:"), txtNome,
                btnSalvar
        );

        VBox conteudoEsq = new VBox(header, tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        HBox mainLayout = new HBox(conteudoEsq, painelDetalhe);
        HBox.setHgrow(conteudoEsq, Priority.ALWAYS);
        view.setCenter(mainLayout);
    }

    private void abrirFormNovo() {
        // Só admin chega aqui (botão está oculto para professor)
        tabela.getSelectionModel().clearSelection();
        escolaSelecionada = null;
        lblAcaoEscola.setText("Nova Escola");
        btnSalvar.setText("Cadastrar Escola");
        txtNome.clear();
        mostrarDetalhe();
    }

    private void abrirDetalheEscola(Escola escola) {
        escolaSelecionada = escola;
        lblAcaoEscola.setText(mainApp.isAdmin() ? "Editar Escola" : escola.getNome());
        if (mainApp.isAdmin()) btnSalvar.setText("Salvar Alterações");
        txtNome.setText(escola.getNome());
        mostrarDetalhe();
    }

    private void cadastrar() {
        // Só admin chega aqui (botão está oculto para professor)
        String nome = txtNome.getText().trim();
        if (nome.isBlank()) {
            mainApp.mostrarAviso("Preencha o nome da escola.", true);
            return;
        }

        boolean sucesso = escolaSelecionada == null
                ? dao.inserir(new Escola(nome, "ativo"))
                : dao.atualizar(escolaSelecionada.getId(), nome);

        if (sucesso) {
            mainApp.mostrarAviso(escolaSelecionada == null ? "Escola cadastrada com sucesso!" : "Escola atualizada!", false);
            txtNome.clear();
            carregarDados();
            fecharDetalhe();
        } else {
            mainApp.mostrarAviso("Erro ao salvar a escola no banco.", true);
        }
    }

    private void mostrarDetalhe() { painelDetalhe.setVisible(true); painelDetalhe.setManaged(true); }

    private void fecharDetalhe() {
        painelDetalhe.setVisible(false);
        painelDetalhe.setManaged(false);
        tabela.getSelectionModel().clearSelection();
        escolaSelecionada = null;
    }

    private void carregarDados() {
        // PATCH: professor vê apenas escolas das suas turmas
        if (mainApp.isAdmin()) {
            dados.setAll(dao.listarTodas());
        } else {
            dados.setAll(dao.listarPorProfessor(mainApp.getSessao().getId()));
        }
    }

    public BorderPane getView() { return view; }
}