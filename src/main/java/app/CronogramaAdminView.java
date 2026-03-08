package app;

import core.GrupoCronograma;
import core.Professor;
import core.Turma;
import dao.CronogramaDAO;
import dao.ProfessorDAO;
import dao.TurmaDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CronogramaAdminView {

    private static final String[] DIAS =
            {"SEGUNDA","TERCA","QUARTA","QUINTA","SEXTA","SABADO"};

    private BorderPane view;
    private MainFX mainApp;
    private CronogramaDAO cronogramaDAO = new CronogramaDAO();
    private ProfessorDAO  professorDAO  = new ProfessorDAO();
    private TurmaDAO      turmaDAO      = new TurmaDAO();

    private TableView<GrupoCronograma> tabela;
    private ObservableList<GrupoCronograma> todos  = FXCollections.observableArrayList();
    private FilteredList<GrupoCronograma>   filtro;

    private VBox       painelForm;
    private Label      lblFormTitulo;
    private ComboBox<Professor> cbProfessor;
    private ComboBox<Turma>     cbTurma;
    private ComboBox<String>    cbTipo;

    private final Map<String, CheckBox> checkDias = new LinkedHashMap<>();
    private FlowPane painelCheckDias;

    private DatePicker dpDataInicio, dpDataFim, dpDataEspecifica;
    private TextField  txtInicio, txtFim;
    private Label      lblDiaEspecifico, lblPeriodo, lblDiasSemana;
    private Button     btnSalvar;
    private GrupoCronograma editandoGrupo;

    private ComboBox<Professor> cbFiltroProf;

    public CronogramaAdminView(MainFX mainApp) {
        this.mainApp = mainApp;
        construirInterface();
        carregar();
    }

    private void construirInterface() {
        view = new BorderPane();

        Label lbl = new Label("Cronograma — Administracao");
        lbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        cbFiltroProf = new ComboBox<>();
        cbFiltroProf.setPromptText("Todos os professores");
        cbFiltroProf.setPrefWidth(220);
        cbFiltroProf.getItems().add(null);
        cbFiltroProf.getItems().addAll(professorDAO.listarTodos());
        cbFiltroProf.setCellFactory(lv -> celulaProfessor());
        cbFiltroProf.setButtonCell(celulaProfessor());
        cbFiltroProf.setOnAction(e -> aplicarFiltro());

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregar());

        Button btnNovo = new Button("+ Novo Horario");
        btnNovo.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNovo.setOnAction(e -> abrirFormNovo());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox header = new HBox(12, lbl, sp, new Label("Prof:"), cbFiltroProf, btnAtualizar, btnNovo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 12, 20));

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhum horario cadastrado."));
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<GrupoCronograma, String> colProf = new TableColumn<>("Professor");
        colProf.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getProfessorNome()));

        TableColumn<GrupoCronograma, String> colTurma = new TableColumn<>("Turma");
        colTurma.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTurmaNome()));

        TableColumn<GrupoCronograma, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTipoLabel()));
        colTipo.setMaxWidth(120);

        TableColumn<GrupoCronograma, String> colDias = new TableColumn<>("Dias");
        colDias.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDiasFormatados()));
        colDias.setMinWidth(140);

        TableColumn<GrupoCronograma, String> colHor = new TableColumn<>("Horario");
        colHor.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getHorarioFormatado()));
        colHor.setMaxWidth(120);

        TableColumn<GrupoCronograma, String> colPeriodo = new TableColumn<>("Periodo");
        colPeriodo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPeriodo()));

        tabela.getColumns().addAll(colProf, colTurma, colTipo, colDias, colHor, colPeriodo);

        filtro = new FilteredList<>(todos, a -> true);
        tabela.setItems(filtro);

        tabela.setRowFactory(tv -> {
            TableRow<GrupoCronograma> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem miEd  = new MenuItem("Editar");
            MenuItem miDel = new MenuItem("Excluir");
            miDel.setStyle("-fx-text-fill: red;");
            miEd.setOnAction(e -> { if (row.getItem() != null) abrirFormEditar(row.getItem()); });
            // Sem confirmacao: exclui direto com toast
            miDel.setOnAction(e -> excluirGrupo(row.getItem()));
            cm.getItems().addAll(miEd, miDel);
            row.emptyProperty().addListener((o, w, n) -> row.setContextMenu(n ? null : cm));
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) abrirFormEditar(row.getItem());
            });
            return row;
        });

        // ── Formulario lateral ────────────────────────────────────────────
        painelForm = new VBox(10);
        painelForm.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
        painelForm.setMinWidth(300); painelForm.setMaxWidth(340);
        painelForm.setVisible(false); painelForm.setManaged(false);

        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-cursor: hand;");
        btnFechar.setOnAction(e -> fecharForm());
        HBox hdrF = new HBox(btnFechar); hdrF.setAlignment(Pos.TOP_RIGHT);

        lblFormTitulo = new Label("Novo Horario");
        lblFormTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        cbProfessor = new ComboBox<>();
        cbProfessor.setPromptText("Professor..."); cbProfessor.setMaxWidth(Double.MAX_VALUE);
        cbProfessor.setCellFactory(lv -> celulaProfessor());
        cbProfessor.setButtonCell(celulaProfessor());
        cbProfessor.setOnAction(e -> {
            Professor p = cbProfessor.getValue();
            cbTurma.getItems().clear();
            if (p != null) cbTurma.getItems().setAll(turmaDAO.listarPorProfessor(p.getId()));
        });

        cbTurma = new ComboBox<>();
        cbTurma.setPromptText("Turma..."); cbTurma.setMaxWidth(Double.MAX_VALUE);
        cbTurma.setCellFactory(lv -> celulaTurma()); cbTurma.setButtonCell(celulaTurma());

        cbTipo = new ComboBox<>(FXCollections.observableArrayList(
                "AULA", "REUNIAO", "AULA_SUBSTITUTA"));
        cbTipo.setValue("AULA"); cbTipo.setMaxWidth(Double.MAX_VALUE);
        cbTipo.setOnAction(e -> atualizarVisibilidadeTipo());

        lblDiasSemana = new Label("Dias da Semana:");
        lblDiasSemana.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        painelCheckDias = new FlowPane(10, 6);
        painelCheckDias.setPadding(new Insets(4, 0, 4, 0));

        for (String dia : DIAS) {
            CheckBox cb = new CheckBox(abreviarDia(dia));
            cb.setStyle("-fx-font-size: 12px;");
            checkDias.put(dia, cb);
            painelCheckDias.getChildren().add(cb);
        }

        Label lblDicaDias = new Label("Selecione um ou mais dias");
        lblDicaDias.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 11px;");

        lblPeriodo = new Label("Periodo de vigencia:");
        lblPeriodo.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");
        dpDataInicio = new DatePicker(); dpDataInicio.setPromptText("Data inicio");
        dpDataInicio.setMaxWidth(Double.MAX_VALUE);
        dpDataFim    = new DatePicker(); dpDataFim.setPromptText("Data fim");
        dpDataFim.setMaxWidth(Double.MAX_VALUE);

        lblDiaEspecifico = new Label("Data do evento:");
        lblDiaEspecifico.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");
        dpDataEspecifica = new DatePicker(LocalDate.now());
        dpDataEspecifica.setMaxWidth(Double.MAX_VALUE);

        txtInicio = new TextField(); txtInicio.setPromptText("Inicio: 08:00");
        txtFim    = new TextField(); txtFim.setPromptText("Fim: 09:30");

        Label lblDicaHor = new Label("Horario no formato HH:mm");
        lblDicaHor.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 11px;");

        btnSalvar = new Button("Salvar");
        btnSalvar.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> salvar());

        VBox conteudoForm = new VBox(10,
                hdrF, lblFormTitulo, new Separator(),
                new Label("Professor:"),    cbProfessor,
                new Label("Turma:"),        cbTurma,
                new Label("Tipo:"),         cbTipo,
                lblDiasSemana,              painelCheckDias, lblDicaDias,
                lblPeriodo,                 dpDataInicio, dpDataFim,
                lblDiaEspecifico,           dpDataEspecifica,
                new Label("Horario Inicio:"), txtInicio,
                new Label("Horario Fim:"),    txtFim,
                lblDicaHor,                 btnSalvar
        );
        conteudoForm.setPadding(new Insets(20));

        ScrollPane scrollForm = new ScrollPane(conteudoForm);
        scrollForm.setFitToWidth(true);
        scrollForm.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollForm.setStyle("-fx-background: white; -fx-background-color: white;");
        VBox.setVgrow(scrollForm, Priority.ALWAYS);

        painelForm.getChildren().add(scrollForm);
        atualizarVisibilidadeTipo();

        VBox centro = new VBox(header, tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        HBox main = new HBox(centro, painelForm);
        HBox.setHgrow(centro, Priority.ALWAYS);
        view.setCenter(main);
    }

    private void atualizarVisibilidadeTipo() {
        String tipo = cbTipo.getValue();
        boolean isAula = "AULA".equals(tipo);

        lblDiasSemana.setVisible(isAula);    lblDiasSemana.setManaged(isAula);
        painelCheckDias.setVisible(isAula);  painelCheckDias.setManaged(isAula);
        lblPeriodo.setVisible(isAula);       lblPeriodo.setManaged(isAula);
        dpDataInicio.setVisible(isAula);     dpDataInicio.setManaged(isAula);
        dpDataFim.setVisible(isAula);        dpDataFim.setManaged(isAula);

        lblDiaEspecifico.setVisible(!isAula); lblDiaEspecifico.setManaged(!isAula);
        dpDataEspecifica.setVisible(!isAula); dpDataEspecifica.setManaged(!isAula);
    }

    private void carregar() {
        todos.setAll(GrupoCronograma.agrupar(cronogramaDAO.listarTodos()));
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        Professor p = cbFiltroProf.getValue();
        filtro.setPredicate(p == null ? g -> true : g -> p.getId().equals(g.getProfessorId()));
    }

    private void abrirFormNovo() {
        editandoGrupo = null;
        lblFormTitulo.setText("Novo Horario");
        btnSalvar.setText("Adicionar");

        cbProfessor.getItems().setAll(professorDAO.listarTodos());
        cbProfessor.getSelectionModel().clearSelection();
        cbProfessor.setDisable(false);
        cbTurma.getItems().clear();
        cbTurma.setDisable(false);
        cbTipo.setValue("AULA");

        checkDias.values().forEach(cb -> { cb.setSelected(false); cb.setDisable(false); });

        dpDataInicio.setValue(null);
        dpDataFim.setValue(null);
        dpDataEspecifica.setValue(LocalDate.now());
        txtInicio.clear(); txtFim.clear();
        atualizarVisibilidadeTipo();
        mostrarForm();
    }

    private void abrirFormEditar(GrupoCronograma grupo) {
        editandoGrupo = grupo;
        lblFormTitulo.setText("Editar Horario");
        btnSalvar.setText("Salvar Alteracoes");

        cbProfessor.getItems().setAll(professorDAO.listarTodos());
        cbProfessor.getItems().stream()
                .filter(p -> p.getId().equals(grupo.getProfessorId()))
                .findFirst().ifPresent(cbProfessor::setValue);
        cbProfessor.setDisable(true);

        cbTurma.getItems().setAll(turmaDAO.listarPorProfessor(grupo.getProfessorId()));
        cbTurma.getItems().stream()
                .filter(t -> t.getId().equals(grupo.getTurmaId()))
                .findFirst().ifPresent(cbTurma::setValue);
        cbTurma.setDisable(true);

        cbTipo.setValue(grupo.getTipo());

        checkDias.forEach((dia, cb) -> {
            cb.setSelected(grupo.getDias().contains(dia));
            cb.setDisable(false);
        });

        try {
            if (grupo.getDataInicio() != null)
                dpDataInicio.setValue(LocalDate.parse(grupo.getDataInicio()));
            if (grupo.getDataFim() != null)
                dpDataFim.setValue(LocalDate.parse(grupo.getDataFim()));
            if (grupo.isOcasional() && grupo.getDataInicio() != null)
                dpDataEspecifica.setValue(LocalDate.parse(grupo.getDataInicio()));
        } catch (Exception ignored) {}

        txtInicio.setText(grupo.getHorarioInicio());
        txtFim.setText(grupo.getHorarioFim());
        atualizarVisibilidadeTipo();
        mostrarForm();
    }

    private void salvar() {
        Professor prof = cbProfessor.getValue();
        Turma turma    = cbTurma.getValue();
        String tipo    = cbTipo.getValue();
        String inicio  = txtInicio.getText().trim();
        String fim     = txtFim.getText().trim();

        if ((prof == null && editandoGrupo == null) || turma == null
                || inicio.isBlank() || fim.isBlank()) {
            mainApp.mostrarAviso("Preencha todos os campos.", true); return;
        }
        if (!inicio.matches("\\d{2}:\\d{2}") || !fim.matches("\\d{2}:\\d{2}")) {
            mainApp.mostrarAviso("Horario invalido. Use HH:mm.", true); return;
        }

        if ("AULA".equals(tipo)) salvarAula(prof, turma, inicio, fim);
        else                      salvarOcasional(prof, turma, tipo, inicio, fim);
    }

    private void salvarAula(Professor prof, Turma turma, String inicio, String fim) {
        List<String> diasSelecionados = new ArrayList<>();
        checkDias.forEach((dia, cb) -> { if (cb.isSelected()) diasSelecionados.add(dia); });

        if (diasSelecionados.isEmpty()) {
            mainApp.mostrarAviso("Selecione pelo menos um dia da semana.", true); return;
        }
        if (dpDataInicio.getValue() == null || dpDataFim.getValue() == null) {
            mainApp.mostrarAviso("Defina o periodo de inicio e fim.", true); return;
        }

        LocalDate di = dpDataInicio.getValue();
        LocalDate df = dpDataFim.getValue();

        if (editandoGrupo != null) {
            editandoGrupo.getIds().forEach(cronogramaDAO::excluir);
        }

        String profId  = editandoGrupo != null ? editandoGrupo.getProfessorId() : prof.getId();
        String turmaId = editandoGrupo != null ? editandoGrupo.getTurmaId()     : turma.getId();

        int erros = 0;
        for (String dia : diasSelecionados) {
            if (!cronogramaDAO.inserirAdmin(profId, turmaId, dia, inicio, fim, di, df)) erros++;
        }

        String msg = erros == 0
                ? diasSelecionados.size() + " dia(s) salvo(s) com sucesso!"
                : (diasSelecionados.size() - erros) + "/" + diasSelecionados.size()
                + " dias salvos — " + erros + " conflito(s).";
        mainApp.mostrarAviso(msg, erros > 0);
        fecharForm(); carregar();
    }

    private void salvarOcasional(Professor prof, Turma turma, String tipo, String inicio, String fim) {
        LocalDate data = dpDataEspecifica.getValue();
        if (data == null) { mainApp.mostrarAviso("Selecione a data do evento.", true); return; }

        String dia    = diaSemanaPortugues(data.getDayOfWeek());
        String profId = editandoGrupo != null ? editandoGrupo.getProfessorId() : prof.getId();

        boolean ok;
        if (editandoGrupo != null && !editandoGrupo.getIds().isEmpty()) {
            ok = cronogramaDAO.atualizar(editandoGrupo.getIds().get(0), dia, inicio, fim, data, data);
        } else {
            ok = cronogramaDAO.inserirOcasional(profId, turma.getId(), dia,
                    inicio, fim, tipo, data, "ADMIN");
        }

        mainApp.mostrarAviso(ok ? "Evento salvo!" : "Erro ao salvar evento.", !ok);
        if (ok) { fecharForm(); carregar(); }
    }

    // Sem confirmacao: exclui direto com feedback via toast
    private void excluirGrupo(GrupoCronograma grupo) {
        if (grupo == null) return;
        int excluidos = (int) grupo.getIds().stream()
                .filter(cronogramaDAO::excluir).count();
        mainApp.mostrarAviso(excluidos + " dia(s) removido(s).", excluidos == 0);
        carregar();
    }

    private void mostrarForm() { painelForm.setVisible(true); painelForm.setManaged(true); }

    private void fecharForm() {
        painelForm.setVisible(false); painelForm.setManaged(false);
        cbProfessor.setDisable(false); cbTurma.setDisable(false);
        tabela.getSelectionModel().clearSelection(); editandoGrupo = null;
    }

    private String abreviarDia(String dia) {
        return switch (dia) {
            case "SEGUNDA" -> "Seg"; case "TERCA"  -> "Ter"; case "QUARTA" -> "Qua";
            case "QUINTA"  -> "Qui"; case "SEXTA"  -> "Sex"; case "SABADO" -> "Sab";
            default -> dia;
        };
    }

    private String diaSemanaPortugues(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY    -> "SEGUNDA"; case TUESDAY   -> "TERCA";
            case WEDNESDAY -> "QUARTA";  case THURSDAY  -> "QUINTA";
            case FRIDAY    -> "SEXTA";   case SATURDAY  -> "SABADO";
            default        -> "DOMINGO";
        };
    }

    private ListCell<Professor> celulaProfessor() {
        return new ListCell<>() {
            @Override protected void updateItem(Professor p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? (getListView() == null ? "Todos os professores" : "Professor...") : p.getNome());
            }
        };
    }

    private ListCell<Turma> celulaTurma() {
        return new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "Turma..." : t.getNome());
            }
        };
    }

    public BorderPane getView() { return view; }
}