package app;

import core.CronogramaAula;
import core.Turma;
import dao.CronogramaDAO;
import dao.TurmaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class CronogramaView {

    private static final String[] DIAS =
            {"SEGUNDA","TERCA","QUARTA","QUINTA","SEXTA","SABADO"};

    private BorderPane view;
    private MainFX mainApp;
    private CronogramaDAO cronogramaDAO = new CronogramaDAO();
    private TurmaDAO turmaDAO = new TurmaDAO();

    private GridPane gridSemanal;
    private ObservableList<CronogramaAula> dados = FXCollections.observableArrayList();
    private TableView<CronogramaAula> tabela;

    private VBox      painelForm;
    private ComboBox<Turma>   cbTurma;
    private ComboBox<String>  cbTipo;
    private DatePicker        dpData;
    private TextField         txtInicio, txtFim;
    private CronogramaAula    editando;

    public CronogramaView(MainFX mainApp) {
        this.mainApp = mainApp;
        construirInterface();
        carregar();
    }

    private void construirInterface() {
        view = new BorderPane();

        Label lblTitulo = new Label("Meu Cronograma");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label lblInfo = new Label("Horarios de aula regulares sao definidos pelo administrador. " +
                "Voce pode registrar reunioes e aulas substitutas.");
        lblInfo.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");
        lblInfo.setWrapText(true);

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregar());

        Button btnNovo = new Button("+ Reuniao / Substituta");
        btnNovo.setStyle("-fx-background-color: #6b46c1; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnNovo.setOnAction(e -> abrirFormOcasional());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox headerTop = new HBox(12, lblTitulo, sp, btnAtualizar, btnNovo);
        headerTop.setAlignment(Pos.CENTER_LEFT);
        headerTop.setPadding(new Insets(20, 20, 4, 20));

        HBox headerInfo = new HBox(lblInfo);
        headerInfo.setPadding(new Insets(0, 20, 12, 20));

        gridSemanal = new GridPane();
        gridSemanal.setHgap(8); gridSemanal.setVgap(4);
        gridSemanal.setPadding(new Insets(0, 20, 12, 20));

        for (int i = 0; i < DIAS.length; i++) {
            Label lblDia = new Label(abreviarDia(DIAS[i]));
            lblDia.setMaxWidth(Double.MAX_VALUE);
            lblDia.setAlignment(Pos.CENTER);
            lblDia.setStyle("-fx-background-color: #2d3748; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 4; " +
                    "-fx-background-radius: 6 6 0 0;");
            gridSemanal.add(lblDia, i, 0);
            GridPane.setHgrow(lblDia, Priority.ALWAYS);
        }

        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhum horario no cronograma."));
        tabela.setItems(dados);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<CronogramaAula, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getTipoLabel()));
        cTipo.setMaxWidth(130);

        TableColumn<CronogramaAula, String> cDia = new TableColumn<>("Dia");
        cDia.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getDiaSemana()));
        cDia.setMaxWidth(90);

        TableColumn<CronogramaAula, String> cTurma = new TableColumn<>("Turma");
        cTurma.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getTurmaNome()));

        TableColumn<CronogramaAula, String> cHor = new TableColumn<>("Horario");
        cHor.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getHorarioFormatado()));
        cHor.setMaxWidth(110);

        TableColumn<CronogramaAula, String> cPer = new TableColumn<>("Periodo");
        cPer.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getPeriodo()));

        tabela.getColumns().addAll(cTipo, cDia, cTurma, cHor, cPer);

        // Colorir linhas ocasionais e menu contexto apenas para criados pelo professor
        tabela.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(CronogramaAula item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle(""); setContextMenu(null); return;
                }
                if (item.isOcasional()) {
                    setStyle("-fx-background-color: #faf5ff;");
                    if ("PROFESSOR".equals(item.getCriadoPor())) {
                        ContextMenu cm = new ContextMenu();
                        MenuItem miEd  = new MenuItem("Editar");
                        MenuItem miDel = new MenuItem("Excluir");
                        miDel.setStyle("-fx-text-fill: red;");
                        miEd.setOnAction(e  -> abrirFormEditar(item));
                        miDel.setOnAction(e -> excluir(item));
                        cm.getItems().addAll(miEd, miDel);
                        setContextMenu(cm);
                    } else {
                        setContextMenu(null);
                    }
                } else {
                    setStyle(""); setContextMenu(null);
                }
                setOnMouseClicked(ev -> {
                    if (ev.getClickCount() == 2 && !isEmpty()
                            && item.isOcasional()
                            && "PROFESSOR".equals(item.getCriadoPor())) {
                        abrirFormEditar(item);
                    }
                });
            }
        });

        // ── Formulario ocasional ─────────────────────────────────────────
        painelForm = new VBox(12);
        painelForm.setPadding(new Insets(24));
        painelForm.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
        painelForm.setMinWidth(280); painelForm.setMaxWidth(320);
        painelForm.setVisible(false); painelForm.setManaged(false);

        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-cursor: hand;");
        btnFechar.setOnAction(e -> fecharForm());
        HBox hdrF = new HBox(btnFechar); hdrF.setAlignment(Pos.TOP_RIGHT);

        Label lblFT = new Label("Evento Ocasional");
        lblFT.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        cbTurma = new ComboBox<>();
        cbTurma.setPromptText("Turma..."); cbTurma.setMaxWidth(Double.MAX_VALUE);
        cbTurma.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty); setText(empty || t == null ? null : t.getNome());
            }
        });
        cbTurma.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Turma t, boolean empty) {
                super.updateItem(t, empty); setText(empty || t == null ? "Turma..." : t.getNome());
            }
        });

        cbTipo = new ComboBox<>(FXCollections.observableArrayList("REUNIAO","AULA_SUBSTITUTA"));
        cbTipo.setValue("REUNIAO"); cbTipo.setMaxWidth(Double.MAX_VALUE);

        dpData = new DatePicker(LocalDate.now());
        dpData.setMaxWidth(Double.MAX_VALUE);

        txtInicio = new TextField(); txtInicio.setPromptText("Inicio: 08:00");
        txtFim    = new TextField(); txtFim.setPromptText("Fim: 09:30");

        Label lblDica = new Label("Formato HH:mm");
        lblDica.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 11px;");

        Button btnSalvar = new Button("Salvar");
        btnSalvar.setStyle("-fx-background-color: #6b46c1; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold;");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> salvarOcasional());

        painelForm.getChildren().addAll(
                hdrF, lblFT, new Separator(),
                new Label("Turma:"), cbTurma,
                new Label("Tipo:"), cbTipo,
                new Label("Data:"), dpData,
                new Label("Horario Inicio:"), txtInicio,
                new Label("Horario Fim:"), txtFim,
                lblDica, btnSalvar
        );

        VBox centro = new VBox(headerTop, headerInfo, gridSemanal, tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        HBox main = new HBox(centro, painelForm);
        HBox.setHgrow(centro, Priority.ALWAYS);
        view.setCenter(main);
    }

    private void carregar() {
        String profId = mainApp.getSessao().getId();
        List<CronogramaAula> slots = cronogramaDAO.listarPorProfessor(profId);
        dados.setAll(slots);
        atualizarGrid(slots);
    }

    private void atualizarGrid(List<CronogramaAula> slots) {
        gridSemanal.getChildren().removeIf(n ->
                GridPane.getRowIndex(n) != null && GridPane.getRowIndex(n) > 0);
        int[] linhas = new int[DIAS.length];
        for (CronogramaAula slot : slots) {
            int col = diaParaColuna(slot.getDiaSemana());
            if (col < 0) continue;
            int row = ++linhas[col];

            VBox card = new VBox(2);
            card.setPadding(new Insets(5));
            card.setMaxWidth(Double.MAX_VALUE);

            String bg, border, fg;
            switch (slot.getTipo()) {
                case "REUNIAO"         -> { bg = "#faf5ff"; border = "#d6bcfa"; fg = "#553c9a"; }
                case "AULA_SUBSTITUTA" -> { bg = "#fffaf0"; border = "#fbd38d"; fg = "#744210"; }
                default                -> { bg = "#ebf8ff"; border = "#bee3f8"; fg = "#2b6cb0"; }
            }
            card.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border +
                    "; -fx-border-radius: 4; -fx-background-radius: 4;");

            Label lNome = new Label(slot.getTurmaNome());
            lNome.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-text-fill: " + fg + ";");
            lNome.setWrapText(true);

            Label lHora = new Label(slot.getHorarioFormatado());
            lHora.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a5568;");

            Label lTipo = new Label(slot.getTipoLabel());
            lTipo.setStyle("-fx-font-size: 9px; -fx-text-fill: #718096;");

            card.getChildren().addAll(lNome, lHora, lTipo);
            gridSemanal.add(card, col, row);
        }
        for (int col = 0; col < DIAS.length; col++) {
            if (linhas[col] == 0) {
                Label v = new Label("—");
                v.setStyle("-fx-text-fill: #cbd5e0; -fx-font-size: 12px; -fx-padding: 8 4;");
                v.setMaxWidth(Double.MAX_VALUE); v.setAlignment(Pos.CENTER);
                gridSemanal.add(v, col, 1);
            }
        }
    }

    private void abrirFormOcasional() {
        editando = null;
        cbTurma.getItems().setAll(turmaDAO.listarPorProfessor(mainApp.getSessao().getId()));
        cbTurma.getSelectionModel().clearSelection();
        cbTipo.setValue("REUNIAO");
        dpData.setValue(LocalDate.now());
        txtInicio.clear(); txtFim.clear();
        mostrarForm();
    }

    private void abrirFormEditar(CronogramaAula slot) {
        editando = slot;
        cbTurma.getItems().setAll(turmaDAO.listarPorProfessor(mainApp.getSessao().getId()));
        cbTurma.getItems().stream().filter(t -> t.getId().equals(slot.getTurmaId()))
                .findFirst().ifPresent(cbTurma::setValue);
        cbTipo.setValue(slot.getTipo());
        try {
            if (slot.getDataInicio() != null) dpData.setValue(LocalDate.parse(slot.getDataInicio()));
        } catch (Exception ignored) {}
        txtInicio.setText(slot.getHorarioInicio());
        txtFim.setText(slot.getHorarioFim());
        mostrarForm();
    }

    private void salvarOcasional() {
        Turma turma   = cbTurma.getValue();
        String tipo   = cbTipo.getValue();
        LocalDate dt  = dpData.getValue();
        String inicio = txtInicio.getText().trim();
        String fim    = txtFim.getText().trim();

        if (turma == null || dt == null || inicio.isBlank() || fim.isBlank()) {
            mainApp.mostrarAviso("Preencha todos os campos.", true); return;
        }
        if (!inicio.matches("\\d{2}:\\d{2}") || !fim.matches("\\d{2}:\\d{2}")) {
            mainApp.mostrarAviso("Horario invalido. Use HH:mm.", true); return;
        }

        String dia = diaSemanaPortugues(dt.getDayOfWeek());
        boolean ok;
        if (editando != null) {
            ok = cronogramaDAO.atualizar(editando.getId(), dia, inicio, fim, dt, dt);
        } else {
            ok = cronogramaDAO.inserirOcasional(mainApp.getSessao().getId(),
                    turma.getId(), dia, inicio, fim, tipo, dt, "PROFESSOR");
        }

        if (ok) {
            mainApp.mostrarAviso("Evento salvo no cronograma!", false);
            fecharForm(); carregar();
        } else {
            mainApp.mostrarAviso("Erro ao salvar evento.", true);
        }
    }

    private void excluir(CronogramaAula slot) {
        if (slot == null) return;
        if (cronogramaDAO.excluir(slot.getId())) {
            mainApp.mostrarAviso("Evento removido.", false); carregar();
        } else {
            mainApp.mostrarAviso("Erro ao remover.", true);
        }
    }

    private int diaParaColuna(String dia) {
        for (int i = 0; i < DIAS.length; i++) if (DIAS[i].equals(dia)) return i;
        return -1;
    }

    private String abreviarDia(String dia) {
        return switch (dia) {
            case "SEGUNDA" -> "SEG"; case "TERCA"  -> "TER"; case "QUARTA" -> "QUA";
            case "QUINTA"  -> "QUI"; case "SEXTA"  -> "SEX"; case "SABADO" -> "SAB";
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

    private void mostrarForm() { painelForm.setVisible(true); painelForm.setManaged(true); }
    private void fecharForm()  {
        painelForm.setVisible(false); painelForm.setManaged(false);
        tabela.getSelectionModel().clearSelection(); editando = null;
    }

    public BorderPane getView() { return view; }
}