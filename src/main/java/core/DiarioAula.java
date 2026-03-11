package core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Representa uma entrada no Diário de Aulas do professor.
 * Armazena: turma, data, título da aula, conteúdo trabalhado e observações gerais.
 */
public class DiarioAula {

    private static final DateTimeFormatter FMT_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String    id;
    private final String    professorId;
    private final String    turmaId;
    private final String    turmaNome;
    private final String    escolaNome;
    private final LocalDate dataAula;
    private final String    titulo;
    private final String    conteudo;
    private final String    observacoes;

    public DiarioAula(String id, String professorId,
                      String turmaId, String turmaNome, String escolaNome,
                      LocalDate dataAula, String titulo,
                      String conteudo, String observacoes) {
        this.id          = id;
        this.professorId = professorId;
        this.turmaId     = turmaId;
        this.turmaNome   = turmaNome    != null ? turmaNome    : "";
        this.escolaNome  = escolaNome   != null ? escolaNome   : "";
        this.dataAula    = dataAula;
        this.titulo      = titulo       != null ? titulo       : "";
        this.conteudo    = conteudo     != null ? conteudo     : "";
        this.observacoes = observacoes  != null ? observacoes  : "";
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String    getId()            { return id; }
    public String    getProfessorId()   { return professorId; }
    public String    getTurmaId()       { return turmaId; }
    public String    getTurmaNome()     { return turmaNome; }
    public String    getEscolaNome()    { return escolaNome; }
    public LocalDate getDataAula()      { return dataAula; }
    public String    getTitulo()        { return titulo; }
    public String    getConteudo()      { return conteudo; }
    public String    getObservacoes()   { return observacoes; }

    public String getDataFormatada() {
        return dataAula != null ? dataAula.format(FMT_BR) : "";
    }

    /** Retorna a primeira linha do conteúdo para preview na tabela. */
    public String getConteudoPreview() {
        if (conteudo.isBlank()) return "—";
        String linha = conteudo.lines().findFirst().orElse("").trim();
        return linha.length() > 80 ? linha.substring(0, 77) + "..." : linha;
    }

    @Override
    public String toString() {
        return dataAula + " — " + turmaNome + ": " + titulo;
    }
}
