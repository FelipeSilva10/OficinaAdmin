package core;

public class Escola {
    private String id;
    private String nome;
    private String status;
    private String tipo;   // "PUBLICA" | "PRIVADA"

    // Construtor interno unificado (privado — use os factory methods abaixo)
    private Escola(String id, String nome, String status, String tipo) {
        this.id     = id;
        this.nome   = nome;
        this.status = status;
        this.tipo   = tipo != null ? tipo : "PUBLICA";
    }

    /** Nova escola sem ID ainda, tipo padrão PUBLICA */
    public static Escola nova(String nome, String status) {
        return new Escola(null, nome, status, "PUBLICA");
    }

    /** Nova escola sem ID ainda, com tipo explícito */
    public static Escola nova(String nome, String status, String tipo) {
        return new Escola(null, nome, status, tipo);
    }

    /** Escola lida do banco — backward compat sem tipo */
    public static Escola doBanco(String id, String nome, String status) {
        return new Escola(id, nome, status, "PUBLICA");
    }

    /** Escola lida do banco — com tipo */
    public static Escola doBanco(String id, String nome, String status, String tipo) {
        return new Escola(id, nome, status, tipo);
    }

    public String getId()     { return id; }
    public void   setId(String id) { this.id = id; }

    public String getNome()   { return nome; }
    public void   setNome(String nome) { this.nome = nome; }

    public String getStatus() { return status; }
    public void   setStatus(String status) { this.status = status; }

    public String getTipo()   { return tipo != null ? tipo : "PUBLICA"; }
    public void   setTipo(String tipo) { this.tipo = tipo; }

    public boolean isPublica() { return "PUBLICA".equals(getTipo()); }

    public String getTipoLabel() {
        return "PRIVADA".equals(getTipo()) ? "Privada" : "Pública";
    }

    @Override
    public String toString() { return nome; }
}