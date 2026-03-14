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
    private RadioButton rbPublica, rbPrivada;
    private ToggleGroup tgTipo;
    private Label lblTipoLabel;
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

        Label lblEscopo = new Label("Exibindo apenas as escolas das suas turmas");
        lblEscopo.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");
        lblEscopo.setVisible(!mainApp.isAdmin());
        lblEscopo.setManaged(!mainApp.isAdmin());

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("Buscar por nome/status...");
        txtBusca.setPrefWidth(280);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregarDados());

        Button btnNova = new Button("+ Cadastrar Escola");
        btnNova.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
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

        TableColumn<Escola, String> colTipo = new TableColumn<>("Rede");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoLabel"));
        colTipo.setMaxWidth(90);
        // Cor por tipo
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("PRIVADA".equals(getTableView().getItems().get(getIndex()).getTipo())
                        ? "-fx-text-fill: #6b46c1; -fx-font-weight: bold;"
                        : "-fx-text-fill: #2b6cb0; -fx-font-weight: bold;");
            }
        });

        TableColumn<Escola, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setMaxWidth(80);

        tabela.getColumns().addAll(colNome, colTipo, colStatus);

        FilteredList<Escola> filtrado = new FilteredList<>(dados, e -> true);
        txtBusca.textProperty().addListener((obs, old, term) -> {
            String f = term == null ? "" : term.trim().toLowerCase();
            filtrado.setPredicate(e -> f.isBlank()
                    || e.getNome().toLowerCase().contains(f)
                    || e.getStatus().toLowerCase().contains(f)
                    || e.getTipoLabel().toLowerCase().contains(f));
        });
        SortedList<Escola> ordenado = new SortedList<>(filtrado);
        ordenado.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenado);

        tabela.setRowFactory(tv -> {
            TableRow<Escola> row = new TableRow<>();

            if (mainApp.isAdmin()) {
                ContextMenu cm = new ContextMenu();
                MenuItem del = new MenuItem("Excluir Escola");
                del.setStyle("-fx-text-fill: red;");
                del.setOnAction(ev -> {
                    Escola e = row.getItem();
                    if (e != null) {
                        if (dao.excluir(e.getId())) {
                            mainApp.mostrarAviso("Escola removida!", false);
                            carregarDados(); fecharDetalhe();
                        } else {
                            mainApp.mostrarAviso("Erro ao excluir. Verifique vínculos.", true);
                        }
                    }
                });
                cm.getItems().add(del);
                row.emptyProperty().addListener((obs, w, n) -> row.setContextMenu(n ? null : cm));
            }

            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    mainApp.abrirDashboardEscola(row.getItem());
                } else if (ev.getClickCount() == 1 && !row.isEmpty()) {
                    abrirDetalheEscola(row.getItem());
                }
            });
            return row;
        });

        // ── Painel de detalhe ──────────────────────────────────────────────
        painelDetalhe = new VBox(14);
        painelDetalhe.setPadding(new Insets(24));
        painelDetalhe.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
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
        txtNome.setEditable(mainApp.isAdmin());

        // Seletor de tipo (admin only)
        tgTipo  = new ToggleGroup();
        rbPublica = new RadioButton("Pública");
        rbPrivada = new RadioButton("Privada");
        rbPublica.setToggleGroup(tgTipo);
        rbPrivada.setToggleGroup(tgTipo);
        rbPublica.setSelected(true);

        HBox tipoBox = new HBox(16, rbPublica, rbPrivada);
        tipoBox.setAlignment(Pos.CENTER_LEFT);

        Label lblTipoTit = new Label("Tipo de Rede:");
        lblTipoTit.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        // Para professor: label somente leitura
        lblTipoLabel = new Label();
        lblTipoLabel.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 13px;");

        // Visibilidade condicional
        lblTipoTit.setVisible(mainApp.isAdmin());
        lblTipoTit.setManaged(mainApp.isAdmin());
        tipoBox.setVisible(mainApp.isAdmin());
        tipoBox.setManaged(mainApp.isAdmin());
        lblTipoLabel.setVisible(!mainApp.isAdmin());
        lblTipoLabel.setManaged(!mainApp.isAdmin());

        btnSalvar = new Button("Cadastrar Escola");
        btnSalvar.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> cadastrar());
        btnSalvar.setVisible(mainApp.isAdmin());
        btnSalvar.setManaged(mainApp.isAdmin());

        painelDetalhe.getChildren().addAll(
                hdrD, lblAcaoEscola, new Separator(),
                lblFormTitle,
                new Label("Nome:"), txtNome,
                lblTipoTit, tipoBox,
                lblTipoLabel,
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
        rbPublica.setSelected(true);
        mostrarDetalhe();
    }

    private void abrirDetalheEscola(Escola escola) {
        escolaSelecionada = escola;
        lblAcaoEscola.setText(mainApp.isAdmin() ? "Editar Escola" : escola.getNome());
        if (mainApp.isAdmin()) btnSalvar.setText("Salvar Alterações");
        txtNome.setText(escola.getNome());

        if ("PRIVADA".equals(escola.getTipo())) rbPrivada.setSelected(true);
        else rbPublica.setSelected(true);

        lblTipoLabel.setText("Rede: " + escola.getTipoLabel());
        mostrarDetalhe();
    }

    private void cadastrar() {
        String nome = txtNome.getText().trim();
        if (nome.isBlank()) {
            mainApp.mostrarAviso("Preencha o nome da escola.", true);
            return;
        }
        String tipo = rbPrivada.isSelected() ? "PRIVADA" : "PUBLICA";

        boolean sucesso = escolaSelecionada == null
                ? dao.inserir(Escola.nova(nome, "ativo", tipo))
                : dao.atualizar(escolaSelecionada.getId(), nome, tipo);

        if (sucesso) {
            mainApp.mostrarAviso(escolaSelecionada == null
                    ? "Escola cadastrada com sucesso!" : "Escola atualizada!", false);
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
        if (mainApp.isAdmin()) dados.setAll(dao.listarTodas());
        else dados.setAll(dao.listarPorProfessor(mainApp.getSessao().getId()));
    }

    public BorderPane getView() { return view; }
}