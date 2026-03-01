package app;

import core.Escola;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainFX {

    private final BorderPane root = new BorderPane();
    private final Stage stage;

    public MainFX(Stage stage) {
        this.stage = stage;
    }

    public Scene createScene() {

        root.setStyle("-fx-font-size: 15px;");

        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle(
                "-fx-background-color: #f6f8fa;" +
                        "-fx-border-color: #d0d7de;" +
                        "-fx-border-width: 0 1 0 0;"
        );
        sidebar.setPrefWidth(220);

        Label lblLogo = new Label("OficinaAdmin");
        lblLogo.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #0969da;" +
                        "-fx-padding: 0 0 20 0;"
        );

        Button btnEscolas = criarBotaoMenu("🏢 Escolas");
        Button btnTurmas = criarBotaoMenu("📚 Turmas");
        Button btnSair = criarBotaoMenu("🚪 Sair");

        btnEscolas.setOnAction(e ->
                root.setCenter(new EscolasView().getView())
        );

        btnTurmas.setOnAction(e ->
                root.setCenter(new TurmasView().getView())
        );

        btnSair.setOnAction(e ->
                stage.setScene(new LoginFX().createScene(stage))
        );

        sidebar.getChildren().addAll(lblLogo, btnEscolas, btnTurmas, btnSair);
        root.setLeft(sidebar);

        root.setCenter(new EscolasView().getView());

        return new Scene(root, 1000, 650);
    }

    public void irParaTurmasDaEscola(Escola escola) {
        TurmasView telaTurmas = new TurmasView();
        telaTurmas.selecionarEscola(escola);
        root.setCenter(telaTurmas.getView());
    }

    private Button criarBotaoMenu(String texto) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #24292f;" +
                        "-fx-font-size: 15px;" +
                        "-fx-alignment: CENTER-LEFT;" +
                        "-fx-padding: 10;"
        );
        return btn;
    }
}