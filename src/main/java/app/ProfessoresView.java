package app;

import core.Professor;
import core.Turma;
import dao.ProfessorDAO;
import dao.SupabaseAuthDAO;
import dao.TurmaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class ProfessoresView {

    private BorderPane view;
    private TableView<Professor> tabela;
    private TableView<Turma> tabelaTurmas;
    private ProfessorDAO professorDAO;
    private TurmaDAO turmaDAO;
    private MainFX mainApp;
    private final ObservableList<Professor> dados = FXCollections.observableArrayList();

    private Professor professorSelecionado; // Variável de controle (Novo ou Editar)

    private VBox painelDetalhe;
    private Label lblNomeProf;
    private TextField txtNome, txtEmail;
    private PasswordField txtSenha;
    private Label lblSecaoTurmas;
    private Button btnSalvar; // Movido para escopo da classe

    public ProfessoresView(MainFX mainApp) {
        this.mainApp = mainApp;
        professorDAO = new ProfessorDAO();
        turmaDAO = new TurmaDAO();
        construirInterface();
        carregarDados();
    }

    private void construirInterface() {
        view = new BorderPane();

        Label lblTitulo = new Label("Professores");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("Buscar professor...");
        txtBusca.setPrefWidth(220);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregarDados());

        Button btnNovo = new Button("+ Cadastrar Professor");
        btnNovo.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNovo.setOnAction(e -> abrirFormNovo());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, lblTitulo, spacer, txtBusca, btnAtualizar, btnNovo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 16, 20));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhum professor cadastrado."));
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<Professor, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        // Adicionadas as colunas de Email e Senha. A de ID Auth foi removida.
        TableColumn<Professor, String> colEmail = new TableColumn<>("E-mail");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Professor, String> colSenha = new TableColumn<>("Senha");
        colSenha.setCellValueFactory(new PropertyValueFactory<>("senha"));

        tabela.getColumns().addAll(colNome, colEmail, colSenha);

        FilteredList<Professor> filtrado = new FilteredList<>(dados, p -> true);
        txtBusca.textProperty().addListener((obs, old, term) -> {
            String f = term == null ? "" : term.trim().toLowerCase();
            filtrado.setPredicate(p -> f.isBlank()
                    || p.getNome().toLowerCase().contains(f)
                    || (p.getEmail() != null && p.getEmail().toLowerCase().contains(f)));
        });
        SortedList<Professor> ordenado = new SortedList<>(filtrado);
        ordenado.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenado);

        // Ação para abrir edição ao clicar em uma linha
        tabela.setRowFactory(tv -> {
            TableRow<Professor> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() >= 1 && (!row.isEmpty())) {
                    abrirDetalheProf(row.getItem());
                }
            });
            return row;
        });

        // ── Painel Detalhe ────────────────────────────────────────
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

        lblNomeProf = new Label("Novo Professor");
        lblNomeProf.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Label lblFormTitle = new Label("Dados de Acesso");
        lblFormTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        txtNome = new TextField(); txtNome.setPromptText("Nome completo");
        txtEmail = new TextField(); txtEmail.setPromptText("E-mail");
        txtSenha = new PasswordField(); txtSenha.setPromptText("Senha (mín. 6 caracteres)");

        btnSalvar = new Button("Cadastrar Professor");
        btnSalvar.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> cadastrar());

        lblSecaoTurmas = new Label("Turmas deste Professor");
        lblSecaoTurmas.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");
        lblSecaoTurmas.setVisible(false); lblSecaoTurmas.setManaged(false);

        tabelaTurmas = new TableView<>();
        tabelaTurmas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelaTurmas.setPlaceholder(new Label("Sem turmas atribuídas."));
        tabelaTurmas.setPrefHeight(200);
        tabelaTurmas.setVisible(false); tabelaTurmas.setManaged(false);

        TableColumn<Turma, String> colTNome = new TableColumn<>("Turma");
        colTNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        TableColumn<Turma, String> colTEscola = new TableColumn<>("Escola");
        colTEscola.setCellValueFactory(new PropertyValueFactory<>("escolaNome"));
        tabelaTurmas.getColumns().addAll(colTNome, colTEscola);

        tabelaTurmas.setRowFactory(tv -> {
            TableRow<Turma> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty())
                    mainApp.abrirDashboardTurma(row.getItem());
            });
            return row;
        });

        painelDetalhe.getChildren().addAll(
                hdrD, lblNomeProf, new Separator(),
                lblFormTitle, txtNome, txtEmail, txtSenha, btnSalvar,
                new Separator(), lblSecaoTurmas, tabelaTurmas
        );

        VBox conteudoEsq = new VBox(header, tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        HBox mainLayout = new HBox(conteudoEsq, painelDetalhe);
        HBox.setHgrow(conteudoEsq, Priority.ALWAYS);
        view.setCenter(mainLayout);
    }

    private void abrirFormNovo() {
        tabela.getSelectionModel().clearSelection();
        professorSelecionado = null; // Zera a seleção
        lblNomeProf.setText("Novo Professor");
        btnSalvar.setText("Cadastrar Professor");
        txtNome.clear(); txtEmail.clear(); txtSenha.clear();
        txtEmail.setDisable(false); // Libera o email para cadastro
        setSecaoTurmasVisivel(false);
        mostrarDetalhe();
    }

    private void abrirDetalheProf(Professor prof) {
        professorSelecionado = prof; // Guarda qual professor estamos editando
        lblNomeProf.setText("Editar: " + prof.getNome());
        btnSalvar.setText("Salvar Alterações");

        txtNome.setText(prof.getNome());
        txtEmail.setText(prof.getEmail());
        txtSenha.setText(prof.getSenha());
        txtEmail.setDisable(true); // Bloqueia o email na edição para evitar conflito com o Auth (opcional)

        setSecaoTurmasVisivel(true);
        tabelaTurmas.getItems().setAll(turmaDAO.listarPorProfessor(prof.getId()));
        mostrarDetalhe();
    }

    private void setSecaoTurmasVisivel(boolean v) {
        lblSecaoTurmas.setVisible(v); lblSecaoTurmas.setManaged(v);
        tabelaTurmas.setVisible(v); tabelaTurmas.setManaged(v);
    }

    private void cadastrar() {
        String nome = txtNome.getText().trim();
        String email = txtEmail.getText().trim();
        String senha = txtSenha.getText();

        if (nome.isBlank() || email.isBlank() || senha.length() < 6) {
            new Alert(Alert.AlertType.WARNING, "Preencha todos os campos. Senha mínima: 6 caracteres.").showAndWait();
            return;
        }

        // Verifica se é um NOVO cadastro
        if (professorSelecionado == null) {
            String novoId = SupabaseAuthDAO.criarUsuarioAuth(email, senha);
            if (novoId != null && professorDAO.inserir(novoId, nome, email, senha)) {
                txtNome.clear(); txtEmail.clear(); txtSenha.clear();
                carregarDados(); fecharDetalhe();
            } else {
                new Alert(Alert.AlertType.ERROR, "Erro: e-mail já em uso ou falha de rede.").showAndWait();
            }
        }
        // Caso contrário, é uma EDIÇÃO
        else {
            if (professorDAO.atualizar(professorSelecionado.getId(), nome, email, senha)) {
                txtNome.clear(); txtEmail.clear(); txtSenha.clear();
                carregarDados(); fecharDetalhe();
            } else {
                new Alert(Alert.AlertType.ERROR, "Erro ao atualizar os dados.").showAndWait();
            }
        }
    }

    private void mostrarDetalhe() { painelDetalhe.setVisible(true); painelDetalhe.setManaged(true); }

    private void fecharDetalhe() {
        painelDetalhe.setVisible(false); painelDetalhe.setManaged(false);
        tabela.getSelectionModel().clearSelection();
        professorSelecionado = null; // Limpa ao fechar
    }

    private void carregarDados() { dados.setAll(professorDAO.listarTodos()); }

    public BorderPane getView() { return view; }
}