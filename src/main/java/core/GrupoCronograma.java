package core;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa um grupo de slots do cronograma que compartilham
 * professor + turma + horário + período — diferindo apenas nos dias da semana.
 *
 * No banco cada dia é uma linha; na tabela exibimos como uma só.
 */
public class GrupoCronograma {

    private final List<String> ids;           // um id por dia no DB
    private final String professorId;
    private final String professorNome;
    private final String turmaId;
    private final String turmaNome;
    private final List<String> dias;          // ["SEGUNDA","TERÇA","SEXTA"]
    private final String horarioInicio;
    private final String horarioFim;
    private final String dataInicio;          // yyyy-MM-dd ou null
    private final String dataFim;
    private final String tipo;
    private final String criadoPor;

    // Abreviações para exibição
    private static final java.util.Map<String, String> ABREV = java.util.Map.of(
            "SEGUNDA", "SEG", "TERÇA",  "TER", "QUARTA", "QUA",
            "QUINTA",  "QUI", "SEXTA",  "SEX", "SÁBADO", "SÁB"
    );
    // Ordem canônica para ordenar dias ao exibir
    private static final List<String> ORDEM =
            List.of("SEGUNDA","TERÇA","QUARTA","QUINTA","SEXTA","SÁBADO");

    public GrupoCronograma(List<String> ids, String professorId, String professorNome,
                           String turmaId, String turmaNome, List<String> dias,
                           String horarioInicio, String horarioFim,
                           String dataInicio, String dataFim,
                           String tipo, String criadoPor) {
        this.ids           = ids;
        this.professorId   = professorId;
        this.professorNome = professorNome != null ? professorNome : "";
        this.turmaId       = turmaId;
        this.turmaNome     = turmaNome;
        this.dias          = dias.stream()
                .sorted(java.util.Comparator.comparingInt(ORDEM::indexOf))
                .collect(Collectors.toList());
        this.horarioInicio = horarioInicio;
        this.horarioFim    = horarioFim;
        this.dataInicio    = dataInicio;
        this.dataFim       = dataFim;
        this.tipo          = tipo != null ? tipo : "AULA";
        this.criadoPor     = criadoPor != null ? criadoPor : "ADMIN";
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public List<String> getIds()          { return ids; }
    public String getProfessorId()        { return professorId; }
    public String getProfessorNome()      { return professorNome; }
    public String getTurmaId()            { return turmaId; }
    public String getTurmaNome()          { return turmaNome; }
    public List<String> getDias()         { return dias; }
    public String getHorarioInicio()      { return horarioInicio; }
    public String getHorarioFim()         { return horarioFim; }
    public String getDataInicio()         { return dataInicio; }
    public String getDataFim()            { return dataFim; }
    public String getTipo()               { return tipo; }
    public String getCriadoPor()          { return criadoPor; }

    // ── Exibição ─────────────────────────────────────────────────────────────

    /** "SEG · TER · SEX" */
    public String getDiasFormatados() {
        return dias.stream()
                .map(d -> ABREV.getOrDefault(d, d))
                .collect(Collectors.joining(" · "));
    }

    /** "HH:mm – HH:mm" */
    public String getHorarioFormatado() {
        return horarioInicio + " – " + horarioFim;
    }

    /** "01/04/2026 → 07/11/2026" */
    public String getPeriodo() {
        if (dataInicio == null && dataFim == null) return "Sem período";
        return formatarData(dataInicio) + " → " + formatarData(dataFim);
    }

    public String getTipoLabel() {
        return switch (tipo) {
            case "REUNIÃO"         -> "📋 Reunião";
            case "AULA_SUBSTITUTA" -> "🔄 Substituta";
            default                -> "📚 Aula";
        };
    }

    public boolean isOcasional() {
        return "REUNIÃO".equals(tipo) || "AULA_SUBSTITUTA".equals(tipo);
    }

    private static String formatarData(String iso) {
        if (iso == null) return "—";
        try { String[] p = iso.split("-"); return p[2]+"/"+p[1]+"/"+p[0]; }
        catch (Exception e) { return iso; }
    }

    // ── Chave de agrupamento ─────────────────────────────────────────────────

    /**
     * Chave usada para decidir se dois CronogramaAula pertencem ao mesmo grupo:
     * mesmo professor + turma + horário + período + tipo.
     */
    public static String chave(CronogramaAula s) {
        return s.getProfessorId() + "|" + s.getTurmaId() + "|"
                + s.getHorarioInicio() + "|" + s.getHorarioFim() + "|"
                + s.getDataInicio()    + "|" + s.getDataFim()
                + "|" + s.getTipo();
    }

    /**
     * Agrupa uma lista plana de CronogramaAula em GrupoCronograma,
     * preservando a ordem de aparição.
     */
    public static List<GrupoCronograma> agrupar(List<CronogramaAula> slots) {
        java.util.LinkedHashMap<String, List<CronogramaAula>> mapa = new java.util.LinkedHashMap<>();
        for (CronogramaAula s : slots) {
            mapa.computeIfAbsent(chave(s), k -> new java.util.ArrayList<>()).add(s);
        }
        List<GrupoCronograma> grupos = new java.util.ArrayList<>();
        for (List<CronogramaAula> grupo : mapa.values()) {
            CronogramaAula ref = grupo.get(0);
            grupos.add(new GrupoCronograma(
                    grupo.stream().map(CronogramaAula::getId).collect(Collectors.toList()),
                    ref.getProfessorId(), ref.getProfessorNome(),
                    ref.getTurmaId(), ref.getTurmaNome(),
                    grupo.stream().map(CronogramaAula::getDiaSemana).collect(Collectors.toList()),
                    ref.getHorarioInicio(), ref.getHorarioFim(),
                    ref.getDataInicio(), ref.getDataFim(),
                    ref.getTipo(), ref.getCriadoPor()
            ));
        }
        return grupos;
    }
}