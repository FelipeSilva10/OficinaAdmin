package dao;

import core.Turma;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TurmaDAO {

    public boolean inserir(String escolaId, String nome, String anoLetivo) {
        String sql = "INSERT INTO turmas (escola_id, nome, ano_letivo) VALUES (?::uuid, ?, ?)";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, escolaId);
            stmt.setString(2, nome);
            stmt.setString(3, anoLetivo);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean atualizar(String id, String escolaId, String nome, String anoLetivo) {
        String sql = "UPDATE turmas SET escola_id = ?::uuid, nome = ?, ano_letivo = ? WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, escolaId);
            stmt.setString(2, nome);
            stmt.setString(3, anoLetivo);
            stmt.setString(4, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean excluir(String id) {
        String sql = "DELETE FROM turmas WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean definirProfessor(String turmaId, String professorId) {
        String sql = "UPDATE turmas SET professor_id = ?::uuid WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            stmt.setString(2, turmaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    private static final String BASE_SELECT =
            "SELECT t.*, e.nome as escola_nome, p.nome as prof_nome " +
                    "FROM turmas t " +
                    "JOIN escolas e ON t.escola_id = e.id " +
                    "LEFT JOIN perfis p ON t.professor_id = p.id ";

    public List<Turma> listarTodas() {
        return buscarComFiltro(BASE_SELECT + "ORDER BY t.created_at DESC", null);
    }

    public List<Turma> listarPorEscola(String escolaId) {
        return buscarComFiltro(BASE_SELECT + "WHERE t.escola_id = ?::uuid ORDER BY t.created_at DESC", escolaId);
    }

    public List<Turma> listarPorProfessor(String professorId) {
        return buscarComFiltro(BASE_SELECT + "WHERE t.professor_id = ?::uuid ORDER BY e.nome, t.nome", professorId);
    }

    private List<Turma> buscarComFiltro(String sql, String param) {
        List<Turma> turmas = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (param != null) stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    turmas.add(new Turma(
                            rs.getString("id"),
                            rs.getString("escola_id"),
                            rs.getString("nome"),
                            rs.getString("ano_letivo"),
                            rs.getString("escola_nome"),
                            rs.getString("prof_nome")
                    ));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return turmas;
    }
}