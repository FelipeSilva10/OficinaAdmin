package app;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;

public class LoginFX extends Application {

    private TextField txtUsuario;
    private PasswordField txtSenha;
    private Label lblErro;

    @Override
    public void start(Stage stage) {
        // Aplica o tema moderno do AtlantaFX
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // --- Componentes do Card de Login ---
        Label lblLogo = new Label("Oficina Code");
        lblLogo.getStyleClass().addAll(Styles.TITLE_1);
        lblLogo.setStyle("-fx-font-weight: bold; -fx-text-fill: #3498db;");

        Label lblSub = new Label("Painel Administrativo");
        lblSub.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TITLE_4);

        txtUsuario = new TextField();
        txtUsuario.setPromptText("Usuário");

        txtUsuario.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtSenha.requestFocus();
                event.consume();
            }
        });

        txtSenha = new PasswordField();
        txtSenha.setPromptText("Senha");

        Button btnEntrar = new Button("Entrar");
        btnEntrar.getStyleClass().addAll(Styles.ACCENT, Styles.LARGE);
        btnEntrar.setMaxWidth(Double.MAX_VALUE);
        btnEntrar.setOnAction(e -> tentarLogin(stage));

        lblErro = new Label("Usuário ou senha incorretos.");
        lblErro.getStyleClass().addAll(Styles.DANGER);
        lblErro.setVisible(false);

        // --- Layout do Card ---
        VBox formCard = new VBox(15, lblLogo, lblSub, new Label("Usuário:"), txtUsuario, new Label("Senha:"), txtSenha, lblErro, btnEntrar);
        formCard.setPadding(new Insets(40));
        formCard.setAlignment(Pos.CENTER_LEFT);
        formCard.setMaxWidth(350);
        formCard.getStyleClass().addAll("card", Styles.ELEVATED_2);

        // Fundo do Card levemente mais claro que o fundo da tela (para tema Dark)
        formCard.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 8px;");

        // --- Fundo da Tela ---
        StackPane root = new StackPane(formCard);
        root.setStyle("-fx-background-color: #121212;");
        root.setPadding(new Insets(20));

        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                tentarLogin(stage);
            }
        });

        Scene scene = new Scene(root, 500, 550);
        stage.setTitle("Oficina Code - Login Administrativo");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void tentarLogin(Stage stageLogin) {
        String user = txtUsuario.getText();
        String pass = txtSenha.getText();

        if (user.isBlank() || pass.isBlank()) {
            lblErro.setText("Por favor, preencha todos os campos.");
            lblErro.setVisible(true);
            return;
        }

        // LOGIN FIXO PARA DESENVOLVIMENTO (Substitui o antigo UsuarioDAO)
        if (user.equals("admin") && pass.equals("admin")) {
            abrirTelaPrincipal(stageLogin);
        } else {
            lblErro.setText("Acesso negado. Use admin / admin");
            lblErro.setVisible(true);
            txtSenha.clear();
        }
    }

    private void abrirTelaPrincipal(Stage stageLogin) {
        try {
            MainFX telaPrincipal = new MainFX();
            Stage stageMain = new Stage();
            telaPrincipal.start(stageMain);
            stageLogin.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}