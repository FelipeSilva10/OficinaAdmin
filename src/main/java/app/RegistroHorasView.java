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

public class RegistroHorasView {

    private BorderPane view;
    private MainFX mainApp;
    private ChamadaDAO chamadaDAO;

    private TableView<RegistroHoras> tabela;
    private ObservableList<RegistroHoras> dados = FXCollections.observableArrayList();

    private ComboBox<String> cbMes;
    private ComboBox<Integer> cbAno;

    // Rodapé de totais
    private Label lblTotalAulas, lblTotalHoras, lblTotalPresencaMed;

    public RegistroHorasView(MainFX mainApp) {
        this.mainApp    = mainApp;
        this.chamadaDAO = new ChamadaDAO();
        construirInterface();
        carregar();
    }

    private void construirInterface() {
        view = new BorderPane();

        // ── Cabeçalho ─────────────────────────────────────────────────────────
        Label lblTitulo = new Label("Registro de Horas");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        // Filtro por mês
        cbMes = new ComboBox<>();
        cbMes.setPromptText("Todos os meses");
        cbMes.setPrefWidth(170);
        cbMes.getItems().add("Todos os meses");
        for (Month m : Month.values()) {
            cbMes.getItems().add(m.getDisplayName(TextStyle.FULL, new Locale("pt","BR")));
        }
        cbMes.setValue("Todos os meses");
        cbMes.setOnAction(e -> carregar());

        // Filtro por ano
        cbAno = new ComboBox<>();
        cbAno.setPromptText("Ano");
        cbAno.setPrefWidth(100);
        int anoAtual = LocalDate.now().getYear();
        for (int a = anoAtual; a >= anoAtual - 3; a--) cbAno.getItems().add(a);
        cbAno.setValue(anoAtual);
        cbAno.setOnAction(e -> carregar());

        Button btnAtualizar = new Button("Atualizar");
        btnAtualizar.setOnAction(e -> carregar());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, lblTitulo, spacer, new Label("Mês:"), cbMes,
                new Label("Ano:"), cbAno, btnAtualizar);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 12, 20));

        // ── Tabela ────────────────────────────────────────────────────────────
        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPlaceholder(new Label("Nenhuma aula registrada para o período selecionado."));
        tabela.setItems(dados);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<RegistroHoras, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDataFormatada()));
        colData.setMaxWidth(110);

        TableColumn<RegistroHoras, String> colTurma = new TableColumn<>("Turma");
        colTurma.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTurmaNome()));

        TableColumn<RegistroHoras, String> colEscola = new TableColumn<>("Escola");
        colEscola.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEscolaNome()));

        TableColumn<RegistroHoras, String> colHorario = new TableColumn<>("Horário");
        colHorario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHorario()));
        colHorario.setMaxWidth(130);

        TableColumn<RegistroHoras, String> colHoras = new TableColumn<>("Horas");
        colHoras.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHorasFormatadas()));
        colHoras.setMaxWidth(80);
        colHoras.setStyle("-fx-alignment: CENTER;");

        TableColumn<RegistroHoras, String> colPresenca = new TableColumn<>("Presença");
        colPresenca.setCellValueFactory(c -> {
            RegistroHoras r = c.getValue();
            String txt = r.getTotalPresentes() + "/" + r.getTotalAlunos();
            return new SimpleStringProperty(txt);
        });
        colPresenca.setMaxWidth(90);
        colPresenca.setStyle("-fx-alignment: CENTER;");

        tabela.getColumns().addAll(colData, colTurma, colEscola, colHorario, colHoras, colPresenca);

        // ── Rodapé de totais ──────────────────────────────────────────────────
        lblTotalAulas     = new Label();
        lblTotalHoras     = new Label();
        lblTotalPresencaMed = new Label();

        String estiloCard = "-fx-background-color: #f7fafc; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12 20;";

        VBox cardAulas = criarCard("Total de Aulas", lblTotalAulas, "#3182ce");
        VBox cardHoras = criarCard("Total de Horas", lblTotalHoras, "#38a169");
        VBox cardPresenca = criarCard("Média de Presença", lblTotalPresencaMed, "#d69e2e");

        cardAulas.setStyle(estiloCard);
        cardHoras.setStyle(estiloCard);
        cardPresenca.setStyle(estiloCard);

        HBox rodape = new HBox(16, cardAulas, cardHoras, cardPresenca);
        rodape.setPadding(new Insets(12, 20, 16, 20));
        rodape.setAlignment(Pos.CENTER_LEFT);

        // ── Montagem ──────────────────────────────────────────────────────────
        VBox centro = new VBox(header, tabela, rodape);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        view.setCenter(centro);
    }

    private VBox criarCard(String titulo, Label lblValor, String cor) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");
        lblValor.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + cor + ";");
        VBox card = new VBox(4, lblTitulo, lblValor);
        card.setMinWidth(160);
        return card;
    }

    private void carregar() {
        String profId = mainApp.getSessao().getId();

        // Resolve filtros
        Integer mes = null;
        String mesSel = cbMes.getValue();
        if (mesSel != null && !mesSel.equals("Todos os meses")) {
            Month[] months = Month.values();
            for (int i = 0; i < months.length; i++) {
                if (months[i].getDisplayName(TextStyle.FULL, new Locale("pt","BR")).equals(mesSel)) {
                    mes = i + 1;
                    break;
                }
            }
        }
        Integer ano = cbAno.getValue();

        List<RegistroHoras> lista = chamadaDAO.listarRegistroHoras(profId, mes, ano);
        dados.setAll(lista);
        atualizarTotais(lista);
    }

    private void atualizarTotais(List<RegistroHoras> lista) {
        int totalAulas = lista.size();
        double totalHoras = lista.stream().mapToDouble(RegistroHoras::getHorasMinistradas).sum();

        double mediaPresenca = lista.isEmpty() ? 0 :
                lista.stream()
                        .filter(r -> r.getTotalAlunos() > 0)
                        .mapToDouble(r -> (double) r.getTotalPresentes() / r.getTotalAlunos() * 100)
                        .average()
                        .orElse(0);

        lblTotalAulas.setText(String.valueOf(totalAulas));
        lblTotalHoras.setText(String.format("%.1fh", totalHoras));
        lblTotalPresencaMed.setText(String.format("%.0f%%", mediaPresenca));
    }

    public BorderPane getView() { return view; }
}