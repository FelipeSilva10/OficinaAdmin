package app;

import dao.UsuarioDAO;
import core.Usuario;
import core.Sessao;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode; // Importação necessária para manipular teclas

public class LoginFX extends Application {

    private TextField txtUsuario;
    private PasswordField txtSenha;
    private Label lblErro;

    @Override
    public void start(Stage stage) {
        // Aplica o tema moderno do AtlantaFX
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // --- Componentes do Card de Login ---
        Label lblLogo = new Label("GEST");
        lblLogo.getStyleClass().addAll(Styles.TITLE_1);
        lblLogo.setStyle("-fx-font-style: italic; -fx-text-fill: #0969da;");

        Label lblSub = new Label("Acesso ao Sistema");
        lblSub.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TITLE_4);

        txtUsuario = new TextField();
        txtUsuario.setPromptText("Digite seu usuário ou e-mail");

        // ALTERAÇÃO: Enter no campo usuário apenas muda o foco para a senha
        txtUsuario.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtSenha.requestFocus();
                event.consume(); // Impede que o evento suba para o root e dispare o login
            }
        });

        txtSenha = new PasswordField();
        txtSenha.setPromptText("Digite sua senha");

        Button btnEntrar = new Button("Entrar");
        btnEntrar.getStyleClass().addAll(Styles.ACCENT, Styles.LARGE);
        btnEntrar.setMaxWidth(Double.MAX_VALUE); // Faz o botão preencher a largura
        btnEntrar.setOnAction(e -> tentarLogin(stage));

        lblErro = new Label("Usuário ou senha incorretos.");
        lblErro.getStyleClass().addAll(Styles.DANGER); // Texto vermelho do AtlantaFX
        lblErro.setVisible(false); // Escondido por padrão

        // --- Layout do Card ---
        VBox formCard = new VBox(15, lblLogo, lblSub, new Label("Usuário:"), txtUsuario, new Label("Senha:"), txtSenha, lblErro, btnEntrar);
        formCard.setPadding(new Insets(40));
        formCard.setAlignment(Pos.CENTER_LEFT);
        formCard.setMaxWidth(350);
        formCard.getStyleClass().addAll("card", Styles.ELEVATED_2);
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 8px;");

        // --- Fundo da Tela ---
        StackPane root = new StackPane(formCard);
        root.setStyle("-fx-background-color: #f6f8fa;"); // Fundo cinza claro para destacar o card branco
        root.setPadding(new Insets(20));

        // Permite logar apertando "Enter" (Global - funcionará para o campo Senha)
        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                tentarLogin(stage);
            }
        });

        Scene scene = new Scene(root, 500, 550);
        stage.setTitle("GEST - Login");
        stage.setScene(scene);
        stage.setResizable(false); // Tela de login com tamanho fixo
        stage.show();
    }

    // =================================================================================
    // LÓGICA DE AUTENTICAÇÃO E NAVEGAÇÃO
    // =================================================================================
    private void tentarLogin(Stage stageLogin) {
        String user = txtUsuario.getText();
        String pass = txtSenha.getText();

        if (user.isBlank() || pass.isBlank()) {
            lblErro.setText("Por favor, preencha todos os campos.");
            lblErro.setVisible(true);
            return;
        }

        UsuarioDAO dao = new UsuarioDAO();
        Usuario u = dao.autenticar(user, pass);

        if (u != null) {
            Sessao.login(u);          // <-- AQUI
            abrirTelaPrincipal(stageLogin);
        } else {
            lblErro.setText("Usuário ou senha incorretos.");
            lblErro.setVisible(true);
            txtSenha.clear();
        }
    }

    private void abrirTelaPrincipal(Stage stageLogin) {
        try {
            // Instancia a sua MainFX
            MainFX telaPrincipal = new MainFX();

            // Cria um novo Stage (Janela) para o sistema completo
            Stage stageMain = new Stage();
            telaPrincipal.start(stageMain);

            // Fecha a janelinha de Login
            stageLogin.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}