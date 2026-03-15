package app;

import core.Aluno;
import core.Escola;
import core.Turma;
import dao.AlunoDAO;
import dao.EscolasDAO;
import dao.SupabaseAuthDAO;
import dao.TurmaDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class AlunosView {

    // ── Modelo auxiliar para preview de lote ─────────────────────────────────
    private static class AlunoLote {
        final String nome;
        final String email;
        final SimpleBooleanProperty conflito = new SimpleBooleanProperty(false);

        AlunoLote(String nome, String email) {
            this.nome = nome;
            this.email = email;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private BorderPane view;
    private TableView<Aluno> tabela;
    private final AlunoDAO alunoDAO;
    private final EscolasDAO escolasDAO;
    private final TurmaDAO turmaDAO;
    private final MainFX mainApp;
    private final ObservableList<Aluno> dados = FXCollections.observableArrayList();

    private Aluno alunoSelecionado;

    // Painel de detalhe (cadastro individual)
    private VBox painelDetalhe;
    private Label lblNomeAluno;
    private TextField txtNome, txtEmail;
    private PasswordField txtSenha;
    private ComboBox<Escola> cbEscola;
    private ComboBox<Turma> cbTurma;
    private Label lblDetalheInfo;
    private Button btnSalvar;
    private Button btnVerTurma;

    // Painel de cadastro em lote
    private VBox painelLote;
    private TextArea txtNomesLote;
    private ComboBox<Escola> cbEscolaLote;
    private ComboBox<Turma> cbTurmaLote;
    private PasswordField txtSenhaLote;
    private final ObservableList<AlunoLote> previewDados = FXCollections.observableArrayList();
    private Label lblStatusLote;

    public AlunosView(MainFX mainApp) {
        this.mainApp = mainApp;
        this.alunoDAO = new AlunoDAO();
        this.escolasDAO = new EscolasDAO();
        this.turmaDAO = new TurmaDAO();
        construirInterface();
        carregarDados();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CONSTRUÇÃO DA INTERFACE
    // ═════════════════════════════════════════════════════════════════════════

    private void construirInterface() {
        view = new BorderPane();

        // ── Cabeçalho ──────────────────────────────────────────────────────
        Label lblTitulo = new Label("Alunos");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Label lblEscopo = new Label("Exibindo apenas alunos das suas turmas");
        lblEscopo.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");
        lblEscopo.setVisible(!mainApp.isAdmin());
        lblEscopo.setManaged(!mainApp.isAdmin());

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("Buscar aluno/escola/turma...");
        txtBusca.setPrefWidth(260);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregarDados());

        Button btnNovo = new Button("+ Cadastrar Aluno");
        btnNovo.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNovo.setOnAction(e -> abrirFormNovo());
        btnNovo.setVisible(mainApp.isAdmin());
        btnNovo.setManaged(mainApp.isAdmin());

        Button btnLote = new Button("📋 Cadastro em Lote");
        btnLote.setStyle("-fx-background-color: #6b46c1; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnLote.setOnAction(e -> abrirPainelLote());
        btnLote.setVisible(mainApp.isAdmin());
        btnLote.setManaged(mainApp.isAdmin());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox tituloBox = new VBox(2, lblTitulo, lblEscopo);
        HBox header = new HBox(12, tituloBox, spacer, txtBusca, btnAtualizar, btnNovo, btnLote);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 16, 20));

        // ── Tabela ─────────────────────────────────────────────────────────
        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhum aluno cadastrado."));
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<Aluno, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Aluno, String> colEmail = new TableColumn<>("E-mail");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Aluno, String> colSenha = new TableColumn<>("Senha");
        colSenha.setCellValueFactory(new PropertyValueFactory<>("senha"));
        colSenha.setVisible(mainApp.isAdmin());

        TableColumn<Aluno, String> colEscola = new TableColumn<>("Escola");
        colEscola.setCellValueFactory(new PropertyValueFactory<>("escolaNome"));

        TableColumn<Aluno, String> colTurma = new TableColumn<>("Turma");
        colTurma.setCellValueFactory(new PropertyValueFactory<>("turmaNome"));

        tabela.getColumns().addAll(List.of(colNome, colEmail, colSenha, colEscola, colTurma));

        FilteredList<Aluno> filtrado = new FilteredList<>(dados, a -> true);
        txtBusca.textProperty().addListener((obs, old, term) -> {
            String f = term == null ? "" : term.trim().toLowerCase();
            filtrado.setPredicate(a -> f.isBlank()
                    || a.getNome().toLowerCase().contains(f)
                    || (a.getEmail() != null && a.getEmail().toLowerCase().contains(f))
                    || a.getEscolaNome().toLowerCase().contains(f)
                    || a.getTurmaNome().toLowerCase().contains(f));
        });
        SortedList<Aluno> ordenado = new SortedList<>(filtrado);
        ordenado.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenado);

        tabela.setRowFactory(tv -> {
            TableRow<Aluno> row = new TableRow<>();
            if (mainApp.isAdmin()) {
                ContextMenu cm = new ContextMenu();
                MenuItem miDel = new MenuItem("Excluir Aluno");
                miDel.setStyle("-fx-text-fill: red;");
                miDel.setOnAction(e -> {
                    Aluno a = row.getItem();
                    if (a != null) {
                        if (alunoDAO.excluir(a.getId())) {
                            mainApp.mostrarAviso("Aluno removido com sucesso!", false);
                            carregarDados();
                            fecharPaineis();
                        } else {
                            mainApp.mostrarAviso("Erro ao excluir aluno.", true);
                        }
                    }
                });
                cm.getItems().add(miDel);
                row.emptyProperty().addListener((obs, w, n) -> row.setContextMenu(n ? null : cm));
            }
            row.setOnMouseClicked(ev -> {
                if (ev.getButton() != MouseButton.PRIMARY) return;
                if (row.isEmpty()) return;
                Aluno a = row.getItem();
                if (a != null) abrirDetalheAluno(a);
            });
            return row;
        });

        // ── Painel de detalhe (individual) ────────────────────────────────
        construirPainelDetalhe();

        // ── Painel de cadastro em lote ────────────────────────────────────
        construirPainelLote();

        VBox conteudoEsq = new VBox(header, tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        HBox mainLayout = new HBox(conteudoEsq, painelDetalhe, painelLote);
        HBox.setHgrow(conteudoEsq, Priority.ALWAYS);
        view.setCenter(mainLayout);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAINEL INDIVIDUAL
    // ─────────────────────────────────────────────────────────────────────────

    private void construirPainelDetalhe() {
        painelDetalhe = new VBox(14);
        painelDetalhe.setPadding(new Insets(24));
        painelDetalhe.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
        painelDetalhe.setMinWidth(300);
        painelDetalhe.setMaxWidth(360);
        painelDetalhe.setVisible(false);
        painelDetalhe.setManaged(false);

        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-cursor: hand;");
        btnFechar.setOnAction(e -> fecharPaineis());
        HBox hdrD = new HBox(btnFechar);
        hdrD.setAlignment(Pos.TOP_RIGHT);

        lblNomeAluno = new Label("Novo Aluno");
        lblNomeAluno.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        lblDetalheInfo = new Label();
        lblDetalheInfo.setStyle("-fx-text-fill: #718096; -fx-font-size: 13px;");
        lblDetalheInfo.setWrapText(true);
        lblDetalheInfo.setVisible(false);
        lblDetalheInfo.setManaged(false);

        Label lblFormTitle = new Label("Dados de Acesso e Matricula");
        lblFormTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        txtNome = new TextField();
        txtNome.setPromptText("Nome completo");
        txtNome.setEditable(mainApp.isAdmin());

        txtEmail = new TextField();
        txtEmail.setPromptText("E-mail");
        txtEmail.setEditable(false);

        txtSenha = new PasswordField();
        txtSenha.setPromptText("Senha (min. 6 caracteres)");
        txtSenha.setEditable(mainApp.isAdmin());
        txtSenha.setVisible(mainApp.isAdmin());
        txtSenha.setManaged(mainApp.isAdmin());

        Label lblSenha = new Label("Senha:");
        lblSenha.setVisible(mainApp.isAdmin());
        lblSenha.setManaged(mainApp.isAdmin());

        cbEscola = new ComboBox<>();
        cbEscola.setPromptText("1. Selecione a escola...");
        cbEscola.setMaxWidth(Double.MAX_VALUE);
        cbEscola.setDisable(!mainApp.isAdmin());
        cbEscola.setCellFactory(lv -> celulaEscola());
        cbEscola.setButtonCell(celulaEscolaBtn("1. Selecione a escola..."));
        cbEscola.setOnAction(e -> {
            if (!mainApp.isAdmin()) return;
            Escola esc = cbEscola.getValue();
            if (esc != null) {
                cbTurma.getItems().setAll(turmaDAO.listarPorEscola(esc.getId()));
                cbTurma.setDisable(false);
            }
        });

        cbTurma = new ComboBox<>();
        cbTurma.setPromptText("2. Selecione a turma...");
        cbTurma.setMaxWidth(Double.MAX_VALUE);
        cbTurma.setDisable(true);
        cbTurma.setCellFactory(lv -> celulaTurma());
        cbTurma.setButtonCell(celulaTurmaBtn("2. Selecione a turma..."));

        btnSalvar = new Button("Cadastrar Aluno");
        btnSalvar.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> cadastrar());
        btnSalvar.setVisible(mainApp.isAdmin());
        btnSalvar.setManaged(mainApp.isAdmin());

        btnVerTurma = new Button("Ir para Turma");
        btnVerTurma.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #2d3748; " +
                "-fx-background-radius: 8; -fx-padding: 8 16;");
        btnVerTurma.setMaxWidth(Double.MAX_VALUE);
        btnVerTurma.setVisible(false);
        btnVerTurma.setManaged(false);

        painelDetalhe.getChildren().addAll(
                hdrD, lblNomeAluno, lblDetalheInfo, new Separator(),
                lblFormTitle,
                new Label("Nome:"), txtNome,
                new Label("E-mail:"), txtEmail,
                lblSenha, txtSenha,
                new Label("Escola:"), cbEscola,
                new Label("Turma:"), cbTurma,
                btnSalvar, new Separator(), btnVerTurma
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAINEL EM LOTE
    // ─────────────────────────────────────────────────────────────────────────

    private void construirPainelLote() {
        painelLote = new VBox(0);
        painelLote.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
        painelLote.setMinWidth(420);
        painelLote.setMaxWidth(500);
        painelLote.setVisible(false);
        painelLote.setManaged(false);

        // ── Header ─────────────────────────────────────────────────────────
        Label lblTit = new Label("Cadastro em Lote");
        lblTit.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-cursor: hand;");
        btnFechar.setOnAction(e -> fecharPaineis());

        Region spH = new Region();
        HBox.setHgrow(spH, Priority.ALWAYS);
        HBox hdr = new HBox(12, lblTit, spH, btnFechar);
        hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setPadding(new Insets(14, 16, 12, 20));
        hdr.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        // ── Formulário ─────────────────────────────────────────────────────
        VBox form = new VBox(10);
        form.setPadding(new Insets(16, 20, 12, 20));

        Label lblDica = new Label("""
                Cole os nomes, um por linha.
                O e-mail será gerado automaticamente: analaura@oficina.com
                Duplicatas viram: analaura2@oficina.com""");
        lblDica.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px; " +
                "-fx-background-color: #f7fafc; -fx-padding: 8 10; -fx-background-radius: 6;");
        lblDica.setWrapText(true);

        txtNomesLote = new TextArea();
        txtNomesLote.setPromptText("Ex:\nAna Laura\nJoão Silva\nMaria de Fátima");
        txtNomesLote.setWrapText(true);
        txtNomesLote.setPrefRowCount(6);
        txtNomesLote.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                        "-fx-border-color: #e2e8f0; -fx-font-size: 13px;");

        Label lblEscolaL = campo("Escola");
        cbEscolaLote = new ComboBox<>();
        cbEscolaLote.setPromptText("Selecione a escola...");
        cbEscolaLote.setMaxWidth(Double.MAX_VALUE);
        cbEscolaLote.setCellFactory(lv -> celulaEscola());
        cbEscolaLote.setButtonCell(celulaEscolaBtn("Selecione a escola..."));
        cbEscolaLote.setOnAction(e -> {
            Escola esc = cbEscolaLote.getValue();
            cbTurmaLote.getItems().clear();
            cbTurmaLote.setDisable(true);
            if (esc != null) {
                cbTurmaLote.getItems().setAll(turmaDAO.listarPorEscola(esc.getId()));
                cbTurmaLote.setDisable(false);
            }
        });

        Label lblTurmaL = campo("Turma");
        cbTurmaLote = new ComboBox<>();
        cbTurmaLote.setPromptText("Selecione a turma...");
        cbTurmaLote.setMaxWidth(Double.MAX_VALUE);
        cbTurmaLote.setDisable(true);
        cbTurmaLote.setCellFactory(lv -> celulaTurma());
        cbTurmaLote.setButtonCell(celulaTurmaBtn("Selecione a turma..."));

        Label lblSenhaL = campo("Senha padrão para todos");
        txtSenhaLote = new PasswordField();
        txtSenhaLote.setPromptText("Mínimo 6 caracteres");
        txtSenhaLote.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                        "-fx-border-color: #e2e8f0; -fx-font-size: 13px; -fx-padding: 6 10;");

        Button btnPreview = new Button("Pré-visualizar");
        btnPreview.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #2d3748; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnPreview.setMaxWidth(Double.MAX_VALUE);
        btnPreview.setOnAction(e -> gerarPreview());

        form.getChildren().addAll(
                lblDica,
                campo("Nomes (um por linha)"), txtNomesLote,
                lblEscolaL, cbEscolaLote,
                lblTurmaL, cbTurmaLote,
                lblSenhaL, txtSenhaLote,
                btnPreview
        );

        // ── Tabela de preview ───────────────────────────────────────────────
        Label lblPreviewTit = new Label("Preview dos cadastros");
        lblPreviewTit.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 12px;");

        TableView<AlunoLote> tabelaPreview = new TableView<>();
        tabelaPreview.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabelaPreview.setItems(previewDados);
        tabelaPreview.setPlaceholder(new Label("Clique em Pré-visualizar acima."));
        VBox.setVgrow(tabelaPreview, Priority.ALWAYS);

        TableColumn<AlunoLote, String> cNome = new TableColumn<>("Nome");
        cNome.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().nome));

        TableColumn<AlunoLote, String> cEmail = new TableColumn<>("E-mail gerado");
        cEmail.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().email));

        tabelaPreview.getColumns().addAll(List.of(cNome, cEmail));

        // Colorir linhas com conflito
        tabelaPreview.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(AlunoLote item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                    return;
                }
                // Resolvemos a cor primeiro para evitar o aviso do IntelliJ
                String corFundo = item.conflito.get() ? "#fff5f5" : "#f0fff4";
                setStyle("-fx-background-color: " + corFundo + ";");
            }
        });

        lblStatusLote = new Label();
        lblStatusLote.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a5568;");
        lblStatusLote.setWrapText(true);

        // ── Botão confirmar ─────────────────────────────────────────────────
        Button btnConfirmar = new Button("✓  Cadastrar Todos");
        btnConfirmar.setStyle("-fx-background-color: #38a169; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 10 20; -fx-font-weight: bold; -fx-font-size: 13px;");
        btnConfirmar.setMaxWidth(Double.MAX_VALUE);
        btnConfirmar.setOnAction(e -> cadastrarLote(btnConfirmar));

        VBox areaPreview = new VBox(8, lblPreviewTit, tabelaPreview, lblStatusLote);
        areaPreview.setPadding(new Insets(0, 20, 8, 20));
        VBox.setVgrow(tabelaPreview, Priority.ALWAYS);

        VBox rodape = new VBox(btnConfirmar);
        rodape.setPadding(new Insets(10, 20, 16, 20));
        rodape.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

        painelLote.getChildren().addAll(hdr, form, areaPreview, rodape);
        VBox.setVgrow(areaPreview, Priority.ALWAYS);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // AÇÕES — INDIVIDUAL
    // ═════════════════════════════════════════════════════════════════════════

    private void abrirFormNovo() {
        fecharPaineis();
        alunoSelecionado = null;
        lblNomeAluno.setText("Novo Aluno");
        btnSalvar.setText("Cadastrar Aluno");
        txtNome.clear();
        txtEmail.clear();
        txtSenha.clear();
        txtEmail.setEditable(true);
        txtEmail.setDisable(false);
        cbEscola.getItems().setAll(escolasDAO.listarTodas());
        cbTurma.getItems().clear();
        cbTurma.setDisable(true);
        lblDetalheInfo.setVisible(false);
        lblDetalheInfo.setManaged(false);
        btnVerTurma.setVisible(false);
        btnVerTurma.setManaged(false);
        mostrarDetalhe();
    }

    private void abrirDetalheAluno(Aluno aluno) {
        fecharPaineis();
        alunoSelecionado = aluno;
        lblNomeAluno.setText(mainApp.isAdmin() ? "Editar: " + aluno.getNome() : aluno.getNome());
        if (mainApp.isAdmin()) btnSalvar.setText("Salvar Alterações");

        lblDetalheInfo.setText("Matrícula Atual:\nEscola: " + aluno.getEscolaNome()
                + "\nTurma: " + aluno.getTurmaNome());
        lblDetalheInfo.setVisible(true);
        lblDetalheInfo.setManaged(true);

        txtNome.setText(aluno.getNome());
        txtEmail.setText(aluno.getEmail());
        txtEmail.setEditable(false);
        txtEmail.setDisable(true);
        if (mainApp.isAdmin()) txtSenha.setText(aluno.getSenha());

        cbEscola.getItems().setAll(escolasDAO.listarTodas());
        Escola escolaMatch = cbEscola.getItems().stream()
                .filter(e -> e.getNome().equals(aluno.getEscolaNome())).findFirst().orElse(null);

        if (escolaMatch != null) {
            cbEscola.setValue(escolaMatch);
            cbTurma.getItems().setAll(turmaDAO.listarPorEscola(escolaMatch.getId()));
            cbTurma.setDisable(!mainApp.isAdmin());
            cbTurma.getItems().stream()
                    .filter(t -> t.getId().equals(aluno.getTurmaId()))
                    .findFirst().ifPresent(cbTurma::setValue);
        } else {
            cbTurma.getItems().clear();
            cbTurma.setDisable(true);
        }

        btnVerTurma.setVisible(true);
        btnVerTurma.setManaged(true);
        btnVerTurma.setOnAction(ev -> mainApp.abrirTurmas(null));
        mostrarDetalhe();
    }

    private void cadastrar() {
        String nome = txtNome.getText().trim();
        String email = txtEmail.getText().trim();
        String senha = txtSenha.getText();
        Turma turma = cbTurma.getValue();

        if (nome.isBlank() || email.isBlank() || senha.length() < 6 || turma == null) {
            mainApp.mostrarAviso("Preencha tudo e selecione uma turma (Senha min. 6).", true);
            return;
        }

        if (alunoSelecionado == null) {
            String novoId = SupabaseAuthDAO.criarUsuarioAuth(email, senha);
            if (novoId != null) {
                if (alunoDAO.inserir(novoId, nome, email, senha, turma.getId())) {
                    mainApp.mostrarAviso("Aluno cadastrado com sucesso!", false);
                    txtNome.clear();
                    txtEmail.clear();
                    txtSenha.clear();
                    carregarDados();
                    fecharPaineis();
                } else {
                    SupabaseAuthDAO.deletarUsuarioAuth(novoId);
                    mainApp.mostrarAviso("Erro de banco de dados. Tente novamente.", true);
                }
            } else {
                mainApp.mostrarAviso("E-mail já cadastrado no sistema!", true);
            }
        } else {
            if (alunoDAO.atualizar(alunoSelecionado.getId(), nome, email, senha, turma.getId())) {
                mainApp.mostrarAviso("Aluno atualizado com sucesso!", false);
                txtNome.clear();
                txtEmail.clear();
                txtSenha.clear();
                carregarDados();
                fecharPaineis();
            } else {
                mainApp.mostrarAviso("Erro ao atualizar aluno.", true);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // AÇÕES — LOTE
    // ═════════════════════════════════════════════════════════════════════════

    private void abrirPainelLote() {
        fecharPaineis();
        cbEscolaLote.getItems().setAll(escolasDAO.listarTodas());
        cbTurmaLote.getItems().clear();
        cbTurmaLote.setDisable(true);
        txtNomesLote.clear();
        txtSenhaLote.clear();
        previewDados.clear();
        lblStatusLote.setText("");
        painelLote.setVisible(true);
        painelLote.setManaged(true);
    }

    /**
     * Gera a lista de preview com e-mails deduplicated.
     */
    private void gerarPreview() {
        previewDados.clear();
        lblStatusLote.setText("");

        String raw = txtNomesLote.getText();
        if (raw == null || raw.isBlank()) {
            mainApp.mostrarAviso("Cole pelo menos um nome.", true);
            return;
        }

        // Coleta e-mails já existentes para marcar conflitos
        Set<String> emailsExistentes = new HashSet<>();
        for (Aluno a : dados) {
            if (a.getEmail() != null) emailsExistentes.add(a.getEmail().toLowerCase());
        }

        // Gera e-mails únicos dentro do lote também
        Set<String> emailsLote = new HashSet<>();
        List<AlunoLote> gerados = new ArrayList<>();

        String[] linhas = raw.split("\\r?\\n");
        for (String linha : linhas) {
            String nome = linha.trim();
            if (nome.isBlank()) continue;

            String base = emailBase(nome);
            String email = base + "@oficina.com";
            int sufixo = 2;
            // Evita colisão tanto com existentes quanto com outros do mesmo lote
            while (emailsExistentes.contains(email) || emailsLote.contains(email)) {
                email = base + sufixo + "@oficina.com";
                sufixo++;
            }
            emailsLote.add(email);

            AlunoLote al = new AlunoLote(nome, email);
            al.conflito.set(emailsExistentes.contains(email)); // marca amarelo se já existe no banco
            gerados.add(al);
        }

        previewDados.setAll(gerados);
        lblStatusLote.setText(gerados.size() + " aluno(s) prontos para cadastro. "
                + "Linhas em verde = novos  |  Rosa = e-mail já existia (será ajustado automaticamente).");
    }

    /**
     * Converte nome em base de e-mail: "Ana Laura Ferreira" → "analauraferreira"
     */
    private static String emailBase(String nome) {
        // Normaliza acentos (NFD → remove diacríticos → ASCII)
        String normalizado = Normalizer.normalize(nome, Normalizer.Form.NFD);
        normalizado = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalizado).replaceAll("");
        // Lowercase, remove tudo que não é letra ou número, remove espaços
        return normalizado.toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", "");
    }

    /**
     * Cadastra o lote após confirmação.
     */
    private void cadastrarLote(Button btnConfirmar) {
        if (previewDados.isEmpty()) {
            mainApp.mostrarAviso("Gere o preview primeiro.", true);
            return;
        }

        Turma turma = cbTurmaLote.getValue();
        String senha = txtSenhaLote.getText();

        if (turma == null) {
            mainApp.mostrarAviso("Selecione a turma de destino.", true);
            return;
        }
        if (senha.length() < 6) {
            mainApp.mostrarAviso("Senha padrão deve ter no mínimo 6 caracteres.", true);
            return;
        }

        // Confirma com o usuário
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Confirmar o cadastro de " + previewDados.size() + " aluno(s) na turma "
                        + turma.getNome() + "?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar Cadastro em Lote");
        alert.setHeaderText(null);
        Optional<ButtonType> resultado = alert.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.YES) return;

        btnConfirmar.setDisable(true);
        btnConfirmar.setText("Cadastrando…");

        final List<AlunoLote> lista = List.copyOf(previewDados);
        final String turmaId = turma.getId();
        final String senhaFinal = senha;

        new Thread(() -> {
            int ok = 0, erros = 0;
            for (AlunoLote al : lista) {
                String novoId = SupabaseAuthDAO.criarUsuarioAuth(al.email, senhaFinal);
                if (novoId != null) {
                    if (alunoDAO.inserir(novoId, al.nome, al.email, senhaFinal, turmaId)) {
                        ok++;
                    } else {
                        SupabaseAuthDAO.deletarUsuarioAuth(novoId);
                        erros++;
                    }
                } else {
                    erros++;
                }
            }
            final int finalOk = ok;
            final int finalErros = erros;
            Platform.runLater(() -> {
                btnConfirmar.setDisable(false);
                btnConfirmar.setText("✓  Cadastrar Todos");
                carregarDados();
                fecharPaineis();
                if (finalErros == 0) {
                    mainApp.mostrarAviso(finalOk + " aluno(s) cadastrado(s) com sucesso!", false);
                } else {
                    mainApp.mostrarAviso(finalOk + " cadastrado(s), " + finalErros
                            + " falha(s). Verifique duplicatas.", true);
                }
            });
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private void mostrarDetalhe() {
        painelDetalhe.setVisible(true);
        painelDetalhe.setManaged(true);
    }

    private void fecharPaineis() {
        painelDetalhe.setVisible(false);
        painelDetalhe.setManaged(false);
        painelLote.setVisible(false);
        painelLote.setManaged(false);
        tabela.getSelectionModel().clearSelection();
        alunoSelecionado = null;
    }

    private void carregarDados() {
        if (mainApp.isAdmin()) dados.setAll(alunoDAO.listarTodos());
        else dados.setAll(alunoDAO.listarPorProfessor(mainApp.getSessao().getId()));
    }

    private Label campo(String texto) {
        Label l = new Label(texto + ":");
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 12px;");
        return l;
    }

    private ListCell<Escola> celulaEscola() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Escola e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? null : e.getNome());
            }
        };
    }

    private ListCell<Escola> celulaEscolaBtn(String placeholder) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Escola e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? placeholder : e.getNome());
            }
        };
    }

    private ListCell<Turma> celulaTurma() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getNome());
            }
        };
    }

    private ListCell<Turma> celulaTurmaBtn(String placeholder) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? placeholder : t.getNome());
            }
        };
    }

    public BorderPane getView() {
        return view;
    }

}