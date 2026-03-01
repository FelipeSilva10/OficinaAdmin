package core;

public class Turma {
    private String id;
    private String escolaId;
    private String nome;
    private String anoLetivo;

    // Campos visuais (vindos do JOIN no BD)
    private String escolaNome;
    private String professorNome;

    public Turma(String id, String escolaId, String nome, String anoLetivo, String escolaNome, String professorNome) {
        this.id = id;
        this.escolaId = escolaId;
        this.nome = nome;
        this.anoLetivo = anoLetivo;
        this.escolaNome = escolaNome;
        this.professorNome = professorNome != null ? professorNome : "Sem Professor";
    }

    public String getId() { return id; }
    public String getEscolaId() { return escolaId; }
    public String getNome() { return nome; }
    public String getAnoLetivo() { return anoLetivo; }
    public String getEscolaNome() { return escolaNome; }
    public String getProfessorNome() { return professorNome; }

    @Override
    public String toString() {
        return nome + " (" + anoLetivo + ")";
    }
}