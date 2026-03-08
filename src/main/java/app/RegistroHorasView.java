package app;

import core.RegistroHoras;
import dao.ChamadaDAO;
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

/**
 * Visão do PROFESSOR — suas próprias horas, com filtro de mês/ano.
 * A visão do admin está em RegistroHorasAdminView.
 */
public class RegistroHorasView {

    private BorderPane view;
    private MainFX mainApp;
    private ChamadaDAO chamadaDAO;

    private TableView<RegistroHoras> tabela;
    private ObservableList<RegistroHoras> dados = FXCollections.observableArrayList();

    private ComboBox<String>  cbMes;
    private ComboBox<Integer> cbAno;

    private Label lblTotalAulas, lblTotalHoras, lblMediaPresenca;

    public RegistroHorasView(MainFX mainApp) {
        this.mainApp    = mainApp;
        this.chamadaDAO = new ChamadaDAO();
        construirInterface();
        carregar();
    }

    private void construirInterface() {
        view = new BorderPane();

        // ── Cabeçalho ──────────────────────────────────────────────────────
        Label lblTitulo = new Label("Meu Registro de Horas");
        lblTitulo.setStyle("-fx-font-size:22px;-fx-font-weight:bold;");

        cbMes = new ComboBox<>();
        cbMes.setPrefWidth(170);
        cbMes.getItems().add("Todos os meses");
        for (Month m : Month.values())
            cbMes.getItems().add(m.getDisplayName(TextStyle.FULL, Locale.of("pt","BR")));
        cbMes.setValue("Todos os meses");
        cbMes.setOnAction(e -> carregar());

        cbAno = new ComboBox<>();
        cbAno.setPrefWidth(100);
        int ano = LocalDate.now().getYear();
        for (int a = ano; a >= ano - 3; a--) cbAno.getItems().add(a);
        cbAno.setValue(ano);
        cbAno.setOnAction(e -> carregar());

        Button btnAt = new Button("Atualizar");
        btnAt.setOnAction(e -> carregar());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox header = new HBox(12, lblTitulo, sp, new Label("Mês:"), cbMes,
                new Label("Ano:"), cbAno, btnAt);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 12, 20));

        // ── Tabela ─────────────────────────────────────────────────────────
        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhuma aula registrada para o período selecionado."));
        tabela.setItems(dados);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<RegistroHoras, String> cData = new TableColumn<>("Data");
        cData.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDataFormatada()));
        cData.setMaxWidth(110);

        TableColumn<RegistroHoras, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTipoLabel()));
        cTipo.setMaxWidth(120);

        TableColumn<RegistroHoras, String> cTurma = new TableColumn<>("Turma");
        cTurma.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTurmaNome()));

        TableColumn<RegistroHoras, String> cEscola = new TableColumn<>("Escola");
        cEscola.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEscolaNome()));

        TableColumn<RegistroHoras, String> cHor = new TableColumn<>("Horário");
        cHor.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getHorario()));
        cHor.setMaxWidth(120);

        TableColumn<RegistroHoras, String> cHoras = new TableColumn<>("Horas");
        cHoras.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getHorasFormatadas()));
        cHoras.setMaxWidth(80);
        cHoras.setStyle("-fx-alignment:CENTER;");

        TableColumn<RegistroHoras, String> cPres = new TableColumn<>("Presença");
        cPres.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTotalPresentes() + "/" + cd.getValue().getTotalAlunos()));
        cPres.setMaxWidth(90);
        cPres.setStyle("-fx-alignment:CENTER;");

        tabela.getColumns().addAll(cData, cTipo, cTurma, cEscola, cHor, cHoras, cPres);

        // Cor por tipo de aula
        tabela.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(RegistroHoras item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                setStyle(switch (item.getTipoAula()) {
                    case "REUNIÃO"         -> "-fx-background-color:#faf5ff;";
                    case "AULA_SUBSTITUTA" -> "-fx-background-color:#fffaf0;";
                    default                -> "";
                });
            }
        });

        // ── Cards de totais ────────────────────────────────────────────────
        lblTotalAulas    = new Label("—");
        lblTotalHoras    = new Label("—");
        lblMediaPresenca = new Label("—");

        String cardStyle = "-fx-background-color:#f7fafc;-fx-border-color:#e2e8f0;" +
                "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:12 24;";

        HBox rodape = new HBox(16,
                card("Total de Aulas",    lblTotalAulas,    "#3182ce", cardStyle),
                card("Total de Horas",    lblTotalHoras,    "#38a169", cardStyle),
                card("Média de Presença", lblMediaPresenca, "#d69e2e", cardStyle));
        rodape.setPadding(new Insets(12, 20, 16, 20));
        rodape.setAlignment(Pos.CENTER_LEFT);

        // ── Montagem ───────────────────────────────────────────────────────
        VBox centro = new VBox(header, tabela, rodape);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        view.setCenter(centro);
    }

    private void carregar() {
        String profId = mainApp.getSessao().getId();
        Integer mes   = resolverMes();
        Integer ano   = cbAno.getValue();
        List<RegistroHoras> lista = chamadaDAO.listarRegistroHoras(profId, mes, ano);
        dados.setAll(lista);
        atualizarTotais(lista);
    }

    private void atualizarTotais(List<RegistroHoras> lista) {
        double horas = lista.stream().mapToDouble(RegistroHoras::getHorasMinistradas).sum();
        double pres  = lista.stream()
                .filter(r -> r.getTotalAlunos() > 0)
                .mapToDouble(r -> (double) r.getTotalPresentes() / r.getTotalAlunos() * 100)
                .average().orElse(0);
        lblTotalAulas.setText(String.valueOf(lista.size()));
        lblTotalHoras.setText(String.format("%.1fh", horas));
        lblMediaPresenca.setText(String.format("%.0f%%", pres));
    }

    private Integer resolverMes() {
        String sel = cbMes.getValue();
        if (sel == null || sel.equals("Todos os meses")) return null;
        Month[] months = Month.values();
        for (int i = 0; i < months.length; i++)
            if (months[i].getDisplayName(TextStyle.FULL, Locale.of("pt","BR")).equals(sel))
                return i + 1;
        return null;
    }

    private VBox card(String titulo, Label lblVal, String cor, String style) {
        Label lt = new Label(titulo);
        lt.setStyle("-fx-text-fill:#718096;-fx-font-size:12px;");
        lblVal.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + cor + ";");
        VBox c = new VBox(4, lt, lblVal);
        c.setStyle(style); c.setMinWidth(160);
        return c;
    }

    public BorderPane getView() { return view; }
}