package app;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
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
        lblLogo.getStyleClass().add(Styles.TITLE_1);

        txtUsuario = new TextField();
        txtUsuario.setPromptText("Usuário");

        txtSenhaOculta = new PasswordField();
        txtSenhaOculta.setPromptText("Senha");

        txtSenhaVisivel = new TextField();
        txtSenhaVisivel.setPromptText("Senha");
        txtSenhaVisivel.setVisible(false);
        txtSenhaVisivel.setManaged(false);

        txtSenhaVisivel.textProperty().bindBidirectional(txtSenhaOculta.textProperty());

        StackPane senhaStack = new StackPane(txtSenhaOculta, txtSenhaVisivel);
        HBox.setHgrow(senhaStack, Priority.ALWAYS);

        Button btnVerSenha = new Button("👁");
        btnVerSenha.setFocusTraversable(false);
        btnVerSenha.setOnAction(e -> {
            boolean visivel = txtSenhaVisivel.isVisible();
            txtSenhaVisivel.setVisible(!visivel);
            txtSenhaVisivel.setManaged(!visivel);
            txtSenhaOculta.setVisible(visivel);
            txtSenhaOculta.setManaged(visivel);
        });

        HBox boxSenha = new HBox(8, senhaStack, btnVerSenha);
        boxSenha.setAlignment(Pos.CENTER_LEFT);

        Button btnEntrar = new Button("Entrar no Sistema");
        btnEntrar.getStyleClass().add(Styles.ACCENT);
        btnEntrar.setMaxWidth(Double.MAX_VALUE);

        btnEntrar.setOnAction(e -> tentarLogin(stage));

        lblErro = new Label();
        lblErro.getStyleClass().add(Styles.DANGER);
        lblErro.setVisible(false);

        VBox formCard = new VBox(20);
        formCard.getChildren().addAll(
                lblLogo,
                txtUsuario,
                boxSenha,
                lblErro,
                btnEntrar
        );

        formCard.setAlignment(Pos.CENTER);
        formCard.setPadding(new Insets(40));
        formCard.setMaxWidth(420);
        formCard.setFillWidth(true);
        formCard.getStyleClass().add(Styles.ELEVATED_3);

        VBox.setVgrow(btnEntrar, Priority.NEVER);

        StackPane root = new StackPane(formCard);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: -color-bg-default;");

        Scene scene = new Scene(root, 900, 600);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                tentarLogin(stage);
            }
        });

        stage.setTitle("Oficina Admin");
        stage.setScene(scene);
        stage.setMinWidth(480);
        stage.setMinHeight(420);
        stage.show();

        txtUsuario.requestFocus();
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
            new MainFX().iniciarSistema(stage);
        } else {
            lblErro.setText("Credenciais inválidas.");
            lblErro.setVisible(true);
            txtSenhaOculta.clear();
        }
    }
}