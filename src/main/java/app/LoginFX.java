package app;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class LoginFX extends Application {

    private TextField txtUsuario;
    private PasswordField txtSenhaOculta;
    private TextField txtSenhaVisivel;
    private Label lblErro;
    private Button btnEntrar;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // Painel esquerdo: identidade visual
        VBox painelEsq = new VBox(24);
        painelEsq.setAlignment(Pos.CENTER);
        painelEsq.setPadding(new Insets(60, 50, 60, 50));
        painelEsq.setPrefWidth(340);
        painelEsq.setMinWidth(280);
        painelEsq.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a202c, #2d3748);");

        Label lblIniciais = new Label("OA");
        lblIniciais.setStyle("""
            -fx-font-size: 40px;
            -fx-font-weight: bold;
            -fx-text-fill: #63b3ed;
            -fx-background-color: #2c5282;
            -fx-background-radius: 16;
            -fx-padding: 14 22;
        """);

        Label lblNome = new Label("Oficina Admin");
        lblNome.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label lblDesc = new Label("Painel de gestão escolar\npara o Oficina Code");
        lblDesc.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 14px; -fx-text-alignment: center;");
        lblDesc.setTextAlignment(TextAlignment.CENTER);

        Region sepEsq = new Region();
        sepEsq.setPrefHeight(1);
        sepEsq.setMaxWidth(100);
        sepEsq.setStyle("-fx-background-color: #4a5568;");

        Label lblVersao = new Label("v1.0 — Alpha");
        lblVersao.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 12px;");

        painelEsq.getChildren().addAll(lblIniciais, lblNome, lblDesc, sepEsq, lblVersao);

        // Painel direito: formulário
        VBox formulario = new VBox(20);
        formulario.setAlignment(Pos.CENTER_LEFT);
        formulario.setMaxWidth(420);

        Label lblBemVindo = new Label("Bem-vindo de volta");
        lblBemVindo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Label lblSub = new Label("Entre com suas credenciais de administrador");
        lblSub.setStyle("-fx-text-fill: #718096; -fx-font-size: 14px;");

        Region sepForm = new Region();
        sepForm.setPrefHeight(1);
        sepForm.setMaxWidth(Double.MAX_VALUE);
        sepForm.setStyle("-fx-background-color: #e2e8f0;");

        // Campo usuário
        VBox campoUsuario = new VBox(8);
        Label lUsuario = new Label("Usuário");
        lUsuario.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3748;");
        txtUsuario = new TextField();
        txtUsuario.setPromptText("Digite seu usuário");
        txtUsuario.setPrefHeight(48);
        txtUsuario.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #e2e8f0;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-font-size: 15px;
            -fx-padding: 0 14;
        """);
        campoUsuario.getChildren().addAll(lUsuario, txtUsuario);

        // Campo senha
        VBox campoSenha = new VBox(8);
        Label lSenha = new Label("Senha");
        lSenha.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3748;");

        String estiloInput = """
            -fx-background-color: white;
            -fx-border-color: #e2e8f0;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-font-size: 15px;
            -fx-padding: 0 14;
        """;

        txtSenhaOculta = new PasswordField();
        txtSenhaOculta.setPromptText("Digite sua senha");
        txtSenhaOculta.setPrefHeight(48);
        txtSenhaOculta.setStyle(estiloInput);

        txtSenhaVisivel = new TextField();
        txtSenhaVisivel.setPromptText("Digite sua senha");
        txtSenhaVisivel.setPrefHeight(48);
        txtSenhaVisivel.setStyle(estiloInput);
        txtSenhaVisivel.setVisible(false);
        txtSenhaVisivel.setManaged(false);
        txtSenhaVisivel.textProperty().bindBidirectional(txtSenhaOculta.textProperty());

        Button btnVer = new Button("Ver");
        btnVer.setPrefHeight(48);
        btnVer.setPrefWidth(64);
        btnVer.setStyle("""
            -fx-background-color: #edf2f7;
            -fx-background-radius: 8;
            -fx-border-radius: 8;
            -fx-cursor: hand;
            -fx-font-size: 13px;
            -fx-font-weight: bold;
            -fx-text-fill: #4a5568;
        """);
        btnVer.setFocusTraversable(false);
        btnVer.setOnAction(e -> {
            boolean v = txtSenhaVisivel.isVisible();
            txtSenhaVisivel.setVisible(!v); txtSenhaVisivel.setManaged(!v);
            txtSenhaOculta.setVisible(v);   txtSenhaOculta.setManaged(v);
            btnVer.setText(v ? "Ver" : "Ocultar");
        });

        StackPane stackSenha = new StackPane(txtSenhaOculta, txtSenhaVisivel);
        HBox.setHgrow(stackSenha, Priority.ALWAYS);
        HBox boxSenha = new HBox(8, stackSenha, btnVer);
        boxSenha.setAlignment(Pos.CENTER_LEFT);
        campoSenha.getChildren().addAll(lSenha, boxSenha);

        // Mensagem de erro
        lblErro = new Label();
        lblErro.setStyle("""
            -fx-text-fill: #c53030;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-background-color: #fff5f5;
            -fx-border-color: #feb2b2;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-padding: 12 16;
        """);
        lblErro.setMaxWidth(Double.MAX_VALUE);
        lblErro.setWrapText(true);
        lblErro.setVisible(false);
        lblErro.setManaged(false);

        // Botão entrar
        btnEntrar = new Button("Entrar no Sistema");
        btnEntrar.setMaxWidth(Double.MAX_VALUE);
        btnEntrar.setPrefHeight(50);
        btnEntrar.setStyle("""
            -fx-background-color: #3182ce;
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-background-radius: 10;
            -fx-cursor: hand;
        """);
        btnEntrar.setOnMouseEntered(e ->
                btnEntrar.setStyle(btnEntrar.getStyle().replace("#3182ce", "#2b6cb0")));
        btnEntrar.setOnMouseExited(e ->
                btnEntrar.setStyle(btnEntrar.getStyle().replace("#2b6cb0", "#3182ce")));
        btnEntrar.setOnAction(e -> tentarLogin(stage));

        formulario.getChildren().addAll(
                lblBemVindo, lblSub, sepForm,
                campoUsuario, campoSenha,
                lblErro, btnEntrar
        );

        // Centraliza o formulario no painel direito, que expande para preencher tudo
        StackPane painelDir = new StackPane(formulario);
        painelDir.setPadding(new Insets(60));
        painelDir.setStyle("-fx-background-color: #f7fafc;");
        HBox.setHgrow(painelDir, Priority.ALWAYS);

        // Root preenche toda a cena - sem area preta
        HBox root = new HBox(painelEsq, painelDir);
        root.setMinSize(0, 0);
        root.setStyle("-fx-background-color: #f7fafc;");

        Scene scene = new Scene(root, 860, 560);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) tentarLogin(stage);
        });

        stage.setTitle("Oficina Admin — Login");
        stage.setScene(scene);
        stage.setMinWidth(640);
        stage.setMinHeight(460);
        stage.setWidth(860);
        stage.setHeight(560);
        stage.show();
        txtUsuario.requestFocus();
    }

    private void tentarLogin(Stage stage) {
        String user = txtUsuario.getText().trim();
        String pass = txtSenhaOculta.getText();

        if (user.isBlank() || pass.isBlank()) {
            mostrarErro("Preencha usuário/e-mail e senha.");
            return;
        }

        btnEntrar.setText("Verificando...");
        btnEntrar.setDisable(true);
        lblErro.setVisible(false);
        lblErro.setManaged(false);

        // Usando a nova AutenticacaoDAO que retorna a Sessão
        Task<core.UsuarioSessao> taskAuth = new Task<>() {
            @Override protected core.UsuarioSessao call() {
                return new dao.AutenticacaoDAO().autenticar(user, pass);
            }
        };

        taskAuth.setOnSucceeded(e -> {
            core.UsuarioSessao sessao = taskAuth.getValue();
            if (sessao != null) {
                new MainFX().iniciarComLoading(stage, sessao); // Passando a sessão!
            } else {
                mostrarErro("Credenciais inválidas. Tente novamente.");
                btnEntrar.setText("Entrar no Sistema");
                btnEntrar.setDisable(false);
                txtSenhaOculta.clear();
            }
        });

        taskAuth.setOnFailed(e -> {
            mostrarErro("Erro de conexão. Verifique a rede.");
            btnEntrar.setText("Entrar no Sistema");
            btnEntrar.setDisable(false);
        });

        new Thread(taskAuth).start();
    }

    private void mostrarErro(String msg) {
        lblErro.setText(msg);
        lblErro.setVisible(true);
        lblErro.setManaged(true);
    }
}