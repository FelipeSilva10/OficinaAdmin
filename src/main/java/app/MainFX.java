package app;

import core.*;
import core.Sessao;
import dao.ProdutoDAO;
import dao.ConexaoBD;

import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import java.sql.Connection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.beans.property.SimpleStringProperty;

import java.util.Comparator;

public class MainFX extends Application {

    private BorderPane rootLayout;
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    @Override
    public void start(Stage stage) {
        // Inicializa o tema moderno do AtlantaFX
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        rootLayout = new BorderPane();
        rootLayout.setLeft(criarMenuLateral());
        rootLayout.setCenter(criarDashboard()); // Tela Inicial

        Scene scene = new Scene(rootLayout, 1200, 768);
        stage.setTitle("GEST - Sistema de Gestão de Estoque");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    // =================================================================================
    // MENU LATERAL (AtlantaFX Styled)
    // =================================================================================
    private VBox criarMenuLateral() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(260);

        // Ajuste sutil na cor de fundo para destacar mais a logo
        sidebar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d0d7de; -fx-border-width: 0 1 0 0;");

        // 1. LOGO REFINADA
        ImageView logoView = new ImageView();
        try {
            // Tenta carregar. Se falhar, não quebra o app.
            java.io.InputStream is = getClass().getResourceAsStream("/logoG.png");
            if (is == null) is = getClass().getResourceAsStream("/images/logoG.png"); // Tenta subpasta

            if (is != null) {
                logoView.setImage(new Image(is));
                // Aumentei um pouco para preencher melhor
                logoView.setFitWidth(160);
                logoView.setPreserveRatio(true);
                // TRUQUE DE QUALIDADE: Suaviza a imagem
                logoView.setSmooth(true);
                logoView.setCache(true);
            }
        } catch (Exception e) {
            /* Ignora erro silenciosamente ou usa System.out */
        }

        // Container para centralizar a logo se quiser
        HBox logoContainer = new HBox(logoView);
        logoContainer.setAlignment(Pos.CENTER_LEFT);
        logoContainer.setPadding(new Insets(0, 0, 10, 0));

        // 2. INFORMAÇÕES DO USUÁRIO
        Label lblUser = new Label(Sessao.getUsuario().getNomeCompleto()); // Usa Nome Completo é mais formal
        lblUser.getStyleClass().addAll(Styles.TITLE_4);
        lblUser.setWrapText(true); // Quebra linha se o nome for longo

        // Pega o cargo do usuário
        String cargoTexto = Sessao.getUsuario().getCargo() != null ? Sessao.getUsuario().getCargo() : "Colaborador";
        Label lblCargo = new Label(cargoTexto.toUpperCase());
        lblCargo.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);

        // Cria um "Avatar" simples
        Label lblAvatar = new Label(Sessao.getUsuario().getLogin().substring(0, 1).toUpperCase());
        lblAvatar.setStyle("-fx-background-color: #0969da; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 50%; -fx-min-width: 35; -fx-min-height: 35; -fx-alignment: center;");

        VBox userDetails = new VBox(2, lblUser, lblCargo);
        HBox userBox = new HBox(10, lblAvatar, userDetails);
        userBox.setAlignment(Pos.CENTER_LEFT);
        userBox.setPadding(new Insets(10, 0, 10, 0));

        // 3. BOTÃO DE SAIR
        Button btnLogout = new Button("Sair");
        btnLogout.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL); // Botão menor
        btnLogout.setPrefWidth(100);
        btnLogout.setOnAction(e -> onLogout());

        // Montagem do Header
        VBox header = new VBox(15, logoContainer, userBox, btnLogout);
        header.setPadding(new Insets(30, 20, 20, 20));
        header.setAlignment(Pos.CENTER_LEFT);

        // --- Navegação
        Button btnDash = criarBotaoMenu("Início");
        Button btnVendas = criarBotaoMenu("Vendas");
        Button btnEstoque = criarBotaoMenu("Estoque");
        Button btnFornecedores = criarBotaoMenu("Fornecedores");
        Button btnRelatorios = criarBotaoMenu("Relatórios & Logs");

        btnDash.setOnAction(e -> rootLayout.setCenter(criarDashboard()));
        btnVendas.setOnAction(e -> rootLayout.setCenter(criarTelaVendas()));
        btnEstoque.setOnAction(e -> rootLayout.setCenter(criarTelaEstoque()));
        btnFornecedores.setOnAction(e -> rootLayout.setCenter(criarTelaFornecedores()));
        btnRelatorios.setOnAction(e -> rootLayout.setCenter(criarTelaRelatorios()));

        VBox botoesLayout = new VBox(2, btnDash, btnVendas, btnEstoque, btnFornecedores, btnRelatorios);

        // --- STATUS DA CONEXÃO ---
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label lblStatusBD = new Label("● Conectado: PostgreSQL");
        lblStatusBD.getStyleClass().add(Styles.TEXT_SMALL);
        lblStatusBD.setPadding(new Insets(0, 0, 10, 20)); // Margem inferior e esquerda

        // Lógica simples de verificação (Visual)
        try (Connection conn = ConexaoBD.conectar()) {
            if (conn != null && !conn.isClosed()) {
                lblStatusBD.setStyle("-fx-text-fill: green; -fx-font-size: 11px;");
            }
        } catch (Exception e) {
            lblStatusBD.setText("● Offline / Erro BD");
            lblStatusBD.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        }

        Label lblVersao = new Label("v1.0.0 (Sprint 2)");
        lblVersao.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        lblVersao.setPadding(new Insets(0, 0, 20, 20));

        // Adiciona tudo na sidebar
        sidebar.getChildren().addAll(header, botoesLayout, spacer, lblStatusBD, lblVersao);
        return sidebar;
    }

    private Button criarBotaoMenu(String texto) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.BASELINE_LEFT);
        btn.setPadding(new Insets(12, 20, 12, 20));
        // Botões flat (sem borda/fundo até passar o mouse)
        btn.getStyleClass().addAll(Styles.FLAT, Styles.TITLE_4);
        return btn;
    }

    // =================================================================================
    // TELA 0: DASHBOARD (H13)
    // =================================================================================
    private VBox criarDashboard() {
        Label lblTitulo = new Label("Visão Geral");
        lblTitulo.getStyleClass().addAll(Styles.TITLE_1);

        // 1. Buscando dados reais
        List<Produto> listaProdutos = produtoDAO.listarTodos();

        // Cálculo: Total de Itens
        int totalProdutos = listaProdutos.size();

        // Cálculo: Estoque Crítico
        long estoqueCritico = listaProdutos.stream()
                .filter(p -> p.getQntdDisp() < 10)
                .count();

        // Cálculo: Valor total em estoque
        double valorEmEstoque = listaProdutos.stream().mapToDouble(p -> p.getQntdDisp() * p.getValorUnitVenda()).sum();

        HBox cards = new HBox(20);
        cards.getChildren().addAll(
                criarCard("Catálogo Ativo", String.valueOf(totalProdutos), "Produtos cadastrados"),
                criarCard("Estoque Crítico", String.valueOf(estoqueCritico), "Abaixo de 10 un."),
                criarCard("Valor em Estoque", formatarValorAbreviado(valorEmEstoque), "Preço de venda")
        );

        Label lblRecentes = new Label("Últimas Movimentações");
        lblRecentes.getStyleClass().addAll(Styles.TITLE_3);
        VBox.setMargin(lblRecentes, new Insets(20, 0, 0, 0));

        TableView<String> tabRecentes = new TableView<>();
        tabRecentes.getStyleClass().add(Styles.STRIPED);
        tabRecentes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabRecentes.getColumns().addAll(
                new TableColumn<>("Ação"), new TableColumn<>("Produto"),
                new TableColumn<>("Qtd"), new TableColumn<>("Data/Hora")
        );
        VBox.setVgrow(tabRecentes, Priority.ALWAYS);

        VBox layout = new VBox(20, lblTitulo, cards, lblRecentes, tabRecentes);
        layout.setPadding(new Insets(40));
        return layout;
    }

    private VBox criarCard(String titulo, String valor, String subtitulo) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TITLE_4);

        Label lblValor = new Label(valor);
        lblValor.getStyleClass().addAll(Styles.TITLE_1);
        lblValor.setStyle("-fx-font-size: 36px;"); // Sobrescreve apenas o tamanho

        Label lblSub = new Label(subtitulo);
        lblSub.getStyleClass().addAll(Styles.TEXT_SMALL);

        VBox card = new VBox(5, lblTitulo, lblValor, lblSub);
        card.setPadding(new Insets(20));
        card.setPrefSize(280, 120);
        // Aplica a classe de elevação do AtlantaFX (Sombra suave)
        card.getStyleClass().addAll("card", Styles.ELEVATED_1);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 6px;");
        return card;
    }

    // =================================================================================
    // TELA 1: CONSULTA DE ESTOQUE (Busca Avançada)
    // =================================================================================

    private VBox criarAbaConsulta() {

        // 1. CARREGAMENTO DE DADOS (Master Data)
        // Carregamos tudo do banco uma vez para memória para filtrar rápido
        List<Produto> listaOriginal = produtoDAO.listarTodos();
        ObservableList<Produto> masterData = FXCollections.observableArrayList(listaOriginal);

        // 2. FILTROS E BUSCA (FilteredList)
        // Envolvemos a lista original em uma lista filtrável
        FilteredList<Produto> filteredData = new FilteredList<>(masterData, p -> true);

        // Componentes de Filtro
        TextField txtBusca = new TextField();
        txtBusca.setPromptText("🔎 Buscar por nome, código ou categoria...");
        txtBusca.setPrefWidth(320);

        ComboBox<String> cmbOrdenacao = new ComboBox<>();
        cmbOrdenacao.getItems().addAll(
                "Padrão (Código)",
                "Maior Quantidade",
                "Menor Quantidade",
                "Maior Valor",
                "Menor Valor",
                "Nome (A-Z)"
        );
        cmbOrdenacao.setValue("Padrão (Código)");
        cmbOrdenacao.setPrefWidth(180);


// Botão de "Refresh" caso novos dados entrem no banco
        Button btnAtualizar = new Button("Recarregar");
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAtualizar.setOnAction(e -> {
            masterData.setAll(produtoDAO.listarTodos());
            txtBusca.clear();
        });

        // 1. PRIMEIRO DECLARAMOS A TABELA AQUI EM CIMA!
        TableView<Produto> tabela = new TableView<>();
        tabela.getStyleClass().addAll(Styles.STRIPED, Styles.BORDERED);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // 2. AGORA SIM, CRIAMOS OS BOTÕES DE AÇÃO
        Button btnNovo = new Button("Cadastrar Produto");
        btnNovo.getStyleClass().addAll(Styles.SUCCESS);
        btnNovo.setOnAction(e -> abrirModalCadastroProduto(masterData));

        Button btnExcluir = new Button("Excluir Selecionado");
        btnExcluir.getStyleClass().addAll(Styles.DANGER);
        btnExcluir.setOnAction(e -> {
            Produto selecionado = tabela.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                mostrarErro("Atenção", "Selecione um produto na tabela clicando nele primeiro.");
                return;
            }

            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                    "Tem certeza que deseja excluir o produto '" + selecionado.getDescricao() + "' (Cód: " + selecionado.getCodigo() + ")?",
                    ButtonType.YES, ButtonType.NO);
            confirmacao.setHeaderText("Confirmação de Exclusão");

            confirmacao.showAndWait().ifPresent(resposta -> {
                if (resposta == ButtonType.YES) {
                    try {
                        produtoDAO.excluir(selecionado.getCodigo());
                        masterData.remove(selecionado);
                        mostrarAlerta("Produto excluído com sucesso!");
                    } catch (Exception ex) {
                        mostrarErro("Erro", "Não foi possível excluir o produto: " + ex.getMessage());
                    }
                }
            });
        });

        // 3. MONTAMOS A BARRA DE FERRAMENTAS
        Region espacador = new Region();
        HBox.setHgrow(espacador, Priority.ALWAYS);

        HBox barraFerramentas = new HBox(10, txtBusca, cmbOrdenacao, btnAtualizar, espacador, btnExcluir, btnNovo);
        barraFerramentas.setAlignment(Pos.CENTER_LEFT);

        // 3. TABELA (SortedList)
        tabela.getStyleClass().addAll(Styles.STRIPED, Styles.BORDERED);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // --- Definição das Colunas ---



        TableColumn<Produto, Integer> colId = new TableColumn<>("Cód");
        colId.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colId.setMaxWidth(80);

        TableColumn<Produto, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));

        TableColumn<Produto, String> colCat = new TableColumn<>("Categoria");
        colCat.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCat.setMaxWidth(150);

        // Coluna Especial: Fabricante (se Cosmético) ou Fornecedor (Geral)
        TableColumn<Produto, String> colOrigem = new TableColumn<>("Fabricante / Fornecedor");
        colOrigem.setCellValueFactory(cellData -> {
            Produto p = cellData.getValue();
            if (p instanceof Cosmetico) {
                return new SimpleStringProperty(((Cosmetico) p).getFabricante());
            }
            return new SimpleStringProperty(p.getFornecedor().getNome());
        });

        TableColumn<Produto, Integer> colQtd = new TableColumn<>("Estoque");
        colQtd.setCellValueFactory(new PropertyValueFactory<>("qntdDisp"));
        colQtd.setMaxWidth(100);
        // Destaca em vermelho se estoque baixo
        colQtd.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    if (item < 5) {
                        setStyle("-fx-text-fill: #cf222e; -fx-font-weight: bold;"); // Vermelho
                    } else {
                        setStyle("-fx-text-fill: #1a7f37;"); // Verde
                    }
                }
            }
        });

        TableColumn<Produto, String> colValor = new TableColumn<>("Valor (R$)");
        colValor.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("R$ %.2f", cellData.getValue().getValorUnitVenda()))
        );
        colValor.setMaxWidth(120);

        tabela.getColumns().addAll(colId, colDesc, colCat, colOrigem, colQtd, colValor);

        // 1. Lógica para contar quantos estão críticos (Subtarefa G7-72)
        long qtdCriticos = listaOriginal.stream()
                .filter(p -> p.getQntdDisp() <= 5)
                .count();

        // 2. Criar um alerta visual de resumo
        HBox alertaCritico = new HBox(10);
        alertaCritico.setAlignment(Pos.CENTER_LEFT);
        alertaCritico.setPadding(new Insets(10));

        if (qtdCriticos > 0) {
            alertaCritico.setStyle("-fx-background-color: #ffebe9; -fx-border-color: #ff8182; -fx-border-radius: 5;");
            Label lblAlerta = new Label("⚠ Atenção: Existem " + qtdCriticos + " produtos com estoque crítico (abaixo de 5 unidades)!");
            lblAlerta.setStyle("-fx-text-fill: #cf222e; -fx-font-weight: bold;");
            alertaCritico.getChildren().add(lblAlerta);
        } else {
            alertaCritico.setStyle("-fx-background-color: #dafbe1; -fx-border-color: #4ac26b; -fx-border-radius: 5;");
            Label lblOk = new Label("✅ Todos os produtos estão com estoque saudável.");
            lblOk.setStyle("-fx-text-fill: #1a7f37;");
            alertaCritico.getChildren().add(lblOk);
        }

        // 4. LÓGICA DE FILTRAGEM (Ao digitar)
        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> filteredData.setPredicate(produto -> {
            // Se o filtro estiver vazio, mostra tudo
            if (newValue == null || newValue.isEmpty()) {
                return true;
            }

            String lowerCaseFilter = newValue.toLowerCase();

            // Regras de busca:
            if (String.valueOf(produto.getCodigo()).contains(lowerCaseFilter)) return true;
            if (produto.getDescricao().toLowerCase().contains(lowerCaseFilter)) return true;
            if (produto.getCategoria().toLowerCase().contains(lowerCaseFilter)) return true;

            // Busca extra: Verifica fabricante se for cosmético
            if (produto instanceof Cosmetico) {
                return ((Cosmetico) produto).getFabricante().toLowerCase().contains(lowerCaseFilter);
            }

            return false; // Não encontrou nada
        }));

        // 5. LÓGICA DE ORDENAÇÃO (ComboBox + Tabela)
        // Envolvemos a Lista Filtrada em uma Lista Ordenada
        SortedList<Produto> sortedData = new SortedList<>(filteredData);

        // Vincula o comparador da tabela (clique no cabeçalho) à lista ordenada
        sortedData.comparatorProperty().bind(tabela.comparatorProperty());

        // Adiciona lógica do ComboBox de Ordenação
        cmbOrdenacao.setOnAction(e -> {
            String selecionado = cmbOrdenacao.getValue();
            switch (selecionado) {
                case "Maior Quantidade":
                    tabela.getSortOrder().clear();
                    colQtd.setSortType(TableColumn.SortType.DESCENDING);
                    tabela.getSortOrder().add(colQtd);
                    break;
                case "Menor Quantidade":
                    tabela.getSortOrder().clear();
                    colQtd.setSortType(TableColumn.SortType.ASCENDING);
                    tabela.getSortOrder().add(colQtd);
                    break;
                case "Maior Valor":
                    // Precisamos ordenar pela propriedade original, não pela String formatada
                    // Pequeno hack: forçar comparador na lista ou usar a coluna se ela fosse numérica
                    // Como a colValor é String (R$), a ordenação padrão falha.
                    // Vamos ordenar a lista diretamente:
                    tabela.getSortOrder().clear(); // Remove ordenação visual de coluna
                    sortedData.setComparator(Comparator.comparingDouble(Produto::getValorUnitVenda).reversed());
                    break;
                case "Menor Valor":
                    tabela.getSortOrder().clear();
                    sortedData.setComparator(Comparator.comparingDouble(Produto::getValorUnitVenda));
                    break;
                case "Nome (A-Z)":
                    tabela.getSortOrder().clear();
                    colDesc.setSortType(TableColumn.SortType.ASCENDING);
                    tabela.getSortOrder().add(colDesc);
                    break;
                default: // Padrão
                    tabela.getSortOrder().clear();
                    colId.setSortType(TableColumn.SortType.ASCENDING);
                    tabela.getSortOrder().add(colId);
                    sortedData.setComparator(null); // Reseta comparadores manuais
                    break;
            }
        });

        tabela.setItems(sortedData); // Define a lista inteligente na tabela
        VBox.setVgrow(tabela, Priority.ALWAYS);

        // Rodapé com totais
        Label lblTotal = new Label();
        lblTotal.getStyleClass().add(Styles.TEXT_MUTED);

        // Criamos uma mini-função para atualizar o texto
        Runnable atualizarContador = () -> lblTotal.setText("Exibindo " + filteredData.size() + " registros");

        // 1. Atualiza quando o filtro de busca muda (ao digitar)
        filteredData.predicateProperty().addListener(o -> atualizarContador.run());

        // 2. A MÁGICA NOVA: Atualiza automaticamente quando um produto é salvo ou recarregado
        masterData.addListener((javafx.collections.ListChangeListener.Change<? extends Produto> c) -> atualizarContador.run());

        atualizarContador.run(); // Define o valor inicial assim que a tela abre

        VBox layout = new VBox(15, barraFerramentas, alertaCritico, tabela, lblTotal);
        layout.setPadding(new Insets(20, 0, 0, 0));

        return layout;
    }


    // =================================================================================
    // MODAL DE CADASTRO DE PRODUTOS (Dinâmico e Integrado ao Banco)
    // =================================================================================
    private void abrirModalCadastroProduto(ObservableList<Produto> listaAtual) {
        Stage stageModal = new Stage();
        stageModal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stageModal.setTitle("Novo Cadastro de Produto");

        // 1. Campos Básicos
        TextField txtCodigo = new TextField();
        TextField txtDescricao = new TextField();
        TextField txtCategoria = new TextField();
        TextField txtPreco = new TextField();
        aplicarMascaraMoeda(txtPreco);
        TextField txtLucro = new TextField();
        TextField txtQtd = new TextField();

        // 2. ComboBox de Fornecedores
        ComboBox<Fornecedor> cmbFornecedor = new ComboBox<>();
        cmbFornecedor.getItems().addAll(new dao.FornecedorDAO().listarTodos());
        cmbFornecedor.setPromptText("Selecione um fornecedor...");

        // 3. Campos Específicos (Declarados fora para o Botão Salvar conseguir ler)
        TextField txtValidade = new TextField();
        txtValidade.setPromptText("dd/mm/aaaa");
        aplicarMascaraData(txtValidade);
        TextField txtFabricante = new TextField();
        TextField txtGarantia = new TextField();
        txtGarantia.setPromptText("Ex: 12");

        // 4. ComboBox de Tipo e Área Dinâmica
        ComboBox<String> cmbTipo = new ComboBox<>();
        cmbTipo.getItems().addAll("Cosmético", "Eletrônico", "Perecível");
        cmbTipo.setPromptText("Selecione o tipo...");

        VBox areaDinamica = new VBox(10);
        areaDinamica.setPadding(new Insets(10));
        areaDinamica.setStyle("-fx-background-color: #f6f8fa; -fx-border-color: #d0d7de; -fx-border-radius: 4px;");

        // Listener: Mostra os campos corretos baseados no Tipo escolhido
        cmbTipo.setOnAction(e -> {
            areaDinamica.getChildren().clear();
            String selecionado = cmbTipo.getValue();

            if ("Cosmético".equals(selecionado)) {
                areaDinamica.getChildren().addAll(
                        new Label("Data de Validade:"), txtValidade,
                        new Label("Fabricante:"), txtFabricante
                );
            } else if ("Eletrônico".equals(selecionado)) {
                areaDinamica.getChildren().addAll(
                        new Label("Meses de Garantia:"), txtGarantia
                );
            } else if ("Perecível".equals(selecionado)) {
                areaDinamica.getChildren().addAll(
                        new Label("Data de Validade:"), txtValidade
                );
            }
        });

        // 5. Montando o Formulário
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(15);
        form.addRow(0, new Label("Código:"), txtCodigo, new Label("Quantidade:"), txtQtd);
        form.addRow(1, new Label("Descrição:"), txtDescricao, new Label("Categoria:"), txtCategoria);
        form.addRow(2, new Label("Fornecedor:"), cmbFornecedor);
        GridPane.setColumnSpan(cmbFornecedor, 3);
        form.addRow(3, new Label("Preço Venda:"), txtPreco, new Label("Lucro (%):"), txtLucro);
        form.addRow(4, new Label("Tipo:"), cmbTipo);

        Button btnSalvar = new Button("Salvar Produto no Banco");
        btnSalvar.getStyleClass().addAll(Styles.SUCCESS, Styles.LARGE);
        btnSalvar.setMaxWidth(Double.MAX_VALUE);

        // --- A MÁGICA: LÓGICA DE SALVAMENTO ---
        btnSalvar.setOnAction(event -> {
            try {
                // Validação de Preenchimento Básico
                if (txtCodigo.getText().isEmpty() || txtDescricao.getText().isEmpty() ||
                        txtPreco.getText().isEmpty() || txtQtd.getText().isEmpty() ||
                        cmbFornecedor.getValue() == null || cmbTipo.getValue() == null) {
                    throw new IllegalArgumentException("Por favor, preencha todos os campos obrigatórios e selecione um fornecedor e um tipo.");
                }

                // Conversão dos tipos
                int cod = Integer.parseInt(txtCodigo.getText());
                String desc = txtDescricao.getText();
                String cat = txtCategoria.getText();
                int qtd = Integer.parseInt(txtQtd.getText());
                // Substitui vírgula por ponto para não dar erro no Double
                String precoLimpo = txtPreco.getText().replaceAll("[^\\d,]", "").replace(",", ".");
                double preco = Double.parseDouble(precoLimpo);
                double lucro = txtLucro.getText().isEmpty() ? 0.0 : Double.parseDouble(txtLucro.getText().replace(",", "."));
                Fornecedor forn = cmbFornecedor.getValue();
                String tipo = cmbTipo.getValue();

                Produto novoProduto = null;
                // Formatador para transformar a String dd/MM/yyyy em java.util.Date
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                sdf.setLenient(false); // Impede datas absurdas como 32/13/2025

                // Criação do objeto por Polimorfismo
                if ("Cosmético".equals(tipo)) {
                    if (txtValidade.getText().isEmpty() || txtFabricante.getText().isEmpty()) {
                        throw new IllegalArgumentException("Cosméticos exigem Data de Validade e Fabricante.");
                    }
                    java.util.Date validade = sdf.parse(txtValidade.getText());
                    novoProduto = new Cosmetico(cod, desc, cat, qtd, preco, lucro, forn, validade, txtFabricante.getText());

                } else if ("Eletrônico".equals(tipo)) {
                    if (txtGarantia.getText().isEmpty()) {
                        throw new IllegalArgumentException("Eletrônicos exigem o tempo de garantia (em meses).");
                    }
                    int garantia = Integer.parseInt(txtGarantia.getText());
                    novoProduto = new Eletronico(cod, desc, cat, qtd, preco, lucro, forn, garantia);

                } else if ("Perecível".equals(tipo)) {
                    if (txtValidade.getText().isEmpty()) {
                        throw new IllegalArgumentException("Produtos perecíveis exigem Data de Validade.");
                    }
                    java.util.Date validade = sdf.parse(txtValidade.getText());
                    novoProduto = new ProdutoPerecivel(cod, desc, cat, qtd, preco, lucro, forn, validade);
                }

                // Salva no Banco de Dados
                dao.ProdutoDAO daoProd = new dao.ProdutoDAO();
                if (novoProduto != null) {
                    daoProd.salvar(novoProduto);
                }

                // Atualiza a tabela que está na janela principal atrás do modal
                listaAtual.setAll(daoProd.listarTodos());

                mostrarAlerta("Produto salvo com sucesso no banco de dados!");
                stageModal.close();

            } catch (NumberFormatException ex) {
                mostrarErro("Erro de Formato", "Por favor, digite apenas números válidos nos campos: Código, Quantidade, Preço, Lucro e Garantia.");
            } catch (java.text.ParseException ex) {
                mostrarErro("Erro na Data", "Por favor, introduza uma data válida no formato dd/mm/aaaa (Ex: 25/12/2026).");
            } catch (IllegalArgumentException ex) {
                mostrarErro("Aviso", ex.getMessage());
            } catch (java.sql.SQLException ex) {
                if (ex.getMessage().contains("duplicate key")) {
                    mostrarErro("Código Duplicado", "Já existe um produto com o código " + txtCodigo.getText() + " cadastrado no banco.");
                } else {
                    mostrarErro("Erro de Banco de Dados", ex.getMessage());
                }
            } catch (Exception ex) {
                mostrarErro("Erro Inesperado", ex.getMessage());
                ex.printStackTrace();
            }
        });

        VBox layout = new VBox(20, form, new Label("Detalhes Específicos:"), areaDinamica, btnSalvar);
        layout.setPadding(new Insets(30));

        Scene scene = new Scene(layout, 700, 550);
        stageModal.setScene(scene);
        stageModal.showAndWait();
    }

    // =================================================================================
    // TELA EXTRA: FRENTE DE CAIXA / VENDAS (PDV)
    // =================================================================================
    private VBox criarTelaVendas() {
        Label lblTitulo = new Label("PDV - Ponto de Venda");
        lblTitulo.getStyleClass().addAll(Styles.TITLE_1);

        // Lista que vai guardar os itens do carrinho
        ObservableList<ItemCarrinho> carrinho = FXCollections.observableArrayList();
        dao.ProdutoDAO daoProduto = new dao.ProdutoDAO();

        // --- LADO ESQUERDO: Adicionar Produto ---
        VBox painelEsquerdo = new VBox(15);
        painelEsquerdo.setPrefWidth(350);
        painelEsquerdo.setPadding(new Insets(20));
        painelEsquerdo.setStyle("-fx-background-color: #f6f8fa; -fx-border-color: #d0d7de; -fx-border-radius: 6px;");

        Label lblAdd = new Label("Adicionar Item");
        lblAdd.getStyleClass().add(Styles.TITLE_3);

        TextField txtCod = new TextField();
        txtCod.setPromptText("Digite o Cód");
        TextField txtQtd = new TextField("1"); // Padrão é 1
        txtQtd.setPromptText("Qtd");

        // Info do produto "bipado"
        Label lblInfo = new Label("Aguardando produto...");
        lblInfo.getStyleClass().add(Styles.TEXT_MUTED);
        lblInfo.setWrapText(true);

        Button btnAdd = new Button("Adicionar ao Carrinho ➕");
        btnAdd.getStyleClass().addAll(Styles.ACCENT, Styles.LARGE);
        btnAdd.setMaxWidth(Double.MAX_VALUE);

        painelEsquerdo.getChildren().addAll(lblAdd, new Label("Código do Produto:"), txtCod, new Label("Quantidade:"), txtQtd, btnAdd, lblInfo);

        // --- LADO DIREITO: Tabela (Carrinho) e Total ---
        VBox painelDireito = new VBox(15);
        HBox.setHgrow(painelDireito, Priority.ALWAYS); // Cresce para ocupar a tela

        TableView<ItemCarrinho> tabelaCart = new TableView<>();
        tabelaCart.getStyleClass().addAll(Styles.STRIPED, Styles.BORDERED);
        tabelaCart.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tabelaCart, Priority.ALWAYS);

        TableColumn<ItemCarrinho, String> colCod = new TableColumn<>("Cód");
        colCod.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCod.setMaxWidth(80);

        TableColumn<ItemCarrinho, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));

        TableColumn<ItemCarrinho, String> colQtd = new TableColumn<>("Qtd");
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colQtd.setMaxWidth(80);

        TableColumn<ItemCarrinho, String> colUn = new TableColumn<>("Vlr. Unitário");
        colUn.setCellValueFactory(new PropertyValueFactory<>("valorUnitarioFormatado"));

        TableColumn<ItemCarrinho, String> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(new PropertyValueFactory<>("subtotalFormatado"));

        tabelaCart.getColumns().addAll(colCod, colDesc, colQtd, colUn, colSub);
        tabelaCart.setItems(carrinho);

        // Rodapé com Total e Botão Finalizar
        Label lblTextoTotal = new Label("TOTAL:");
        lblTextoTotal.getStyleClass().addAll(Styles.TITLE_2, Styles.TEXT_MUTED);

        Label lblTotalValor = new Label("R$ 0,00");
        lblTotalValor.getStyleClass().add(Styles.TITLE_1);
        lblTotalValor.setStyle("-fx-font-size: 40px; -fx-text-fill: #1a7f37;"); // Verde grandão

        Button btnFinalizar = new Button("Finalizar Venda");
        btnFinalizar.getStyleClass().addAll(Styles.SUCCESS, Styles.LARGE);
        btnFinalizar.setPrefWidth(250);
        btnFinalizar.setPrefHeight(60);
        btnFinalizar.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        HBox rodapeTotal = new HBox(20, lblTextoTotal, lblTotalValor, new Region(), btnFinalizar);
        rodapeTotal.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(rodapeTotal.getChildren().get(2), Priority.ALWAYS); // Espaçador empurra o botão pra direita

        painelDireito.getChildren().addAll(tabelaCart, rodapeTotal);

        // --- LÓGICA DO CARRINHO (A MÁGICA VISUAL) ---

        // Atalho do Enter
        // Enter no campo de Código: Faz a pré-busca e depois pula para Quantidade
        txtCod.setOnAction(e -> {
            try {
                int cod = Integer.parseInt(txtCod.getText());
                // Busca rápida do produto
                Produto pBusca = daoProduto.listarTodos().stream().filter(p -> p.getCodigo() == cod).findFirst().orElse(null);

                if (pBusca != null) {
                    lblInfo.setText("Produto: " + pBusca.getDescricao() + " | Estoque: " + pBusca.getQntdDisp());
                    lblInfo.setStyle("-fx-text-fill: #0969da; -fx-font-weight: bold;"); // Azul para pré-visualização
                    txtQtd.requestFocus(); // Pula para a quantidade
                } else {
                    lblInfo.setText("❌ Produto não encontrado!");
                    lblInfo.setStyle("-fx-text-fill: red;");
                    txtCod.selectAll(); // Seleciona o texto errado para facilitar apagar
                }
            } catch (NumberFormatException ex) {
                lblInfo.setText("❌ Digite um código válido.");
                lblInfo.setStyle("-fx-text-fill: red;");
            }
        });

        // Enter no campo de Quantidade: "clica" no botão de Adicionar
        txtQtd.setOnAction(e -> btnAdd.fire());

        // Função mágica para atualizar o R$ grandão sempre que a tabela mudar
        Runnable atualizarTotal = () -> {
            double soma = carrinho.stream().mapToDouble(ItemCarrinho::getSubtotal).sum();
            lblTotalValor.setText(String.format("R$ %.2f", soma));
        };

        btnAdd.setOnAction(e -> {
            try {
                int cod = Integer.parseInt(txtCod.getText());
                int qtd = Integer.parseInt(txtQtd.getText());

                if (qtd <= 0) throw new NumberFormatException();

                Produto pEncontrado = daoProduto.listarTodos().stream().filter(p -> p.getCodigo() == cod).findFirst().orElse(null);

                if (pEncontrado == null) {
                    lblInfo.setText("❌ Produto não encontrado!");
                    lblInfo.setStyle("-fx-text-fill: red;");
                    return;
                }

                // Verifica se já tem no carrinho para somar a quantidade em vez de duplicar a linha
                ItemCarrinho itemExistente = carrinho.stream().filter(i -> i.getCodigo() == cod).findFirst().orElse(null);

                int qtdTotalDesejada = qtd + (itemExistente != null ? itemExistente.getQuantidade() : 0);

                if (qtdTotalDesejada > pEncontrado.getQntdDisp()) {
                    lblInfo.setText("⚠ Estoque insuficiente! (Disponível: " + pEncontrado.getQntdDisp() + ")");
                    lblInfo.setStyle("-fx-text-fill: red;");
                    return;
                }

                if (itemExistente != null) {
                    itemExistente.adicionarQuantidade(qtd);
                    tabelaCart.refresh(); // Força a tabela a atualizar os números visualmente
                } else {
                    carrinho.add(new ItemCarrinho(pEncontrado, qtd));
                }

                lblInfo.setText("✅ Adicionado: " + pEncontrado.getDescricao());
                lblInfo.setStyle("-fx-text-fill: green;");
                txtCod.clear();
                txtQtd.setText("1");
                txtCod.requestFocus(); // Foco volta pro código pra bipar o próximo!

                atualizarTotal.run();

            } catch (NumberFormatException ex) {
                lblInfo.setText("❌ Código e Quantidade devem ser números válidos.");
                lblInfo.setStyle("-fx-text-fill: red;");
            }
        });

        // --- AÇÃO DO BOTÃO FINALIZAR VENDA ---
        btnFinalizar.setOnAction(e -> {
            if (carrinho.isEmpty()) {
                mostrarErro("Carrinho Vazio", "Adicione produtos antes de finalizar a venda.");
                return;
            }

            // Exibe janela de confirmação para o vendedor
            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                    "Deseja finalizar a venda no valor total de " + lblTotalValor.getText() + "?",
                    ButtonType.YES, ButtonType.NO);
            confirmacao.setHeaderText("Confirmar Venda");

            confirmacao.showAndWait().ifPresent(resposta -> {
                if (resposta == ButtonType.YES) {
                    try {
                        dao.MovimentacaoDAO daoMov = new dao.MovimentacaoDAO();
                        String usuarioAtual = core.Sessao.getUsuario().getNomeCompleto();

                        // Pega a lista do JavaFX, converte para uma lista padrão do Java e envia para o DAO!
                        daoMov.registrarVenda(new java.util.ArrayList<>(carrinho), usuarioAtual);

                        mostrarAlerta("Venda finalizada com sucesso!");

                        // Limpa o PDV para o próximo cliente da fila
                        carrinho.clear();
                        lblTotalValor.setText("R$ 0,00");
                        txtCod.clear();
                        txtQtd.setText("1");
                        lblInfo.setText("Caixa livre. Aguardando próximo cliente...");
                        lblInfo.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        txtCod.requestFocus(); // Foco no código de barras!

                    } catch (java.sql.SQLException ex) {
                        mostrarErro("Venda Cancelada", ex.getMessage());
                    }
                }
            });
        });

        HBox split = new HBox(30, painelEsquerdo, painelDireito);
        VBox layout = new VBox(20, lblTitulo, split);
        layout.setPadding(new Insets(40));
        return layout;
    }

    // =================================================================================
    // TELA 4: RELATÓRIOS E LOGS (Auditoria)
    // =================================================================================
    private VBox criarTelaRelatorios() {
        Label lblTitulo = new Label("Logs de Movimentação (Auditoria)");
        lblTitulo.getStyleClass().addAll(Styles.TITLE_1);

        Label lblSub = new Label("Histórico completo de entradas, vendas e baixas de estoque.");
        lblSub.getStyleClass().add(Styles.TEXT_MUTED);

        // --- TABELA DE LOGS ---
        TableView<core.LogMovimentacao> tabelaLogs = new TableView<>();
        tabelaLogs.getStyleClass().addAll(Styles.STRIPED, Styles.BORDERED);
        tabelaLogs.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tabelaLogs, Priority.ALWAYS);

        // Colunas
        TableColumn<core.LogMovimentacao, String> colData = new TableColumn<>("Data / Hora");
        colData.setCellValueFactory(new PropertyValueFactory<>("dataHoraFormatada"));
        colData.setMaxWidth(160);

        TableColumn<core.LogMovimentacao, String> colUser = new TableColumn<>("Usuário Responsável");
        colUser.setCellValueFactory(new PropertyValueFactory<>("usuario"));

        TableColumn<core.LogMovimentacao, String> colTipo = new TableColumn<>("Ação");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoMovimentacao"));
        colTipo.setMaxWidth(100);
        // Colore a ação para facilitar a leitura visual
        colTipo.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold;");
                    switch (item) {
                        case "ENTRADA" -> setTextFill(javafx.scene.paint.Color.web("#1a7f37")); // Verde
                        case "BAIXA" -> setTextFill(javafx.scene.paint.Color.web("#cf222e")); // Vermelho
                        case "VENDA" -> setTextFill(javafx.scene.paint.Color.web("#0969da")); // Azul
                    }
                }
            }
        });

        TableColumn<core.LogMovimentacao, Integer> colCod = new TableColumn<>("Cód. Prod");
        colCod.setCellValueFactory(new PropertyValueFactory<>("codigoProduto"));
        colCod.setMaxWidth(100);

        TableColumn<core.LogMovimentacao, Integer> colQtd = new TableColumn<>("Qtd");
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colQtd.setMaxWidth(80);

        TableColumn<core.LogMovimentacao, String> colMotivo = new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));

        TableColumn<core.LogMovimentacao, String> colObs = new TableColumn<>("Observações");
        colObs.setCellValueFactory(new PropertyValueFactory<>("observacao"));

        tabelaLogs.getColumns().addAll(colData, colUser, colTipo, colCod, colQtd, colMotivo, colObs);

        // --- CARREGAMENTO DOS DADOS ---
        dao.MovimentacaoDAO daoMov = new dao.MovimentacaoDAO();
        ObservableList<core.LogMovimentacao> listaLogs = FXCollections.observableArrayList(daoMov.listarHistorico());
        tabelaLogs.setItems(listaLogs);

        // Botão para atualizar a lista sem precisar sair da tela
        Button btnAtualizar = new Button("🔄 Atualizar Histórico");
        btnAtualizar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnAtualizar.setOnAction(e -> listaLogs.setAll(daoMov.listarHistorico()));

        HBox barraFerramentas = new HBox(10, btnAtualizar);
        barraFerramentas.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(15, lblTitulo, lblSub, barraFerramentas, tabelaLogs);
        layout.setPadding(new Insets(40));
        return layout;
    }

    // =================================================================================
    // TELA UNIFICADA: ESTOQUE (Consulta + Entradas + Baixas)
    // =================================================================================
    private VBox criarTelaEstoque() {
        Label lblTitulo = new Label("Gestão de Estoque");
        lblTitulo.getStyleClass().addAll(Styles.TITLE_1);

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().addAll(Styles.DENSE);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Nossas 3 funcionalidades agora vivem lado a lado de forma super limpa!
        Tab tabConsulta = new Tab("Consultar e Gerenciar", criarAbaConsulta());
        Tab tabEntrada = new Tab("Entrada em Lote", criarFormularioEntradaLote());
        Tab tabBaixa = new Tab("Baixa de Perdas/Ajustes", criarFormularioBaixa());

        tabPane.getTabs().addAll(tabConsulta, tabEntrada, tabBaixa);

        VBox layout = new VBox(20, lblTitulo, tabPane);
        layout.setPadding(new Insets(40));
        return layout;
    }

    // =================================================================================
    // FORMULÁRIO DE BAIXA DE ESTOQUE (Com Justificativa)
    // =================================================================================
    private VBox criarFormularioBaixa() {
        // --- 1. ÁREA DE BUSCA ---
        TextField txtCod = new TextField();
        txtCod.setPromptText("Ex: 101");
        Button btnBuscar = new Button("Buscar Produto");
        btnBuscar.getStyleClass().addAll(Styles.ACCENT);

        HBox boxBusca = new HBox(10, new Label("Código do Produto:"), txtCod, btnBuscar);
        boxBusca.setAlignment(Pos.CENTER_LEFT);

        // --- 2. ÁREA DE INFORMAÇÃO DO PRODUTO (Visual) ---
        Label lblNomeProduto = new Label("Produto: (Nenhum selecionado)");
        lblNomeProduto.getStyleClass().add(Styles.TEXT_MUTED);
        Label lblEstoqueAtual = new Label("Estoque Disponível: -");
        lblEstoqueAtual.getStyleClass().add(Styles.TEXT_MUTED);

        VBox boxInfo = new VBox(5, lblNomeProduto, lblEstoqueAtual);
        boxInfo.setPadding(new Insets(10));
        boxInfo.setStyle("-fx-background-color: #f6f8fa; -fx-border-color: #d0d7de; -fx-border-radius: 4px;");

        // --- 3. ÁREA DE BAIXA (Com o Motivo que você sugeriu!) ---
        TextField txtQtd = new TextField();
        txtQtd.setPromptText("Qtd a remover");

        ComboBox<String> cmbMotivo = new ComboBox<>();
        cmbMotivo.getItems().addAll("Produto Danificado", "Vencimento (Perecível)", "Defeito / Acionamento de Garantia", "Roubo / Furto", "Ajuste de Inventário (Erro de contagem)");
        cmbMotivo.setPromptText("Selecione o motivo da baixa...");
        cmbMotivo.setPrefWidth(300);

        TextField txtObservacao = new TextField();
        txtObservacao.setPromptText("Detalhes extras (Ex: Garantia acionada pelo cliente João)");

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(15);
        form.addRow(0, new Label("Quantidade:"), txtQtd);
        form.addRow(1, new Label("Motivo:"), cmbMotivo);
        form.addRow(2, new Label("Observações:"), txtObservacao);

        Button btnSalvar = new Button("Registrar Baixa no Banco");
        btnSalvar.getStyleClass().addAll(Styles.DANGER, Styles.LARGE);
        btnSalvar.setMaxWidth(Double.MAX_VALUE);

        // =================================================================
        // LÓGICA DE BUSCA E SALVAMENTO
        // =================================================================

        // Instanciamos os DAOs
        dao.ProdutoDAO daoProduto = new dao.ProdutoDAO();
        dao.MovimentacaoDAO daoMov = new dao.MovimentacaoDAO();

        // Variável "invisível" (Array de 1 posição) para o sistema lembrar qual produto está na tela
        final Produto[] produtoSelecionado = {null};

        // AÇÃO DO BOTÃO BUSCAR
        txtCod.setOnAction(e -> btnBuscar.fire());
        btnBuscar.setOnAction(e -> {

            try {
                int codBusca = Integer.parseInt(txtCod.getText());

                // Busca o produto (reaproveitando a lista completa do DAO para agilidade)
                Produto pEncontrado = daoProduto.listarTodos().stream()
                        .filter(p -> p.getCodigo() == codBusca)
                        .findFirst().orElse(null);

                if (pEncontrado != null) {
                    produtoSelecionado[0] = pEncontrado;
                    lblNomeProduto.setText("Produto: " + pEncontrado.getDescricao());
                    lblNomeProduto.getStyleClass().remove(Styles.TEXT_MUTED); // Tira o tom cinza

                    lblEstoqueAtual.setText("Estoque Disponível: " + pEncontrado.getQntdDisp() + " un.");
                    // Alerta visual imediato se o estoque já estiver zerado
                    if(pEncontrado.getQntdDisp() <= 0) {
                        lblEstoqueAtual.setStyle("-fx-text-fill: #cf222e; -fx-font-weight: bold;"); // Vermelho
                    } else {
                        lblEstoqueAtual.setStyle("-fx-text-fill: #1a7f37; -fx-font-weight: bold;"); // Verde
                    }
                    txtQtd.requestFocus();
                } else {
                    produtoSelecionado[0] = null;
                    lblNomeProduto.setText("Produto não encontrado no sistema!");
                    lblNomeProduto.setStyle("-fx-text-fill: #cf222e;");
                    lblEstoqueAtual.setText("Estoque Disponível: -");
                }
            } catch (NumberFormatException ex) {
                mostrarErro("Formato Inválido", "Por favor, digite um código numérico válido.");
            }
        });

        // AÇÃO DO BOTÃO REGISTRAR BAIXA
        btnSalvar.setOnAction(e -> {
            // Travas de segurança visuais
            if (produtoSelecionado[0] == null) {
                mostrarErro("Atenção", "Primeiro, busque e selecione um produto válido clicando em 'Buscar'.");
                return;
            }
            if (txtQtd.getText().isEmpty() || cmbMotivo.getValue() == null) {
                mostrarErro("Atenção", "Os campos 'Quantidade' e 'Motivo' são obrigatórios!");
                return;
            }

            try {
                int qtdRemover = Integer.parseInt(txtQtd.getText());
                if (qtdRemover <= 0) throw new NumberFormatException();

                String motivo = cmbMotivo.getValue();
                String obs = txtObservacao.getText();

                // Pegamos magicamente o nome de quem logou no sistema (História 11)
                String usuarioResponsavel = core.Sessao.getUsuario().getNomeCompleto();

                // Dispara a Transação de Segurança no Banco de Dados
                daoMov.registrarBaixa(produtoSelecionado[0].getCodigo(), qtdRemover, motivo, obs, usuarioResponsavel);

                mostrarAlerta("Baixa de estoque registrada com sucesso no banco de dados!");

                // Limpa a tela para a próxima operação
                txtCod.clear();
                txtQtd.clear();
                cmbMotivo.setValue(null);
                txtObservacao.clear();
                lblNomeProduto.setText("Produto: (Nenhum selecionado)");
                lblNomeProduto.setStyle("");
                lblEstoqueAtual.setText("Estoque Disponível: -");
                lblEstoqueAtual.setStyle("");
                produtoSelecionado[0] = null;

            } catch (NumberFormatException ex) {
                mostrarErro("Erro de Formato", "A quantidade a remover deve ser um número inteiro e maior que zero.");
            } catch (java.sql.SQLException ex) {
                // Aqui o banco avisa se tentarmos tirar mais do que tem (a trava G7-53!)
                mostrarErro("Operação Bloqueada pelo Banco", ex.getMessage());
            }
        });

        VBox layout = new VBox(20, boxBusca, boxInfo, form, btnSalvar);
        layout.setPadding(new Insets(20, 0, 0, 0));
        layout.setMaxWidth(600);

        return layout;
    }


    private VBox criarFormularioEntradaLote() {
        // --- 1. LISTA TEMPORÁRIA (Subtarefa G7-61) ---
        // Reaproveitamos a classe ItemCarrinho para guardar o "Produto + Qtd" na memória
        ObservableList<core.ItemCarrinho> listaLote = FXCollections.observableArrayList();
        dao.ProdutoDAO daoProduto = new dao.ProdutoDAO();

        // --- 2. ÁREA DE INPUT ---
        TextField txtCod = new TextField();
        txtCod.setPromptText("Cód. Produto");
        TextField txtQtd = new TextField();
        txtQtd.setPromptText("Qtd Recebida");

        Label lblInfo = new Label("Aguardando produto...");
        lblInfo.getStyleClass().add(Styles.TEXT_MUTED);

        Button btnAdd = new Button("Adicionar à Lista");
        btnAdd.getStyleClass().addAll(Styles.ACCENT);

        // Mantemos o botão de Novo Produto caso chegue algo inédito na nota fiscal
        Button btnNovo = new Button("Cadastrar Novo Produto");
        btnNovo.getStyleClass().addAll(Styles.WARNING, Styles.BUTTON_OUTLINED);
        // Deixamos a ação dele vazia por enquanto, podemos ligar à tela de cadastro depois

        HBox formAdd = new HBox(10, txtCod, txtQtd, btnAdd, lblInfo, new Region(), btnNovo);
        formAdd.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(formAdd.getChildren().get(5), Priority.ALWAYS); // Empurra o btnNovo pro canto direito

        // --- 3. TABELA (Subtarefa G7-62) ---
        TableView<core.ItemCarrinho> tabelaLote = new TableView<>();
        tabelaLote.getStyleClass().addAll(Styles.STRIPED, Styles.BORDERED);
        tabelaLote.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabelaLote.setEditable(true); // <--- MÁGICA 1: Libera a tabela para edição!
        VBox.setVgrow(tabelaLote, Priority.ALWAYS);

        TableColumn<core.ItemCarrinho, String> colCod = new TableColumn<>("Cód");
        colCod.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCod.setMaxWidth(100);

        TableColumn<core.ItemCarrinho, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));

        // Coluna de Quantidade Editável
        TableColumn<core.ItemCarrinho, Integer> colQtd = new TableColumn<>("Qtd a Adicionar (2 cliques para editar)");
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colQtd.setMaxWidth(250);

        // Ensina a coluna a virar uma "caixinha de texto" quando clicada
        colQtd.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));

        // O que acontece quando o usuário aperta Enter após editar:
        colQtd.setOnEditCommit(event -> {
            core.ItemCarrinho item = event.getRowValue();
            int novaQtd = event.getNewValue();

            if (novaQtd > 0) {
                item.setQuantidade(novaQtd); // Salva a nova quantidade
            } else {
                tabelaLote.refresh(); // Desfaz visualmente se ele digitar 0
                mostrarErro("Atenção", "A quantidade deve ser maior que zero. Para remover o item, clique no botão ❌.");
            }
        });

        // <--- MÁGICA 2: COLUNA COM O BOTÃO DE EXCLUIR --->
        TableColumn<core.ItemCarrinho, Void> colAcao = new TableColumn<>("Remover");
        colAcao.setMaxWidth(100);
        colAcao.setCellFactory(param -> new TableCell<>() {
            private final Button btnExcluir = new Button("❌");

            {
                // Deixa o botão com cara de "perigo" e sem fundo
                btnExcluir.getStyleClass().addAll(Styles.DANGER, Styles.FLAT);
                btnExcluir.setOnAction(event -> {
                    core.ItemCarrinho item = getTableView().getItems().get(getIndex());
                    listaLote.remove(item); // Remove da lista instantaneamente!
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnExcluir);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        tabelaLote.getColumns().addAll(colCod, colDesc, colQtd, colAcao);
        tabelaLote.setItems(listaLote);

        Button btnSalvarLote = new Button("Processar Lote de Entrada");
        btnSalvarLote.getStyleClass().addAll(Styles.SUCCESS, Styles.LARGE);
        btnSalvarLote.setMaxWidth(Double.MAX_VALUE);

        // --- 4. LÓGICA DE ADICIONAR NA LISTA ---

        // Atalhos de UX (Enter para pular campo e adicionar)
        txtCod.setOnAction(e -> txtQtd.requestFocus());
        txtQtd.setOnAction(e -> btnAdd.fire());

        btnAdd.setOnAction(e -> {
            try {
                int cod = Integer.parseInt(txtCod.getText());
                int qtd = Integer.parseInt(txtQtd.getText());

                if (qtd <= 0) throw new NumberFormatException();

                // Busca o produto no banco
                core.Produto pEncontrado = daoProduto.listarTodos().stream()
                        .filter(p -> p.getCodigo() == cod)
                        .findFirst().orElse(null);

                if (pEncontrado == null) {
                    lblInfo.setText("❌ Produto não encontrado!");
                    lblInfo.setStyle("-fx-text-fill: red;");
                    txtCod.requestFocus();
                    txtCod.selectAll();
                    return;
                }

                // Verifica se já está na lista para apenas somar (evita linhas duplicadas)
                core.ItemCarrinho itemExistente = listaLote.stream()
                        .filter(i -> i.getCodigo() == cod)
                        .findFirst().orElse(null);

                if (itemExistente != null) {
                    itemExistente.adicionarQuantidade(qtd);
                    tabelaLote.refresh(); // Atualiza a tela
                } else {
                    listaLote.add(new core.ItemCarrinho(pEncontrado, qtd));
                }

                lblInfo.setText("✅ " + pEncontrado.getDescricao() + " (" + qtd + "x) na lista.");
                lblInfo.setStyle("-fx-text-fill: green;");
                txtCod.clear();
                txtQtd.clear();
                txtCod.requestFocus(); // Volta o cursor para o próximo bipe!

            } catch (NumberFormatException ex) {
                lblInfo.setText("❌ Código e Quantidade inválidos.");
                lblInfo.setStyle("-fx-text-fill: red;");
                txtCod.requestFocus();
                txtCod.selectAll();
            }
        });

        // --- 5. AÇÃO DO BOTÃO SALVAR LOTE NO BANCO ---
        btnSalvarLote.setOnAction(e -> {
            if (listaLote.isEmpty()) {
                mostrarErro("Lista Vazia", "Adicione pelo menos um produto na lista antes de processar a entrada.");
                return;
            }

            // Pede confirmação para evitar cliques acidentais
            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                    "Deseja confirmar a entrada destes " + listaLote.size() + " itens no estoque?",
                    ButtonType.YES, ButtonType.NO);
            confirmacao.setHeaderText("Confirmar Entrada em Lote");

            confirmacao.showAndWait().ifPresent(resposta -> {
                if (resposta == ButtonType.YES) {
                    try {
                        dao.MovimentacaoDAO daoMov = new dao.MovimentacaoDAO();
                        String usuarioAtual = core.Sessao.getUsuario().getNomeCompleto();

                        // Envia a lista toda para o banco
                        daoMov.registrarEntradaLote(new java.util.ArrayList<>(listaLote), usuarioAtual);

                        mostrarAlerta("Lote processado e estoque atualizado com sucesso!");

                        // Limpa a tela para a próxima carreta de produtos
                        listaLote.clear();
                        txtCod.clear();
                        txtQtd.clear();
                        lblInfo.setText("Lote finalizado. Aguardando novos itens...");
                        lblInfo.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        txtCod.requestFocus();

                    } catch (java.sql.SQLException ex) {
                        mostrarErro("Erro ao processar lote no banco", ex.getMessage());
                    }
                }
            });
        });
        VBox layout = new VBox(15, new Label("Bipe ou digite os itens recebidos na nota fiscal:"), formAdd, tabelaLote, btnSalvarLote);
        layout.setPadding(new Insets(20, 0, 0, 0));
        return layout;
    }

    // =================================================================================
    // TELA 3: FORNECEDORES (Nova Versão - Tabela Cheia)
    // =================================================================================
    private VBox criarTelaFornecedores() {
        Label lblTitulo = new Label("Fornecedores Cadastrados");
        lblTitulo.getStyleClass().addAll(Styles.TITLE_1);

        // --- TABELA DE CONSULTA ---
        TableView<Fornecedor> tabela = new TableView<>();
        tabela.getStyleClass().add(Styles.STRIPED);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tabela, Priority.ALWAYS); // Faz a tabela crescer e preencher o espaço

        TableColumn<Fornecedor, String> colNome = new TableColumn<>("Razão Social");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Fornecedor, String> colCnpj = new TableColumn<>("CNPJ/CPF");
        colCnpj.setCellValueFactory(new PropertyValueFactory<>("cnpjCpf"));
        colCnpj.setMaxWidth(200);

        TableColumn<Fornecedor, String> colContato = new TableColumn<>("Contato");
        colContato.setCellValueFactory(new PropertyValueFactory<>("contato"));
        colContato.setMaxWidth(250);

        tabela.getColumns().addAll(colNome, colCnpj, colContato);

        // Carrega dados
        dao.FornecedorDAO fornecedorDAO = new dao.FornecedorDAO();
        ObservableList<Fornecedor> listaFornecedores = FXCollections.observableArrayList(fornecedorDAO.listarTodos());
        tabela.setItems(listaFornecedores);

        // --- BOTÕES DE AÇÃO ---

        // NOVO BOTÃO DE ATUALIZAR
        Button btnAtualizar = new Button("Atualizar Lista");
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.LARGE);
        btnAtualizar.setOnAction(e -> {
            // Vai no banco, busca tudo de novo e atualiza a lista da tela na mesma hora!
            listaFornecedores.setAll(fornecedorDAO.listarTodos());
        });

        Button btnNovo = new Button("Cadastrar Fornecedor");
        btnNovo.getStyleClass().addAll(Styles.SUCCESS, Styles.LARGE);
        // Chama a nossa nova janela (modal), passando a lista para ela se auto-atualizar
        btnNovo.setOnAction(e -> abrirModalCadastroFornecedor(listaFornecedores));

        Button btnExcluir = new Button("Excluir Selecionado");
        btnExcluir.getStyleClass().addAll(Styles.DANGER, Styles.LARGE);
        btnExcluir.setOnAction(e -> {
            Fornecedor selecionado = tabela.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                mostrarErro("Atenção", "Selecione um fornecedor na tabela clicando nele primeiro.");
                return;
            }

            // Confirmação de segurança
            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                    "Tem certeza que deseja excluir o fornecedor '" + selecionado.getNome() + "'?",
                    ButtonType.YES, ButtonType.NO);
            confirmacao.setHeaderText("Confirmação de Exclusão");

            confirmacao.showAndWait().ifPresent(resposta -> {
                if (resposta == ButtonType.YES) {
                    try {
                        fornecedorDAO.excluir(selecionado.getId());
                        listaFornecedores.remove(selecionado); // Remove da tela
                        mostrarAlerta("Fornecedor excluído com sucesso!");
                    } catch (Exception ex) {
                        // Se o fornecedor estiver associado a algum produto, o PostgreSQL bloqueia a exclusão
                        mostrarErro("Ação Bloqueada", "Não é possível excluir este fornecedor pois existem produtos vinculados a ele no sistema.");
                    }
                }
            });
        });

        // Colocamos o btnAtualizar ao lado do btnExcluir
        HBox barraBotoes = new HBox(15, btnAtualizar, btnExcluir, new Region(), btnNovo);
        HBox.setHgrow(barraBotoes.getChildren().get(2), Priority.ALWAYS); // O espaçador agora é o 3º item (índice 2)
        barraBotoes.setAlignment(Pos.CENTER);

        VBox layout = new VBox(20, lblTitulo, tabela, barraBotoes);
        layout.setPadding(new Insets(40));
        return layout;
    }

    // =================================================================================
    // MODAL DE CADASTRO DE FORNECEDORES
    // =================================================================================
    private void abrirModalCadastroFornecedor(ObservableList<Fornecedor> listaAtual) {
        Stage stageModal = new Stage();
        stageModal.initModality(javafx.stage.Modality.APPLICATION_MODAL); // Trava a tela de trás
        stageModal.setTitle("Novo Cadastro de Fornecedor");

        TextField txtCnpj = new TextField();
        TextField txtNome = new TextField();
        TextField txtContato = new TextField();

        // Nossas máscaras mágicas continuam funcionando aqui!
        aplicarMascaraCnpjCpf(txtCnpj);
        aplicarMascaraContato(txtContato);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(15);
        form.addRow(0, new Label("CNPJ/CPF:"), txtCnpj);
        form.addRow(1, new Label("Razão Social:"), txtNome);
        form.addRow(2, new Label("Contato (Tel/Email):"), txtContato);

        Button btnSalvar = new Button("Salvar Cadastro");
        btnSalvar.getStyleClass().addAll(Styles.SUCCESS);
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        GridPane.setColumnSpan(btnSalvar, 2);
        form.addRow(4, btnSalvar);

        dao.FornecedorDAO fornecedorDAO = new dao.FornecedorDAO();

        btnSalvar.setOnAction(e -> {
            try {
                if (txtNome.getText().isEmpty() || txtCnpj.getText().isEmpty()) {
                    throw new IllegalArgumentException("Os campos 'Razão Social' e 'CNPJ/CPF' são obrigatórios.");
                }

                Fornecedor f = new Fornecedor();
                f.setNome(txtNome.getText());
                f.setCnpjCpf(txtCnpj.getText());
                f.setContato(txtContato.getText());

                fornecedorDAO.salvar(f);

                // Recarrega a tabela da tela de trás magicamente!
                listaAtual.setAll(fornecedorDAO.listarTodos());

                mostrarAlerta("Fornecedor cadastrado com sucesso!");
                stageModal.close(); // Fecha a janelinha automaticamente

            } catch (IllegalArgumentException ex) {
                mostrarErro("Atenção", ex.getMessage());
            } catch (java.sql.SQLException ex) {
                if (ex.getMessage().contains("duplicate key")) {
                    mostrarErro("Erro", "Este CNPJ/CPF já está cadastrado no sistema.");
                } else {
                    mostrarErro("Erro", "Falha de banco: " + ex.getMessage());
                }
            }
        });

        VBox layout = new VBox(20, form);
        layout.setPadding(new Insets(30));

        Scene scene = new Scene(layout, 380, 250);
        stageModal.setScene(scene);
        stageModal.setResizable(false);
        stageModal.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // =================================================================================
    // NAVEGAÇÃO E SESSÃO
    // =================================================================================

    private void onLogout() {
        try {
            Sessao.logout();
            LoginFX login = new LoginFX();
            Stage stageLogin = new Stage();
            login.start(stageLogin);
            Stage stageAtual = (Stage) rootLayout.getScene().getWindow();
            stageAtual.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =================================================================================
    // AJUSTES
    // =================================================================================

    public static String formatarValorAbreviado(double valor) {
        if (valor < 1_000) {
            return String.format("R$ %.0f", valor);
        } else if (valor < 1_000_000) {
            return String.format("R$ %.1f mil", valor / 1_000);
        } else {
            return String.format("R$ %.1f milhão", valor / 1_000_000);
        }
    }

    // =================================================================================
    // MÁSCARAS DE FORMATAÇÃO (TEMPO REAL)
    // =================================================================================

    private void aplicarMascaraCnpjCpf(TextField textField) {
        final boolean[] isUpdating = {false};

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdating[0]) return;
            isUpdating[0] = true;

            String apenasNumeros = newValue.replaceAll("[^\\d]", "");

            if (apenasNumeros.length() > 14) {
                apenasNumeros = apenasNumeros.substring(0, 14);
            }

            StringBuilder sb = new StringBuilder(apenasNumeros);

            if (apenasNumeros.length() <= 11) {
                if (sb.length() > 9) sb.insert(9, "-");
                if (sb.length() > 6) sb.insert(6, ".");
                if (sb.length() > 3) sb.insert(3, ".");
            } else {
                if (sb.length() > 12) sb.insert(12, "-");
                if (sb.length() > 8) sb.insert(8, "/");
                if (sb.length() > 5) sb.insert(5, ".");
                if (sb.length() > 2) sb.insert(2, ".");
            }

            String mascara = sb.toString();

            // Colocamos TUDO (texto, cursor e destrava) na fila de execução do JavaFX
            javafx.application.Platform.runLater(() -> {
                textField.setText(mascara);
                textField.positionCaret(mascara.length());
                isUpdating[0] = false;
            });
        });
    }

    private void aplicarMascaraContato(TextField textField) {
        final boolean[] isUpdating = {false};

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdating[0]) return;

            if (newValue.matches(".*[a-zA-Z@].*")) return;

            isUpdating[0] = true;

            String apenasNumeros = newValue.replaceAll("[^\\d]", "");

            if (apenasNumeros.length() > 11) {
                apenasNumeros = apenasNumeros.substring(0, 11);
            }

            StringBuilder sb = new StringBuilder(apenasNumeros);

            if (apenasNumeros.length() > 2) {
                sb.insert(2, ") ").insert(0, "(");
            }

            if (apenasNumeros.length() == 11) {
                if (sb.length() > 10) sb.insert(10, "-");
            } else if (apenasNumeros.length() > 6) {
                if (sb.length() > 9) sb.insert(9, "-");
            }

            String mascara = sb.toString();

            // Colocamos TUDO na fila de execução do JavaFX
            javafx.application.Platform.runLater(() -> {
                textField.setText(mascara);
                textField.positionCaret(mascara.length());
                isUpdating[0] = false;
            });
        });
    }

    private void aplicarMascaraData(TextField textField) {
        final boolean[] isUpdating = {false};

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdating[0]) return;
            isUpdating[0] = true;

            // Remove tudo o que não for número
            String apenasNumeros = newValue.replaceAll("[^\\d]", "");

            // Trava: Limita a 8 dígitos (ddMMyyyy)
            if (apenasNumeros.length() > 8) {
                apenasNumeros = apenasNumeros.substring(0, 8);
            }

            StringBuilder sb = new StringBuilder(apenasNumeros);

            // Insere as barras nas posições corretas
            if (sb.length() > 4) {
                sb.insert(4, "/"); // Barra do ano
            }
            if (sb.length() > 2) {
                sb.insert(2, "/"); // Barra do mês
            }

            String mascara = sb.toString();

            javafx.application.Platform.runLater(() -> {
                textField.setText(mascara);
                textField.positionCaret(mascara.length());
                isUpdating[0] = false;
            });
        });
    }

    private void aplicarMascaraMoeda(TextField textField) {
        final boolean[] isUpdating = {false};

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdating[0]) return;
            isUpdating[0] = true;

            // Deixa apenas os números digitados
            String apenasNumeros = newValue.replaceAll("[^\\d]", "");

            if (apenasNumeros.isEmpty()) {
                javafx.application.Platform.runLater(() -> {
                    textField.setText("");
                    isUpdating[0] = false;
                });
                return;
            }

            // Divide por 100 para criar os centavos matematicamente
            double valor = Double.parseDouble(apenasNumeros) / 100;

            // Usa o formatador nativo do Java para a moeda do Brasil (R$ 1.500,00)
            java.text.NumberFormat formatoMoeda = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.of("pt", "BR"));
            String textoFormatado = formatoMoeda.format(valor);

            // Joga para a tela
            javafx.application.Platform.runLater(() -> {
                textField.setText(textoFormatado);
                textField.positionCaret(textoFormatado.length()); // Mantém o cursor no fim
                isUpdating[0] = false;
            });
        });
    }

    // =================================================================================
    // MÉTODOS UTILITÁRIOS (ALERTAS)
    // =================================================================================

    private void mostrarAlerta(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sistema GEST");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private void mostrarErro(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}