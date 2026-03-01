package app;

import core.Escola;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainFX {

    private BorderPane root;
    private Stage stage;

    // Novo método que apenas altera o "recheio" da janela já existente
    public void iniciarSistema(Stage stage) {
        this.stage = stage;
        root = new BorderPane();
        root.setStyle("-fx-font-size: 16px;"); // Fonte excelente para leitura

        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: white; -fx-border-color: #e1e4e8; -fx-border-width: 0 1 0 0;");

        Label title = new Label("Menu Principal");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #0366d6; -fx-padding: 0 0 15 0;");

        Button btnEscolas = criarBotaoSidebar("Escolas");
        Button btnTurmas = criarBotaoSidebar("Turmas");
        Button btnProfessores = criarBotaoSidebar("Professores");
        Button btnAlunos = criarBotaoSidebar("Alunos");
        Button btnSair = criarBotaoSidebar("Sair");
        btnSair.setStyle("-fx-text-fill: #cb2431; -fx-background-color: transparent; -fx-alignment: CENTER-LEFT;");

        sidebar.getChildren().addAll(title, btnEscolas, btnTurmas, btnProfessores, btnAlunos, btnSair);
        root.setLeft(sidebar);

        // Ao criar as Views, passamos o mainApp (this) para que elas possam usar o Stage para travar os Modais
        btnEscolas.setOnAction(e -> root.setCenter(new EscolasView(this).getView()));
        btnTurmas.setOnAction(e -> root.setCenter(new TurmasView(this).getView()));
        btnProfessores.setOnAction(e -> root.setCenter(new ProfessoresView(this).getView()));
        btnAlunos.setOnAction(e -> root.setCenter(new AlunosView(this).getView()));
        btnSair.setOnAction(e -> sair());

        abrirEscolas();
        stage.getScene().setRoot(root); // Troca a cena sem piscar!
    }

    private Button criarBotaoSidebar(String texto) {
        Button b = new Button(texto);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER-LEFT; -fx-padding: 10;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #f6f8fa; -fx-alignment: CENTER-LEFT; -fx-padding: 10; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER-LEFT; -fx-padding: 10;"));
        return b;
    }

    public void abrirEscolas() { root.setCenter(new EscolasView(this).getView()); }

    public void abrirTurmas(Escola escola) {
        TurmasView tv = new TurmasView(this);
        root.setCenter(tv.getView());
        if (escola != null) tv.selecionarEscola(escola);
    }

    public void abrirDashboardTurma(core.Turma turma) {
        root.setCenter(new TurmaDashboardView(this, turma).getView());
    }

    public void abrirDashboardEscola(Escola escola) { root.setCenter(new EscolaDashboardView(this, escola).getView()); }

    public Stage getStage() { return stage; }

    private void sair() {
        try { new LoginFX().start(stage); } catch (Exception e) {}
    }
}