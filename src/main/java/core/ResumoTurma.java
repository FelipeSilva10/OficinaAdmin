package core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** DTO para preview de chamadas por turma na tela de Chamada. */
public class ResumoTurma {

    private final String    turmaId;
    private final String    turmaNome;
    private final String    escolaNome;
    private final int       totalChamadas;
    private final double    mediaPresenca;   // 0-100
    private final LocalDate ultimaChamada;   // null se nenhuma chamada ainda

    public ResumoTurma(String turmaId, String turmaNome, String escolaNome,
                       int totalChamadas, double mediaPresenca, LocalDate ultimaChamada) {
        this.turmaId       = turmaId;
        this.turmaNome     = turmaNome;
        this.escolaNome    = escolaNome;
        this.totalChamadas = totalChamadas;
        this.mediaPresenca = mediaPresenca;
        this.ultimaChamada = ultimaChamada;
    }

    public String    getTurmaId()       { return turmaId; }
    public String    getTurmaNome()     { return turmaNome; }
    public String    getEscolaNome()    { return escolaNome; }
    public int       getTotalChamadas() { return totalChamadas; }
    public double    getMediaPresenca() { return mediaPresenca; }
    public LocalDate getUltimaChamada() { return ultimaChamada; }

    public String getUltimaChamadaFormatada() {
        if (ultimaChamada == null) return "Nenhuma ainda";
        return ultimaChamada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getMediaPresencaFormatada() {
        if (totalChamadas == 0) return "—";
        return String.format("%.0f%%", mediaPresenca);
    }
}