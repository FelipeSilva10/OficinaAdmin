package core;

public class Fornecedor implements IRelatorio {
        private int id;
        private String nome;
        private String cnpjCpf;
        private String contato;

        public Fornecedor() {}

        public Fornecedor(int id, String nome, String cnpjCpf, String contato) {
            this.id = id;
            this.nome = nome;
            this.cnpjCpf = cnpjCpf;
            this.contato = contato;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getNome() {
            return nome;
        }
        public void setNome(String nome) {
            this.nome = nome;
        }
        public String getCnpjCpf() {
            return cnpjCpf;
        }
        public void setCnpjCpf(String cnpjCpf) {
            this.cnpjCpf = cnpjCpf;
        }
        public String getContato() {
            return contato;
        }
        public void setContato(String contato) {
            this.contato = contato;
        }

        @Override
        public String toString() {
            return this.nome;
        }

        @Override
        public void gerarCabecalho() {
            System.out.println("=========================================");
            System.out.println("        RELATÓRIO DE FORNECEDOR");
            System.out.println("=========================================");
        }

        @Override
        public void gerarCorpo() {
            System.out.println("Nome: " + this.nome);
            System.out.println("CNPJ/CPF: " + this.cnpjCpf);
            System.out.println("Contato: " + this.contato);
        }

        @Override
        public void imprimirRelatorio() {
            gerarCabecalho();
            gerarCorpo();
            System.out.println("=========================================\n");
        }
}
