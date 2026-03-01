package app;

import atlantafx.base.theme.PrimerLight;
import core.Escola;
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
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        root = new BorderPane();
        root.setStyle("-fx-font-size: 18px;");

        // --- SIDEBAR SIMPLES ---
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(300);
        sidebar.setStyle("-fx-background-color: white; -fx-border-color: #e1e4e8; -fx-border-width: 0 1 0 0;");

        Label title = new Label("Menu Principal");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #0366d6; -fx-padding: 0 0 10 0;");

        Button btnEscolas = new Button("Escolas");
        btnEscolas.setMaxWidth(Double.MAX_VALUE);

        Button btnTurmas = new Button("Turmas");
        btnTurmas.setMaxWidth(Double.MAX_VALUE);

        Button btnProfessores = new Button("Professores");
        btnProfessores.setMaxWidth(Double.MAX_VALUE);

        Button btnSair = new Button("Sair");
        btnSair.setMaxWidth(Double.MAX_VALUE);
        btnSair.setStyle("-fx-text-fill: #cb2431;");

        sidebar.getChildren().addAll(title, btnEscolas, btnTurmas, btnProfessores, btnSair);
        root.setLeft(sidebar);

        btnEscolas.setOnAction(e -> abrirEscolas());
        btnTurmas.setOnAction(e -> abrirTurmas(null));
        btnSair.setOnAction(e -> sair());
        btnProfessores.setOnAction(e -> root.setCenter(new ProfessoresView().getView()));

        // Inicia na tela de Escolas
        abrirEscolas();

        Scene scene = new Scene(root, 1024, 768);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Oficina Code - Backoffice");
        primaryStage.setMaximized(true); // Abre maximizado nativamente!
        primaryStage.show();
    }

    public void abrirEscolas() {
        root.setCenter(new EscolasView(this).getView());
    }

    public void abrirDashboardEscola(Escola escola) {
        root.setCenter(new EscolaDashboardView(this, escola).getView());
    }

    public void abrirTurmas(Escola escolaSelecionada) {
        TurmasView turmasView = new TurmasView();
        root.setCenter(turmasView.getView());

        if (escolaSelecionada != null) {
            turmasView.selecionarEscola(escolaSelecionada);
        }
    }

    private void sair() {
        new LoginFX().start(new Stage());
        stage.close();
    }
}