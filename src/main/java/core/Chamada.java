package core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Chamada {

    private static final DateTimeFormatter FMT_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String     id;
    private String     professorId;
    private String     turmaId;
    private String     turmaNome;
    private String     cronogramaId;   // pode ser null se criada manualmente
    private LocalDate  dataAula;
    private String     horarioInicio;
    private String     horarioFim;
    private int        totalAlunos;
    private int        totalPresentes;
    private List<ChamadaPresenca> presencas; // preenchido ao abrir chamada

    public Chamada(String id, String professorId, String turmaId, String turmaNome,
                   String cronogramaId, LocalDate dataAula,
                   String horarioInicio, String horarioFim,
                   int totalAlunos, int totalPresentes) {
        this.id             = id;
        this.professorId    = professorId;
        this.turmaId        = turmaId;
        this.turmaNome      = turmaNome;
        this.cronogramaId   = cronogramaId;
        this.dataAula       = dataAula;
        this.horarioInicio  = horarioInicio;
        this.horarioFim     = horarioFim;
        this.totalAlunos    = totalAlunos;
        this.totalPresentes = totalPresentes;
    }

    public String    getId()             { return id; }
    public String    getProfessorId()    { return professorId; }
    public String    getTurmaId()        { return turmaId; }
    public String    getTurmaNome()      { return turmaNome; }
    public String    getCronogramaId()   { return cronogramaId; }
    public LocalDate getDataAula()       { return dataAula; }
    public String    getDataFormatada()  { return dataAula != null ? dataAula.format(FMT_BR) : ""; }
    public String    getHorarioInicio()  { return horarioInicio; }
    public String    getHorarioFim()     { return horarioFim; }
    public int       getTotalAlunos()    { return totalAlunos; }
    public int       getTotalPresentes() { return totalPresentes; }
    public int       getTotalAusentes()  { return totalAlunos - totalPresentes; }

    public List<ChamadaPresenca> getPresencas()                      { return presencas; }
    public void setPresencas(List<ChamadaPresenca> presencas)        { this.presencas = presencas; }

    public String getResumoPresenca() {
        return totalPresentes + "/" + totalAlunos;
    }
}