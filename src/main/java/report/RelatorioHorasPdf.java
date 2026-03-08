package report;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import core.RegistroHoras;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gera o relatório mensal/bimestral de horas em PDF,
 * no formato utilizado pela Oficina do Amanhã.
 *
 * Dependência necessária no build (Maven / Gradle):
 *   Maven  → <dependency> com.itextpdf / itextpdf / 5.5.13.3 </dependency>
 *   Gradle → implementation 'com.itextpdf:itextpdf:5.5.13.3'
 */
public class RelatorioHorasPdf {

    // ── Paleta ──────────────────────────────────────────────────────────────
    private static final BaseColor COR_HEADER    = new BaseColor(26, 32, 44);   // #1a202c
    private static final BaseColor COR_SUBHEADER = new BaseColor(45, 55, 72);   // #2d3748
    private static final BaseColor COR_LINHA_PAR = new BaseColor(247, 250, 252); // #f7fafc
    private static final BaseColor COR_TOTAL     = new BaseColor(226, 232, 240); // #e2e8f0
    private static final BaseColor COR_BRANCO    = BaseColor.WHITE;

    // ── Fontes ───────────────────────────────────────────────────────────────
    private static final Font F_TITULO    = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD,   BaseColor.WHITE);
    private static final Font F_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   BaseColor.WHITE);
    private static final Font F_MES       = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   new BaseColor(45, 55, 72));
    private static final Font F_CABECALHO = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   BaseColor.WHITE);
    private static final Font F_CELL      = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, new BaseColor(45, 55, 72));
    private static final Font F_TOTAL     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   new BaseColor(26, 32, 44));
    private static final Font F_VALOR     = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(26, 32, 44));
    private static final Font F_TOTAL_G   = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   new BaseColor(26, 32, 44));
    private static final Font F_RODAPE    = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, BaseColor.GRAY);

    private static final DateTimeFormatter FMT_BR  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PTBR = Locale.of("pt", "BR");

    // ────────────────────────────────────────────────────────────────────────

    /**
     * @param registros    Lista de RegistroHoras a incluir no relatório
     * @param professorNome Nome do professor
     * @param valorHora    Valor em R$ por hora (0 = não exibir seção de valores)
     * @param caminhoSaida Caminho completo do arquivo de destino (.pdf)
     */
    public static void gerar(List<RegistroHoras> registros,
                             String professorNome,
                             double valorHora,
                             String caminhoSaida) throws Exception {

        if (registros == null || registros.isEmpty()) {
            throw new IllegalArgumentException("Nenhum registro para gerar relatório.");
        }

        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(caminhoSaida));

        // Numeração de páginas no rodapé
        writer.setPageEvent(new RodapeEvento(professorNome));

        doc.open();

        // ── Cabeçalho geral ───────────────────────────────────────────────
        adicionarCabecalhoGeral(doc, registros, professorNome);

        // ── Agrupar por mês ───────────────────────────────────────────────
        Map<String, List<RegistroHoras>> porMes = agruparPorMes(registros);
        Map<String, Double> totalPorMes = new LinkedHashMap<>();

        for (Map.Entry<String, List<RegistroHoras>> entry : porMes.entrySet()) {
            String labelMes = entry.getKey();
            List<RegistroHoras> linhas = entry.getValue();

            adicionarSecaoMes(doc, labelMes, linhas);

            double totalMes = linhas.stream()
                    .mapToDouble(RegistroHoras::getHorasMinistradas).sum();
            totalPorMes.put(labelMes, totalMes);

            doc.add(Chunk.NEWLINE);
        }

        // ── Seção de valores ──────────────────────────────────────────────
        if (valorHora > 0) {
            adicionarSecaoValores(doc, totalPorMes, valorHora);
        }

        // ── Data de emissão ───────────────────────────────────────────────
        Paragraph emissao = new Paragraph(
                "Data de emissão: " + LocalDate.now().format(FMT_BR), F_RODAPE);
        emissao.setAlignment(Element.ALIGN_RIGHT);
        emissao.setSpacingBefore(12);
        doc.add(emissao);

        doc.close();
    }

    // ── Cabeçalho ────────────────────────────────────────────────────────────

    private static void adicionarCabecalhoGeral(Document doc,
                                                List<RegistroHoras> registros,
                                                String professorNome) throws DocumentException {
        // Faixa azul escura
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        Paragraph titulo = new Paragraph("RELATÓRIO DE HORAS", F_TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);

        // Determina o período
        String periodo = determinarPeriodo(registros);
        Paragraph sub = new Paragraph(periodo, F_SUBTITULO);
        sub.setAlignment(Element.ALIGN_CENTER);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COR_HEADER);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(16);
        cell.addElement(titulo);
        cell.addElement(sub);

        Paragraph prof = new Paragraph("Prof.: " + professorNome,
                new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(160, 174, 192)));
        prof.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(prof);

        header.addCell(cell);
        doc.add(header);
        doc.add(new Paragraph(" "));
    }

    private static String determinarPeriodo(List<RegistroHoras> registros) {
        Set<String> meses = new TreeSet<>();
        for (RegistroHoras r : registros) {
            if (r.getDataAula() != null) {
                Month m = r.getDataAula().getMonth();
                int ano = r.getDataAula().getYear();
                meses.add(m.getDisplayName(TextStyle.FULL, PTBR).toUpperCase() + "/" + ano);
            }
        }
        return String.join(" E ", meses);
    }

    // ── Seção de mês ─────────────────────────────────────────────────────────

    private static void adicionarSecaoMes(Document doc,
                                          String labelMes,
                                          List<RegistroHoras> linhas) throws DocumentException {
        // Título do mês
        Paragraph pMes = new Paragraph(labelMes, F_MES);
        pMes.setSpacingBefore(10);
        pMes.setSpacingAfter(4);
        doc.add(pMes);

        // Tabela: DESCRIÇÃO | DATA | ENTRADA | SAÍDA | HORAS
        PdfPTable tabela = new PdfPTable(new float[]{45f, 15f, 10f, 10f, 20f});
        tabela.setWidthPercentage(100);

        // Cabeçalho da tabela
        String[] colunas = {"DESCRIÇÃO", "DATA", "ENTRADA", "SAÍDA", "HORAS"};
        for (String col : colunas) {
            PdfPCell c = new PdfPCell(new Phrase(col, F_CABECALHO));
            c.setBackgroundColor(COR_SUBHEADER);
            c.setPadding(6);
            c.setBorderColor(COR_HEADER);
            tabela.addCell(c);
        }

        // Linhas de dados
        boolean par = false;
        for (RegistroHoras r : linhas) {
            BaseColor bg = par ? COR_LINHA_PAR : COR_BRANCO;
            par = !par;

            addCell(tabela, descricao(r),               bg, Element.ALIGN_LEFT);
            addCell(tabela, r.getDataFormatada(),         bg, Element.ALIGN_CENTER);
            addCell(tabela, r.getHorarioInicio(),          bg, Element.ALIGN_CENTER);
            addCell(tabela, r.getHorarioFim(),             bg, Element.ALIGN_CENTER);
            addCell(tabela, formatarHoras(r.getHorasMinistradas()), bg, Element.ALIGN_CENTER);
        }

        // Linha de total
        double total = linhas.stream().mapToDouble(RegistroHoras::getHorasMinistradas).sum();
        PdfPCell cTotalLabel = new PdfPCell(new Phrase("TOTAL DE HORAS", F_TOTAL));
        cTotalLabel.setColspan(4);
        cTotalLabel.setBackgroundColor(COR_TOTAL);
        cTotalLabel.setPadding(6);
        cTotalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabela.addCell(cTotalLabel);

        PdfPCell cTotalVal = new PdfPCell(new Phrase(formatarHorasLongo(total), F_TOTAL));
        cTotalVal.setBackgroundColor(COR_TOTAL);
        cTotalVal.setPadding(6);
        cTotalVal.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabela.addCell(cTotalVal);

        doc.add(tabela);
    }

    // ── Seção de valores ─────────────────────────────────────────────────────

    private static void adicionarSecaoValores(Document doc,
                                              Map<String, Double> totalPorMes,
                                              double valorHora) throws DocumentException {
        doc.add(new Paragraph(" "));

        Paragraph titulo = new Paragraph("VALORES", F_MES);
        titulo.setSpacingAfter(6);
        doc.add(titulo);

        PdfPTable tabela = new PdfPTable(new float[]{50f, 50f});
        tabela.setWidthPercentage(70);
        tabela.setHorizontalAlignment(Element.ALIGN_LEFT);

        double totalGeral = 0;
        boolean par = false;
        for (Map.Entry<String, Double> entry : totalPorMes.entrySet()) {
            double horas = entry.getValue();
            double valor = horas * valorHora;
            totalGeral += valor;

            BaseColor bg = par ? COR_LINHA_PAR : COR_BRANCO;
            par = !par;

            String descr = entry.getKey() + ": " + formatarHorasLongo(horas)
                    + " × R$ " + String.format("%.2f", valorHora);
            addCell(tabela, descr,                           bg, Element.ALIGN_LEFT);
            addCell(tabela, "R$ " + String.format("%.2f", valor), bg, Element.ALIGN_RIGHT);
        }

        // Total geral
        PdfPCell cLbl = new PdfPCell(new Phrase("TOTAL GERAL", F_TOTAL_G));
        cLbl.setBackgroundColor(COR_TOTAL);
        cLbl.setPadding(8);
        tabela.addCell(cLbl);

        PdfPCell cVal = new PdfPCell(new Phrase("R$ " + String.format("%.2f", totalGeral), F_TOTAL_G));
        cVal.setBackgroundColor(COR_TOTAL);
        cVal.setPadding(8);
        cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabela.addCell(cVal);

        doc.add(tabela);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Map<String, List<RegistroHoras>> agruparPorMes(List<RegistroHoras> registros) {
        // Ordena por data antes de agrupar
        List<RegistroHoras> ordenados = registros.stream()
                .filter(r -> r.getDataAula() != null)
                .sorted(Comparator.comparing(RegistroHoras::getDataAula))
                .collect(Collectors.toList());

        Map<String, List<RegistroHoras>> mapa = new LinkedHashMap<>();
        for (RegistroHoras r : ordenados) {
            String chave = r.getDataAula().getMonth()
                    .getDisplayName(TextStyle.FULL, PTBR).toUpperCase()
                    + "/" + r.getDataAula().getYear();
            mapa.computeIfAbsent(chave, k -> new ArrayList<>()).add(r);
        }
        return mapa;
    }

    private static String descricao(RegistroHoras r) {
        StringBuilder sb = new StringBuilder();
        if (r.getEscolaNome() != null && !r.getEscolaNome().isBlank())
            sb.append(r.getEscolaNome());
        if (r.getTurmaNome() != null && !r.getTurmaNome().isBlank()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(r.getTurmaNome());
        }
        // Tipo (apenas se não for aula regular)
        if (!"AULA".equals(r.getTipoAula())) {
            if (sb.length() > 0) sb.append(" (");
            sb.append(r.getTipoAula().equals("REUNIÃO") ? "Reunião" : "Substituta");
            if (r.getEscolaNome() != null) sb.append(")");
        }
        return sb.length() > 0 ? sb.toString() : "—";
    }

    /** Ex: 0.833 → "50 min" | 1.5 → "1h 30min" | 2.0 → "2h" */
    private static String formatarHoras(double h) {
        int totalMin = (int) Math.round(h * 60);
        int hrs = totalMin / 60;
        int min = totalMin % 60;
        if (hrs == 0)       return min + " min";
        if (min == 0)       return hrs + "h";
        return hrs + "h " + min + "min";
    }

    /** Ex: 7.0 → "7h" | 22.333 → "22h 20min" */
    private static String formatarHorasLongo(double h) {
        int totalMin = (int) Math.round(h * 60);
        int hrs = totalMin / 60;
        int min = totalMin % 60;
        if (min == 0) return hrs + "h";
        return hrs + "h " + min + "min";
    }

    private static void addCell(PdfPTable t, String texto, BaseColor bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "—", F_CELL));
        c.setBackgroundColor(bg);
        c.setPadding(5);
        c.setHorizontalAlignment(align);
        c.setBorderColor(new BaseColor(226, 232, 240));
        t.addCell(c);
    }

    // ── Rodapé com número de página ───────────────────────────────────────────

    private static class RodapeEvento extends PdfPageEventHelper {
        private final String professor;
        RodapeEvento(String professor) { this.professor = professor; }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte cb = writer.getDirectContent();
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                cb.setFontAndSize(bf, 8);
                cb.beginText();
                cb.setColorFill(BaseColor.GRAY);
                String text = "Oficina do Amanha  |  " + professor + "  |  Pagina " + writer.getPageNumber();
                cb.showTextAligned(Element.ALIGN_CENTER, text,
                        (document.right() - document.left()) / 2 + document.leftMargin(),
                        document.bottom() - 15, 0);
                cb.endText();
            } catch (DocumentException | IOException e) {
                // fonte padrão indisponível — rodapé omitido silenciosamente
            }
        }
    }
}