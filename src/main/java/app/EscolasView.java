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

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("Buscar por nome/status...");
        txtBusca.setPrefWidth(280);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregarDados());

        Button btnNova = new Button("+ Cadastrar Escola");
        btnNova.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNova.setOnAction(e -> abrirFormNovo());
        btnNova.setVisible(mainApp.isAdmin());
        btnNova.setManaged(mainApp.isAdmin());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, lblTitulo, spacer, txtBusca, btnAtualizar, btnNova);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 16, 20));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
            filtrado.setPredicate(escola -> {
                if (filtro.isBlank()) return true;
                return escola.getNome().toLowerCase().contains(filtro)
                        || escola.getStatus().toLowerCase().contains(filtro);
            });
        });

        SortedList<Escola> ordenado = new SortedList<>(filtrado);
        ordenado.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenado);

        tabela.setRowFactory(tv -> {
            TableRow<Escola> row = new TableRow<>();

            // Context Menu SOMENTE se for admin
            if (mainApp.isAdmin()) {
                ContextMenu contextMenu = new ContextMenu();
                MenuItem deleteItem = new MenuItem("Excluir Escola");
                deleteItem.setStyle("-fx-text-fill: red;");
                deleteItem.setOnAction(event -> { /*... exclui ...*/ });
                contextMenu.getItems().add(deleteItem);
                row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> row.setContextMenu(isNowEmpty ? null : contextMenu));
            }

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    mainApp.abrirDashboardEscola(row.getItem());
                } else if (event.getClickCount() == 1 && (!row.isEmpty())) {
                    abrirDetalheEscola(row.getItem());
                }
            });
            return row;
        });

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

        lblAcaoEscola = new Label("Nova Escola");
        lblAcaoEscola.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Label lblFormTitle = new Label("Dados da Escola");
        lblFormTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        txtNome = new TextField();
        txtNome.setPromptText("Nome da Escola");

        btnSalvar = new Button("Cadastrar Escola");
        btnSalvar.setVisible(mainApp.isAdmin());
        btnSalvar.setManaged(mainApp.isAdmin());
        txtNome.setEditable(mainApp.isAdmin());
        btnSalvar.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> cadastrar());

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
        tabela.getSelectionModel().clearSelection();
        escolaSelecionada = null;
        lblAcaoEscola.setText("Nova Escola");
        btnSalvar.setText("Cadastrar Escola");
        txtNome.clear();
        mostrarDetalhe();
    }

    private void abrirDetalheEscola(Escola escola) {
        escolaSelecionada = escola;
        lblAcaoEscola.setText("Editar Escola");
        btnSalvar.setText("Salvar Alterações");
        txtNome.setText(escola.getNome());
        mostrarDetalhe();
    }

    private void cadastrar() {
        String nome = txtNome.getText().trim();
        if (nome.isBlank()) {
            mainApp.mostrarAviso("Preencha o nome da escola.", true);
            return;
        }

        boolean sucesso;

        if (escolaSelecionada == null) {
            sucesso = dao.inserir(new Escola(nome, "ativo"));
        } else {
            sucesso = dao.atualizar(escolaSelecionada.getId(), nome);
        }

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
        painelDetalhe.setVisible(false); painelDetalhe.setManaged(false);
        tabela.getSelectionModel().clearSelection();
        escolaSelecionada = null;
    }

    private void carregarDados() { dados.setAll(dao.listarTodas()); }

    public BorderPane getView() { return view; }
}