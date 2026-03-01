package app;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginFX extends Application {

    private TextField txtUsuario;
    private PasswordField txtSenhaOculta;
    private TextField txtSenhaVisivel;
    private Label lblErro;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        Label lblLogo = new Label("Oficina Admin");
        lblLogo.getStyleClass().addAll(Styles.TITLE_1); // Maior
        lblLogo.setStyle("-fx-font-weight: bold;");

        txtUsuario = new TextField();
        txtUsuario.setPromptText("Usuário");
        txtUsuario.setStyle("-fx-font-size: 16px;");

        txtSenhaOculta = new PasswordField();
        txtSenhaOculta.setPromptText("Senha");
        txtSenhaOculta.setStyle("-fx-font-size: 16px;");

        txtSenhaVisivel = new TextField();
        txtSenhaVisivel.setPromptText("Senha");
        txtSenhaVisivel.setStyle("-fx-font-size: 16px;");
        txtSenhaVisivel.setVisible(false);
        txtSenhaVisivel.setManaged(false);

        txtSenhaVisivel.textProperty().bindBidirectional(txtSenhaOculta.textProperty());

        Button btnVerSenha = new Button("👁");
        btnVerSenha.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 18px;");
        btnVerSenha.setOnAction(e -> {
            boolean v = txtSenhaVisivel.isVisible();
            txtSenhaVisivel.setVisible(!v); txtSenhaVisivel.setManaged(!v);
            txtSenhaOculta.setVisible(v); txtSenhaOculta.setManaged(v);
        });

        HBox boxSenha = new HBox(5, new StackPane(txtSenhaOculta, txtSenhaVisivel), btnVerSenha);
        boxSenha.setAlignment(Pos.CENTER_LEFT);

        Button btnEntrar = new Button("Entrar no Sistema");
        btnEntrar.getStyleClass().addAll(Styles.ACCENT);
        btnEntrar.setMaxWidth(Double.MAX_VALUE);
        btnEntrar.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        btnEntrar.setOnAction(e -> tentarLogin(stage));

        lblErro = new Label("Acesso negado.");
        lblErro.getStyleClass().addAll(Styles.DANGER);
        lblErro.setVisible(false);

        VBox formCard = new VBox(25, lblLogo, txtUsuario, boxSenha, lblErro, btnEntrar);
        formCard.setPadding(new Insets(50, 40, 50, 40));
        formCard.setAlignment(Pos.CENTER);
        formCard.setMaxWidth(400); // Mais largo
        formCard.getStyleClass().addAll(Styles.ELEVATED_3);
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 12px;");

        StackPane root = new StackPane(formCard);
        root.setStyle("-fx-background-color: #e9ecef;");

        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) tentarLogin(stage);
        });

        Scene scene = new Scene(root, 1024, 768);
        stage.setTitle("Oficina Code - Backoffice");
        stage.setScene(scene);
        stage.setMaximized(true); // Já começa maximizado desde o login!
        stage.show();
    }

    private void tentarLogin(Stage stage) {
        String user = txtUsuario.getText();
        String pass = txtSenhaOculta.getText();

        if (user.isBlank() || pass.isBlank()) {
            lblErro.setText("Preencha todos os campos.");
            lblErro.setVisible(true);
            return;
        }

        if (new dao.AdminDAO().autenticar(user, pass)) {
            // Em vez de criar um novo Stage, passamos o mesmo Stage maximizado!
            new MainFX().iniciarSistema(stage);
        } else {
            lblErro.setText("Credenciais inválidas.");
            lblErro.setVisible(true);
            txtSenhaOculta.clear();
        }
    }
}