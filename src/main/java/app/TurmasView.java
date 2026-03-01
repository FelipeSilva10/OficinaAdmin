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
    private TurmaDAO turmaDAO;
    private EscolasDAO escolasDAO;
    private ComboBox<Escola> cbEscolas;

    public TurmasView() {
        turmaDAO = new TurmaDAO();
        escolasDAO = new EscolasDAO();
        construirInterface();
        carregarEscolas();
    }

    private void construirInterface() {
        view = new BorderPane();
        view.setPadding(new Insets(20));

        // --- CABEÇALHO ---
        Label lblTitulo = new Label("📚 Gestão de Turmas");
        lblTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        cbEscolas = new ComboBox<>();
        cbEscolas.setPromptText("Selecione uma Escola...");
        cbEscolas.setPrefWidth(250);
        // Quando escolhe uma escola, carrega as turmas dela!
        cbEscolas.setOnAction(e -> carregarTurmas());

        Button btnNovaTurma = new Button("+ Nova Turma");
        btnNovaTurma.getStyleClass().add("accent");
        btnNovaTurma.setOnAction(e -> abrirModalNovaTurma());

        // AQUI ESTÁ O "header" QUE O JAVA NÃO ESTAVA A ENCONTRAR!
        HBox header = new HBox(20, lblTitulo, cbEscolas, btnNovaTurma);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        // --- TABELA ---
        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Turma, String> colNome = new TableColumn<>("Nome da Turma");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Turma, String> colAno = new TableColumn<>("Ano Letivo");
        colAno.setCellValueFactory(new PropertyValueFactory<>("anoLetivo"));

        tabela.getColumns().addAll(colNome, colAno);

        VBox centro = new VBox(header, tabela);
        view.setCenter(centro);
    }

    private void carregarEscolas() {
        List<Escola> escolas = escolasDAO.listarTodas();
        cbEscolas.getItems().setAll(escolas);
    }

    private void carregarTurmas() {
        Escola escolaSelecionada = cbEscolas.getValue();
        if (escolaSelecionada != null) {
            List<Turma> turmas = turmaDAO.listarPorEscola(escolaSelecionada.getId());
            tabela.getItems().setAll(turmas);
        } else {
            tabela.getItems().clear();
        }
    }

    private void abrirModalNovaTurma() {
        Escola escolaSelecionada = cbEscolas.getValue();
        if (escolaSelecionada == null) {
            new Alert(Alert.AlertType.WARNING, "Selecione uma escola primeiro!").show();
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Turma");
        dialog.setHeaderText("Turma para: " + escolaSelecionada.getNome());
        dialog.setContentText("Nome da Turma (Ex: 6º Ano A):");

        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.trim().isEmpty()) {
                // Estamos cravando 2026 como ano letivo padrão por agora
                Turma nova = new Turma(escolaSelecionada.getId(), nome, "2026");
                if (turmaDAO.inserir(nova)) {
                    carregarTurmas(); // Atualiza a tabela
                } else {
                    new Alert(Alert.AlertType.ERROR, "Erro ao gravar na nuvem!").show();
                }
            }
        });
    }

    public void selecionarEscola(Escola escolaAlvo) {
        for (Escola e : cbEscolas.getItems()) {
            if (e.getId().equals(escolaAlvo.getId())) {
                cbEscolas.setValue(e);
                break;
            }
        }
    }

    public BorderPane getView() {
        return view;
    }
}