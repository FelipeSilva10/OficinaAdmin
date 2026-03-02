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

    private VBox painelDetalhe;
    private Label lblNomeAluno;
    private TextField txtNome, txtEmail;
    private PasswordField txtSenha;
    private ComboBox<Escola> cbEscola;
    private ComboBox<Turma> cbTurma;
    private Label lblDetalheInfo;

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

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("Buscar aluno/escola/turma...");
        txtBusca.setPrefWidth(260);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregarDados());

        Button btnNovo = new Button("+ Cadastrar Aluno");
        btnNovo.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNovo.setOnAction(e -> abrirFormNovo());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, lblTitulo, spacer, txtBusca, btnAtualizar, btnNovo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 16, 20));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhum aluno cadastrado."));
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<Aluno, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        TableColumn<Aluno, String> colEscola = new TableColumn<>("Escola");
        colEscola.setCellValueFactory(new PropertyValueFactory<>("escolaNome"));
        TableColumn<Aluno, String> colTurma = new TableColumn<>("Turma");
        colTurma.setCellValueFactory(new PropertyValueFactory<>("turmaNome"));
        tabela.getColumns().addAll(colNome, colEscola, colTurma);

        FilteredList<Aluno> filtrado = new FilteredList<>(dados, a -> true);
        txtBusca.textProperty().addListener((obs, old, term) -> {
            String f = term == null ? "" : term.trim().toLowerCase();
            filtrado.setPredicate(a -> f.isBlank()
                    || a.getNome().toLowerCase().contains(f)
                    || a.getEscolaNome().toLowerCase().contains(f)
                    || a.getTurmaNome().toLowerCase().contains(f));
        });
        SortedList<Aluno> ordenado = new SortedList<>(filtrado);
        ordenado.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenado);

        // Clique simples → detalhe (mostra turma/escola)
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, old, a) -> {
            if (a != null) abrirDetalheAluno(a);
        });

        tabela.setRowFactory(tv -> {
            TableRow<Aluno> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem miDel = new MenuItem("Excluir Aluno");
            miDel.setStyle("-fx-text-fill: red;");
            miDel.setOnAction(e -> {
                Aluno a = row.getItem();
                if (a != null) {
                    Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                            "Excluir aluno \"" + a.getNome() + "\"?", ButtonType.OK, ButtonType.CANCEL);
                    conf.setHeaderText(null);
                    mainApp.exibirAlerta(conf).ifPresent(r -> {
                        if (r == ButtonType.OK && alunoDAO.excluir(a.getId())) {
                            carregarDados(); fecharDetalhe();
                        }
                    });
                }
            });
            cm.getItems().add(miDel);
            row.emptyProperty().addListener((obs, w, n) -> row.setContextMenu(n ? null : cm));
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

        lblNomeAluno = new Label("Novo Aluno");
        lblNomeAluno.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        // Info do aluno selecionado (escola + turma)
        lblDetalheInfo = new Label();
        lblDetalheInfo.setStyle("-fx-text-fill: #718096; -fx-font-size: 13px;");
        lblDetalheInfo.setWrapText(true);
        lblDetalheInfo.setVisible(false);
        lblDetalheInfo.setManaged(false);

        Label lblFormTitle = new Label("Cadastrar Novo Aluno");
        lblFormTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        txtNome = new TextField(); txtNome.setPromptText("Nome completo");
        txtEmail = new TextField(); txtEmail.setPromptText("E-mail");
        txtSenha = new PasswordField(); txtSenha.setPromptText("Senha (mín. 6 caracteres)");

        cbEscola = new ComboBox<>();
        cbEscola.setPromptText("1º Selecione a escola...");
        cbEscola.setMaxWidth(Double.MAX_VALUE);
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

        Button btnSalvar = new Button("Cadastrar Aluno");
        btnSalvar.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> cadastrar());

        // Botão "Ver Turma" (só aparece quando aluno selecionado)
        Button btnVerTurma = new Button("Ir para Turma");
        btnVerTurma.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #2d3748; -fx-background-radius: 8; -fx-padding: 8 16;");
        btnVerTurma.setMaxWidth(Double.MAX_VALUE);
        btnVerTurma.setVisible(false);
        btnVerTurma.setManaged(false);

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, old, a) -> {
            boolean temAluno = a != null;
            btnVerTurma.setVisible(temAluno);
            btnVerTurma.setManaged(temAluno);
            lblDetalheInfo.setVisible(temAluno);
            lblDetalheInfo.setManaged(temAluno);
            if (temAluno) {
                btnVerTurma.setOnAction(ev -> {
                    // Navega para turmas filtrando pela escola do aluno
                    Escola esc = new Escola(a.getEscolaNome(), "ativo"); // Só visual
                    mainApp.abrirTurmas(null);
                });
            }
        });

        painelDetalhe.getChildren().addAll(
                hdrD, lblNomeAluno, lblDetalheInfo, new Separator(),
                lblFormTitle,
                new Label("Nome:"), txtNome,
                new Label("E-mail:"), txtEmail,
                new Label("Senha:"), txtSenha,
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
        tabela.getSelectionModel().clearSelection();
        lblNomeAluno.setText("Novo Aluno");
        txtNome.clear(); txtEmail.clear(); txtSenha.clear();
        cbEscola.getItems().setAll(escolasDAO.listarTodas());
        cbTurma.getItems().clear(); cbTurma.setDisable(true);
        mostrarDetalhe();
    }

    private void abrirDetalheAluno(Aluno aluno) {
        lblNomeAluno.setText(aluno.getNome());
        lblDetalheInfo.setText("Escola: " + aluno.getEscolaNome() + "\nTurma: " + aluno.getTurmaNome());
        txtNome.clear(); txtEmail.clear(); txtSenha.clear();
        cbEscola.getItems().setAll(escolasDAO.listarTodas());
        cbTurma.getItems().clear(); cbTurma.setDisable(true);
        mostrarDetalhe();
    }

    private void cadastrar() {
        String nome = txtNome.getText().trim();
        String email = txtEmail.getText().trim();
        String senha = txtSenha.getText();
        Turma turma = cbTurma.getValue();
        if (nome.isBlank() || email.isBlank() || senha.length() < 6 || turma == null) {
            new Alert(Alert.AlertType.WARNING, "Preencha tudo e selecione uma turma.").showAndWait();
            return;
        }
        String novoId = SupabaseAuthDAO.criarUsuarioAuth(email, senha);
        if (novoId != null && alunoDAO.inserir(novoId, nome, turma.getId())) {
            txtNome.clear(); txtEmail.clear(); txtSenha.clear();
            carregarDados(); fecharDetalhe();
        } else {
            new Alert(Alert.AlertType.ERROR, "Erro: e-mail já em uso ou falha de rede.").showAndWait();
        }
    }

    private void mostrarDetalhe() { painelDetalhe.setVisible(true); painelDetalhe.setManaged(true); }
    private void fecharDetalhe() {
        painelDetalhe.setVisible(false); painelDetalhe.setManaged(false);
        tabela.getSelectionModel().clearSelection();
    }
    private void carregarDados() { dados.setAll(alunoDAO.listarTodos()); }
    public BorderPane getView() { return view; }
}