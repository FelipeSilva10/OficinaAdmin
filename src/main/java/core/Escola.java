package core;

public class Escola {
    private String id; // O Supabase gera este UUID automaticamente
    private String nome;
    private String status;

    // Construtor para criar uma NOVA escola (sem ID ainda)
    public Escola(String nome, String status) {
        this.nome = nome;
        this.status = status;
    }

    // Construtor para ler escolas que JÁ ESTÃO no banco (com ID)
    public Escola(String id, String nome, String status) {
        this.id = id;
        this.nome = nome;
        this.status = status;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return this.nome; // É isto que diz ao ComboBox para mostrar o nome e não um código!
    }
}