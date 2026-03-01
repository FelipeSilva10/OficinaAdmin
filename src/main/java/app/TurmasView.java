package app;

import core.Escola;
import core.Turma;
import dao.EscolasDAO;
import dao.TurmaDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class TurmasView {

    private BorderPane view;
    private TableView<Turma> tabela;
    private ComboBox<Escola> cbEscolas;
    private TurmaDAO turmaDAO;
    private EscolasDAO escolasDAO;

    public TurmasView() {
        turmaDAO = new TurmaDAO();
        escolasDAO = new EscolasDAO();
        construirInterface();
        carregarEscolas();
    }

    private void construirInterface() {
        view = new BorderPane();
        view.setPadding(new Insets(20));

        Label lblTitulo = new Label("Gestão de Turmas");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        cbEscolas = new ComboBox<>();
        cbEscolas.setPromptText("Selecione uma Escola...");
        cbEscolas.setPrefWidth(250);
        cbEscolas.setOnAction(e -> carregarTurmas());

        Button btnNova = new Button("+ Cadastrar Turma");
        btnNova.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        btnNova.setOnAction(e -> abrirModalNovaTurma());

        HBox header = new HBox(20, lblTitulo, cbEscolas, btnNova);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Turma, String> colNome = new TableColumn<>("Nome da Turma");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Turma, String> colAno = new TableColumn<>("Ano Letivo");
        colAno.setCellValueFactory(new PropertyValueFactory<>("anoLetivo"));

        tabela.getColumns().addAll(colNome, colAno);

        view.setCenter(new VBox(header, tabela));
    }

    private void carregarEscolas() {
        cbEscolas.getItems().setAll(escolasDAO.listarTodas());
    }

    private void carregarTurmas() {
        Escola selecionada = cbEscolas.getValue();
        if (selecionada != null) {
            tabela.getItems().setAll(turmaDAO.listarPorEscola(selecionada.getId()));
        }
    }

    private void abrirModalNovaTurma() {
        Escola selecionada = cbEscolas.getValue();
        if (selecionada == null) {
            new Alert(Alert.AlertType.WARNING, "Selecione uma escola primeiro!").show();
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Turma");
        dialog.setHeaderText(null);
        dialog.setContentText("Nome da Turma:");

        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.isBlank()) {
                if (turmaDAO.inserir(new Turma(selecionada.getId(), nome, "2026"))) carregarTurmas();
            }
        });
    }

    // Chamado pelo MainFX quando o utilizador dá duplo clique na escola!
    public void selecionarEscola(Escola escolaAlvo) {
        for (Escola e : cbEscolas.getItems()) {
            if (e.getId().equals(escolaAlvo.getId())) {
                cbEscolas.getSelectionModel().select(e);
                break;
            }
        }
    }

    public BorderPane getView() {
        return view;
    }
}