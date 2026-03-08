package core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RegistroHoras {

    private static final DateTimeFormatter FMT_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String    chamadaId;
    private String    professorId;
    private String    professorNome;
    private String    turmaId;
    private String    turmaNome;
    private String    escolaNome;
    private LocalDate dataAula;
    private String    horarioInicio;
    private String    horarioFim;
    private String    tipoAula;          // AULA | REUNIÃO | AULA_SUBSTITUTA
    private double    horasMinistradas;
    private int       totalAlunos;
    private int       totalPresentes;
    private int       totalAusentes;

    public RegistroHoras(String chamadaId, String professorId, String professorNome,
                         String turmaId, String turmaNome, String escolaNome,
                         LocalDate dataAula, String horarioInicio, String horarioFim,
                         String tipoAula, double horasMinistradas,
                         int totalAlunos, int totalPresentes, int totalAusentes) {
        this.chamadaId        = chamadaId;
        this.professorId      = professorId;
        this.professorNome    = professorNome;
        this.turmaId          = turmaId;
        this.turmaNome        = turmaNome;
        this.escolaNome       = escolaNome;
        this.dataAula         = dataAula;
        this.horarioInicio    = horarioInicio;
        this.horarioFim       = horarioFim;
        this.tipoAula         = tipoAula != null ? tipoAula : "AULA";
        this.horasMinistradas = horasMinistradas;
        this.totalAlunos      = totalAlunos;
        this.totalPresentes   = totalPresentes;
        this.totalAusentes    = totalAusentes;
    }

    /** Construtor de compatibilidade sem professorId/Nome e tipoAula */
    public RegistroHoras(String chamadaId, String turmaId, String turmaNome,
                         String escolaNome, LocalDate dataAula,
                         String horarioInicio, String horarioFim,
                         double horasMinistradas,
                         int totalAlunos, int totalPresentes, int totalAusentes) {
        this(chamadaId, null, null, turmaId, turmaNome, escolaNome,
                dataAula, horarioInicio, horarioFim, "AULA",
                horasMinistradas, totalAlunos, totalPresentes, totalAusentes);
    }

    public String    getChamadaId()         { return chamadaId; }
    public String    getProfessorId()       { return professorId; }
    public String    getProfessorNome()     { return professorNome != null ? professorNome : ""; }
    public String    getTurmaId()           { return turmaId; }
    public String    getTurmaNome()         { return turmaNome; }
    public String    getEscolaNome()        { return escolaNome; }
    public LocalDate getDataAula()          { return dataAula; }
    public String    getDataFormatada()     { return dataAula != null ? dataAula.format(FMT_BR) : ""; }
    public String    getHorarioInicio()     { return horarioInicio; }
    public String    getHorarioFim()        { return horarioFim; }
    public String    getHorario()           { return horarioInicio + " – " + horarioFim; }
    public String    getTipoAula()          { return tipoAula; }
    public String    getTipoLabel()         {
        return switch (tipoAula) {
            case "REUNIÃO"         -> "📋 Reunião";
            case "AULA_SUBSTITUTA" -> "🔄 Substituta";
            default                -> "📚 Aula";
        };
    }
    public double    getHorasMinistradas()  { return horasMinistradas; }
    public String    getHorasFormatadas()   { return String.format("%.1fh", horasMinistradas); }
    public int       getTotalAlunos()       { return totalAlunos; }
    public int       getTotalPresentes()    { return totalPresentes; }
    public int       getTotalAusentes()     { return totalAusentes; }
}