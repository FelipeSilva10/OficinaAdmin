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
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class RegistroHorasView {

    private BorderPane view;
    private MainFX mainApp;
    private ChamadaDAO chamadaDAO;

    private TableView<RegistroHoras> tabela;
    private ObservableList<RegistroHoras> dados = FXCollections.observableArrayList();

    private ComboBox<String>  cbMes;
    private ComboBox<Integer> cbAno;

    // Três campos de valor — Pública / Privada / Reunião
    private TextField txtValorPublica;
    private TextField txtValorPrivada;
    private TextField txtValorReuniao;

    private Label lblTotalAulas;
    private Label lblTotalHoras;

    public RegistroHorasView(MainFX mainApp) {
        this.mainApp    = mainApp;
        this.chamadaDAO = new ChamadaDAO();
        construirInterface();
        carregar();
    }

    private void construirInterface() {
        view = new BorderPane();

        // ── Cabecalho ──────────────────────────────────────────────────────
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
        HBox header = new HBox(12, lblTitulo, sp,
                new Label("Mês:"), cbMes,
                new Label("Ano:"), cbAno,
                btnAt);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 12, 20));

        // ── Tabela ─────────────────────────────────────────────────────────
        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhuma aula registrada para o periodo selecionado."));
        tabela.setItems(dados);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<RegistroHoras, String> cData = new TableColumn<>("Data");
        cData.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDataFormatada()));
        cData.setMaxWidth(110);

        TableColumn<RegistroHoras, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTipoLabel()));
        cTipo.setMaxWidth(120);

        // Coluna Rede (Pública/Privada)
        TableColumn<RegistroHoras, String> cRede = new TableColumn<>("Rede");
        cRede.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEscolaTipoLabel()));
        cRede.setMaxWidth(80);
        cRede.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                // Reunião/Substituta não tem rede relevante
                RegistroHoras r = getTableView().getItems().get(getIndex());
                if (r.isOcasional()) {
                    setStyle("-fx-text-fill: #6b46c1;");
                } else if (r.isEscolaPublica()) {
                    setStyle("-fx-text-fill: #2b6cb0; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #276749; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<RegistroHoras, String> cTurma = new TableColumn<>("Turma");
        cTurma.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTurmaNome()));

        TableColumn<RegistroHoras, String> cEscola = new TableColumn<>("Escola");
        cEscola.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEscolaNome()));

        TableColumn<RegistroHoras, String> cHor = new TableColumn<>("Horario");
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

        tabela.getColumns().addAll(cData, cTipo, cRede, cTurma, cEscola, cHor, cHoras, cPres);

        // Cor por tipo de aula / rede
        tabela.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(RegistroHoras item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                if (item.isOcasional()) {
                    setStyle("-fx-background-color:#faf5ff;");
                } else if (!item.isEscolaPublica()) {
                    setStyle("-fx-background-color:#f0fff4;");  // verde claro = privada
                } else {
                    setStyle("");
                }
            }
        });

        // ── Rodape: totais + valores diferenciados + PDF ──────────────────
        lblTotalAulas = new Label("—");
        lblTotalHoras = new Label("—");

        String cardStyle = "-fx-background-color:#f7fafc;-fx-border-color:#e2e8f0;" +
                "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:12 24;";

        HBox cardsBox = new HBox(16,
                card("Total de Aulas", lblTotalAulas, "#3182ce", cardStyle),
                card("Total de Horas", lblTotalHoras, "#38a169", cardStyle));
        cardsBox.setAlignment(Pos.CENTER_LEFT);

        // Campos de valor por categoria
        String estiloInput =
                "-fx-background-radius:8;-fx-border-radius:8;" +
                        "-fx-border-color:#e2e8f0;-fx-padding:6 10;-fx-font-size:13px;";

        txtValorPublica = new TextField();
        txtValorPublica.setPromptText("Ex: 41,00");
        txtValorPublica.setPrefWidth(90);
        txtValorPublica.setStyle(estiloInput);

        txtValorPrivada = new TextField();
        txtValorPrivada.setPromptText("Ex: 55,00");
        txtValorPrivada.setPrefWidth(90);
        txtValorPrivada.setStyle(estiloInput);

        txtValorReuniao = new TextField();
        txtValorReuniao.setPromptText("Ex: 35,00");
        txtValorReuniao.setPrefWidth(90);
        txtValorReuniao.setStyle(estiloInput);

        Label lblVP = label("Pública (R$):");
        Label lblVR2 = label("Privada (R$):");
        Label lblVR = label("Reunião (R$):");

        Button btnPdf = new Button("Gerar Relatório PDF");
        btnPdf.setStyle(
                "-fx-background-color:#2d3748;-fx-text-fill:white;" +
                        "-fx-background-radius:8;-fx-padding:8 18;-fx-font-weight:bold;-fx-font-size:13px;");
        btnPdf.setOnAction(e -> gerarPdf());

        Region spRodape = new Region(); HBox.setHgrow(spRodape, Priority.ALWAYS);

        VBox valoresBox = new VBox(4,
                new Label("Valor por hora:") {{
                    setStyle("-fx-font-weight:bold;-fx-text-fill:#2d3748;-fx-font-size:12px;");
                }},
                new HBox(8,
                        lblVP, txtValorPublica,
                        lblVR2, txtValorPrivada,
                        lblVR, txtValorReuniao
                ) {{ setAlignment(Pos.CENTER_LEFT); }}
        );

        HBox rodape = new HBox(16, cardsBox, spRodape, valoresBox, btnPdf);
        rodape.setAlignment(Pos.CENTER_LEFT);
        rodape.setPadding(new Insets(12, 20, 16, 20));
        rodape.setStyle("-fx-border-color:#e2e8f0;-fx-border-width:1 0 0 0;");

        // ── Montagem ───────────────────────────────────────────────────────
        VBox centro = new VBox(header, tabela, rodape);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        view.setCenter(centro);
    }

    private Label label(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size:12px;-fx-text-fill:#4a5568;");
        return l;
    }

    // ── Carregar dados ─────────────────────────────────────────────────────

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
        lblTotalAulas.setText(String.valueOf(lista.size()));
        lblTotalHoras.setText(String.format("%.1fh", horas));
    }

    private void gerarPdf() {
        if (dados.isEmpty()) {
            mainApp.mostrarAviso("Nenhum registro para gerar relatório.", true);
            return;
        }

        double valorPublica = parseValor(txtValorPublica.getText(), "Pública");
        if (valorPublica < 0) return;
        double valorPrivada = parseValor(txtValorPrivada.getText(), "Privada");
        if (valorPrivada < 0) return;
        double valorReuniao = parseValor(txtValorReuniao.getText(), "Reunião");
        if (valorReuniao < 0) return;

        String mesStr = cbMes.getValue().equals("Todos os meses")
                ? "Todos" : cbMes.getValue().substring(0, 3).toUpperCase();
        String nomeArquivo = "Relatorio_Horas_" + mesStr + "_" + cbAno.getValue() + ".pdf";

        // Salva na Área de Trabalho do usuário atual
        String desktop = System.getProperty("user.home") + java.io.File.separator + "Desktop";
        java.io.File pasta = new java.io.File(desktop);
        if (!pasta.exists() || !pasta.isDirectory()) {
            // Fallback: pasta home caso Desktop não exista
            pasta = new java.io.File(System.getProperty("user.home"));
        }
        String caminho = pasta.getAbsolutePath() + java.io.File.separator + nomeArquivo;

        final double vP = valorPublica, vR2 = valorPrivada, vR = valorReuniao;
        final String nomeProf = mainApp.getSessao().getNome();
        final List<RegistroHoras> copia = List.copyOf(dados);
        final String caminhoFinal = caminho;

        new Thread(() -> {
            try {
                report.RelatorioHorasPdf.gerar(copia, nomeProf, vP, vR2, vR, caminhoFinal);
                javafx.application.Platform.runLater(() ->
                        mainApp.mostrarAviso("PDF salvo na Área de Trabalho: " + nomeArquivo, false));
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        mainApp.mostrarAviso("Erro ao gerar PDF: " + ex.getMessage(), true));
            }
        }).start();
    }

    private double parseValor(String texto, String campo) {
        if (texto == null || texto.isBlank()) return 0;
        try {
            return Double.parseDouble(texto.replace(",", ".").trim());
        } catch (NumberFormatException ex) {
            mainApp.mostrarAviso("Valor inválido em '" + campo + "'. Use formato: 41,00", true);
            return -1;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

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