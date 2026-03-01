package app;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainFX extends Application {

    private BorderPane root;

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        root = new BorderPane();

        // --- SIDEBAR (Menu Lateral) ---
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 1 0 0;");
        sidebar.setPrefWidth(220);

        Label lblLogo = new Label("OficinaAdmin");
        lblLogo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #58a6ff; -fx-padding: 0 0 20 0;");

        Button btnEscolas = criarBotaoMenu("🏢 Escolas");
        Button btnTurmas = criarBotaoMenu("📚 Turmas");
        Button btnSair = criarBotaoMenu("🚪 Sair");
        btnSair.setStyle("-fx-text-fill: #f85149; -fx-background-color: transparent; -fx-alignment: CENTER-LEFT;");

        // --- Ações do Menu ---
        // Ao clicar, trocamos o "Centro" da tela para a View correspondente!
        btnEscolas.setOnAction(e -> root.setCenter(new EscolasView().getView()));
        btnTurmas.setOnAction(e -> root.setCenter(new TurmasView().getView()));
        btnSair.setOnAction(e -> {
            new LoginFX().start(new Stage());
            primaryStage.close();
        });

        sidebar.getChildren().addAll(lblLogo, btnEscolas, btnTurmas, btnSair);
        root.setLeft(sidebar);

        // Inicia o programa mostrando a tela de Escolas por padrão
        root.setCenter(new EscolasView().getView());

        // --- CONFIGURAÇÃO DA JANELA ---
        Scene scene = new Scene(root, 1000, 650);
        primaryStage.setTitle("Oficina Code - Backoffice Administrativo");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    // Função auxiliar para deixar os botões do menu com cara de "link web" e efeito Hover
    private Button criarBotaoMenu(String texto) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #c9d1d9; -fx-font-size: 15px; -fx-alignment: CENTER-LEFT; -fx-padding: 10;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #ffffff; -fx-font-size: 15px; -fx-alignment: CENTER-LEFT; -fx-padding: 10; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #c9d1d9; -fx-font-size: 15px; -fx-alignment: CENTER-LEFT; -fx-padding: 10;"));
        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}