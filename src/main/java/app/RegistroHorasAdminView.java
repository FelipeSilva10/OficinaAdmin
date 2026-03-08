package app;

import core.Professor;
import core.RegistroHoras;
import dao.ChamadaDAO;
import dao.ProfessorDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class RegistroHorasAdminView {

    private BorderPane view;
    private MainFX mainApp;
    private ChamadaDAO chamadaDAO = new ChamadaDAO();
    private ProfessorDAO professorDAO = new ProfessorDAO();

    private ComboBox<Professor> cbProfessor;
    private ComboBox<String>    cbMes;
    private ComboBox<Integer>   cbAno;

    private TableView<RegistroHoras> tabelaDetalhe;
    private ObservableList<RegistroHoras> dados = FXCollections.observableArrayList();

    private TableView<ProfResumo> tabelaResumo;
    private ObservableList<ProfResumo> resumos = FXCollections.observableArrayList();

    private Label lblTotalAulas, lblTotalHoras, lblMediaPresenca;

    public RegistroHorasAdminView(MainFX mainApp) {
        this.mainApp = mainApp;
        construirInterface();
        carregar();
    }

    private void construirInterface() {
        view = new BorderPane();

        Label lblTitulo = new Label("Registro de Horas — Administracao");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        cbProfessor = new ComboBox<>();
        cbProfessor.setPromptText("Todos os professores");
        cbProfessor.setPrefWidth(220);
        cbProfessor.getItems().add(null);
        cbProfessor.getItems().addAll(professorDAO.listarTodos());
        cbProfessor.setCellFactory(lv -> celulaProfessor());
        cbProfessor.setButtonCell(celulaProfessor());
        cbProfessor.setOnAction(e -> carregar());

        cbMes = new ComboBox<>();
        cbMes.setPrefWidth(160);
        cbMes.getItems().add("Todos os meses");
        for (Month m : Month.values())
            cbMes.getItems().add(m.getDisplayName(TextStyle.FULL, Locale.of("pt","BR")));
        cbMes.setValue("Todos os meses");
        cbMes.setOnAction(e -> carregar());

        cbAno = new ComboBox<>();
        cbAno.setPrefWidth(90);
        int anoAtual = LocalDate.now().getYear();
        for (int a = anoAtual; a >= anoAtual - 3; a--) cbAno.getItems().add(a);
        cbAno.setValue(anoAtual);
        cbAno.setOnAction(e -> carregar());

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregar());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox header = new HBox(12, lblTitulo, sp,
                new Label("Prof:"), cbProfessor,
                new Label("Mes:"), cbMes,
                new Label("Ano:"), cbAno,
                btnAtualizar);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 12, 20));

        lblTotalAulas     = new Label("—");
        lblTotalHoras     = new Label("—");
        lblMediaPresenca  = new Label("—");

        String cardStyle = "-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 16 24;";
        VBox cAulas    = card("Total de Aulas",    lblTotalAulas,    "#3182ce", cardStyle);
        VBox cHoras    = card("Total de Horas",    lblTotalHoras,    "#38a169", cardStyle);
        VBox cPresenca = card("Media de Presenca", lblMediaPresenca, "#d69e2e", cardStyle);

        HBox cards = new HBox(16, cAulas, cHoras, cPresenca);
        cards.setPadding(new Insets(0, 20, 12, 20));

        // Tabela resumo (para pagamento)
        tabelaResumo = new TableView<>();
        tabelaResumo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabelaResumo.setPlaceholder(new Label("Nenhum dado."));
        tabelaResumo.setItems(resumos);
        tabelaResumo.setPrefWidth(300);

        TableColumn<ProfResumo, String> rProf = new TableColumn<>("Professor");
        rProf.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nome));
        TableColumn<ProfResumo, String> rAulas = new TableColumn<>("Aulas");
        rAulas.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().totalAulas)));
        rAulas.setMaxWidth(70);
        TableColumn<ProfResumo, String> rHoras = new TableColumn<>("Horas");
        rHoras.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.1fh", c.getValue().totalHoras)));
        rHoras.setMaxWidth(80);
        TableColumn<ProfResumo, String> rPres = new TableColumn<>("Presenca");
        rPres.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.0f%%", c.getValue().mediaPresenca)));
        rPres.setMaxWidth(80);
        tabelaResumo.getColumns().addAll(rProf, rAulas, rHoras, rPres);

        tabelaResumo.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (nv != null) filtrarDetalhesPorProfessor(nv.professorId);
        });

        Label lblResumoTit = new Label("Resumo por Professor");
        lblResumoTit.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3748;");
        Label lblDica = new Label("Clique numa linha para ver o detalhamento");
        lblDica.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 11px;");
        VBox painelResumo = new VBox(6, lblResumoTit, lblDica, tabelaResumo);
        VBox.setVgrow(tabelaResumo, Priority.ALWAYS);

        // Tabela de detalhes
        tabelaDetalhe = new TableView<>();
        tabelaDetalhe.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabelaDetalhe.setPlaceholder(new Label("Selecione um professor ou aguarde."));
        tabelaDetalhe.setItems(dados);
        VBox.setVgrow(tabelaDetalhe, Priority.ALWAYS);

        TableColumn<RegistroHoras, String> dData = new TableColumn<>("Data");
        dData.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDataFormatada()));
        dData.setMaxWidth(100);
        TableColumn<RegistroHoras, String> dProf = new TableColumn<>("Professor");
        dProf.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProfessorNome()));
        TableColumn<RegistroHoras, String> dTurma = new TableColumn<>("Turma");
        dTurma.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTurmaNome()));
        TableColumn<RegistroHoras, String> dEscola = new TableColumn<>("Escola");
        dEscola.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEscolaNome()));
        TableColumn<RegistroHoras, String> dTipo = new TableColumn<>("Tipo");
        dTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipoLabel()));
        dTipo.setMaxWidth(110);
        TableColumn<RegistroHoras, String> dHor = new TableColumn<>("Horario");
        dHor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHorario()));
        dHor.setMaxWidth(110);
        TableColumn<RegistroHoras, String> dHoras = new TableColumn<>("Horas");
        dHoras.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHorasFormatadas()));
        dHoras.setMaxWidth(70);
        TableColumn<RegistroHoras, String> dPres = new TableColumn<>("Presenca");
        dPres.setCellValueFactory(c -> {
            var r = c.getValue();
            return new SimpleStringProperty(r.getTotalPresentes() + "/" + r.getTotalAlunos());
        });
        dPres.setMaxWidth(80);
        tabelaDetalhe.getColumns().addAll(dData, dProf, dTurma, dEscola, dTipo, dHor, dHoras, dPres);

        Label lblDetTit = new Label("Aulas Detalhadas");
        lblDetTit.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3748;");
        VBox painelDetalhe = new VBox(6, lblDetTit, tabelaDetalhe);
        VBox.setVgrow(tabelaDetalhe, Priority.ALWAYS);

        SplitPane split = new SplitPane(painelResumo, painelDetalhe);
        split.setDividerPositions(0.32);
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox corpo = new VBox(header, cards, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        corpo.setPadding(new Insets(0, 20, 16, 20));
        view.setCenter(corpo);
    }

    private void carregar() {
        Professor prof = cbProfessor.getValue();
        String profId  = prof != null ? prof.getId() : null;
        Integer mes    = resolverMes();
        Integer ano    = cbAno.getValue();

        List<RegistroHoras> lista = chamadaDAO.listarRegistroHorasAdmin(profId, mes, ano);
        dados.setAll(lista);
        atualizarCards(lista);
        atualizarResumos(lista);
    }

    private void filtrarDetalhesPorProfessor(String profId) {
        Integer mes = resolverMes();
        Integer ano = cbAno.getValue();
        List<RegistroHoras> lista = chamadaDAO.listarRegistroHorasAdmin(profId, mes, ano);
        dados.setAll(lista);
        atualizarCards(lista);
    }

    private void atualizarCards(List<RegistroHoras> lista) {
        int total        = lista.size();
        double horas     = lista.stream().mapToDouble(RegistroHoras::getHorasMinistradas).sum();
        double mediaPres = lista.stream()
                .filter(r -> r.getTotalAlunos() > 0)
                .mapToDouble(r -> (double) r.getTotalPresentes() / r.getTotalAlunos() * 100)
                .average().orElse(0);
        lblTotalAulas.setText(String.valueOf(total));
        lblTotalHoras.setText(String.format("%.1fh", horas));
        lblMediaPresenca.setText(String.format("%.0f%%", mediaPres));
    }

    private void atualizarResumos(List<RegistroHoras> lista) {
        Map<String, List<RegistroHoras>> porProf = lista.stream()
                .collect(Collectors.groupingBy(r ->
                        r.getProfessorId() != null ? r.getProfessorId() : ""));

        List<ProfResumo> rs = porProf.entrySet().stream()
                .map(entry -> {
                    List<RegistroHoras> pl = entry.getValue();
                    String nome  = pl.get(0).getProfessorNome();
                    int aulas    = pl.size();
                    double horas = pl.stream().mapToDouble(RegistroHoras::getHorasMinistradas).sum();
                    double pres  = pl.stream()
                            .filter(r -> r.getTotalAlunos() > 0)
                            .mapToDouble(r -> (double) r.getTotalPresentes() / r.getTotalAlunos() * 100)
                            .average().orElse(0);
                    return new ProfResumo(entry.getKey(), nome, aulas, horas, pres);
                })
                .sorted((a, b) -> Double.compare(b.totalHoras, a.totalHoras))
                .toList();

        resumos.setAll(rs);
    }

    private Integer resolverMes() {
        String sel = cbMes.getValue();
        if (sel == null || sel.equals("Todos os meses")) return null;
        Month[] months = Month.values();
        for (int i = 0; i < months.length; i++) {
            if (months[i].getDisplayName(TextStyle.FULL, Locale.of("pt","BR")).equals(sel))
                return i + 1;
        }
        return null;
    }

    private VBox card(String titulo, Label lblVal, String cor, String style) {
        Label lTit = new Label(titulo);
        lTit.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");
        lblVal.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + cor + ";");
        VBox c = new VBox(4, lTit, lblVal);
        c.setStyle(style);
        c.setMinWidth(160);
        return c;
    }

    private ListCell<Professor> celulaProfessor() {
        return new ListCell<>() {
            @Override protected void updateItem(Professor p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty ? null : p == null ? "Todos os professores" : p.getNome());
            }
        };
    }

    public BorderPane getView() { return view; }

    private record ProfResumo(String professorId, String nome,
                              int totalAulas, double totalHoras, double mediaPresenca) {}
}