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
import javafx.scene.layout.*;

public class TurmasView {

    private BorderPane view;
    private TableView<Turma> tabela;
    private ComboBox<Escola> cbEscolasFiltro;
    private TurmaDAO turmaDAO;
    private EscolasDAO escolasDAO;
    private MainFX mainApp;
    private Turma turmaSelecionada; // Variável de controle para saber se é edição ou novo
    private final ObservableList<Turma> dados = FXCollections.observableArrayList();

    // Elementos do painel lateral
    private VBox painelDetalhe;
    private Label lblAcaoTurma;
    private TextField txtNome, txtAnoLetivo;
    private ComboBox<Escola> cbEscolaForm;
    private Button btnSalvar; // Declarado aqui para podermos alterar o texto dinamicamente

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

        Label lblTitulo = new Label("Gestão de Turmas");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        cbEscolasFiltro = new ComboBox<>();
        cbEscolasFiltro.setPromptText("Filtrar por escola...");
        cbEscolasFiltro.setPrefWidth(250);
        cbEscolasFiltro.setOnAction(e -> carregarTurmas());

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("Buscar turma/professor...");
        txtBusca.setPrefWidth(220);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregarTurmas());

        Button btnNova = new Button("+ Cadastrar Turma");
        btnNova.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNova.setOnAction(e -> abrirFormNovo());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, lblTitulo, cbEscolasFiltro, spacer, txtBusca, btnAtualizar, btnNova);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 16, 20));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhuma turma encontrada."));
        VBox.setVgrow(tabela, Priority.ALWAYS);

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
            MenuItem deleteItem = new MenuItem("Excluir Turma");
            deleteItem.setStyle("-fx-text-fill: red;");

            deleteItem.setOnAction(event -> {
                Turma turma = row.getItem();
                // Exclusão direta, sem confirmação
                if (turma != null && turmaDAO.excluir(turma.getId())) {
                    carregarTurmas();
                    fecharDetalhe();
                }
            });
            contextMenu.getItems().add(deleteItem);

            row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> row.setContextMenu(isNowEmpty ? null : contextMenu));

            row.setOnMouseClicked(event -> {
                // Duplo clique vai para o Dashboard da Turma
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    mainApp.abrirDashboardTurma(row.getItem());
                }
                // Um clique abre o painel de edição
                else if (event.getClickCount() == 1 && (!row.isEmpty())) {
                    abrirDetalheTurma(row.getItem());
                }
            });

            return row;
        });

        // ── Painel Detalhe Lateral ────────────────────────────────────────
        painelDetalhe = new VBox(14);
        painelDetalhe.setPadding(new Insets(24));
        painelDetalhe.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
        painelDetalhe.setMinWidth(300);
        painelDetalhe.setMaxWidth(360);
        painelDetalhe.setVisible(false);
        painelDetalhe.setManaged(false);

        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-cursor: hand;");
        btnFechar.setOnAction(e -> fecharDetalhe());
        HBox hdrD = new HBox(btnFechar);
        hdrD.setAlignment(Pos.TOP_RIGHT);

        lblAcaoTurma = new Label("Nova Turma");
        lblAcaoTurma.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Label lblFormTitle = new Label("Dados da Turma");
        lblFormTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        txtNome = new TextField();
        txtNome.setPromptText("Ex: 1º Ano A");

        txtAnoLetivo = new TextField("2026");
        txtAnoLetivo.setPromptText("Ex: 2026");

        cbEscolaForm = new ComboBox<>();
        cbEscolaForm.setPromptText("Selecione a escola...");
        cbEscolaForm.setMaxWidth(Double.MAX_VALUE);

        btnSalvar = new Button("Cadastrar Turma");
        btnSalvar.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> cadastrar());

        painelDetalhe.getChildren().addAll(
                hdrD, lblAcaoTurma, new Separator(),
                lblFormTitle,
                new Label("Nome:"), txtNome,
                new Label("Ano Letivo:"), txtAnoLetivo,
                new Label("Escola:"), cbEscolaForm,
                btnSalvar
        );

        VBox conteudoEsq = new VBox(header, tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        HBox mainLayout = new HBox(conteudoEsq, painelDetalhe);
        HBox.setHgrow(conteudoEsq, Priority.ALWAYS);
        view.setCenter(mainLayout);
    }

    private void abrirFormNovo() {
        tabela.getSelectionModel().clearSelection();
        turmaSelecionada = null; // Reseta seleção
        lblAcaoTurma.setText("Nova Turma");
        btnSalvar.setText("Cadastrar Turma"); // Muda texto do botão
        txtNome.clear();
        txtAnoLetivo.setText("2026");
        cbEscolaForm.getItems().setAll(escolasDAO.listarTodas());

        if(cbEscolasFiltro.getValue() != null) {
            cbEscolaForm.setValue(cbEscolasFiltro.getValue());
        } else {
            cbEscolaForm.getSelectionModel().clearSelection();
        }

        mostrarDetalhe();
    }

    private void abrirDetalheTurma(Turma turma) {
        turmaSelecionada = turma; // Guarda a turma que está sendo editada
        lblAcaoTurma.setText("Editar Turma");
        btnSalvar.setText("Salvar Alterações"); // Muda texto do botão
        txtNome.setText(turma.getNome());
        txtAnoLetivo.setText(turma.getAnoLetivo());

        cbEscolaForm.getItems().setAll(escolasDAO.listarTodas());
        for (Escola e : cbEscolaForm.getItems()) {
            if (e.getId().equals(turma.getEscolaId())) {
                cbEscolaForm.setValue(e);
                break;
            }
        }
        mostrarDetalhe();
    }

    private void cadastrar() {
        String nome = txtNome.getText().trim();
        String ano = txtAnoLetivo.getText().trim();
        Escola escola = cbEscolaForm.getValue();

        if (nome.isBlank() || ano.isBlank() || escola == null) {
            new Alert(Alert.AlertType.WARNING, "Preencha o nome, ano letivo e selecione uma escola.").showAndWait();
            return;
        }

        boolean sucesso;

        // Inserir se não tiver turma selecionada, senão Atualizar
        if (turmaSelecionada == null) {
            sucesso = turmaDAO.inserir(escola.getId(), nome, ano);
        } else {
            sucesso = turmaDAO.atualizar(turmaSelecionada.getId(), escola.getId(), nome, ano);
        }

        if (sucesso) {
            txtNome.clear();
            carregarTurmas();
            fecharDetalhe();
        } else {
            new Alert(Alert.AlertType.ERROR, "Erro ao salvar a turma.").showAndWait();
        }
    }

    private void mostrarDetalhe() {
        painelDetalhe.setVisible(true);
        painelDetalhe.setManaged(true);
    }

    private void fecharDetalhe() {
        painelDetalhe.setVisible(false);
        painelDetalhe.setManaged(false);
        tabela.getSelectionModel().clearSelection();
        turmaSelecionada = null;
    }

    private void carregarEscolas() {
        cbEscolasFiltro.getItems().setAll(escolasDAO.listarTodas());
    }

    private void carregarTurmas() {
        Escola selecionada = cbEscolasFiltro.getValue();
        if (selecionada == null) {
            dados.setAll(turmaDAO.listarTodas());
        } else {
            dados.setAll(turmaDAO.listarPorEscola(selecionada.getId()));
        }
    }

    public void selecionarEscola(Escola escolaAlvo) {
        for (Escola e : cbEscolasFiltro.getItems()) {
            if (e.getId().equals(escolaAlvo.getId())) {
                cbEscolasFiltro.getSelectionModel().select(e);
                carregarTurmas();
                break;
            }
        }
    }

    public BorderPane getView() {
        return view;
    }
}