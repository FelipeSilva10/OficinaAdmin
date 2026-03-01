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
    private MainFX mainApp;

    public TurmasView(MainFX mainApp) {
        this.mainApp = mainApp;
        turmaDAO = new TurmaDAO();
        escolasDAO = new EscolasDAO();
        construirInterface();
        carregarEscolas();
        carregarTurmas();
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

        TableColumn<Turma, String> colEscola = new TableColumn<>("Escola");
        colEscola.setCellValueFactory(new PropertyValueFactory<>("escolaNome"));

        TableColumn<Turma, String> colProf = new TableColumn<>("Professor");
        colProf.setCellValueFactory(new PropertyValueFactory<>("professorNome"));

        tabela.getColumns().addAll(colNome, colAno, colEscola, colProf);

        tabela.setRowFactory(tv -> {
            TableRow<Turma> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("🗑 Excluir Turma");
            deleteItem.setStyle("-fx-text-fill: red;");

            deleteItem.setOnAction(event -> {
                Turma turma = row.getItem();
                if (turmaDAO.excluir(turma.getId())) {
                    carregarTurmas(); // Recarrega a tabela
                }
            });
            contextMenu.getItems().add(deleteItem);

            row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    row.setContextMenu(null);
                } else {
                    row.setContextMenu(contextMenu);
                }
            });
            return row;
        });

        view.setCenter(new VBox(header, tabela));
    }

    private void carregarEscolas() {
        cbEscolas.getItems().setAll(escolasDAO.listarTodas());
    }

    private void carregarTurmas() {
        Escola selecionada = cbEscolas.getValue();
        if (selecionada == null) {
            tabela.getItems().setAll(turmaDAO.listarTodas());
        } else {
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

        // TEM QUE SER AQUI: Trava a janela filha dentro da janela mãe ANTES de abrir!
        dialog.initOwner(mainApp.getStage());

        dialog.setTitle("Nova Turma");
        dialog.setHeaderText(null);
        dialog.setContentText("Nome da Turma:");

        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.isBlank()) {
                // Passando os 3 textos diretamente conforme o novo TurmaDAO
                if (turmaDAO.inserir(selecionada.getId(), nome, "2026")) {
                    carregarTurmas();
                }
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