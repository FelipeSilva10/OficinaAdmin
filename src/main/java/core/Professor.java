package core;

public class Professor {
    private String id;
    private String nome;

    public Professor(String id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }

    @Override
    public String toString() {
        return this.nome; // Para aparecer bonito nas caixas de seleção
    }
}