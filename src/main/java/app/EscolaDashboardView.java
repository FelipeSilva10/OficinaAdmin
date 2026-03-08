package app;

import core.Escola;
import core.Professor;
import core.Turma;
import dao.ProfessorDAO;
import dao.TurmaDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class EscolaDashboardView {

    private BorderPane view;
    private Escola escola;
    private MainFX mainApp;

    private TableView<Turma> tabelaTurmas;
    private TurmaDAO turmaDAO;
    private ProfessorDAO professorDAO;

    private VBox painelGerenciar;
    private Label lblTurmaNome;
    private ComboBox<Professor> cbProfessoresGlobais;

    public EscolaDashboardView(MainFX mainApp, Escola escola) {
        this.mainApp = mainApp;
        this.escola = escola;
        this.turmaDAO = new TurmaDAO();
        this.professorDAO = new ProfessorDAO();
        construirInterface();
        carregarDados();
    }

    private void construirInterface() {
        view = new BorderPane();
        view.setPadding(new Insets(20));

        Button btnVoltar = new Button("← Voltar para Escolas");
        btnVoltar.setOnAction(e -> mainApp.abrirEscolas());
        btnVoltar.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #0366d6;");

        Label lblTitulo = new Label("Painel da Escola: " + escola.getNome());
        lblTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        HBox header = new HBox(15, btnVoltar, lblTitulo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));
        view.setTop(header);

        SplitPane splitPane = new SplitPane(criarPainelEsquerdo(), criarPainelDireito());
        splitPane.setDividerPositions(0.65);
        view.setCenter(splitPane);
    }

    private VBox criarPainelEsquerdo() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));

        if (mainApp.isAdmin()) {
            HBox boxNovaTurma = new HBox(10);
            boxNovaTurma.setAlignment(Pos.CENTER_LEFT);
            TextField txtNova = new TextField();
            txtNova.setPromptText("Nome da nova turma...");
            Button btnAdd = new Button("Adicionar Turma");
            btnAdd.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
            btnAdd.setOnAction(e -> {
                String nome = txtNova.getText().trim();
                if (nome.isEmpty()) {
                    mainApp.mostrarAviso("Digite o nome da turma!", true);
                    return;
                }
                if (turmaDAO.inserir(escola.getId(), nome, "2026")) {
                    mainApp.mostrarAviso("Turma criada com sucesso!", false);
                    txtNova.clear();
                    carregarDados();
                } else {
                    mainApp.mostrarAviso("Erro ao criar turma.", true);
                }
            });
            boxNovaTurma.getChildren().addAll(txtNova, btnAdd);
            box.getChildren().add(boxNovaTurma);
        }

        tabelaTurmas = new TableView<>();
        tabelaTurmas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Turma, String> colNome = new TableColumn<>("Turma");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Turma, String> colAno = new TableColumn<>("Ano");
        colAno.setCellValueFactory(new PropertyValueFactory<>("anoLetivo"));
        colAno.setMaxWidth(100);

        TableColumn<Turma, String> colProf = new TableColumn<>("Professor Regente");
        colProf.setCellValueFactory(new PropertyValueFactory<>("professorNome"));

        tabelaTurmas.getColumns().addAll(colNome, colAno, colProf);

        tabelaTurmas.setRowFactory(tv -> {
            TableRow<Turma> row = new TableRow<>();

            if (mainApp.isAdmin()) {
                ContextMenu cm = new ContextMenu();
                MenuItem mi = new MenuItem("Excluir Turma");
                mi.setStyle("-fx-text-fill: red;");
                mi.setOnAction(evt -> {
                    Turma t = row.getItem();
                    if (t != null) {
                        if (turmaDAO.excluir(t.getId())) {
                            mainApp.mostrarAviso("Turma excluida!", false);
                            carregarDados();
                        } else {
                            mainApp.mostrarAviso("Erro ao excluir turma.", true);
                        }
                    }
                });
                cm.getItems().add(mi);
                row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) ->
                        row.setContextMenu(isNowEmpty ? null : cm));
            }

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    mainApp.abrirDashboardTurma(row.getItem());
                }
            });

            if (mainApp.isAdmin()) {
                tabelaTurmas.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) atualizarPainelDireito(newSel);
                });
            }

            return row;
        });

        box.getChildren().addAll(new Label("Turmas da Escola"), tabelaTurmas);
        VBox.setVgrow(tabelaTurmas, Priority.ALWAYS);
        return box;
    }

    private VBox criarPainelDireito() {
        if (!mainApp.isAdmin()) {
            VBox aviso = new VBox(new Label("Selecione uma turma para ver os detalhes."));
            aviso.setPadding(new Insets(10));
            return aviso;
        }

        painelGerenciar = new VBox(15);
        painelGerenciar.setPadding(new Insets(20));
        painelGerenciar.setStyle("-fx-background-color: #f6f8fa; -fx-border-color: #e1e4e8; -fx-border-radius: 8px;");
        painelGerenciar.setVisible(false);

        Label tituloGerencia = new Label("Gerenciar Regencia");
        tituloGerencia.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        lblTurmaNome = new Label("Turma: ");

        cbProfessoresGlobais = new ComboBox<>();
        cbProfessoresGlobais.setPromptText("Selecione um Professor...");
        cbProfessoresGlobais.setMaxWidth(Double.MAX_VALUE);

        Button btnSalvar = new Button("Salvar Alteracao");
        btnSalvar.setStyle("-fx-background-color: #2ea043; -fx-text-fill: white;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> salvarProfessorNaTurma());

        painelGerenciar.getChildren().addAll(tituloGerencia, lblTurmaNome,
                new Label("Atribuir Professor:"), cbProfessoresGlobais, btnSalvar);

        VBox wrapper = new VBox(new Label("Selecione uma turma na tabela para gerenciar."), painelGerenciar);
        wrapper.setPadding(new Insets(10));
        return wrapper;
    }

    private void atualizarPainelDireito(Turma turma) {
        if (!mainApp.isAdmin() || painelGerenciar == null) return;
        lblTurmaNome.setText("Turma: " + turma.getNome() + " (" + turma.getAnoLetivo() + ")");
        cbProfessoresGlobais.getItems().setAll(professorDAO.listarTodos());
        painelGerenciar.setVisible(true);
    }

    private void salvarProfessorNaTurma() {
        Turma selecionada = tabelaTurmas.getSelectionModel().getSelectedItem();
        Professor profSelecionado = cbProfessoresGlobais.getValue();

        if (selecionada != null && profSelecionado != null) {
            if (turmaDAO.definirProfessor(selecionada.getId(), profSelecionado.getId())) {
                mainApp.mostrarAviso("Professor vinculado com sucesso!", false);
                carregarDados();
                painelGerenciar.setVisible(false);
            } else {
                mainApp.mostrarAviso("Erro ao vincular professor.", true);
            }
        }
    }

    private void carregarDados() {
        tabelaTurmas.getItems().setAll(turmaDAO.listarPorEscola(escola.getId()));
    }

    public BorderPane getView() { return view; }
}