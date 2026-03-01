package app;

import core.Escola;
import core.Professor;
import dao.EscolasDAO;
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
    private EscolasDAO escolasDAO;

    public ProfessoresView() {
        professorDAO = new ProfessorDAO();
        escolasDAO = new EscolasDAO();
        construirInterface();
        carregarDados();
    }

    private void construirInterface() {
        view = new BorderPane();
        view.setPadding(new Insets(20));

        Label lblTitulo = new Label("Gestão de Professores");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Button btnNovo = new Button("+ Cadastrar Professor");
        btnNovo.setStyle("-fx-background-color: #0366d6; -fx-text-fill: white;");
        btnNovo.setOnAction(e -> abrirModalNovoProfessor());

        HBox header = new HBox(20, lblTitulo, btnNovo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Professor, String> colNome = new TableColumn<>("Nome do Professor");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Professor, String> colEscola = new TableColumn<>("Instituição (Escola)");
        colEscola.setCellValueFactory(new PropertyValueFactory<>("escolaNome"));

        TableColumn<Professor, String> colId = new TableColumn<>("ID de Acesso (Auth)");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: gray;");

        tabela.getColumns().addAll(colNome, colEscola, colId);

        view.setCenter(new VBox(header, tabela));
    }

    private void carregarDados() {
        tabela.getItems().clear();
        tabela.getItems().addAll(professorDAO.listarTodos());
    }

    private void abrirModalNovoProfessor() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Cadastrar Professor");
        dialog.setHeaderText("Preencha as credenciais de acesso");

        // Criando os campos do formulário
        TextField txtNome = new TextField(); txtNome.setPromptText("Ex: Felipe Silva");
        TextField txtEmail = new TextField(); txtEmail.setPromptText("Ex: professor@oficina.com");
        PasswordField txtSenha = new PasswordField(); txtSenha.setPromptText("Mínimo de 6 caracteres");

        ComboBox<Escola> cbEscola = new ComboBox<>();
        cbEscola.getItems().setAll(escolasDAO.listarTodas());
        cbEscola.setPromptText("Selecione a Escola...");
        cbEscola.setPrefWidth(300);

        VBox form = new VBox(10,
                new Label("Nome Completo:"), txtNome,
                new Label("E-mail de Login:"), txtEmail,
                new Label("Senha de Acesso:"), txtSenha,
                new Label("Vincular à Escola:"), cbEscola
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Interceptando o clique do botão OK para fazer a validação e criar na API
        Button btOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btOk.addEventFilter(ActionEvent.ACTION, event -> {

            String nome = txtNome.getText();
            String email = txtEmail.getText();
            String senha = txtSenha.getText();
            Escola escola = cbEscola.getValue();

            if (nome.isBlank() || email.isBlank() || senha.length() < 6 || escola == null) {
                new Alert(Alert.AlertType.WARNING, "Preencha todos os campos corretamente! (Senha min. 6 chars)").show();
                event.consume(); // Impede o modal de fechar
                return;
            }

            // COMBO MÁGICO 1: Tenta criar no Supabase Auth
            String novoIdAuth = SupabaseAuthDAO.criarUsuarioAuth(email, senha);

            if (novoIdAuth != null) {
                // COMBO MÁGICO 2: Se deu certo, salva o Perfil no banco de dados!
                if (!professorDAO.inserir(novoIdAuth, escola.getId(), nome)) {
                    new Alert(Alert.AlertType.ERROR, "Conta criada, mas falhou ao gravar o perfil!").show();
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "Falha na API: Email já existe ou senha inválida.").show();
                event.consume(); // Impede de fechar
            }
        });

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) carregarDados();
        });
    }

    public BorderPane getView() {
        return view;
    }
}