package core;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ChamadaPresenca {

    private String          id;         // null se ainda não persistida
    private String          chamadaId;
    private String          alunoId;
    private String          alunoNome;
    private BooleanProperty presente;

    public ChamadaPresenca(String id, String chamadaId,
                           String alunoId, String alunoNome,
                           boolean presente) {
        this.id        = id;
        this.chamadaId = chamadaId;
        this.alunoId   = alunoId;
        this.alunoNome = alunoNome;
        this.presente  = new SimpleBooleanProperty(presente);
    }

    public String  getId()        { return id; }
    public void    setId(String id) { this.id = id; }
    public String  getChamadaId() { return chamadaId; }
    public String  getAlunoId()   { return alunoId; }
    public String  getAlunoNome() { return alunoNome; }

    public boolean isPresente()                   { return presente.get(); }
    public void    setPresente(boolean v)          { presente.set(v); }
    public BooleanProperty presenteProperty()      { return presente; }
}