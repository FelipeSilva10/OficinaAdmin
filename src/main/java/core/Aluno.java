package core;

public class Aluno {
    private String id;
    private String nome;
    private String turmaId;
    private String turmaNome;
    private String escolaNome;

    public Aluno(String id, String nome, String turmaId, String turmaNome, String escolaNome) {
        this.id = id;
        this.nome = nome;
        this.turmaId = turmaId;
        this.turmaNome = turmaNome;
        this.escolaNome = escolaNome;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getTurmaId() { return turmaId; }
    public String getTurmaNome() { return turmaNome; }
    public String getEscolaNome() { return escolaNome; }
}