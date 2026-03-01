package dao;

import core.Escola;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EscolasDAO {

    // Metodo para SALVAR no Supabase
    public boolean inserir(Escola escola) {
        // O id e o created_at são gerados automaticamente pelo PostgreSQL!
        String sql = "INSERT INTO escolas (nome, status) VALUES (?, ?)";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, escola.getNome());
            stmt.setString(2, escola.getStatus());

            int linhasAfetadas = stmt.executeUpdate();
            return linhasAfetadas > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erro ao inserir escola: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(String id) {
        String sql = "DELETE FROM escolas WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erro ao excluir escola: " + e.getMessage());
            return false;
        }
    }

    public List<Escola> listarTodas() {
        List<Escola> escolas = new ArrayList<>();
        String sql = "SELECT * FROM escolas ORDER BY created_at DESC";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Escola escola = new Escola(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getString("status")
                );
                escolas.add(escola);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erro ao listar escolas: " + e.getMessage());
        }
        return escolas;
    }
}