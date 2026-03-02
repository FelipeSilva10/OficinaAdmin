package app;

import core.Escola;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class MainFX {

    private BorderPane root;
    private Stage stage;

    private EscolasView escolasView;
    private TurmasView turmasView;
    private ProfessoresView professoresView;
    private AlunosView alunosView;

    public void iniciarSistema(Stage stage) {
        this.stage = stage;
        root = new BorderPane();
        root.setStyle("-fx-font-size: 14px; -fx-background-color: #f6f8fb;");

        root.setTop(criarHeaderSistema());
        root.setLeft(criarSidebar());

        abrirEscolas();
        stage.getScene().setRoot(root);
    }

    // FIX 1: Removed duplicate criarHeaderSistema() — kept only this correct version
    private HBox criarHeaderSistema() {
        Label lblTitulo = new Label("Oficina Admin");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1f2d3d;");

        Label lblSubtitulo = new Label("Painel administrativo");
        lblSubtitulo.setStyle("-fx-text-fill: #6b7785;");

        VBox blocoTitulo = new VBox(2, lblTitulo, lblSubtitulo);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblHint = new Label("Versão Alpha");
        lblHint.setStyle("-fx-text-fill: #6b7785; -fx-font-size: 15px;");

        HBox header = new HBox(12, blocoTitulo, spacer, lblHint);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-border-color: #e5e9ef; -fx-border-width: 0 0 1 0;");
        return header;
    }

    private VBox criarSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(16));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: white; -fx-border-color: #e5e9ef; -fx-border-width: 0 1 0 0;");

        Label title = new Label("Módulos");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-padding: 0 0 8 0;");

        Button btnEscolas = criarBotaoSidebar("Escolas");
        Button btnTurmas = criarBotaoSidebar("Turmas");
        Button btnProfessores = criarBotaoSidebar("Professores");
        Button btnAlunos = criarBotaoSidebar("Alunos");
        Button btnSair = criarBotaoSidebar("Sair");
        btnSair.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER-LEFT; -fx-padding: 10; -fx-text-fill: #cb2431;");

        btnEscolas.setOnAction(e -> abrirEscolas());
        btnTurmas.setOnAction(e -> root.setCenter(getTurmasView().getView()));
        btnProfessores.setOnAction(e -> root.setCenter(getProfessoresView().getView()));
        btnAlunos.setOnAction(e -> root.setCenter(getAlunosView().getView()));
        btnSair.setOnAction(e -> sair());

        sidebar.getChildren().addAll(title, btnEscolas, btnTurmas, btnProfessores, btnAlunos, btnSair);
        return sidebar;
    }

    private Button criarBotaoSidebar(String texto) {
        Button b = new Button(texto);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER-LEFT; -fx-padding: 10;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #edf2f7; -fx-alignment: CENTER-LEFT; -fx-padding: 10; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER-LEFT; -fx-padding: 10;"));
        return b;
    }

    public void abrirEscolas() {
        root.setCenter(getEscolasView().getView());
    }

    public void abrirTurmas(Escola escola) {
        TurmasView tv = getTurmasView();
        root.setCenter(tv.getView());
        if (escola != null) tv.selecionarEscola(escola);
    }

    public void abrirDashboardTurma(core.Turma turma) {
        root.setCenter(new TurmaDashboardView(this, turma).getView());
    }

    public void abrirDashboardEscola(Escola escola) {
        root.setCenter(new EscolaDashboardView(this, escola).getView());
    }

    private EscolasView getEscolasView() {
        if (escolasView == null) escolasView = new EscolasView(this);
        return escolasView;
    }

    private TurmasView getTurmasView() {
        if (turmasView == null) turmasView = new TurmasView(this);
        return turmasView;
    }

    private ProfessoresView getProfessoresView() {
        if (professoresView == null) professoresView = new ProfessoresView(this);
        return professoresView;
    }

    // FIX 2: Removed duplicate getAlunosView() — kept only this one
    private AlunosView getAlunosView() {
        if (alunosView == null) alunosView = new AlunosView(this);
        return alunosView;
    }

    public void configurarModal(Dialog<?> dialog) {
        Stage owner = getStage();
        boolean estavaFullscreen = owner != null && owner.isFullScreen();

        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }

        dialog.setOnShown(event -> {
            if (estavaFullscreen && owner != null) owner.setFullScreen(true);
        });

        dialog.setOnHidden(event -> {
            if (estavaFullscreen && owner != null) owner.setFullScreen(true);
            if (owner != null) owner.toFront();
        });
    }

    public Optional<ButtonType> exibirAlerta(Alert alert) {
        Stage owner = getStage();
        boolean estavaFullscreen = owner != null && owner.isFullScreen();

        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }

        Optional<ButtonType> resultado = alert.showAndWait();

        if (estavaFullscreen && owner != null) owner.setFullScreen(true);
        if (owner != null) owner.toFront();
        return resultado;
    } // FIX 3: Added missing closing brace for exibirAlerta()

    public Stage getStage() {
        return stage;
    }

    private void sair() {
        try {
            new LoginFX().start(stage);
        } catch (Exception e) {
        }
    }
}