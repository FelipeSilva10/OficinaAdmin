package app;

import core.Escola;
import dao.EscolasDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class EscolasView {

    private BorderPane view;
    private TableView<Escola> tabela;
    private EscolasDAO dao;
    private MainFX mainApp;

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

        Button btnNova = new Button("+ Cadastrar Escola");
        btnNova.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        btnNova.setOnAction(e -> abrirModalNovaEscola());

        HBox header = new HBox(20, lblTitulo, btnNova);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Escola, String> colNome = new TableColumn<>("Nome da Escola");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Escola, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tabela.getColumns().addAll(colNome, colStatus);

        // Duplo Clique Limpo
        tabela.setRowFactory(tv -> {
            TableRow<Escola> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    mainApp.abrirTurmas(row.getItem());
                }
            });
            return row;
        });

        view.setCenter(new VBox(header, tabela));
    }

    private void carregarDados() {
        tabela.getItems().clear();
        List<Escola> escolas = dao.listarTodas();
        tabela.getItems().addAll(escolas);
    }

    private void abrirModalNovaEscola() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Escola");
        dialog.setHeaderText(null);
        dialog.setContentText("Nome da Escola:");

        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.isBlank()) {
                if (dao.inserir(new Escola(nome, "ativo"))) carregarDados();
            }
        });
    }

    public BorderPane getView() {
        return view;
    }
}