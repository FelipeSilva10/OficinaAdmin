package app;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginFX extends Application {

    private TextField txtUsuario;
    private PasswordField txtSenha;
    private Label lblErro;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        Label lblLogo = new Label("Oficina Admin");
        lblLogo.getStyleClass().addAll(Styles.TITLE_2);

        txtUsuario = new TextField();
        txtUsuario.setPromptText("Usuário");

        txtSenha = new PasswordField();
        txtSenha.setPromptText("Senha");

        Button btnEntrar = new Button("Entrar no Sistema");
        btnEntrar.getStyleClass().addAll(Styles.ACCENT);
        btnEntrar.setMaxWidth(Double.MAX_VALUE);
        btnEntrar.setOnAction(e -> tentarLogin(stage));

        lblErro = new Label("Acesso negado.");
        lblErro.getStyleClass().addAll(Styles.DANGER);
        lblErro.setVisible(false);

        VBox formCard = new VBox(15, lblLogo, txtUsuario, txtSenha, lblErro, btnEntrar);
        formCard.setPadding(new Insets(30));
        formCard.setAlignment(Pos.CENTER);
        formCard.setMaxWidth(350);
        formCard.getStyleClass().addAll(Styles.ELEVATED_2);
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 8px;");

        StackPane root = new StackPane(formCard);
        root.setStyle("-fx-background-color: #f6f8fa;");

        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) tentarLogin(stage);
        });

        Scene scene = new Scene(root, 600, 500);
        stage.setTitle("Oficina Code - Autenticação");
        stage.setScene(scene);
        stage.show();
    }

    private void tentarLogin(Stage stageLogin) {
        String user = txtUsuario.getText();
        String pass = txtSenha.getText();

        if (user.isBlank() || pass.isBlank()) {
            lblErro.setText("Preencha todos os campos.");
            lblErro.setVisible(true);
            return;
        }

        dao.AdminDAO adminDAO = new dao.AdminDAO();

        if (adminDAO.autenticar(user, pass)) {
            try {
                new MainFX().start(new Stage());
                stageLogin.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            lblErro.setText("Credenciais inválidas.");
            lblErro.setVisible(true);
            txtSenha.clear();
        }
    }
}