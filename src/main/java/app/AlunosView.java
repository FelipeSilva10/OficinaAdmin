package app;

import core.Aluno;
import core.Escola;
import core.Turma;
import dao.AlunoDAO;
import dao.EscolasDAO;
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

public class AlunosView {

    private BorderPane view;
    private TableView<Aluno> tabela;
    private AlunoDAO alunoDAO;
    private EscolasDAO escolasDAO;
    private TurmaDAO turmaDAO;
    private MainFX mainApp;
    private final ObservableList<Aluno> dados = FXCollections.observableArrayList();

    private Aluno alunoSelecionado;

    private VBox painelDetalhe;
    private Label lblNomeAluno;
    private TextField txtNome, txtEmail;
    private PasswordField txtSenha;
    private ComboBox<Escola> cbEscola;
    private ComboBox<Turma> cbTurma;
    private Label lblDetalheInfo;
    private Button btnSalvar;
    private Button btnVerTurma;

    public AlunosView(MainFX mainApp) {
        this.mainApp = mainApp;
        alunoDAO = new AlunoDAO();
        escolasDAO = new EscolasDAO();
        turmaDAO = new TurmaDAO();
        construirInterface();
        carregarDados();
    }

    private void construirInterface() {
        view = new BorderPane();

        Label lblTitulo = new Label("Alunos");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        // PATCH: subtítulo de escopo para professor
        Label lblEscopo = new Label("Exibindo apenas alunos das suas turmas");
        lblEscopo.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");
        lblEscopo.setVisible(!mainApp.isAdmin());
        lblEscopo.setManaged(!mainApp.isAdmin());

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("Buscar aluno/escola/turma...");
        txtBusca.setPrefWidth(260);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregarDados());

        // PATCH: cadastrar aluno apenas para admin
        Button btnNovo = new Button("+ Cadastrar Aluno");
        btnNovo.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNovo.setOnAction(e -> abrirFormNovo());
        btnNovo.setVisible(mainApp.isAdmin());
        btnNovo.setManaged(mainApp.isAdmin());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox tituloBox = new VBox(2, lblTitulo, lblEscopo);
        HBox header = new HBox(12, tituloBox, spacer, txtBusca, btnAtualizar, btnNovo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 16, 20));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhum aluno cadastrado."));
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<Aluno, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Aluno, String> colEmail = new TableColumn<>("E-mail");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Aluno, String> colSenha = new TableColumn<>("Senha");
        colSenha.setCellValueFactory(new PropertyValueFactory<>("senha"));
        // PATCH: professor não vê senha
        colSenha.setVisible(mainApp.isAdmin());

        TableColumn<Aluno, String> colEscola = new TableColumn<>("Escola");
        colEscola.setCellValueFactory(new PropertyValueFactory<>("escolaNome"));

        TableColumn<Aluno, String> colTurma = new TableColumn<>("Turma");
        colTurma.setCellValueFactory(new PropertyValueFactory<>("turmaNome"));

        tabela.getColumns().addAll(colNome, colEmail, colSenha, colEscola, colTurma);

        FilteredList<Aluno> filtrado = new FilteredList<>(dados, a -> true);
        txtBusca.textProperty().addListener((obs, old, term) -> {
            String f = term == null ? "" : term.trim().toLowerCase();
            filtrado.setPredicate(a -> f.isBlank()
                    || a.getNome().toLowerCase().contains(f)
                    || (a.getEmail() != null && a.getEmail().toLowerCase().contains(f))
                    || a.getEscolaNome().toLowerCase().contains(f)
                    || a.getTurmaNome().toLowerCase().contains(f));
        });
        SortedList<Aluno> ordenado = new SortedList<>(filtrado);
        ordenado.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenado);

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, old, a) -> {
            if (a != null) {
                abrirDetalheAluno(a);
                btnVerTurma.setVisible(true);
                btnVerTurma.setManaged(true);
                lblDetalheInfo.setVisible(true);
                lblDetalheInfo.setManaged(true);
                btnVerTurma.setOnAction(ev -> mainApp.abrirTurmas(null));
            } else {
                btnVerTurma.setVisible(false);
                btnVerTurma.setManaged(false);
                lblDetalheInfo.setVisible(false);
                lblDetalheInfo.setManaged(false);
            }
        });

        tabela.setRowFactory(tv -> {
            TableRow<Aluno> row = new TableRow<>();

            // PATCH: menu de excluir apenas para admin
            if (mainApp.isAdmin()) {
                ContextMenu cm = new ContextMenu();
                MenuItem miDel = new MenuItem("Excluir Aluno");
                miDel.setStyle("-fx-text-fill: red;");
                miDel.setOnAction(e -> {
                    Aluno a = row.getItem();
                    if (a != null) {
                        if (alunoDAO.excluir(a.getId())) {
                            mainApp.mostrarAviso("Aluno removido com sucesso!", false);
                            carregarDados();
                            fecharDetalhe();
                        } else {
                            mainApp.mostrarAviso("Erro ao excluir aluno.", true);
                        }
                    }
                });
                cm.getItems().add(miDel);
                row.emptyProperty().addListener((obs, w, n) -> row.setContextMenu(n ? null : cm));
            }

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

        lblNomeAluno = new Label("Novo Aluno");
        lblNomeAluno.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        lblDetalheInfo = new Label();
        lblDetalheInfo.setStyle("-fx-text-fill: #718096; -fx-font-size: 13px;");
        lblDetalheInfo.setWrapText(true);
        lblDetalheInfo.setVisible(false);
        lblDetalheInfo.setManaged(false);

        Label lblFormTitle = new Label("Dados de Acesso e Matrícula");
        lblFormTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        txtNome = new TextField();
        txtNome.setPromptText("Nome completo");
        // PATCH: professor não edita
        txtNome.setEditable(mainApp.isAdmin());

        txtEmail = new TextField();
        txtEmail.setPromptText("E-mail");
        // E-mail começa não editável; abrirFormNovo() habilita para novos cadastros.
        // Na edição permanece bloqueado (e-mail não pode ser alterado).
        txtEmail.setEditable(false);

        txtSenha = new PasswordField();
        txtSenha.setPromptText("Senha (mín. 6 caracteres)");
        // PATCH: professor não vê/edita senha
        txtSenha.setEditable(mainApp.isAdmin());
        txtSenha.setVisible(mainApp.isAdmin());
        txtSenha.setManaged(mainApp.isAdmin());

        // Label de senha também oculta para professor
        Label lblSenha = new Label("Senha:");
        lblSenha.setVisible(mainApp.isAdmin());
        lblSenha.setManaged(mainApp.isAdmin());

        cbEscola = new ComboBox<>();
        cbEscola.setPromptText("1º Selecione a escola...");
        cbEscola.setMaxWidth(Double.MAX_VALUE);
        cbEscola.setDisable(!mainApp.isAdmin()); // PATCH
        cbEscola.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Escola e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? null : e.getNome());
            }
        });
        cbEscola.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Escola e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "1º Selecione a escola..." : e.getNome());
            }
        });
        cbEscola.setOnAction(e -> {
            if (!mainApp.isAdmin()) return;
            Escola esc = cbEscola.getValue();
            if (esc != null) {
                cbTurma.getItems().setAll(turmaDAO.listarPorEscola(esc.getId()));
                cbTurma.setDisable(false);
            }
        });

        cbTurma = new ComboBox<>();
        cbTurma.setPromptText("2º Selecione a turma...");
        cbTurma.setMaxWidth(Double.MAX_VALUE);
        cbTurma.setDisable(true);
        cbTurma.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getNome());
            }
        });
        cbTurma.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "2º Selecione a turma..." : t.getNome());
            }
        });

        // PATCH: botão salvar apenas para admin
        btnSalvar = new Button("Cadastrar Aluno");
        btnSalvar.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> cadastrar());
        btnSalvar.setVisible(mainApp.isAdmin());
        btnSalvar.setManaged(mainApp.isAdmin());

        btnVerTurma = new Button("Ir para Turma");
        btnVerTurma.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #2d3748; -fx-background-radius: 8; -fx-padding: 8 16;");
        btnVerTurma.setMaxWidth(Double.MAX_VALUE);
        btnVerTurma.setVisible(false);
        btnVerTurma.setManaged(false);

        painelDetalhe.getChildren().addAll(
                hdrD, lblNomeAluno, lblDetalheInfo, new Separator(),
                lblFormTitle,
                new Label("Nome:"), txtNome,
                new Label("E-mail:"), txtEmail,
                lblSenha, txtSenha,
                new Label("Escola:"), cbEscola,
                new Label("Turma:"), cbTurma,
                btnSalvar,
                new Separator(),
                btnVerTurma
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
        alunoSelecionado = null;
        lblNomeAluno.setText("Novo Aluno");
        btnSalvar.setText("Cadastrar Aluno");
        txtNome.clear(); txtEmail.clear(); txtSenha.clear();
        txtEmail.setEditable(true);   // novo cadastro: e-mail deve ser digitado
        txtEmail.setDisable(false);
        cbEscola.getItems().setAll(escolasDAO.listarTodas());
        cbTurma.getItems().clear(); cbTurma.setDisable(true);
        mostrarDetalhe();
    }

    private void abrirDetalheAluno(Aluno aluno) {
        alunoSelecionado = aluno;
        // PATCH: título diferente para professor (somente visualização)
        lblNomeAluno.setText(mainApp.isAdmin() ? "Editar: " + aluno.getNome() : aluno.getNome());
        if (mainApp.isAdmin()) btnSalvar.setText("Salvar Alterações");

        lblDetalheInfo.setText("Matrícula Atual:\nEscola: " + aluno.getEscolaNome() + "\nTurma: " + aluno.getTurmaNome());

        txtNome.setText(aluno.getNome());
        txtEmail.setText(aluno.getEmail());
        txtEmail.setEditable(false);  // na edição o e-mail nunca pode ser alterado
        txtEmail.setDisable(true);
        if (mainApp.isAdmin()) txtSenha.setText(aluno.getSenha());

        cbEscola.getItems().setAll(escolasDAO.listarTodas());
        Escola escolaMatch = null;
        for (Escola e : cbEscola.getItems()) {
            if (e.getNome().equals(aluno.getEscolaNome())) { escolaMatch = e; break; }
        }

        if (escolaMatch != null) {
            cbEscola.setValue(escolaMatch);
            cbTurma.getItems().setAll(turmaDAO.listarPorEscola(escolaMatch.getId()));
            cbTurma.setDisable(!mainApp.isAdmin());
            for (Turma t : cbTurma.getItems()) {
                if (t.getId().equals(aluno.getTurmaId())) { cbTurma.setValue(t); break; }
            }
        } else {
            cbTurma.getItems().clear();
            cbTurma.setDisable(true);
        }

        mostrarDetalhe();
    }

    private void cadastrar() {
        // Só admin chega aqui (botão está oculto para professor)
        String nome = txtNome.getText().trim();
        String email = txtEmail.getText().trim();
        String senha = txtSenha.getText();
        Turma turma = cbTurma.getValue();

        if (nome.isBlank() || email.isBlank() || senha.length() < 6 || turma == null) {
            mainApp.mostrarAviso("Preencha tudo e selecione uma turma (Senha min. 6).", true);
            return;
        }

        if (alunoSelecionado == null) {
            String novoId = SupabaseAuthDAO.criarUsuarioAuth(email, senha);
            if (novoId != null) {
                if (alunoDAO.inserir(novoId, nome, email, senha, turma.getId())) {
                    mainApp.mostrarAviso("Aluno cadastrado com sucesso!", false);
                    txtNome.clear(); txtEmail.clear(); txtSenha.clear();
                    carregarDados(); fecharDetalhe();
                } else {
                    SupabaseAuthDAO.deletarUsuarioAuth(novoId);
                    mainApp.mostrarAviso("Erro de banco de dados. Tente novamente.", true);
                }
            } else {
                mainApp.mostrarAviso("E-mail já cadastrado no sistema!", true);
            }
        } else {
            if (alunoDAO.atualizar(alunoSelecionado.getId(), nome, email, senha, turma.getId())) {
                mainApp.mostrarAviso("Aluno atualizado com sucesso!", false);
                txtNome.clear(); txtEmail.clear(); txtSenha.clear();
                carregarDados(); fecharDetalhe();
            } else {
                mainApp.mostrarAviso("Erro ao atualizar aluno.", true);
            }
        }
    }

    private void mostrarDetalhe() { painelDetalhe.setVisible(true); painelDetalhe.setManaged(true); }

    private void fecharDetalhe() {
        painelDetalhe.setVisible(false);
        painelDetalhe.setManaged(false);
        tabela.getSelectionModel().clearSelection();
        alunoSelecionado = null;
    }

    private void carregarDados() {
        // PATCH: professor vê apenas alunos das suas turmas
        if (mainApp.isAdmin()) {
            dados.setAll(alunoDAO.listarTodos());
        } else {
            dados.setAll(alunoDAO.listarPorProfessor(mainApp.getSessao().getId()));
        }
    }

    public BorderPane getView() { return view; }
}