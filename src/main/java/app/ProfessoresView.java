package app;

import core.Professor;
import dao.ProfessorDAO;
import dao.SupabaseAuthDAO;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProfessoresView {

    private BorderPane view;
    private TableView<Professor> tabela;
    private ProfessorDAO professorDAO;
    private MainFX mainApp; // Adicionamos a referência para a janela principal

    // O construtor agora recebe o MainFX
    public ProfessoresView(MainFX mainApp) {
        this.mainApp = mainApp;
        professorDAO = new ProfessorDAO();
        construirInterface();
        carregarDados();
    }

    private void construirInterface() {
        view = new BorderPane();
        view.setPadding(new Insets(20));

        Label lblTitulo = new Label("Gestão de Professores");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Button btnNovo = new Button("+ Cadastrar Novo");
        btnNovo.setStyle("-fx-background-color: #0366d6; -fx-text-fill: white;");
        btnNovo.setOnAction(e -> abrirModalNovoProfessor());

        HBox header = new HBox(20, lblTitulo, btnNovo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Professor, String> colNome = new TableColumn<>("Nome do Professor");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Professor, String> colId = new TableColumn<>("ID de Acesso (Auth)");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setStyle("-fx-font-family: monospace; -fx-font-size: 13px; -fx-text-fill: gray;");

        tabela.getColumns().addAll(colNome, colId);
        view.setCenter(new VBox(header, tabela));
    }

    private void carregarDados() {
        tabela.getItems().clear();
        tabela.getItems().addAll(professorDAO.listarTodos());
    }

    private void abrirModalNovoProfessor() {
        Dialog<ButtonType> dialog = new Dialog<>();

        // A MÁGICA ACONTECE AQUI: Trava o modal na janela principal ANTES de abrir!
        dialog.initOwner(mainApp.getStage());

        dialog.setTitle("Cadastro Global");
        dialog.setHeaderText("Cadastre as credenciais do Professor");

        TextField txtNome = new TextField(); txtNome.setPromptText("Nome Completo");
        TextField txtEmail = new TextField(); txtEmail.setPromptText("E-mail");
        PasswordField txtSenha = new PasswordField(); txtSenha.setPromptText("Min. 6 caracteres");

        VBox form = new VBox(10, new Label("Nome:"), txtNome, new Label("E-mail:"), txtEmail, new Label("Senha:"), txtSenha);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btOk.addEventFilter(ActionEvent.ACTION, event -> {
            String nome = txtNome.getText();
            String email = txtEmail.getText();
            String senha = txtSenha.getText();

            if (nome.isBlank() || email.isBlank() || senha.length() < 6) {
                new Alert(Alert.AlertType.WARNING, "Preencha tudo! (Senha min 6 chars)").show();
                event.consume(); return;
            }

            String novoId = SupabaseAuthDAO.criarUsuarioAuth(email, senha);
            if (novoId != null && professorDAO.inserir(novoId, nome)) {
                // Sucesso
            } else {
                new Alert(Alert.AlertType.ERROR, "Erro: Email em uso ou falha na rede.").show();
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(res -> { if (res == ButtonType.OK) carregarDados(); });
    }

    public BorderPane getView() { return view; }
}