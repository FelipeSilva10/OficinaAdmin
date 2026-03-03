package app;

import core.Escola;
import core.Turma;
import core.UsuarioSessao;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Optional;

public class MainFX {

    private StackPane mainContainer;
    private BorderPane root;
    private VBox toastContainer;
    private Stage stage;

    private UsuarioSessao sessaoAtual; // Guarda quem está logado!

    private EscolasView escolasView;
    private TurmasView turmasView;
    private ProfessoresView professoresView;
    private AlunosView alunosView;

    private Button btnAtivo;
    private Label breadcrumb;

    // Novo getter para as Views verificarem se é admin
    public boolean isAdmin() {
        return sessaoAtual != null && "ADMIN".equals(sessaoAtual.getRole());
    }

    public void iniciarComLoading(Stage stage, UsuarioSessao sessao) {
        this.stage = stage;
        this.sessaoAtual = sessao;

        VBox loadingPane = new VBox(24);
        loadingPane.setAlignment(Pos.CENTER);
        loadingPane.setStyle("-fx-background-color: #1a202c;");

        Label lblIniciais = new Label("OA");
        lblIniciais.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: #63b3ed; -fx-background-color: #2c5282; -fx-background-radius: 16; -fx-padding: 14 22;");

        Label lblMsg = new Label("Bem-vindo(a), " + sessao.getNome() + "! Carregando dados...");
        lblMsg.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 16px; -fx-font-weight: bold;");

        ProgressBar progressBar = new ProgressBar(-1);
        progressBar.setPrefWidth(280);
        progressBar.setPrefHeight(8);
        progressBar.setStyle("-fx-accent: #3182ce;");

        Label lblDetalhe = new Label("Conectando ao banco de dados...");
        lblDetalhe.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 13px;");

        loadingPane.getChildren().addAll(lblIniciais, lblMsg, progressBar, lblDetalhe);

        stage.getScene().setRoot(loadingPane);
        stage.setWidth(960);
        stage.setHeight(660);

        Task<Void> taskCarregar = new Task<>() {
            @Override protected Void call() throws Exception {
                updateMessage("Carregando escolas..."); Platform.runLater(() -> escolasView = new EscolasView(MainFX.this)); Thread.sleep(100);
                updateMessage("Carregando turmas...");  Platform.runLater(() -> turmasView = new TurmasView(MainFX.this)); Thread.sleep(100);
                if(isAdmin()) { updateMessage("Carregando professores..."); Platform.runLater(() -> professoresView = new ProfessoresView(MainFX.this)); Thread.sleep(100); }
                updateMessage("Carregando alunos...");  Platform.runLater(() -> alunosView = new AlunosView(MainFX.this)); Thread.sleep(200);
                return null;
            }
        };

        taskCarregar.messageProperty().addListener((obs, old, msg) -> Platform.runLater(() -> lblDetalhe.setText(msg)));
        taskCarregar.setOnSucceeded(e -> Platform.runLater(this::iniciarSistema));
        new Thread(taskCarregar).start();
    }

    private void iniciarSistema() {
        root = new BorderPane();
        root.setStyle("-fx-font-size: 15px; -fx-background-color: #f0f2f5;");
        root.setTop(criarHeader());
        root.setLeft(criarSidebar());

        toastContainer = new VBox(10);
        toastContainer.setAlignment(Pos.BOTTOM_RIGHT);
        toastContainer.setPadding(new Insets(20));
        toastContainer.setPickOnBounds(false);

        mainContainer = new StackPane(root, toastContainer);
        abrirEscolas();
        stage.getScene().setRoot(mainContainer);
    }

    public void mostrarAviso(String mensagem, boolean isErro) {
        Platform.runLater(() -> {
            Label lblAviso = new Label(mensagem);
            lblAviso.setStyle("-fx-background-color: " + (isErro ? "#e53e3e" : "#28a745") + "; -fx-text-fill: white; -fx-padding: 12 24; -fx-background-radius: 8; -fx-font-size: 14px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);");
            toastContainer.getChildren().add(lblAviso);
            PauseTransition delay = new PauseTransition(Duration.seconds(3.5));
            delay.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.millis(300), lblAviso);
                fade.setToValue(0.0);
                fade.setOnFinished(ev -> toastContainer.getChildren().remove(lblAviso));
                fade.play();
            });
            delay.play();
        });
    }

    private HBox criarHeader() {
        Label lblNome = new Label("Oficina Admin");
        lblNome.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        // Identificador visual de quem está logado
        Label lblUser = new Label((isAdmin() ? "👑 Admin: " : "🎓 Prof: ") + sessaoAtual.getNome());
        lblUser.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096; -fx-background-color: #edf2f7; -fx-padding: 4 10; -fx-background-radius: 6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        breadcrumb = new Label("Escolas");
        breadcrumb.setStyle("-fx-text-fill: #718096; -fx-font-size: 14px;");

        HBox header = new HBox(20, lblNome, lblUser, spacer, breadcrumb); // Inserido lblUser aqui
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 2);");
        return header;
    }

    // (Mantenha o método setBreadcrumb inalterado aqui)
    public void setBreadcrumb(String... partes) {
        if (breadcrumb == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            if (i > 0) sb.append("   >   ");
            sb.append(partes[i]);
        }
        breadcrumb.setText(sb.toString());
        breadcrumb.setStyle("-fx-text-fill: #2d3748; -fx-font-size: 14px; -fx-font-weight: bold;");
    }

    private VBox criarSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.setPadding(new Insets(24, 14, 24, 14));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #1a202c;");

        Label lblMenu = new Label("NAVEGACAO");
        lblMenu.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 0 0 10 10;");

        Button btnEscolas     = navBtn("Escolas");
        Button btnTurmas      = navBtn("Turmas");
        Button btnProfessores = navBtn("Professores");
        Button btnAlunos      = navBtn("Alunos");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnSair = new Button("Sair");
        btnSair.setMaxWidth(Double.MAX_VALUE);
        btnSair.setPadding(new Insets(12, 14, 12, 14));
        btnSair.setAlignment(Pos.CENTER_LEFT);
        btnSair.setStyle(navStyle(false) + " -fx-text-fill: #fc8181;");
        btnSair.setOnMouseEntered(e -> btnSair.setStyle(navStyle(true) + " -fx-text-fill: #fc8181;"));
        btnSair.setOnMouseExited(e  -> btnSair.setStyle(navStyle(false) + " -fx-text-fill: #fc8181;"));

        btnEscolas.setOnAction(e -> { ativar(btnEscolas); abrirEscolas(); });
        btnTurmas.setOnAction(e -> { ativar(btnTurmas); root.setCenter(turmasView.getView()); setBreadcrumb("Turmas"); });
        btnProfessores.setOnAction(e -> { ativar(btnProfessores); root.setCenter(professoresView.getView()); setBreadcrumb("Professores"); });
        btnAlunos.setOnAction(e -> { ativar(btnAlunos); root.setCenter(alunosView.getView()); setBreadcrumb("Alunos"); });
        btnSair.setOnAction(e -> sair());

        // LÓGICA DE PERMISSÃO NA SIDEBAR
        if (isAdmin()) {
            sidebar.getChildren().addAll(lblMenu, btnEscolas, btnTurmas, btnProfessores, btnAlunos, spacer, btnSair);
        } else {
            // Se for professor, esconde o botão de gestão de professores
            sidebar.getChildren().addAll(lblMenu, btnEscolas, btnTurmas, btnAlunos, spacer, btnSair);
        }

        ativar(btnEscolas);
        return sidebar;
    }

    private String navStyle(boolean hover) {
        return hover
                ? "-fx-background-color: #2d3748; -fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER-LEFT;"
                : "-fx-background-color: transparent; -fx-text-fill: #a0aec0; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER-LEFT;";
    }

    private String navStyleAtivo() {
        return "-fx-background-color: #3182ce; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER-LEFT;";
    }

    private Button navBtn(String texto) {
        Button b = new Button(texto);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPadding(new Insets(12, 14, 12, 14));
        b.setStyle(navStyle(false));
        b.setOnMouseEntered(e -> { if (b != btnAtivo) b.setStyle(navStyle(true)); });
        b.setOnMouseExited(e  -> { if (b != btnAtivo) b.setStyle(navStyle(false)); });
        return b;
    }

    private void ativar(Button btn) {
        if (btnAtivo != null) btnAtivo.setStyle(navStyle(false));
        btnAtivo = btn;
        btn.setStyle(navStyleAtivo());
    }

    public void abrirEscolas() {
        root.setCenter(escolasView.getView());
        setBreadcrumb("Escolas");
    }

    public void abrirTurmas(Escola escola) {
        root.setCenter(turmasView.getView());
        if (escola != null) turmasView.selecionarEscola(escola);
        setBreadcrumb("Turmas");
    }

    public void abrirDashboardTurma(Turma turma) {
        root.setCenter(new TurmaDashboardView(this, turma).getView());
        setBreadcrumb("Turmas", turma.getEscolaNome(), turma.getNome());
    }

    public void abrirDashboardEscola(Escola escola) {
        root.setCenter(new EscolaDashboardView(this, escola).getView());
        setBreadcrumb("Escolas", escola.getNome());
    }

    public EscolasView getEscolasView()         { return escolasView; }
    public TurmasView getTurmasView()           { return turmasView; }
    public ProfessoresView getProfessoresView() { return professoresView; }
    public AlunosView getAlunosView()           { return alunosView; }
    public Stage getStage() { return stage; }

    private void sair() {
        try { new LoginFX().start(stage); } catch (Exception ignored) {}
    }
}