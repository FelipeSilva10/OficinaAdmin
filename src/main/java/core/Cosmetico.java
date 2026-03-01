package core;

import java.util.Date;
import java.text.SimpleDateFormat;

public class Cosmetico extends Produto {

    private Date dataValidade;
    private String fabricante;

    public Cosmetico(int codigo, String descricao, String categoria, int qntdDisp, double valorUnitVenda, double percentualLucro, Fornecedor fornecedor, Date dataValidade, String fabricante) {
        super(codigo, descricao, categoria, qntdDisp, valorUnitVenda, percentualLucro, fornecedor);
        this.dataValidade = dataValidade;
        this.fabricante = fabricante;
    }

    @Override
    public void exibirDetalhes() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        System.out.println(">>> INFORMAÇÃO ESPECÍFICA <<<");
        System.out.println("Fabricante: " + this.fabricante);
        System.out.println("Data de Validade: " + sdf.format(this.dataValidade));
    }

    public Date getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(Date dataValidade) {
        this.dataValidade = dataValidade;
    }

    public String getFabricante() {
        return fabricante;
    }

    public void setFabricante(String fabricante) {
        this.fabricante = fabricante;
    }
}