package app;

import core.Aluno;
import core.Escola;
import core.Turma;
import dao.AlunoDAO;
import dao.EscolasDAO;
import dao.SupabaseAuthDAO;
import dao.TurmaDAO;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AlunosView {

    private BorderPane view;
    private TableView<Aluno> tabela;
    private AlunoDAO alunoDAO;
    private EscolasDAO escolasDAO;
    private TurmaDAO turmaDAO;
    private MainFX mainApp;

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
        view.setPadding(new Insets(20));

        Label lblTitulo = new Label("🎓 Gestão de Alunos");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Button btnNovo = new Button("+ Cadastrar Aluno");
        btnNovo.setStyle("-fx-background-color: #0366d6; -fx-text-fill: white;");
        btnNovo.setOnAction(e -> abrirModalNovoAluno());

        HBox header = new HBox(20, lblTitulo, btnNovo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Aluno, String> colNome = new TableColumn<>("Nome do Aluno");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Aluno, String> colEscola = new TableColumn<>("Escola");
        colEscola.setCellValueFactory(new PropertyValueFactory<>("escolaNome"));

        TableColumn<Aluno, String> colTurma = new TableColumn<>("Turma");
        colTurma.setCellValueFactory(new PropertyValueFactory<>("turmaNome"));

        tabela.getColumns().addAll(colNome, colEscola, colTurma);

        tabela.setRowFactory(tv -> {
            TableRow<Aluno> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem mi = new MenuItem("🗑 Excluir Aluno");
            mi.setStyle("-fx-text-fill: red;");
            mi.setOnAction(evt -> {
                if (alunoDAO.excluir(row.getItem().getId())) carregarDados();
            });
            cm.getItems().add(mi);
            row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                row.setContextMenu(isNowEmpty ? null : cm);
            });
            return row;
        });

        view.setCenter(new VBox(header, tabela));
    }

    private void carregarDados() {
        tabela.getItems().setAll(alunoDAO.listarTodos());
    }

    private void abrirModalNovoAluno() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(mainApp.getStage());
        dialog.setTitle("Cadastrar Aluno");
        dialog.setHeaderText("Crie as credenciais e defina a turma");

        TextField txtNome = new TextField(); txtNome.setPromptText("Nome Completo");
        TextField txtEmail = new TextField(); txtEmail.setPromptText("E-mail para Login");
        PasswordField txtSenha = new PasswordField(); txtSenha.setPromptText("Min. 6 caracteres");

        ComboBox<Escola> cbEscola = new ComboBox<>();
        cbEscola.getItems().setAll(escolasDAO.listarTodas());
        cbEscola.setPromptText("1º Selecione a Escola...");

        ComboBox<Turma> cbTurma = new ComboBox<>();
        cbTurma.setPromptText("2º Selecione a Turma...");
        cbTurma.setDisable(true); // Fica bloqueado até escolher a escola

        // A MÁGICA: Quando seleciona a escola, busca as turmas DELA!
        cbEscola.setOnAction(e -> {
            Escola esc = cbEscola.getValue();
            if (esc != null) {
                cbTurma.getItems().setAll(turmaDAO.listarPorEscola(esc.getId()));
                cbTurma.setDisable(false);
            }
        });

        VBox form = new VBox(10,
                new Label("Nome:"), txtNome,
                new Label("E-mail (Login):"), txtEmail,
                new Label("Senha:"), txtSenha,
                new Label("Vínculo Institucional:"), cbEscola, cbTurma
        );
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btOk.addEventFilter(ActionEvent.ACTION, event -> {
            String nome = txtNome.getText(), email = txtEmail.getText(), senha = txtSenha.getText();
            Turma turma = cbTurma.getValue();

            if (nome.isBlank() || email.isBlank() || senha.length() < 6 || turma == null) {
                new Alert(Alert.AlertType.WARNING, "Preencha tudo corretamente! (Selecione a turma)").show();
                event.consume(); return;
            }

            String novoId = SupabaseAuthDAO.criarUsuarioAuth(email, senha);
            if (novoId != null && alunoDAO.inserir(novoId, nome, turma.getId())) {
                // Sucesso
            } else {
                new Alert(Alert.AlertType.ERROR, "Erro: Email em uso ou falha!").show();
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(res -> { if (res == ButtonType.OK) carregarDados(); });
    }

    public BorderPane getView() { return view; }
}