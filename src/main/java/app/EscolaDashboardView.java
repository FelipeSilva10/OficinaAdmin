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
    private TableView<Professor> tabelaProfessores;

    private TurmaDAO turmaDAO;
    private ProfessorDAO professorDAO;

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

        SplitPane splitPane = new SplitPane(criarPainelTurmas(), criarPainelProfessores());
        splitPane.setDividerPositions(0.5);
        view.setCenter(splitPane);
    }

    private VBox criarPainelTurmas() {
        Label lbl = new Label("Turmas da Escola");
        lbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button btnNova = new Button("+ Nova Turma");
        btnNova.setOnAction(e -> abrirModalNovaTurma());

        HBox header = new HBox(10, lbl, btnNova);
        header.setAlignment(Pos.CENTER_LEFT);

        tabelaTurmas = new TableView<>();
        tabelaTurmas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Turma, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        TableColumn<Turma, String> colAno = new TableColumn<>("Ano Letivo");
        colAno.setCellValueFactory(new PropertyValueFactory<>("anoLetivo"));
        tabelaTurmas.getColumns().addAll(colNome, colAno);

        tabelaTurmas.setRowFactory(tv -> {
            TableRow<Turma> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem mi = new MenuItem("Excluir Turma");
            mi.setStyle("-fx-text-fill: red;");
            mi.setOnAction(evt -> {
                if (turmaDAO.excluir(row.getItem().getId())) carregarDados();
            });
            cm.getItems().add(mi);
            row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                row.setContextMenu(isNowEmpty ? null : cm);
            });
            return row;
        });

        VBox box = new VBox(10, header, tabelaTurmas);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox criarPainelProfessores() {
        Label lbl = new Label("Professores");
        lbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button btnVincular = new Button("+ Vincular Professor");
        btnVincular.setOnAction(e -> abrirModalVincularProfessor());

        HBox header = new HBox(10, lbl, btnVincular);
        header.setAlignment(Pos.CENTER_LEFT);

        tabelaProfessores = new TableView<>();
        tabelaProfessores.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Professor, String> colNome = new TableColumn<>("Nome do Professor");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        tabelaProfessores.getColumns().add(colNome);

        tabelaProfessores.setRowFactory(tv -> {
            TableRow<Professor> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem mi = new MenuItem("❌ Desvincular desta Escola");
            mi.setStyle("-fx-text-fill: red;");
            mi.setOnAction(evt -> {
                if (professorDAO.desvincularEscola(row.getItem().getId(), escola.getId())) carregarDados();
            });
            cm.getItems().add(mi);
            row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                row.setContextMenu(isNowEmpty ? null : cm);
            });
            return row;
        });

        VBox box = new VBox(10, header, tabelaProfessores);
        box.setPadding(new Insets(10));
        return box;
    }

    private void carregarDados() {
        tabelaTurmas.getItems().setAll(turmaDAO.listarPorEscola(escola.getId()));
        tabelaProfessores.getItems().setAll(professorDAO.listarPorEscola(escola.getId()));
    }

    private void abrirModalNovaTurma() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Turma");
        dialog.setHeaderText("Turma para: " + escola.getNome());
        dialog.setContentText("Nome da Turma:");
        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.isBlank()) {
                if (turmaDAO.inserir(new Turma(escola.getId(), nome, "2026"))) carregarDados();
            }
        });
    }

    private void abrirModalVincularProfessor() {
        Dialog<Professor> dialog = new Dialog<>();
        dialog.setTitle("Vincular Professor");
        dialog.setHeaderText("Selecione um professor da base global");

        ComboBox<Professor> cb = new ComboBox<>();
        cb.getItems().setAll(professorDAO.listarTodos());
        cb.setPromptText("Selecione...");
        cb.setPrefWidth(250);

        dialog.getDialogPane().setContent(new VBox(10, new Label("Professor:"), cb));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? cb.getValue() : null);

        dialog.showAndWait().ifPresent(prof -> {
            if (professorDAO.vincularEscola(prof.getId(), escola.getId())) {
                carregarDados();
            } else {
                new Alert(Alert.AlertType.ERROR, "Professor já vinculado ou erro no banco!").show();
            }
        });
    }

    public BorderPane getView() { return view; }
}