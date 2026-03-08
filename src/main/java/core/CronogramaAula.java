package core;

public class CronogramaAula {

    private String id;
    private String professorId;
    private String professorNome;
    private String turmaId;
    private String turmaNome;
    private String diaSemana;
    private String horarioInicio;
    private String horarioFim;
    private String tipo;        // AULA | REUNIÃO | AULA_SUBSTITUTA
    private String dataInicio;  // yyyy-MM-dd ou null
    private String dataFim;     // yyyy-MM-dd ou null
    private String criadoPor;   // ADMIN | PROFESSOR

    public CronogramaAula(String id, String professorId, String professorNome,
                          String turmaId, String turmaNome, String diaSemana,
                          String horarioInicio, String horarioFim,
                          String tipo, String dataInicio, String dataFim, String criadoPor) {
        this.id            = id;
        this.professorId   = professorId;
        this.professorNome = professorNome;
        this.turmaId       = turmaId;
        this.turmaNome     = turmaNome;
        this.diaSemana     = diaSemana;
        this.horarioInicio = horarioInicio;
        this.horarioFim    = horarioFim;
        this.tipo          = tipo          != null ? tipo      : "AULA";
        this.dataInicio    = dataInicio;
        this.dataFim       = dataFim;
        this.criadoPor     = criadoPor     != null ? criadoPor : "ADMIN";
    }

    /** Construtor de compatibilidade (sem novos campos — tipo=AULA, criadoPor=ADMIN) */
    public CronogramaAula(String id, String professorId, String turmaId,
                          String turmaNome, String diaSemana,
                          String horarioInicio, String horarioFim) {
        this(id, professorId, null, turmaId, turmaNome, diaSemana,
                horarioInicio, horarioFim, "AULA", null, null, "ADMIN");
    }

    public String getId()            { return id; }
    public String getProfessorId()   { return professorId; }
    public String getProfessorNome() { return professorNome != null ? professorNome : ""; }
    public String getTurmaId()       { return turmaId; }
    public String getTurmaNome()     { return turmaNome; }
    public String getDiaSemana()     { return diaSemana; }
    public String getHorarioInicio() { return horarioInicio; }
    public String getHorarioFim()    { return horarioFim; }
    public String getTipo()          { return tipo; }
    public String getDataInicio()    { return dataInicio; }
    public String getDataFim()       { return dataFim; }
    public String getCriadoPor()     { return criadoPor; }

    public boolean isOcasional() {
        return "REUNIÃO".equals(tipo) || "AULA_SUBSTITUTA".equals(tipo);
    }

    public String getPeriodo() {
        if (dataInicio == null && dataFim == null) return "Sem período";
        String ini = dataInicio != null ? formatarData(dataInicio) : "—";
        String fim = dataFim    != null ? formatarData(dataFim)    : "—";
        return ini + " → " + fim;
    }

    /** HH:mm – HH:mm */
    public String getHorarioFormatado() {
        return horarioInicio + " – " + horarioFim;
    }

    /** Emoji + nome do tipo */
    public String getTipoLabel() {
        return switch (tipo) {
            case "REUNIÃO"          -> "📋 Reunião";
            case "AULA_SUBSTITUTA"  -> "🔄 Substituta";
            default                 -> "📚 Aula";
        };
    }

    private static String formatarData(String iso) {
        // "2026-04-01" → "01/04/2026"
        try {
            String[] p = iso.split("-");
            return p[2] + "/" + p[1] + "/" + p[0];
        } catch (Exception e) { return iso; }
    }

    @Override
    public String toString() {
        return turmaNome + " | " + diaSemana + " " + getHorarioFormatado();
    }
}