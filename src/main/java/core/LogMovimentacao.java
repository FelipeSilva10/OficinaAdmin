package core;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class LogMovimentacao {
    private final int id;
    private final int codigoProduto;
    private final String tipoMovimentacao;
    private final int quantidade;
    private final String motivo;
    private final String observacao;
    private final String usuario;
    private final Timestamp dataHora;

    public LogMovimentacao(int id, int codigoProduto, String tipoMovimentacao, int quantidade, String motivo, String observacao, String usuario, Timestamp dataHora) {
        this.id = id;
        this.codigoProduto = codigoProduto;
        this.tipoMovimentacao = tipoMovimentacao;
        this.quantidade = quantidade;
        this.motivo = motivo != null ? motivo : "-";
        this.observacao = observacao != null ? observacao : "-";
        this.usuario = usuario;
        this.dataHora = dataHora;
    }

    // Getters formatados para a Tabela do JavaFX
    public int getId() { return id; }
    public int getCodigoProduto() { return codigoProduto; }
    public String getTipoMovimentacao() { return tipoMovimentacao; }
    public int getQuantidade() { return quantidade; }
    public String getMotivo() { return motivo; }
    public String getObservacao() { return observacao; }
    public String getUsuario() { return usuario; }

    // Converte o Timestamp do banco para uma String bonita (ex: 25/02/2026 14:30:00)
    public String getDataHoraFormatada() {
        if (dataHora == null) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return sdf.format(dataHora);
    }
}