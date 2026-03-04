package core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RegistroHoras {

    private static final DateTimeFormatter FMT_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String    chamadaId;
    private String    turmaId;
    private String    turmaNome;
    private String    escolaNome;
    private LocalDate dataAula;
    private String    horarioInicio;
    private String    horarioFim;
    private double    horasMinistradas;
    private int       totalAlunos;
    private int       totalPresentes;
    private int       totalAusentes;

    public RegistroHoras(String chamadaId, String turmaId, String turmaNome,
                         String escolaNome, LocalDate dataAula,
                         String horarioInicio, String horarioFim,
                         double horasMinistradas,
                         int totalAlunos, int totalPresentes, int totalAusentes) {
        this.chamadaId        = chamadaId;
        this.turmaId          = turmaId;
        this.turmaNome        = turmaNome;
        this.escolaNome       = escolaNome;
        this.dataAula         = dataAula;
        this.horarioInicio    = horarioInicio;
        this.horarioFim       = horarioFim;
        this.horasMinistradas = horasMinistradas;
        this.totalAlunos      = totalAlunos;
        this.totalPresentes   = totalPresentes;
        this.totalAusentes    = totalAusentes;
    }

    public String    getChamadaId()         { return chamadaId; }
    public String    getTurmaId()           { return turmaId; }
    public String    getTurmaNome()         { return turmaNome; }
    public String    getEscolaNome()        { return escolaNome; }
    public LocalDate getDataAula()          { return dataAula; }
    public String    getDataFormatada()     { return dataAula != null ? dataAula.format(FMT_BR) : ""; }
    public String    getHorarioInicio()     { return horarioInicio; }
    public String    getHorarioFim()        { return horarioFim; }
    public String    getHorario()           { return horarioInicio + " – " + horarioFim; }
    public double    getHorasMinistradas()  { return horasMinistradas; }
    public String    getHorasFormatadas()   { return String.format("%.1fh", horasMinistradas); }
    public int       getTotalAlunos()       { return totalAlunos; }
    public int       getTotalPresentes()    { return totalPresentes; }
    public int       getTotalAusentes()     { return totalAusentes; }
}