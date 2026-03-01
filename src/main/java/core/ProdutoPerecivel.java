package core;

import java.util.Date;
import java.text.SimpleDateFormat;

public class ProdutoPerecivel extends Produto {

    private Date dataValidade;

    public ProdutoPerecivel(int codigo, String descricao, String categoria, int qntdDisp, double valorUnitVenda, double percentualLucro, Fornecedor fornecedor, Date dataValidade) {
        super(codigo, descricao, categoria, qntdDisp, valorUnitVenda, percentualLucro, fornecedor);
        this.dataValidade = dataValidade;
    }

    @Override
    public void exibirDetalhes() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        System.out.println(">>> INFORMAÇÃO ESPECÍFICA <<<");
        System.out.println("Data de Validade: " + sdf.format(this.dataValidade));
    }

    public Date getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(Date dataValidade) {
        this.dataValidade = dataValidade;
    }
}