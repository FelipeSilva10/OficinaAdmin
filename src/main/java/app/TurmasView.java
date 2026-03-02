package app;

import core.Escola;
import core.Turma;
import dao.EscolasDAO;
import dao.TurmaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class TurmasView {

    private BorderPane view;
    private TableView<Turma> tabela;
    private ComboBox<Escola> cbEscolas;
    private TurmaDAO turmaDAO;
    private EscolasDAO escolasDAO;
    private MainFX mainApp;

    private final ObservableList<Turma> dados = FXCollections.observableArrayList();

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
        cbEscolas.setPromptText("Filtrar por escola...");
        cbEscolas.setPrefWidth(250);
        cbEscolas.setOnAction(e -> carregarTurmas());

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("Buscar turma/professor...");
        txtBusca.setPrefWidth(220);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregarTurmas());

        Button btnNova = new Button("+ Cadastrar Turma");
        btnNova.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        btnNova.setOnAction(e -> abrirModalNovaTurma());

        HBox header = new HBox(12, lblTitulo, cbEscolas, txtBusca, btnAtualizar, btnNova);
        HBox.setHgrow(txtBusca, Priority.NEVER);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhuma turma encontrada."));

        TableColumn<Turma, String> colNome = new TableColumn<>("Nome da Turma");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Turma, String> colAno = new TableColumn<>("Ano Letivo");
        colAno.setCellValueFactory(new PropertyValueFactory<>("anoLetivo"));

        TableColumn<Turma, String> colEscola = new TableColumn<>("Escola");
        colEscola.setCellValueFactory(new PropertyValueFactory<>("escolaNome"));

        TableColumn<Turma, String> colProf = new TableColumn<>("Professor");
        colProf.setCellValueFactory(new PropertyValueFactory<>("professorNome"));

        tabela.getColumns().addAll(colNome, colAno, colEscola, colProf);

        FilteredList<Turma> filtrado = new FilteredList<>(dados, turma -> true);
        txtBusca.textProperty().addListener((obs, old, term) -> {
            String filtro = term == null ? "" : term.trim().toLowerCase();
            filtrado.setPredicate(turma -> {
                if (filtro.isBlank()) return true;
                String nomeProf = turma.getProfessorNome() == null ? "" : turma.getProfessorNome();
                return turma.getNome().toLowerCase().contains(filtro)
                        || turma.getEscolaNome().toLowerCase().contains(filtro)
                        || nomeProf.toLowerCase().contains(filtro);
            });
        });

        SortedList<Turma> ordenado = new SortedList<>(filtrado);
        ordenado.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenado);

        tabela.setRowFactory(tv -> {
            TableRow<Turma> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("🗑 Excluir Turma");
            deleteItem.setStyle("-fx-text-fill: red;");

            deleteItem.setOnAction(event -> {
                Turma turma = row.getItem();
                if (turma != null && confirmarExclusao(turma.getNome())) {
                    if (turmaDAO.excluir(turma.getId())) carregarTurmas();
                }
            });
            contextMenu.getItems().add(deleteItem);

            row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> row.setContextMenu(isNowEmpty ? null : contextMenu));

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    mainApp.abrirDashboardTurma(row.getItem());
                }
            });

            return row;
        });

        view.setCenter(new VBox(header, tabela));
    }

    private boolean confirmarExclusao(String nomeTurma) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar exclusão");
        alert.setHeaderText("Deseja excluir a turma?");
        alert.setContentText("Turma: " + nomeTurma);
        Optional<ButtonType> result = mainApp.exibirAlerta(alert);
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void carregarEscolas() {
        cbEscolas.getItems().setAll(escolasDAO.listarTodas());
    }

    private void carregarTurmas() {
        Escola selecionada = cbEscolas.getValue();
        if (selecionada == null) {
            dados.setAll(turmaDAO.listarTodas());
        } else {
            dados.setAll(turmaDAO.listarPorEscola(selecionada.getId()));
        }
    }

    private void abrirModalNovaTurma() {
        Escola selecionada = cbEscolas.getValue();
        if (selecionada == null) {
            mainApp.exibirAlerta(new Alert(Alert.AlertType.WARNING, "Selecione uma escola primeiro."));
            new Alert(Alert.AlertType.WARNING, "Selecione uma escola primeiro.").show();
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        mainApp.configurarModal(dialog);
        dialog.initOwner(mainApp.getStage());
        dialog.setTitle("Nova Turma");
        dialog.setHeaderText("Cadastro rápido");
        dialog.setContentText("Nome da Turma:");

        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.isBlank()) {
                if (turmaDAO.inserir(selecionada.getId(), nome.trim(), "2026")) {
                    carregarTurmas();
                }
            }
        });
    }

    public void selecionarEscola(Escola escolaAlvo) {
        for (Escola e : cbEscolas.getItems()) {
            if (e.getId().equals(escolaAlvo.getId())) {
                cbEscolas.getSelectionModel().select(e);
                carregarTurmas();
                break;
            }
        }
    }

    public BorderPane getView() {
        return view;
    }
}
