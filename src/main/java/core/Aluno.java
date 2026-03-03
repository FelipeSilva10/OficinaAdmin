package core;

public class Aluno {
    private String id;
    private String nome;
    private String email;
    private String senha;
    private String turmaId;
    private String turmaNome;
    private String escolaNome;

    public Aluno(String id, String nome, String email, String senha, String turmaId, String turmaNome, String escolaNome) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.turmaId = turmaId;
        this.turmaNome = turmaNome;
        this.escolaNome = escolaNome;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public String getTurmaId() { return turmaId; }
    public String getTurmaNome() { return turmaNome; }
    public String getEscolaNome() { return escolaNome; }
}