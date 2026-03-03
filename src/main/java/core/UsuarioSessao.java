package core;

public class UsuarioSessao {
    private String id;
    private String nome;
    private String role; // "ADMIN" ou "TEACHER"

    public UsuarioSessao(String id, String nome, String role) {
        this.id = id;
        this.nome = nome;
        this.role = role;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getRole() { return role; }
}