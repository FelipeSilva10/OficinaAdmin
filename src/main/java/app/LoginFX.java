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
        lblLogo.getStyleClass().addAll(Styles.TITLE_2);
        lblLogo.setStyle("-fx-font-weight: bold;");

        txtUsuario = new TextField();
        txtUsuario.setPromptText("Usuário");

        // Lógica da Senha com Visibilidade
        txtSenhaOculta = new PasswordField();
        txtSenhaOculta.setPromptText("Senha");

        txtSenhaVisivel = new TextField();
        txtSenhaVisivel.setPromptText("Senha");
        txtSenhaVisivel.setVisible(false);
        txtSenhaVisivel.setManaged(false);

        // Sincroniza o texto entre o campo oculto e o visível
        txtSenhaVisivel.textProperty().bindBidirectional(txtSenhaOculta.textProperty());

        Button btnVerSenha = new Button("👁");
        btnVerSenha.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        btnVerSenha.setOnAction(e -> alternarVisibilidadeSenha());

        HBox boxSenha = new HBox(5, new StackPane(txtSenhaOculta, txtSenhaVisivel), btnVerSenha);
        boxSenha.setAlignment(Pos.CENTER_LEFT);

        Button btnEntrar = new Button("Entrar no Sistema");
        btnEntrar.getStyleClass().addAll(Styles.ACCENT);
        btnEntrar.setMaxWidth(Double.MAX_VALUE);
        btnEntrar.setOnAction(e -> tentarLogin(stage));

        lblErro = new Label("Acesso negado.");
        lblErro.getStyleClass().addAll(Styles.DANGER);
        lblErro.setVisible(false);

        VBox formCard = new VBox(20, lblLogo, txtUsuario, boxSenha, lblErro, btnEntrar);
        formCard.setPadding(new Insets(40, 30, 40, 30));
        formCard.setAlignment(Pos.CENTER);
        formCard.setMaxWidth(320);
        formCard.getStyleClass().addAll(Styles.ELEVATED_2);
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 8px;");

        StackPane root = new StackPane(formCard);
        root.setStyle("-fx-background-color: #f0f2f5;"); // Fundo cinza bem suave

        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) tentarLogin(stage);
        });

        Scene scene = new Scene(root, 450, 500);
        stage.setTitle("Oficina Code - Autenticação");
        stage.setScene(scene);
        stage.setResizable(false); // Janela Fixa!
        stage.show();
    }

    private void alternarVisibilidadeSenha() {
        boolean visivel = txtSenhaVisivel.isVisible();
        txtSenhaVisivel.setVisible(!visivel);
        txtSenhaVisivel.setManaged(!visivel);
        txtSenhaOculta.setVisible(visivel);
        txtSenhaOculta.setManaged(visivel);
    }

    private void tentarLogin(Stage stageLogin) {
        String user = txtUsuario.getText();
        String pass = txtSenhaOculta.getText(); // Pega sempre da oculta (estão sincronizadas)

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
            txtSenhaOculta.clear();
        }
    }
}