package core;

public class Professor {
    private String id;
    private String escolaId;
    private String nome;
    private String escolaNome; // Nome da escola para mostrar na tabela

    public Professor(String id, String escolaId, String nome, String escolaNome) {
        this.id = id;
        this.escolaId = escolaId;
        this.nome = nome;
        this.escolaNome = escolaNome;
    }

    public String getId() { return id; }
    public String getEscolaId() { return escolaId; }
    public String getNome() { return nome; }
    public String getEscolaNome() { return escolaNome; }
}