package app;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginFX extends Application {

    private TextField txtUsuario;
    private PasswordField txtSenha;
    private Label lblErro;
    private final AuthService authService = new AuthService();

    @Override
    public void start(Stage stage) {

        Application.setUserAgentStylesheet(
                new PrimerLight().getUserAgentStylesheet()
        );

        stage.setTitle("Oficina Code - Login Administrativo");
        stage.setResizable(false);
        stage.setScene(createScene(stage));
        stage.show();
    }

    public Scene createScene(Stage stage) {

        Label lblLogo = new Label("Oficina Code");
        lblLogo.getStyleClass().addAll(Styles.TITLE_1);
        lblLogo.setStyle("-fx-font-weight: bold; -fx-text-fill: #3498db;");

        Label lblSub = new Label("Painel Administrativo");
        lblSub.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TITLE_4);

        txtUsuario = new TextField();
        txtUsuario.setPromptText("Usuário");

        txtSenha = new PasswordField();
        txtSenha.setPromptText("Senha");

        Button btnEntrar = new Button("Entrar");
        btnEntrar.getStyleClass().addAll(Styles.ACCENT, Styles.LARGE);
        btnEntrar.setMaxWidth(Double.MAX_VALUE);
        btnEntrar.setDefaultButton(true);
        btnEntrar.setOnAction(e -> tentarLogin(stage));

        lblErro = new Label();
        lblErro.getStyleClass().addAll(Styles.DANGER);
        lblErro.setVisible(false);

        VBox formCard = new VBox(
                15,
                lblLogo,
                lblSub,
                new Label("Usuário:"),
                txtUsuario,
                new Label("Senha:"),
                txtSenha,
                lblErro,
                btnEntrar
        );

        formCard.setPadding(new Insets(40));
        formCard.setAlignment(Pos.CENTER_LEFT);
        formCard.setMaxWidth(350);
        formCard.getStyleClass().addAll("card", Styles.ELEVATED_2);

        StackPane root = new StackPane(formCard);
        root.setStyle("-fx-background-color: #121212;");
        root.setPadding(new Insets(20));

        return new Scene(root, 500, 550);
    }

    private void tentarLogin(Stage stage) {

        lblErro.setVisible(false);

        String user = txtUsuario.getText();
        String pass = txtSenha.getText();

        if (user.isBlank() || pass.isBlank()) {
            mostrarErro("Preencha todos os campos.");
            return;
        }

        if (authService.authenticate(user, pass)) {
            MainFX main = new MainFX(stage);
            stage.setScene(main.createScene());
        } else {
            mostrarErro("Acesso negado.");
            txtSenha.clear();
            txtSenha.requestFocus();
        }
    }

    private void mostrarErro(String mensagem) {
        lblErro.setText(mensagem);
        lblErro.setVisible(true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}