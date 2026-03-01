package cli;

public class TesteConexao {
    public static void main(String[] args) {
        try {
            var c = dao.ConexaoBD.conectar();
            System.out.println("Banco conectado com sucesso!");
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

