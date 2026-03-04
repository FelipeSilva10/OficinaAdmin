package core;

public class CronogramaAula {

    private String id;
    private String professorId;
    private String turmaId;
    private String turmaNome;
    private String diaSemana;      // ex: "SEGUNDA"
    private String horarioInicio;  // ex: "08:00"
    private String horarioFim;     // ex: "09:30"

    public CronogramaAula(String id, String professorId, String turmaId,
                          String turmaNome, String diaSemana,
                          String horarioInicio, String horarioFim) {
        this.id            = id;
        this.professorId   = professorId;
        this.turmaId       = turmaId;
        this.turmaNome     = turmaNome;
        this.diaSemana     = diaSemana;
        this.horarioInicio = horarioInicio;
        this.horarioFim    = horarioFim;
    }

    public String getId()            { return id; }
    public String getProfessorId()   { return professorId; }
    public String getTurmaId()       { return turmaId; }
    public String getTurmaNome()     { return turmaNome; }
    public String getDiaSemana()     { return diaSemana; }
    public String getHorarioInicio() { return horarioInicio; }
    public String getHorarioFim()    { return horarioFim; }

    /** Retorna "HH:mm – HH:mm" para exibição nas células do grid. */
    public String getHorarioFormatado() {
        return horarioInicio + " – " + horarioFim;
    }

    @Override
    public String toString() {
        return turmaNome + " | " + diaSemana + " " + getHorarioFormatado();
    }
}