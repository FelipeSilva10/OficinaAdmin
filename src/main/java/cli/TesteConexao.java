package cli;

public class TesteConexao {
    public static void main(String[] args) {

        System.out.println("⏳ A tentar criar um professor na nuvem...");

        // Chamando a classe que agora está no pacote correto (dao)
        String novoId = dao.SupabaseAuthDAO.criarUsuarioAuth("professor@oficina.com", "senha12345");

        if (novoId != null) {
            System.out.println("✅ SUCESSO! ID Gerado no Supabase: " + novoId);
        } else {
            System.out.println("❌ Falha ao criar usuário.");
        }
    }
}