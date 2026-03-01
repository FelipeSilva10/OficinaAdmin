package core;

public abstract class Produto implements IRelatorio {
    protected String descricao;
    protected String categoria;
    protected int codigo;
    protected int qntdDisp;
    protected double valorUnitVenda;
    protected double percentualLucro;
    protected Fornecedor fornecedor;

    public Produto(int codigo, String descricao, String categoria, int qntdDisp, double valorUnitVenda, double percentualLucro, Fornecedor fornecedor) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.qntdDisp = qntdDisp;
        this.valorUnitVenda = valorUnitVenda;
        this.percentualLucro = percentualLucro;
        this.fornecedor = fornecedor;
    }

    public abstract void exibirDetalhes();

    public void adicionarEstoque(int quantidade) {
        if (quantidade > 0) {
            this.qntdDisp += quantidade;
        } else {
            System.out.println("Erro: A quantidade a ser adicionada deve ser positiva.");
        }
    }

    public double getLucroUnitarioCalculado() {
        return this.valorUnitVenda * (this.percentualLucro / 100.0);
    }

    public void removerEstoque(int quantidade) throws Exception {
        if (quantidade <= 0) {
            throw new Exception("A quantidade a ser removida deve ser positiva.");
        }
        if (this.qntdDisp >= quantidade) {
            this.qntdDisp -= quantidade;
        } else {
            throw new Exception("Erro: core.Estoque insuficiente para realizar a baixa.");
        }
    }

    public int getCodigo() {
        return codigo;
    }
    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
    public int getQntdDisp() {
        return qntdDisp;
    }
    public Fornecedor getFornecedor() {
        return fornecedor;
    }
    public void setFornecedor(Fornecedor fornecedor) {
        this.fornecedor = fornecedor;
    }
    public double getValorUnitVenda() { return valorUnitVenda; }
    public void setValorUnitVenda(double valorUnitVenda) { this.valorUnitVenda = valorUnitVenda; }
    public double getPercentualLucro() { return percentualLucro; }
    public void setPercentualLucro(double percentualLucro) { this.percentualLucro = percentualLucro; }

    public String getCategoria() {return categoria;}
    public void setCategoria(String categoria) {this.categoria = categoria;}

    @Override
    public void gerarCabecalho() {
        System.out.println("-------------------------------------------------");
        System.out.printf("Relatório do core.Produto: %s (Cód: %d)\n", this.descricao, this.codigo);
        System.out.println("-------------------------------------------------");
    }

    @Override
    public void gerarCorpo() {
        System.out.println("Descrição: " + this.descricao);
        System.out.println("Categoria: " + this.categoria);
        System.out.println("Quantidade Disponível: " + this.qntdDisp);
        System.out.printf("Valor de Venda Unitário: R$ %.2f\n", this.valorUnitVenda);
        System.out.printf("Lucro (%.1f%%): R$ %.2f por unidade\n", this.percentualLucro, getLucroUnitarioCalculado());
        System.out.println("core.Fornecedor: " + this.fornecedor.getNome());
    }

    @Override
    public void imprimirRelatorio() {
        gerarCabecalho();
        gerarCorpo();
        exibirDetalhes();
        System.out.println("-------------------------------------------------\n");
    }
}