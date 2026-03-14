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

public class RelatorioHorasPdf {

    // ── Paleta ──────────────────────────────────────────────────────────────
    private static final BaseColor COR_HEADER    = new BaseColor(26, 32, 44);
    private static final BaseColor COR_SUBHEADER = new BaseColor(45, 55, 72);
    private static final BaseColor COR_LINHA_PAR = new BaseColor(247, 250, 252);
    private static final BaseColor COR_PUBLICA   = new BaseColor(235, 248, 255);  // azul claro
    private static final BaseColor COR_PRIVADA   = new BaseColor(240, 255, 244);  // verde claro
    private static final BaseColor COR_REUNIAO   = new BaseColor(250, 245, 255);  // lilás claro
    private static final BaseColor COR_TOTAL     = new BaseColor(226, 232, 240);
    private static final BaseColor COR_BRANCO    = BaseColor.WHITE;

    // ── Fontes ───────────────────────────────────────────────────────────────
    private static final Font F_TITULO    = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD,   BaseColor.WHITE);
    private static final Font F_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   BaseColor.WHITE);
    private static final Font F_MES       = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   new BaseColor(45, 55, 72));
    private static final Font F_CABECALHO = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   BaseColor.WHITE);
    private static final Font F_CELL      = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, new BaseColor(45, 55, 72));
    private static final Font F_TOTAL     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   new BaseColor(26, 32, 44));
    private static final Font F_TOTAL_G   = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   new BaseColor(26, 32, 44));
    private static final Font F_RODAPE    = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, BaseColor.GRAY);
    private static final Font F_LEGENDA   = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, new BaseColor(100, 100, 100));

    private static final DateTimeFormatter FMT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PTBR = Locale.of("pt", "BR");

    // ────────────────────────────────────────────────────────────────────────

    /**
     * Gera PDF com tarifas diferenciadas por tipo de rede.
     *
     * @param valorPublica  R$/h para aulas em escolas públicas
     * @param valorPrivada  R$/h para aulas em escolas privadas
     * @param valorReuniao  R$/h para reuniões e aulas substitutas
     */
    public static void gerar(List<RegistroHoras> registros,
                             String professorNome,
                             double valorPublica,
                             double valorPrivada,
                             double valorReuniao,
                             String caminhoSaida) throws Exception {

        if (registros == null || registros.isEmpty())
            throw new IllegalArgumentException("Nenhum registro para gerar relatório.");

        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(caminhoSaida));
        writer.setPageEvent(new RodapeEvento(professorNome));
        doc.open();

        adicionarCabecalhoGeral(doc, registros, professorNome);

        Map<String, List<RegistroHoras>> porMes = agruparPorMes(registros);
        // Totais por categoria para a seção de valores
        double totalHorasPublica  = 0, totalHorasPrivada  = 0, totalHorasReuniao  = 0;

        for (Map.Entry<String, List<RegistroHoras>> entry : porMes.entrySet()) {
            adicionarSecaoMes(doc, entry.getKey(), entry.getValue());
            for (RegistroHoras r : entry.getValue()) {
                if (r.isOcasional())            totalHorasReuniao  += r.getHorasMinistradas();
                else if (r.isEscolaPublica())   totalHorasPublica  += r.getHorasMinistradas();
                else                            totalHorasPrivada  += r.getHorasMinistradas();
            }
            doc.add(Chunk.NEWLINE);
        }

        boolean temValores = valorPublica > 0 || valorPrivada > 0 || valorReuniao > 0;
        if (temValores) {
            adicionarSecaoValores(doc,
                    totalHorasPublica,  valorPublica,
                    totalHorasPrivada,  valorPrivada,
                    totalHorasReuniao,  valorReuniao);
        }

        Paragraph emissao = new Paragraph("Data de emissão: " + LocalDate.now().format(FMT_BR), F_RODAPE);
        emissao.setAlignment(Element.ALIGN_RIGHT);
        emissao.setSpacingBefore(12);
        doc.add(emissao);

        doc.close();
    }

    /**
     * Compatibilidade com código antigo (valor único para todos os tipos).
     */
    public static void gerar(List<RegistroHoras> registros,
                             String professorNome,
                             double valorHora,
                             String caminhoSaida) throws Exception {
        gerar(registros, professorNome, valorHora, valorHora, valorHora, caminhoSaida);
    }

    // ── Cabeçalho ────────────────────────────────────────────────────────────

    private static void adicionarCabecalhoGeral(Document doc,
                                                List<RegistroHoras> registros,
                                                String professorNome) throws DocumentException {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        Paragraph titulo = new Paragraph("RELATÓRIO DE HORAS", F_TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        Paragraph sub = new Paragraph(determinarPeriodo(registros), F_SUBTITULO);
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
        Paragraph pMes = new Paragraph(labelMes, F_MES);
        pMes.setSpacingBefore(10); pMes.setSpacingAfter(4);
        doc.add(pMes);

        // Tabela: DESCRIÇÃO | REDE | DATA | ENTRADA | SAÍDA | HORAS
        PdfPTable tabela = new PdfPTable(new float[]{38f, 10f, 14f, 9f, 9f, 20f});
        tabela.setWidthPercentage(100);

        for (String col : new String[]{"DESCRIÇÃO", "REDE", "DATA", "ENTRADA", "SAÍDA", "HORAS"}) {
            PdfPCell c = new PdfPCell(new Phrase(col, F_CABECALHO));
            c.setBackgroundColor(COR_SUBHEADER);
            c.setPadding(6);
            c.setBorderColor(COR_HEADER);
            tabela.addCell(c);
        }

        for (RegistroHoras r : linhas) {
            BaseColor bg = corLinha(r);
            addCell(tabela, descricao(r),                             bg, Element.ALIGN_LEFT);
            addCell(tabela, redeLabel(r),                             bg, Element.ALIGN_CENTER);
            addCell(tabela, r.getDataFormatada(),                     bg, Element.ALIGN_CENTER);
            addCell(tabela, r.getHorarioInicio(),                     bg, Element.ALIGN_CENTER);
            addCell(tabela, r.getHorarioFim(),                        bg, Element.ALIGN_CENTER);
            addCell(tabela, formatarHoras(r.getHorasMinistradas()),   bg, Element.ALIGN_CENTER);
        }

        double total = linhas.stream().mapToDouble(RegistroHoras::getHorasMinistradas).sum();
        PdfPCell cLbl = new PdfPCell(new Phrase("TOTAL DE HORAS", F_TOTAL));
        cLbl.setColspan(5); cLbl.setBackgroundColor(COR_TOTAL);
        cLbl.setPadding(6); cLbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabela.addCell(cLbl);
        PdfPCell cVal = new PdfPCell(new Phrase(formatarHorasLongo(total), F_TOTAL));
        cVal.setBackgroundColor(COR_TOTAL); cVal.setPadding(6);
        cVal.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabela.addCell(cVal);

        doc.add(tabela);

        // Legenda de cores
        Paragraph leg = new Paragraph(
                "  ■ Pública  ■ Privada  ■ Reunião/Substituta", F_LEGENDA);
        leg.setSpacingBefore(3);
        doc.add(leg);
    }

    // ── Seção de valores ─────────────────────────────────────────────────────

    private static void adicionarSecaoValores(Document doc,
                                              double hPublica,  double vPublica,
                                              double hPrivada,  double vPrivada,
                                              double hReuniao,  double vReuniao)
            throws DocumentException {
        doc.add(new Paragraph(" "));
        Paragraph titulo = new Paragraph("VALORES", F_MES);
        titulo.setSpacingAfter(6);
        doc.add(titulo);

        PdfPTable tabela = new PdfPTable(new float[]{55f, 45f});
        tabela.setWidthPercentage(70);
        tabela.setHorizontalAlignment(Element.ALIGN_LEFT);

        double totalGeral = 0;
        boolean par = false;

        // Linha pública
        if (hPublica > 0 || vPublica > 0) {
            double valor = hPublica * vPublica;
            totalGeral += valor;
            BaseColor bg = par ? COR_LINHA_PAR : COR_BRANCO; par = !par;
            addCell(tabela, "Pública: " + formatarHorasLongo(hPublica)
                    + " × R$ " + String.format("%.2f", vPublica), bg, Element.ALIGN_LEFT);
            addCell(tabela, "R$ " + String.format("%.2f", valor), bg, Element.ALIGN_RIGHT);
        }
        // Linha privada
        if (hPrivada > 0 || vPrivada > 0) {
            double valor = hPrivada * vPrivada;
            totalGeral += valor;
            BaseColor bg = par ? COR_LINHA_PAR : COR_BRANCO; par = !par;
            addCell(tabela, "Privada: " + formatarHorasLongo(hPrivada)
                    + " × R$ " + String.format("%.2f", vPrivada), bg, Element.ALIGN_LEFT);
            addCell(tabela, "R$ " + String.format("%.2f", valor), bg, Element.ALIGN_RIGHT);
        }
        // Linha reunião/substituta
        if (hReuniao > 0 || vReuniao > 0) {
            double valor = hReuniao * vReuniao;
            totalGeral += valor;
            BaseColor bg = par ? COR_LINHA_PAR : COR_BRANCO;
            addCell(tabela, "Reuniões/Substitutas: " + formatarHorasLongo(hReuniao)
                    + " × R$ " + String.format("%.2f", vReuniao), bg, Element.ALIGN_LEFT);
            addCell(tabela, "R$ " + String.format("%.2f", valor), bg, Element.ALIGN_RIGHT);
        }

        // Total geral
        PdfPCell cLbl = new PdfPCell(new Phrase("TOTAL GERAL", F_TOTAL_G));
        cLbl.setBackgroundColor(COR_TOTAL); cLbl.setPadding(8);
        tabela.addCell(cLbl);
        PdfPCell cVal = new PdfPCell(new Phrase("R$ " + String.format("%.2f", totalGeral), F_TOTAL_G));
        cVal.setBackgroundColor(COR_TOTAL); cVal.setPadding(8);
        cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabela.addCell(cVal);

        doc.add(tabela);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static BaseColor corLinha(RegistroHoras r) {
        if (r.isOcasional())          return COR_REUNIAO;
        if (!r.isEscolaPublica())     return COR_PRIVADA;
        return COR_PUBLICA;
    }

    private static String redeLabel(RegistroHoras r) {
        if (r.isOcasional()) return "—";
        return r.isEscolaPublica() ? "Pública" : "Privada";
    }

    private static Map<String, List<RegistroHoras>> agruparPorMes(List<RegistroHoras> registros) {
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
        if (r.isOcasional()) {
            if (sb.length() > 0) sb.append(" (");
            sb.append("REUNIÃO".equals(r.getTipoAula()) ? "Reunião" : "Substituta");
            if (r.getEscolaNome() != null) sb.append(")");
        }
        return sb.length() > 0 ? sb.toString() : "—";
    }

    private static String formatarHoras(double h) {
        int totalMin = (int) Math.round(h * 60);
        int hrs = totalMin / 60; int min = totalMin % 60;
        if (hrs == 0) return min + " min";
        if (min == 0) return hrs + "h";
        return hrs + "h " + min + "min";
    }

    private static String formatarHorasLongo(double h) {
        int totalMin = (int) Math.round(h * 60);
        int hrs = totalMin / 60; int min = totalMin % 60;
        if (min == 0) return hrs + "h";
        return hrs + "h " + min + "min";
    }

    private static void addCell(PdfPTable t, String texto, BaseColor bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "—", F_CELL));
        c.setBackgroundColor(bg); c.setPadding(5);
        c.setHorizontalAlignment(align);
        c.setBorderColor(new BaseColor(226, 232, 240));
        t.addCell(c);
    }

    // ── Rodapé ───────────────────────────────────────────────────────────────

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
                String text = "Oficina do Amanhã  |  " + professor + "  |  Página " + writer.getPageNumber();
                cb.showTextAligned(Element.ALIGN_CENTER, text,
                        (document.right() - document.left()) / 2 + document.leftMargin(),
                        document.bottom() - 15, 0);
                cb.endText();
            } catch (DocumentException | IOException ignored) {}
        }
    }
}