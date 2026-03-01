package core;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Eletronico extends Produto {

    private Date dataGarantia;

    public Eletronico(int codigo, String descricao, String categoria, int qntdDisp, double valorUnitVenda, double percentualLucro, Fornecedor fornecedor, int mesesGarantia) {
        super(codigo, descricao, categoria, qntdDisp, valorUnitVenda, percentualLucro, fornecedor);

        if (mesesGarantia > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());

            cal.add(Calendar.MONTH, mesesGarantia);

            this.dataGarantia = cal.getTime();
        } else {
            throw new IllegalArgumentException("O prazo de garantia em meses deve ser um número positivo.");
        }
    }

    @Override
    public void exibirDetalhes() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        System.out.println(">>> INFORMAÇÃO ESPECÍFICA <<<");
        System.out.println("Garantia Válida Até: " + sdf.format(this.dataGarantia));
    }

    public Date getDataGarantia() {
        return dataGarantia;
    }

    public void setDataGarantia(Date dataGarantia) {
        this.dataGarantia = dataGarantia;
    }
}