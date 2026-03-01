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
    private EscolasDAO escolasDAO;

    public EscolasView() {
        escolasDAO = new EscolasDAO();
        construirInterface();
        carregarDados();
    }

    private void construirInterface() {
        view = new BorderPane();
        view.setPadding(new Insets(20));

        // --- CABEÇALHO ---
        Label lblTitulo = new Label("🏢 Gestão de Escolas");
        lblTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button btnNovaEscola = new Button("+ Nova Escola");
        btnNovaEscola.getStyleClass().add("accent"); // Classe do AtlantaFX para botão de destaque (Azul/Roxo)

        btnNovaEscola.setOnAction(e -> abrirModalNovaEscola());

        HBox header = new HBox(20, lblTitulo, btnNovaEscola);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        // --- TABELA ---
        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Escola, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(300);

        TableColumn<Escola, String> colNome = new TableColumn<>("Nome da Escola");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Escola, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tabela.getColumns().addAll(colId, colNome, colStatus);

        // --- MONTAGEM DA TELA ---
        VBox centro = new VBox(header, tabela);
        view.setCenter(centro);
    }

    // Busca as escolas no Supabase e joga na tabela
    private void carregarDados() {
        tabela.getItems().clear();
        List<Escola> escolas = escolasDAO.listarTodas();
        tabela.getItems().addAll(escolas);
    }

    // Modal simples para inserir nova escola
    private void abrirModalNovaEscola() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Escola");
        dialog.setHeaderText("Cadastrar Nova Escola");
        dialog.setContentText("Nome da Instituição:");

        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.trim().isEmpty()) {
                Escola nova = new Escola(nome, "ativo");
                if (escolasDAO.inserir(nova)) {
                    carregarDados(); // Recarrega a tabela automaticamente!
                } else {
                    Alert erro = new Alert(Alert.AlertType.ERROR, "Erro ao gravar na nuvem!");
                    erro.show();
                }
            }
        });
    }

    public BorderPane getView() {
        return view;
    }
}