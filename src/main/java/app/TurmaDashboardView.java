package app;

import core.Aluno;
import core.Turma;
import dao.AlunoDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class TurmaDashboardView {

    private BorderPane view;
    private Turma turma;
    private MainFX mainApp;
    private TableView<Aluno> tabelaAlunos;
    private AlunoDAO alunoDAO;

    public TurmaDashboardView(MainFX mainApp, Turma turma) {
        this.mainApp = mainApp;
        this.turma = turma;
        this.alunoDAO = new AlunoDAO();
        construirInterface();
        carregarDados();
    }

    private void construirInterface() {
        view = new BorderPane();
        view.setPadding(new Insets(20));

        Button btnVoltar = new Button("← Voltar");
        btnVoltar.setOnAction(e -> mainApp.abrirTurmas(null));
        btnVoltar.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #0366d6;");

        VBox titulos = new VBox(5);
        Label lblTitulo = new Label("Painel da Turma: " + turma.getNome());
        lblTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label lblSub = new Label("Escola: " + turma.getEscolaNome() + " | Prof: " + turma.getProfessorNome());
        lblSub.setStyle("-fx-text-fill: gray;");
        titulos.getChildren().addAll(lblTitulo, lblSub);

        HBox header = new HBox(15, btnVoltar, titulos);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));
        view.setTop(header);

        tabelaAlunos = new TableView<>();
        tabelaAlunos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Aluno, String> colNome = new TableColumn<>("Nome do Aluno");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        tabelaAlunos.getColumns().add(colNome);

        view.setCenter(new VBox(10, new Label("Alunos Matriculados:"), tabelaAlunos));
    }

    private void carregarDados() {
        tabelaAlunos.getItems().setAll(alunoDAO.listarPorTurma(turma.getId()));
    }

    public BorderPane getView() { return view; }
}