package dao;

import core.Turma;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TurmaDAO {

    // Método para SALVAR no Supabase
    public boolean inserir(Turma turma) {
        String sql = "INSERT INTO turmas (escola_id, nome, ano_letivo) VALUES (?::uuid, ?, ?)";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, turma.getEscolaId()); // O cast ::uuid resolve incompatibilidades do Java com o Postgres
            stmt.setString(2, turma.getNome());
            stmt.setString(3, turma.getAnoLetivo());

            int linhasAfetadas = stmt.executeUpdate();
            return linhasAfetadas > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erro ao inserir turma: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(String id) {
        String sql = "DELETE FROM turmas WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erro ao excluir turma: " + e.getMessage());
            return false;
        }
    }

    public List<Turma> listarPorEscola(String escolaId) {
        List<Turma> turmas = new ArrayList<>();
        String sql = "SELECT * FROM turmas WHERE escola_id = ?::uuid ORDER BY created_at DESC";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, escolaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Turma turma = new Turma(
                            rs.getString("id"),
                            rs.getString("escola_id"),
                            rs.getString("nome"),
                            rs.getString("ano_letivo")
                    );
                    turmas.add(turma);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erro ao listar turmas: " + e.getMessage());
        }
        return turmas;
    }
}