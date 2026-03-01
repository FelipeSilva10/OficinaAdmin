package app;

import core.Escola;
import core.Professor;
import core.Turma;
import dao.ProfessorDAO;
import dao.TurmaDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class EscolaDashboardView {

    private BorderPane view;
    private Escola escola;
    private MainFX mainApp;

    private TableView<Turma> tabelaTurmas;
    private TurmaDAO turmaDAO;
    private ProfessorDAO professorDAO;

    // Painel da direita
    private VBox painelGerenciar;
    private Label lblTurmaNome;
    private ComboBox<Professor> cbProfessoresGlobais;

    public EscolaDashboardView(MainFX mainApp, Escola escola) {
        this.mainApp = mainApp;
        this.escola = escola;
        this.turmaDAO = new TurmaDAO();
        this.professorDAO = new ProfessorDAO();
        construirInterface();
        carregarDados();
    }

    private void construirInterface() {
        view = new BorderPane();
        view.setPadding(new Insets(20));

        Button btnVoltar = new Button("⬅ Voltar para Escolas");
        btnVoltar.setOnAction(e -> mainApp.abrirEscolas());
        btnVoltar.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #0366d6;");

        Label lblTitulo = new Label("Painel da Escola: " + escola.getNome());
        lblTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        HBox header = new HBox(15, btnVoltar, lblTitulo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));
        view.setTop(header);

        SplitPane splitPane = new SplitPane(criarPainelEsquerdo(), criarPainelDireito());
        splitPane.setDividerPositions(0.65);
        view.setCenter(splitPane);
    }

    private VBox criarPainelEsquerdo() {
        Button btnNova = new Button("+ Nova Turma");
        btnNova.setOnAction(e -> abrirModalNovaTurma());

        tabelaTurmas = new TableView<>();
        tabelaTurmas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Turma, String> colNome = new TableColumn<>("Turma");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Turma, String> colAno = new TableColumn<>("Ano");
        colAno.setCellValueFactory(new PropertyValueFactory<>("anoLetivo"));
        colAno.setMaxWidth(100);

        TableColumn<Turma, String> colProf = new TableColumn<>("Professor Regente");
        colProf.setCellValueFactory(new PropertyValueFactory<>("professorNome"));

        tabelaTurmas.getColumns().addAll(colNome, colAno, colProf);

        // Quando clica numa turma, atualiza o painel da direita!
        tabelaTurmas.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) atualizarPainelDireito(newSel);
        });

        VBox box = new VBox(10, new HBox(10, new Label("📚 Turmas"), btnNova), tabelaTurmas);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox criarPainelDireito() {
        painelGerenciar = new VBox(15);
        painelGerenciar.setPadding(new Insets(20));
        painelGerenciar.setStyle("-fx-background-color: #f6f8fa; -fx-border-color: #e1e4e8; -fx-border-radius: 8px;");
        painelGerenciar.setVisible(false); // Escondido até selecionar uma turma

        Label tituloGerencia = new Label("Gerenciar Regência");
        tituloGerencia.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        lblTurmaNome = new Label("Turma: ");

        cbProfessoresGlobais = new ComboBox<>();
        cbProfessoresGlobais.setPromptText("Selecione um Professor...");
        cbProfessoresGlobais.setMaxWidth(Double.MAX_VALUE);

        Button btnSalvar = new Button("Salvar Alteração");
        btnSalvar.setStyle("-fx-background-color: #2ea043; -fx-text-fill: white;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> salvarProfessorNaTurma());

        painelGerenciar.getChildren().addAll(tituloGerencia, lblTurmaNome, new Label("Atribuir Professor:"), cbProfessoresGlobais, btnSalvar);

        VBox wrapper = new VBox(new Label("Selecione uma turma na tabela para gerenciar."), painelGerenciar);
        wrapper.setPadding(new Insets(10));
        return wrapper;
    }

    private void atualizarPainelDireito(Turma turma) {
        lblTurmaNome.setText("Turma: " + turma.getNome() + " (" + turma.getAnoLetivo() + ")");
        cbProfessoresGlobais.getItems().setAll(professorDAO.listarTodos());
        painelGerenciar.setVisible(true);
    }

    private void salvarProfessorNaTurma() {
        Turma selecionada = tabelaTurmas.getSelectionModel().getSelectedItem();
        Professor profSelecionado = cbProfessoresGlobais.getValue();

        if (selecionada != null && profSelecionado != null) {
            if (turmaDAO.definirProfessor(selecionada.getId(), profSelecionado.getId())) {
                carregarDados(); // Atualiza tudo
                painelGerenciar.setVisible(false);
            }
        }
    }

    private void carregarDados() {
        tabelaTurmas.getItems().setAll(turmaDAO.listarPorEscola(escola.getId()));
    }

    private void abrirModalNovaTurma() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(mainApp.getStage()); // Trava a modal na janela principal!
        dialog.setTitle("Nova Turma");
        dialog.setHeaderText("Cadastrar turma para: " + escola.getNome());
        dialog.setContentText("Nome da Turma:");
        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.isBlank()) {
                if (turmaDAO.inserir(escola.getId(), nome, "2026")) carregarDados();
            }
        });
    }

    public BorderPane getView() { return view; }
}