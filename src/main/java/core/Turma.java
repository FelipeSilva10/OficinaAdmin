package core;

public class Turma {
    private String id;
    private String escolaId; // Chave estrangeira que liga à Escola
    private String nome;
    private String anoLetivo;

    // Construtor para NOVA turma (sem ID ainda)
    public Turma(String escolaId, String nome, String anoLetivo) {
        this.escolaId = escolaId;
        this.nome = nome;
        this.anoLetivo = anoLetivo;
    }

    // Construtor para LER do banco de dados (com ID)
    public Turma(String id, String escolaId, String nome, String anoLetivo) {
        this.id = id;
        this.escolaId = escolaId;
        this.nome = nome;
        this.anoLetivo = anoLetivo;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEscolaId() { return escolaId; }
    public void setEscolaId(String escolaId) { this.escolaId = escolaId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getAnoLetivo() { return anoLetivo; }
    public void setAnoLetivo(String anoLetivo) { this.anoLetivo = anoLetivo; }

    @Override
    public String toString() {
        return nome + " (" + anoLetivo + ")"; // Útil para mostrar em listas (ComboBox)
    }
}