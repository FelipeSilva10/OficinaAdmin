package cli;

import core.Escola;
import dao.EscolasDAO;
import java.util.List;

public class TesteConexao {
    public static void main(String[] args) {
        EscolasDAO dao = new EscolasDAO();

        System.out.println("⏳ A enviar dados para a nuvem...");

        // 1. Cadastrando a nossa primeira escola
        Escola novaEscola = new Escola("Colégio Oficina Code", "ativo");

        if (dao.inserir(novaEscola)) {
            System.out.println("✅ Escola inserida com sucesso no Supabase!");
        } else {
            System.out.println("❌ Falha ao inserir a escola.");
        }

        // 2. Lendo as escolas direto da nuvem
        System.out.println("\n📋 Lista de Escolas no Banco de Dados:");
        List<Escola> escolas = dao.listarTodas();

        for (Escola e : escolas) {
            System.out.println("ID: " + e.getId() + " | Nome: " + e.getNome() + " | Status: " + e.getStatus());
        }
    }
}