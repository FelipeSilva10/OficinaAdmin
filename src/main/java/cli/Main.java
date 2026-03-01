package cli;

import core.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {

    private static Scanner scanner = new Scanner(System.in);
    private static Estoque meuEstoque = new Estoque();

    public static void main(String[] args) {
        carregarDadosIniciais();

        int opcao = -1;
        while (opcao != 0) {
            exibirMenu();
            try {
                opcao = scanner.nextInt();
                scanner.nextLine();
                switch (opcao) {
                    case 1:
                        cadastrarNovoProduto();
                        break;
                    case 2:
                        registrarEntrada();
                        break;
                    case 3:
                        registrarSaida();
                        break;
                    case 4:
                        excluirProdutoDoEstoque();
                        break;
                    case 5:
                        exibirSubmenuRelatorios();
                        break;
                    case 0:
                        System.out.println("Encerrando o sistema...");
                        break;
                    default:
                        System.out.println("Opção inválida! Tente novamente.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Erro: Por favor, digite um número válido para a opção.");
                scanner.nextLine();
                opcao = -1;
            }
        }
        scanner.close();
    }

    private static void exibirMenu() {
        System.out.println("\n--- MENU PRINCIPAL ---");
        System.out.println("1. Cadastrar Novo core.Produto");
        System.out.println("2. Registrar Entrada de core.Estoque");
        System.out.println("3. Registrar Saída de core.Estoque (Venda)");
        System.out.println("4. Excluir core.Produto");
        System.out.println("5. Gerar Relatórios");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private static void exibirSubmenuRelatorios() {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- SUBMENU DE RELATÓRIOS ---");
            System.out.println("1. Listagem completa de produtos (com fornecedores)");
            System.out.println("2. Listagem de produtos por fornecedor");
            System.out.println("3. Relatório de produtos com estoque baixo ou próximos do vencimento");
            System.out.println("4. Relatório de valores totais do estoque (Venda e Lucro)");
            System.out.println("5. Listagem completa de fornecedores");
            System.out.println("6. Consultar produto específico");
            System.out.println("0. Voltar ao menu principal");

            opcao = lerInteiroComTratamento("Escolha uma opção de relatório: ");

            switch (opcao) {
                case 1:
                    meuEstoque.imprimirListaProdutos();
                    break;
                case 2:
                    Fornecedor f = escolherFornecedor();
                    meuEstoque.imprimirProdutosPorFornecedor(f);
                    break;
                case 3:
                    int limite = lerInteiroComTratamento("Digite o limite mínimo de estoque para o alerta: ");
                    meuEstoque.gerarRelatorioEstoqueBaixoEProximoVencimento(limite);
                    break;
                case 4:
                    System.out.println("\n--- RELATÓRIO DE VALORES TOTAIS ---");
                    System.out.printf(">> Valor Total de Venda do core.Estoque: R$ %.2f\n", meuEstoque.calcularValorTotalEstoque());
                    System.out.printf(">> Lucro Total Potencial do core.Estoque: R$ %.2f\n", meuEstoque.calcularLucroTotalEstoque());
                    System.out.println("------------------------------------");
                    break;
                case 5:
                    meuEstoque.imprimirListaFornecedores();
                    break;
                case 6:
                    consultarProdutoEspecifico();
                    break;
                case 0:
                    System.out.println("Voltando ao menu principal...");
                    break;
                default:
                    System.out.println("Opção inválida! Tente novamente.");
            }
        }
    }

    public static void carregarDadosIniciais() {
        System.out.println("Carregando produtos pré-cadastrados (nosso 'Banco de Dados')...");
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

            Fornecedor f1 = new Fornecedor(1, "Beleza & Cia", "11.222.333/0001-44", "(34) 99999-1111");
            Fornecedor f2 = new Fornecedor(2, "TecnoMundo", "55.666.777/0001-88", "(34) 98888-2222");
            meuEstoque.cadastrarFornecedor(f1);
            meuEstoque.cadastrarFornecedor(f2);

            Produto p1 = new Cosmetico(101, "Protetor Solar facial", "Pele", 20, 27.90, 40.0, f1, sdf.parse("31/12/2026"), "Sallve");
            Produto p2 = new Eletronico(201, "Smartphone Iphone 16", "Celulares", 15, 4499.99, 30.0, f2, 12);
            Produto p3 = new ProdutoPerecivel(301, "Caixa de Leite", "Laticínios", 50, 4.50, 25.0, f1, sdf.parse("25/09/2025"));
            meuEstoque.cadastrarProduto(p1);
            meuEstoque.cadastrarProduto(p2);
            meuEstoque.cadastrarProduto(p3);
            System.out.println("Dados carregados com sucesso!\n");
        } catch (ParseException e) {
            System.out.println("Ocorreu um erro ao carregar os dados iniciais: " + e.getMessage());
        }
    }

    private static void registrarEntrada() {

        System.out.println("\n--- REGISTRO DE ENTRADA DE ESTOQUE ---");
        int codigo = lerInteiroComTratamento("Digite o código do produto: ");
        Produto produto = meuEstoque.buscarProdutoPorCodigo(codigo);

        if (produto != null) {
            System.out.println("core.Produto encontrado: " + produto.getDescricao());
            int quantidade = 0;
            while (quantidade <= 0) {
                quantidade = lerInteiroComTratamento("Digite a quantidade de entrada (deve ser maior que zero): ");
                if (quantidade <= 0) {
                    System.out.println("Erro: A quantidade deve ser um número positivo.");
                }
            }
            meuEstoque.registrarEntrada(codigo, quantidade);
        } else {
            System.out.println("Erro: core.Produto com código " + codigo + " não encontrado.");
        }
    }

    private static void registrarSaida() {

        System.out.println("\n--- REGISTRO DE SAÍDA DE ESTOQUE (VENDA) ---");
        int codigo = lerInteiroComTratamento("Digite o código do produto: ");
        Produto produto = meuEstoque.buscarProdutoPorCodigo(codigo);

        if (produto != null) {
            System.out.println("core.Produto encontrado: " + produto.getDescricao());
            System.out.println("Quantidade disponível: " + produto.getQntdDisp());
            int quantidade = 0;
            while (quantidade <= 0) {
                quantidade = lerInteiroComTratamento("Digite a quantidade de saída (deve ser maior que zero): ");
                if (quantidade <= 0) {
                    System.out.println("Erro: A quantidade deve ser um número positivo.");
                }
            }
            meuEstoque.registrarSaida(codigo, quantidade);
        } else {
            System.out.println("Erro: core.Produto com código " + codigo + " não encontrado.");
        }
    }

    private static void gerarRelatorioEstoqueBaixo() {
        System.out.print("Digite o limite mínimo de estoque: ");
        int limite = scanner.nextInt();
        scanner.nextLine();
        meuEstoque.gerarRelatorioEstoqueBaixoEProximoVencimento(limite);
    }

    private static void excluirProdutoDoEstoque() {
        System.out.println("\n--- EXCLUSÃO DE PRODUTO ---");
        int codigo = lerInteiroComTratamento("Digite o código do produto que deseja EXCLUIR: ");
        Produto produto = meuEstoque.buscarProdutoPorCodigo(codigo);

        if (produto != null) {
            System.out.println("core.Produto encontrado: " + produto.getDescricao());
            System.out.print("!!! ATENÇÃO !!! Esta ação é permanente. Deseja realmente excluir este produto? (S/N): ");
            String confirmacao = scanner.nextLine().trim().toUpperCase();

            if (confirmacao.equals("S")) {
                meuEstoque.excluirProduto(codigo);
            } else {
                System.out.println("Exclusão cancelada pelo usuário.");
            }
        } else {
            System.out.println("Erro: core.Produto com código " + codigo + " não encontrado para exclusão.");
        }
    }

    private static void consultarProdutoEspecifico() {
        System.out.println("\n--- CONSULTA DE PRODUTO ESPECÍFICO ---");
        int codigo = lerInteiroComTratamento("Digite o código do produto que deseja consultar: ");
        Produto produto = meuEstoque.buscarProdutoPorCodigo(codigo);

        if (produto != null) {
            System.out.println("core.Produto encontrado. Exibindo detalhes:");
            produto.imprimirRelatorio();
        } else {
            System.out.println("Erro: core.Produto com código " + codigo + " não encontrado no estoque.");
        }
    }

    private static int lerInteiroComTratamento(String mensagem) {
        while (true) {
            try {
                System.out.print(mensagem);
                int valor = scanner.nextInt();
                scanner.nextLine();
                return valor;
            } catch (InputMismatchException e) {
                System.out.println("Erro: Entrada inválida. Por favor, digite um número inteiro. Tente novamente.");
                scanner.nextLine();
            }
        }
    }
    private static double lerDoubleComTratamento(String mensagem) {
        while (true) {
            try {
                System.out.print(mensagem);
                double valor = scanner.nextDouble();
                scanner.nextLine();
                return valor;
            } catch (InputMismatchException e) {
                System.out.println("Erro: Entrada inválida. Por favor, use vírgula para decimais. Tente novamente.");
                scanner.nextLine();
            }
        }
    }

    private static Date lerDataFuturaComTratamento(String mensagem) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);

        while (true) {
            try {
                System.out.print(mensagem);
                String dataStr = scanner.nextLine();
                Date data = sdf.parse(dataStr);

                long hojeSemHoras = sdf.parse(sdf.format(new Date())).getTime();
                if (data.getTime() < hojeSemHoras) {
                    System.out.println("Erro: A data não pode ser anterior à data de hoje. Tente novamente.");
                    continue;
                }
                return data;

            } catch (ParseException e) {
                System.out.println("Erro: Formato de data inválido. Use o formato dd/MM/yyyy. Tente novamente.");
            }
        }
    }

    private static Fornecedor escolherFornecedor() {
        System.out.println("\n--- Escolha um core.Fornecedor ---");
        meuEstoque.imprimirListaFornecedores();

        while (true) {
            System.out.print("Digite o CNPJ/CPF do fornecedor desejado: ");
            String cnpjCpf = scanner.nextLine();
            Fornecedor fornecedor = meuEstoque.buscarFornecedorPorCnpjCpf(cnpjCpf);
            if (fornecedor != null) {
                return fornecedor;
            } else {
                System.out.println("Erro: core.Fornecedor não encontrado. Tente novamente.");
            }
        }
    }

    private static void cadastrarNovoProduto() {
        System.out.println("\n--- CADASTRO DE NOVO PRODUTO ---");
        int tipo = 0;
        while (tipo < 1 || tipo > 3) {
            tipo = lerInteiroComTratamento("Escolha o tipo:\n1-Cosmético\n2-Eletrônico\n3-Perecível\nEscolha o tipo: ");
            if (tipo < 1 || tipo > 3) {
                System.out.println("Erro: Opção de tipo inválida. Tente novamente.");
            }
        }
        int codigo;
        while (true) {
            codigo = lerInteiroComTratamento("Digite o código do produto: ");
            if (meuEstoque.buscarProdutoPorCodigo(codigo) == null) {
                break;
            } else {
                System.out.println("Erro: Já existe um produto com este código. Tente novamente.");
            }
        }

        System.out.print("Digite a descrição: ");
        String descricao = scanner.nextLine();

        System.out.print("Digite a categoria: ");
        String categoria = scanner.nextLine();

        int qntd = lerInteiroComTratamento("Digite a quantidade inicial: ");
        double valorVenda = lerDoubleComTratamento("Digite o valor de venda (ex: 25,50): ");
        double percentualLucro = lerDoubleComTratamento("Digite o PERCENTUAL de lucro (ex: 20 para 20%): ");

        Fornecedor fornecedorAssociado = escolherFornecedor();
        Produto novoProduto = null;

        switch (tipo) {
            case 1:
                System.out.print("Digite o fabricante: ");
                String fabricante = scanner.nextLine();
                Date validadeCosmetico = lerDataFuturaComTratamento("Digite a data de validade (dd/MM/yyyy): ");
                novoProduto = new Cosmetico(codigo, descricao, categoria, qntd, valorVenda, percentualLucro, fornecedorAssociado, validadeCosmetico, fabricante);
                break;
            case 2:
                int mesesGarantia = 0;
                while (mesesGarantia <= 0) {
                    mesesGarantia = lerInteiroComTratamento("Digite o prazo da garantia em MESES (ex: 12): ");
                    if (mesesGarantia <= 0) {
                        System.out.println("Erro: O prazo de garantia deve ser um número positivo.");
                    }
                }
                novoProduto = new Eletronico(codigo, descricao, categoria, qntd, valorVenda, percentualLucro, fornecedorAssociado, mesesGarantia);
                break;
            case 3:
                Date validadePerecivel = lerDataFuturaComTratamento("Digite a data de validade (dd/MM/yyyy): ");
                novoProduto = new ProdutoPerecivel(codigo, descricao, categoria, qntd, valorVenda, percentualLucro, fornecedorAssociado, validadePerecivel);
                break;
        }
        meuEstoque.cadastrarProduto(novoProduto);
    }
}