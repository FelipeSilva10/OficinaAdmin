package app;

import core.Escola;
import core.Professor;
import core.Turma;
import dao.EscolasDAO;
import dao.ProfessorDAO;
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
    private ProfessorDAO professorDAO;
    private MainFX mainApp;
    private Turma turmaSelecionada;
    private final ObservableList<Turma> dados = FXCollections.observableArrayList();

    private VBox painelDetalhe;
    private Label lblAcaoTurma;
    private TextField txtNome, txtAnoLetivo;
    private ComboBox<Escola> cbEscolaForm;
    private ComboBox<Professor> cbProfessorForm;
    private Button btnSalvar;

    public TurmasView(MainFX mainApp) {
        this.mainApp = mainApp;
        turmaDAO = new TurmaDAO();
        escolasDAO = new EscolasDAO();
        professorDAO = new ProfessorDAO();
        construirInterface();
        if (mainApp.isAdmin()) carregarEscolas();
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
        cbEscolasFiltro.setVisible(mainApp.isAdmin());
        cbEscolasFiltro.setManaged(mainApp.isAdmin());

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("Buscar turma/professor...");
        txtBusca.setPrefWidth(220);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregarTurmas());

        // PATCH: botão de nova turma apenas para admin
        Button btnNova = new Button("+ Cadastrar Turma");
        btnNova.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNova.setOnAction(e -> abrirFormNovo());
        btnNova.setVisible(mainApp.isAdmin());
        btnNova.setManaged(mainApp.isAdmin());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, lblTitulo, cbEscolasFiltro, spacer, txtBusca, btnAtualizar, btnNova);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 16, 20));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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

            // PATCH: menu de excluir apenas para admin
            if (mainApp.isAdmin()) {
                ContextMenu contextMenu = new ContextMenu();
                MenuItem deleteItem = new MenuItem("Excluir Turma");
                deleteItem.setStyle("-fx-text-fill: red;");
                deleteItem.setOnAction(event -> {
                    Turma turma = row.getItem();
                    if (turma != null) {
                        if (turmaDAO.excluir(turma.getId())) {
                            mainApp.mostrarAviso("Turma excluída com sucesso!", false);
                            carregarTurmas();
                            fecharDetalhe();
                        } else {
                            mainApp.mostrarAviso("Erro ao excluir. Verifique vínculos.", true);
                        }
                    }
                });
                contextMenu.getItems().add(deleteItem);
                row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) ->
                        row.setContextMenu(isNowEmpty ? null : contextMenu));
            }

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    mainApp.abrirDashboardTurma(row.getItem());
                } else if (event.getClickCount() == 1 && !row.isEmpty()) {
                    abrirDetalheTurma(row.getItem());
                }
            });
            return row;
        });

        // ── Painel de detalhe ──────────────────────────────────────────────────
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

        lblAcaoTurma = new Label("Detalhes da Turma");
        lblAcaoTurma.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Label lblFormTitle = new Label("Dados da Turma");
        lblFormTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        txtNome = new TextField();
        txtNome.setPromptText("Ex: 1º Ano A");
        // PATCH: professor não pode editar
        txtNome.setEditable(mainApp.isAdmin());

        txtAnoLetivo = new TextField("2026");
        txtAnoLetivo.setPromptText("Ex: 2026");
        // PATCH: professor não pode editar
        txtAnoLetivo.setEditable(mainApp.isAdmin());

        cbEscolaForm = new ComboBox<>();
        cbEscolaForm.setPromptText("Selecione a escola...");
        cbEscolaForm.setMaxWidth(Double.MAX_VALUE);
        // PATCH: professor não pode trocar a escola
        cbEscolaForm.setDisable(!mainApp.isAdmin());

        // PATCH: ComboBox para atribuir professor à turma (admin only)
        cbProfessorForm = new ComboBox<>();
        cbProfessorForm.setPromptText("Sem professor atribuído");
        cbProfessorForm.setMaxWidth(Double.MAX_VALUE);
        cbProfessorForm.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Professor p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getNome());
            }
        });
        cbProfessorForm.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Professor p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "Sem professor atribuído" : p.getNome());
            }
        });
        cbProfessorForm.setVisible(mainApp.isAdmin());
        cbProfessorForm.setManaged(mainApp.isAdmin());

        Label lblProfessor = new Label("Professor:");
        lblProfessor.setVisible(mainApp.isAdmin());
        lblProfessor.setManaged(mainApp.isAdmin());

        // PATCH: botão salvar apenas para admin
        btnSalvar = new Button("Cadastrar Turma");
        btnSalvar.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> cadastrar());
        btnSalvar.setVisible(mainApp.isAdmin());
        btnSalvar.setManaged(mainApp.isAdmin());

        painelDetalhe.getChildren().addAll(
                hdrD, lblAcaoTurma, new Separator(),
                lblFormTitle,
                new Label("Nome:"), txtNome,
                new Label("Ano Letivo:"), txtAnoLetivo,
                new Label("Escola:"), cbEscolaForm,
                lblProfessor, cbProfessorForm,
                btnSalvar
        );

        VBox conteudoEsq = new VBox(header, tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        HBox mainLayout = new HBox(conteudoEsq, painelDetalhe);
        HBox.setHgrow(conteudoEsq, Priority.ALWAYS);
        view.setCenter(mainLayout);
    }

    private void abrirFormNovo() {
        // Só admin chega aqui (botão está oculto para professor)
        tabela.getSelectionModel().clearSelection();
        turmaSelecionada = null;
        lblAcaoTurma.setText("Nova Turma");
        btnSalvar.setText("Cadastrar Turma");
        txtNome.clear();
        txtAnoLetivo.setText("2026");
        cbEscolaForm.getItems().setAll(escolasDAO.listarTodas());
        cbProfessorForm.getItems().setAll(professorDAO.listarTodos());
        cbProfessorForm.getSelectionModel().clearSelection();

        if (cbEscolasFiltro.getValue() != null) {
            cbEscolaForm.setValue(cbEscolasFiltro.getValue());
        } else {
            cbEscolaForm.getSelectionModel().clearSelection();
        }

        mostrarDetalhe();
    }

    private void abrirDetalheTurma(Turma turma) {
        turmaSelecionada = turma;
        // PATCH: título diferente para professor (somente visualização)
        lblAcaoTurma.setText(mainApp.isAdmin() ? "Editar Turma" : "Detalhes da Turma");
        if (mainApp.isAdmin()) btnSalvar.setText("Salvar Alterações");

        txtNome.setText(turma.getNome());
        txtAnoLetivo.setText(turma.getAnoLetivo());

        cbEscolaForm.getItems().setAll(escolasDAO.listarTodas());
        for (Escola e : cbEscolaForm.getItems()) {
            if (e.getId().equals(turma.getEscolaId())) {
                cbEscolaForm.setValue(e);
                break;
            }
        }

        // PATCH: carrega professores e pré-seleciona o atual
        if (mainApp.isAdmin()) {
            cbProfessorForm.getItems().setAll(professorDAO.listarTodos());
            cbProfessorForm.getSelectionModel().clearSelection();
            if (turma.getProfessorId() != null) {
                for (Professor p : cbProfessorForm.getItems()) {
                    if (p.getId().equals(turma.getProfessorId())) {
                        cbProfessorForm.setValue(p);
                        break;
                    }
                }
            }
        }

        mostrarDetalhe();
    }

    private void cadastrar() {
        // Só admin chega aqui (botão está oculto para professor)
        String nome = txtNome.getText().trim();
        String ano = txtAnoLetivo.getText().trim();
        Escola escola = cbEscolaForm.getValue();

        if (nome.isBlank() || ano.isBlank() || escola == null) {
            mainApp.mostrarAviso("Preencha o nome, ano letivo e selecione uma escola.", true);
            return;
        }

        Professor professorSelecionado = cbProfessorForm.getValue();
        String professorId = professorSelecionado != null ? professorSelecionado.getId() : null;

        boolean sucesso;
        if (turmaSelecionada == null) {
            sucesso = turmaDAO.inserir(escola.getId(), nome, ano);
            // Se criou com sucesso e há professor selecionado, atribui
            if (sucesso && professorId != null) {
                // Busca o ID da turma recém-criada para vincular o professor
                turmaDAO.listarPorEscola(escola.getId()).stream()
                        .filter(t -> t.getNome().equals(nome) && t.getAnoLetivo().equals(ano))
                        .findFirst()
                        .ifPresent(t -> turmaDAO.definirProfessor(t.getId(), professorId));
            }
        } else {
            sucesso = turmaDAO.atualizar(turmaSelecionada.getId(), escola.getId(), nome, ano);
            // Atualiza professor independentemente (pode estar limpando ou trocando)
            if (sucesso) {
                turmaDAO.definirProfessor(turmaSelecionada.getId(), professorId);
            }
        }

        if (sucesso) {
            mainApp.mostrarAviso(turmaSelecionada == null ? "Turma salva com sucesso!" : "Turma atualizada!", false);
            txtNome.clear();
            carregarTurmas();
            fecharDetalhe();
        } else {
            mainApp.mostrarAviso("Erro ao salvar os dados no banco.", true);
        }
    }

    private void mostrarDetalhe() { painelDetalhe.setVisible(true); painelDetalhe.setManaged(true); }

    private void fecharDetalhe() {
        painelDetalhe.setVisible(false);
        painelDetalhe.setManaged(false);
        tabela.getSelectionModel().clearSelection();
        turmaSelecionada = null;
    }

    private void carregarEscolas() {
        cbEscolasFiltro.getItems().add(null);
        cbEscolasFiltro.getItems().addAll(escolasDAO.listarTodas());
    }

    private void carregarTurmas() {
        // PATCH: professor vê apenas suas turmas; admin vê tudo (com filtro opcional por escola)
        if (mainApp.isAdmin()) {
            Escola selecionada = cbEscolasFiltro.getValue();
            if (selecionada == null) {
                dados.setAll(turmaDAO.listarTodas());
            } else {
                dados.setAll(turmaDAO.listarPorEscola(selecionada.getId()));
            }
        } else {
            dados.setAll(turmaDAO.listarPorProfessor(mainApp.getSessao().getId()));
        }
    }

    public void selecionarEscola(Escola escolaAlvo) {
        if (!mainApp.isAdmin()) return; // professor não usa filtro por escola
        for (Escola e : cbEscolasFiltro.getItems()) {
            if (e.getId().equals(escolaAlvo.getId())) {
                cbEscolasFiltro.getSelectionModel().select(e);
                carregarTurmas();
                break;
            }
        }
    }

    public BorderPane getView() { return view; }
}