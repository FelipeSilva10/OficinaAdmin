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
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainFX {

    private StackPane mainContainer;
    private BorderPane root;
    private VBox toastContainer;
    private Stage stage;

    private UsuarioSessao sessaoAtual;

    // ── Módulos comuns ────────────────────────────────────────────────────────
    private EscolasView             escolasView;
    private TurmasView              turmasView;
    private AlunosView              alunosView;

    // ── Módulos admin ─────────────────────────────────────────────────────────
    private ProfessoresView         professoresView;
    private CronogramaAdminView     cronogramaAdminView;
    private RegistroHorasAdminView  registroHorasAdminView;

    // ── Módulos professor ─────────────────────────────────────────────────────
    private CronogramaView          cronogramaView;
    private ChamadaView             chamadaView;
    private RegistroHorasView       registroHorasView;

    private Button btnAtivo;
    private Label  breadcrumb;

    // ─────────────────────────────────────────────────────────────────────────

    public boolean isAdmin() {
        return sessaoAtual != null && "ADMIN".equals(sessaoAtual.getRole());
    }

    public UsuarioSessao getSessao() { return sessaoAtual; }

    // ── Loading ───────────────────────────────────────────────────────────────

    public void iniciarComLoading(Stage stage, UsuarioSessao sessao) {
        this.stage       = stage;
        this.sessaoAtual = sessao;

        VBox loadingPane = new VBox(24);
        loadingPane.setAlignment(Pos.CENTER);
        loadingPane.setStyle("-fx-background-color: #1a202c;");

        Label lblIniciais = new Label("OA");
        lblIniciais.setStyle("-fx-font-size:40px;-fx-font-weight:bold;-fx-text-fill:#63b3ed;" +
                "-fx-background-color:#2c5282;-fx-background-radius:16;-fx-padding:14 22;");

        Label lblMsg = new Label("Bem-vindo(a), " + sessao.getNome() + "! Carregando...");
        lblMsg.setStyle("-fx-text-fill:#a0aec0;-fx-font-size:16px;-fx-font-weight:bold;");

        ProgressBar pb = new ProgressBar(-1);
        pb.setPrefWidth(280); pb.setPrefHeight(8);
        pb.setStyle("-fx-accent:#3182ce;");

        Label lblDet = new Label("Conectando ao banco de dados...");
        lblDet.setStyle("-fx-text-fill:#4a5568;-fx-font-size:13px;");

        loadingPane.getChildren().addAll(lblIniciais, lblMsg, pb, lblDet);
        stage.getScene().setRoot(loadingPane);
        stage.setWidth(1200); stage.setHeight(720);

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                updateMessage("Carregando escolas...");
                Platform.runLater(() -> escolasView = new EscolasView(MainFX.this));
                Thread.sleep(70);

                updateMessage("Carregando turmas...");
                Platform.runLater(() -> turmasView = new TurmasView(MainFX.this));
                Thread.sleep(70);

                updateMessage("Carregando alunos...");
                Platform.runLater(() -> alunosView = new AlunosView(MainFX.this));
                Thread.sleep(70);

                if (isAdmin()) {
                    updateMessage("Carregando professores...");
                    Platform.runLater(() -> professoresView = new ProfessoresView(MainFX.this));
                    Thread.sleep(70);

                    updateMessage("Carregando cronograma...");
                    Platform.runLater(() -> cronogramaAdminView = new CronogramaAdminView(MainFX.this));
                    Thread.sleep(70);

                    updateMessage("Carregando registro de horas...");
                    Platform.runLater(() -> registroHorasAdminView = new RegistroHorasAdminView(MainFX.this));
                    Thread.sleep(70);
                } else {
                    updateMessage("Carregando cronograma...");
                    Platform.runLater(() -> cronogramaView = new CronogramaView(MainFX.this));
                    Thread.sleep(70);

                    updateMessage("Preparando chamada...");
                    Platform.runLater(() -> chamadaView = new ChamadaView(MainFX.this));
                    Thread.sleep(70);

                    updateMessage("Carregando registro de horas...");
                    Platform.runLater(() -> registroHorasView = new RegistroHorasView(MainFX.this));
                    Thread.sleep(70);
                }
                return null;
            }
        };

        task.messageProperty().addListener((o, ov, msg) ->
                Platform.runLater(() -> lblDet.setText(msg)));
        task.setOnSucceeded(e -> Platform.runLater(this::iniciarSistema));
        new Thread(task).start();
    }

    private void iniciarSistema() {
        root = new BorderPane();
        root.setStyle("-fx-font-size:15px;-fx-background-color:#f0f2f5;");
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

    // ── Toast ─────────────────────────────────────────────────────────────────

    public void mostrarAviso(String mensagem, boolean isErro) {
        Platform.runLater(() -> {
            Label lbl = new Label(mensagem);
            lbl.setStyle("-fx-background-color:" + (isErro ? "#e53e3e" : "#28a745") +
                    ";-fx-text-fill:white;-fx-padding:12 24;-fx-background-radius:8;" +
                    "-fx-font-size:14px;-fx-font-weight:bold;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),8,0,0,2);");
            toastContainer.getChildren().add(lbl);
            PauseTransition delay = new PauseTransition(Duration.seconds(3.5));
            delay.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.millis(300), lbl);
                fade.setToValue(0); fade.setOnFinished(ev -> toastContainer.getChildren().remove(lbl));
                fade.play();
            });
            delay.play();
        });
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox criarHeader() {
        Label lblNome = new Label("Oficina Admin");
        lblNome.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:#1a202c;");

        Label lblUser = new Label((isAdmin() ? "👑 Admin: " : "🎓 Prof: ") + sessaoAtual.getNome());
        lblUser.setStyle("-fx-font-size:13px;-fx-text-fill:#718096;" +
                "-fx-background-color:#edf2f7;-fx-padding:4 10;-fx-background-radius:6;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        breadcrumb = new Label("Escolas");
        breadcrumb.setStyle("-fx-text-fill:#718096;-fx-font-size:14px;");

        HBox h = new HBox(20, lblNome, lblUser, sp, breadcrumb);
        h.setPadding(new Insets(16, 24, 16, 24));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-background-color:white;-fx-border-color:#e2e8f0;" +
                "-fx-border-width:0 0 1 0;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),4,0,0,2);");
        return h;
    }

    public void setBreadcrumb(String... partes) {
        if (breadcrumb == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            if (i > 0) sb.append("   ›   ");
            sb.append(partes[i]);
        }
        breadcrumb.setText(sb.toString());
        breadcrumb.setStyle("-fx-text-fill:#2d3748;-fx-font-size:14px;-fx-font-weight:bold;");
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private VBox criarSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.setPadding(new Insets(24, 14, 24, 14));
        sidebar.setPrefWidth(240);
        sidebar.setStyle("-fx-background-color:#1a202c;");

        // Botões comuns
        Button btnEscolas = navBtn("🏫  Escolas");
        Button btnTurmas  = navBtn("📚  Turmas");
        Button btnAlunos  = navBtn("🎒  Alunos");
        btnEscolas.setOnAction(e -> { ativar(btnEscolas); abrirEscolas(); });
        btnTurmas.setOnAction(e  -> { ativar(btnTurmas);  navegarPara(turmasView.getView(), "Turmas"); });
        btnAlunos.setOnAction(e  -> { ativar(btnAlunos);  navegarPara(alunosView.getView(), "Alunos"); });

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnSair = navBtn("⎋  Sair");
        btnSair.setStyle(navStyle(false) + "-fx-text-fill:#fc8181;");
        btnSair.setOnMouseEntered(e -> btnSair.setStyle(navStyle(true)  + "-fx-text-fill:#fc8181;"));
        btnSair.setOnMouseExited(e  -> btnSair.setStyle(navStyle(false) + "-fx-text-fill:#fc8181;"));
        btnSair.setOnAction(e -> sair());

        if (isAdmin()) {
            // ── Sidebar Admin: tudo ──────────────────────────────────────────
            Button btnProfessores    = navBtn("👤  Professores");
            Button btnCronograma     = navBtn("📅  Cronograma");
            Button btnRegHoras       = navBtn("💰  Horas & Pagamento");

            btnProfessores.setOnAction(e -> { ativar(btnProfessores); navegarPara(professoresView.getView(), "Professores"); });
            btnCronograma.setOnAction(e  -> { ativar(btnCronograma);  navegarPara(cronogramaAdminView.getView(), "Cronograma"); });
            btnRegHoras.setOnAction(e    -> { ativar(btnRegHoras);    navegarPara(registroHorasAdminView.getView(), "Horas & Pagamento"); });

            sidebar.getChildren().addAll(
                    secaoLabel("GESTÃO"),
                    btnEscolas, btnTurmas, btnProfessores, btnAlunos,
                    new Separator(),
                    secaoLabel("PROFESSOR"),
                    btnCronograma, btnRegHoras,
                    spacer, btnSair);
            ativar(btnEscolas);

        } else {
            // ── Sidebar Professor ────────────────────────────────────────────
            Button btnCronograma    = navBtn("📅  Cronograma");
            Button btnChamada       = navBtn("✅  Chamada");
            Button btnRegistroHoras = navBtn("🕐  Registro de Horas");

            btnCronograma.setOnAction(e    -> { ativar(btnCronograma);    navegarPara(cronogramaView.getView(), "Cronograma"); });
            btnChamada.setOnAction(e       -> { ativar(btnChamada);       navegarPara(chamadaView.getView(), "Chamada"); });
            btnRegistroHoras.setOnAction(e -> { ativar(btnRegistroHoras); navegarPara(registroHorasView.getView(), "Registro de Horas"); });

            Separator sep = new Separator();
            sep.setStyle("-fx-background-color:#2d3748;");

            sidebar.getChildren().addAll(
                    secaoLabel("GESTÃO"),
                    btnEscolas, btnTurmas, btnAlunos,
                    sep,
                    secaoLabel("MÓDULOS"),
                    btnCronograma, btnChamada, btnRegistroHoras,
                    spacer, btnSair);
            ativar(btnEscolas);
        }

        return sidebar;
    }

    // ── Nav helpers ───────────────────────────────────────────────────────────

    private void navegarPara(javafx.scene.Node view, String titulo) {
        root.setCenter(view);
        setBreadcrumb(titulo);
    }

    private Label secaoLabel(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-text-fill:#4a5568;-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:8 0 4 10;");
        return lbl;
    }

    private String navStyle(boolean hover) {
        return hover
                ? "-fx-background-color:#2d3748;-fx-text-fill:#e2e8f0;-fx-font-size:13px;-fx-background-radius:8;-fx-cursor:hand;-fx-alignment:CENTER-LEFT;"
                : "-fx-background-color:transparent;-fx-text-fill:#a0aec0;-fx-font-size:13px;-fx-background-radius:8;-fx-cursor:hand;-fx-alignment:CENTER-LEFT;";
    }

    private String navStyleAtivo() {
        return "-fx-background-color:#3182ce;-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;-fx-alignment:CENTER-LEFT;";
    }

    private Button navBtn(String texto) {
        Button b = new Button(texto);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPadding(new Insets(10, 14, 10, 14));
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

    // ── Navegação pública ─────────────────────────────────────────────────────

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

    // ── Getters ───────────────────────────────────────────────────────────────

    public EscolasView              getEscolasView()            { return escolasView; }
    public TurmasView               getTurmasView()             { return turmasView; }
    public AlunosView               getAlunosView()             { return alunosView; }
    public ProfessoresView          getProfessoresView()        { return professoresView; }
    public CronogramaAdminView      getCronogramaAdminView()    { return cronogramaAdminView; }
    public RegistroHorasAdminView   getRegistroHorasAdminView() { return registroHorasAdminView; }
    public CronogramaView           getCronogramaView()         { return cronogramaView; }
    public ChamadaView              getChamadaView()            { return chamadaView; }
    public RegistroHorasView        getRegistroHorasView()      { return registroHorasView; }
    public Stage                    getStage()                  { return stage; }

    private void sair() {
        try { new LoginFX().start(stage); } catch (Exception ignored) {}
    }
}