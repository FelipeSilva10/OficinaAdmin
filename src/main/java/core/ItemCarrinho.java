package core;

public class ItemCarrinho {
    private final Produto produto;
    private int quantidade;
    private double subtotal;

    public ItemCarrinho(Produto produto, int quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.subtotal = produto.getValorUnitVenda() * quantidade;
    }

    // Getters formatados especificamente para a Tabela do JavaFX conseguir ler
    public int getCodigo() { return produto.getCodigo(); }
    public String getDescricao() { return produto.getDescricao(); }
    public int getQuantidade() { return quantidade; }

    public String getValorUnitarioFormatado() {
        return String.format("R$ %.2f", produto.getValorUnitVenda());
    }

    public String getSubtotalFormatado() {
        return String.format("R$ %.2f", subtotal);
    }

    // Getters normais para a lógica matemática
    public Produto getProduto() { return produto; }
    public double getSubtotal() { return subtotal; }

    // Metodo para somar itens iguais bipados repetidamente
    public void adicionarQuantidade(int qtd) {
        this.quantidade += qtd;
        this.subtotal = this.produto.getValorUnitVenda() * this.quantidade;
    }
    // Novo metodo para permitir a edição direta na tabela
    public void setQuantidade(int qtd) {
        this.quantidade = qtd;
        this.subtotal = this.produto.getValorUnitVenda() * this.quantidade;
    }
}