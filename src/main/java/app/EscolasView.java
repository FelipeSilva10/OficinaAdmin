package app;

import core.Escola;
import dao.EscolasDAO;
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

public class EscolasView {

    private BorderPane view;
    private TableView<Escola> tabela;
    private EscolasDAO dao;
    private MainFX mainApp;

    private final ObservableList<Escola> dados = FXCollections.observableArrayList();

    public EscolasView(MainFX mainApp) {
        this.mainApp = mainApp;
        this.dao = new EscolasDAO();
        construirInterface();
        carregarDados();
    }

    private void construirInterface() {
        view = new BorderPane();
        view.setPadding(new Insets(20));

        Label lblTitulo = new Label("Gestão de Escolas");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("Buscar por nome/status...");
        txtBusca.setPrefWidth(280);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregarDados());

        Button btnNova = new Button("+ Cadastrar Escola");
        btnNova.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        btnNova.setOnAction(e -> abrirModalNovaEscola());

        HBox header = new HBox(12, lblTitulo, txtBusca, btnAtualizar, btnNova);
        HBox.setHgrow(txtBusca, Priority.NEVER);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 14, 0));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhuma escola encontrada."));

        TableColumn<Escola, String> colNome = new TableColumn<>("Nome da Escola");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Escola, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tabela.getColumns().addAll(colNome, colStatus);

        FilteredList<Escola> filtrado = new FilteredList<>(dados, escola -> true);
        txtBusca.textProperty().addListener((obs, old, term) -> {
            String filtro = term == null ? "" : term.trim().toLowerCase();
            filtrado.setPredicate(escola -> {
                if (filtro.isBlank()) return true;
                return escola.getNome().toLowerCase().contains(filtro)
                        || escola.getStatus().toLowerCase().contains(filtro);
            });
        });

        SortedList<Escola> ordenado = new SortedList<>(filtrado);
        ordenado.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenado);

        tabela.setRowFactory(tv -> {
            TableRow<Escola> row = new TableRow<>();

            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("🗑 Excluir Escola");
            deleteItem.setStyle("-fx-text-fill: red;");
            deleteItem.setOnAction(event -> {
                Escola escola = row.getItem();
                if (escola != null && confirmarExclusao(escola.getNome())) {
                    if (dao.excluir(escola.getId())) {
                        carregarDados();
                    } else {
                        mainApp.exibirAlerta(new Alert(Alert.AlertType.ERROR, "Erro ao excluir. Verifique vínculos com turmas."));
                    }
                }
            });
            contextMenu.getItems().add(deleteItem);

            row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> row.setContextMenu(isNowEmpty ? null : contextMenu));

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    mainApp.abrirDashboardEscola(row.getItem());
                }
            });
            return row;
        });

        view.setCenter(new VBox(header, tabela));
    }

    private boolean confirmarExclusao(String nomeEscola) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar exclusão");
        alert.setHeaderText("Deseja excluir a escola?");
        alert.setContentText("Escola: " + nomeEscola);
        Optional<ButtonType> result = mainApp.exibirAlerta(alert);
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void carregarDados() {
        dados.setAll(dao.listarTodas());
    }

    private void abrirModalNovaEscola() {
        TextInputDialog dialog = new TextInputDialog();
        mainApp.configurarModal(dialog);
        dialog.setTitle("Nova Escola");
        dialog.setHeaderText("Cadastro rápido");
        dialog.setContentText("Nome da Escola:");

        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.isBlank()) {
                if (dao.inserir(new Escola(nome.trim(), "ativo"))) carregarDados();
            }
        });
    }

    public BorderPane getView() {
        return view;
    }
}
