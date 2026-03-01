package core;

import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Date;

public class Estoque {
    private List<Produto> produtos;
    private List<Fornecedor> fornecedores;

    public Estoque() {
        this.produtos = new ArrayList<>();
        this.fornecedores = new ArrayList<>();
    }

    public void cadastrarFornecedor(Fornecedor fornecedor) {
        if (fornecedor == null) {
            System.out.println("Erro: Tentativa de cadastrar um fornecedor nulo.");
            return;
        }

        if (buscarFornecedorPorCnpjCpf(fornecedor.getCnpjCpf()) == null) {
            this.fornecedores.add(fornecedor);
            System.out.println("core.Fornecedor '" + fornecedor.getNome() + "' cadastrado com sucesso!");
        } else {
            System.out.println("Erro: Já existe um fornecedor cadastrado com o CNPJ/CPF " + fornecedor.getCnpjCpf() + ".");
        }
    }

    public void cadastrarProduto(Produto produto) {
        if (produto == null) {
            return;
        }else if (buscarProdutoPorCodigo(produto.getCodigo()) == null) {
            this.produtos.add(produto);
            System.out.println("core.Produto '" + produto.getDescricao() + "' cadastrado com sucesso!");
        } else {
            System.out.println("Erro: Já existe um produto com o código " + produto.getCodigo() + ".");
        }
    }

    public Produto buscarProdutoPorCodigo(int codigo) {
        for (Produto p : this.produtos) {
            if (p.getCodigo() == codigo) {
                return p;
            }
        }
        return null;
    }

    public void registrarEntrada(int codigo, int quantidade) {
        Produto produto = buscarProdutoPorCodigo(codigo);
        if (produto != null) {
            produto.adicionarEstoque(quantidade);
            System.out.println("Entrada de " + quantidade + " unidades do produto '" + produto.getDescricao() + "' registrada.");
        } else {
            System.out.println("Erro: core.Produto com código " + codigo + " não encontrado.");
        }
    }

    public void registrarSaida(int codigo, int quantidade) {
        Produto produto = buscarProdutoPorCodigo(codigo);
        if (produto != null) {
            try {
                produto.removerEstoque(quantidade);
                System.out.println("Saída de " + quantidade + " unidades do produto '" + produto.getDescricao() + "' registrada.");
            } catch (Exception e) {
                System.out.println("Falha ao registrar saída: " + e.getMessage());
            }
        } else {
            System.out.println("Erro: core.Produto com código " + codigo + " não encontrado.");
        }
    }

    public void excluirProduto(int codigo) {
        Produto produto = buscarProdutoPorCodigo(codigo);
        if (produto != null) {
            this.produtos.remove(produto);
            System.out.println("core.Produto '" + produto.getDescricao() + "' excluído com sucesso.");
        } else {
            System.out.println("Erro: core.Produto com código " + codigo + " não encontrado para exclusão.");
        }
    }
    public double calcularValorTotalEstoque() {
        double valorTotal = 0.0;
        for (Produto p : this.produtos) {
            valorTotal += p.getQntdDisp() * p.valorUnitVenda;
        }
        return valorTotal;
    }

    public double calcularLucroTotalEstoque() {
        double lucroTotal = 0.0;
        for (Produto p : this.produtos) {
            lucroTotal += p.getQntdDisp() * p.getLucroUnitarioCalculado();
        }
        return lucroTotal;
    }

    public Fornecedor buscarFornecedorPorCnpjCpf(String cnpjCpf) {
        for (Fornecedor f : this.fornecedores) {
            if (f.getCnpjCpf().equals(cnpjCpf)) {
                return f;
            }
        }
        return null;
    }

    public void imprimirProdutosPorFornecedor(Fornecedor fornecedor) {
        System.out.printf("\n--- LISTA DE PRODUTOS DO FORNECEDOR: %s ---\n", fornecedor.getNome());
        boolean encontrouProduto = false;

        for (Produto p : this.produtos) {
            if (p.getFornecedor().equals(fornecedor)) {
                p.imprimirRelatorio();
                encontrouProduto = true;
            }
        }

        if (!encontrouProduto) {
            System.out.println("Nenhum produto encontrado para este fornecedor.");
        }
        System.out.println("--- FIM DA LISTA ---\n");
    }

    private long calcularDiasRestantes(Date dataValidade) {
        Date hoje = new Date();
        long diffEmMilissegundos = dataValidade.getTime() - hoje.getTime();
        if (diffEmMilissegundos < 0) {
            return 0;
        }
        return diffEmMilissegundos / (1000 * 60 * 60 * 24);
    }

    public void gerarRelatorioEstoqueBaixoEProximoVencimento(int limiteMinimo) {
        System.out.println("\n--- RELATÓRIO DE PRODUTOS CRÍTICOS (core.Estoque Baixo ou Vencimento Próximo) ---");

        Date hoje = new Date();
        Calendar cal = Calendar.getInstance();

        cal.setTime(hoje);
        cal.add(Calendar.MONTH, 1);
        Date limitePereciveis = cal.getTime();

        cal.setTime(hoje);
        cal.add(Calendar.MONTH, 3);
        Date limiteCosmeticos = cal.getTime();

        boolean encontrou = false;
        for (Produto p : this.produtos) {
            String motivo = "";

            if (p.getQntdDisp() < limiteMinimo) {
                motivo += "core.Estoque Baixo";
            }
            if (p instanceof ProdutoPerecivel) {
                ProdutoPerecivel perecivel = (ProdutoPerecivel) p;
                if (perecivel.getDataValidade().before(limitePereciveis) && perecivel.getDataValidade().after(hoje)) {
                    if (!motivo.isEmpty()) motivo += " e ";
                    long dias = calcularDiasRestantes(perecivel.getDataValidade());
                    motivo += "Vence em " + dias + " dias";
                }
            } else if (p instanceof Cosmetico) {
                Cosmetico cosmetico = (Cosmetico) p;
                if (cosmetico.getDataValidade().before(limiteCosmeticos) && cosmetico.getDataValidade().after(hoje)) {
                    if (!motivo.isEmpty()) motivo += " e ";
                    long dias = calcularDiasRestantes(cosmetico.getDataValidade());
                    motivo += "Vence em " + dias + " dias";
                }
            }

            if (!motivo.isEmpty()) {
                System.out.printf("Cód: %-5d | core.Produto: %-25s | Qtd: %-4d | Motivo: %s\n",
                        p.getCodigo(), p.getDescricao(), p.getQntdDisp(), motivo);
                encontrou = true;
            }
        }

        if (!encontrou) {
            System.out.println("Nenhum produto em condição crítica encontrado.");
        }
        System.out.println("--------------------------------------------------------------------------------\n");
    }

    public void imprimirListaProdutos() {
        System.out.println("\n--- LISTA COMPLETA DE PRODUTOS EM ESTOQUE ---");
        if (this.produtos.isEmpty()) {
            System.out.println("Nenhum produto cadastrado.");
        } else {
            for (Produto p : this.produtos) {
                p.imprimirRelatorio();
            }
        }
        System.out.println("--- FIM DA LISTA ---\n");
    }

    public void imprimirListaFornecedores() {
        System.out.println("\n--- LISTA DE FORNECEDORES ---");
        if (this.fornecedores.isEmpty()) {
            System.out.println("Nenhum fornecedor cadastrado.");
        } else {
            for (Fornecedor f : this.fornecedores) {
                f.imprimirRelatorio();
            }
        }
        System.out.println("--- FIM DA LISTA ---\n");
    }

    public Produto getProdutos() {
        return null;
    }

    public Fornecedor getListaFornecedores() {
        return null;
    }
}