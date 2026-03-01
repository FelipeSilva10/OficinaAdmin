package dao;

import core.Professor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProfessorDAO {

    public boolean inserir(String authId, String nome) {
        String sql = "INSERT INTO perfis (id, nome, role) VALUES (?::uuid, ?, 'teacher')";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authId);
            stmt.setString(2, nome);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erro ao inserir professor: " + e.getMessage());
            return false;
        }
    }

    // Traz TODOS os professores da base (para o cadastro global)
    public List<Professor> listarTodos() {
        List<Professor> lista = new ArrayList<>();
        String sql = "SELECT id, nome FROM perfis WHERE role = 'teacher' ORDER BY created_at DESC";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Professor(rs.getString("id"), rs.getString("nome")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    // Lista SÓ os professores vinculados a uma escola específica (Para o Dashboard)
    public List<Professor> listarPorEscola(String escolaId) {
        List<Professor> lista = new ArrayList<>();
        String sql = "SELECT p.id, p.nome FROM perfis p " +
                "JOIN escola_professores ep ON p.id = ep.professor_id " +
                "WHERE ep.escola_id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, escolaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Professor(rs.getString("id"), rs.getString("nome")));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public boolean vincularEscola(String professorId, String escolaId) {
        String sql = "INSERT INTO escola_professores (professor_id, escola_id) VALUES (?::uuid, ?::uuid) ON CONFLICT DO NOTHING";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            stmt.setString(2, escolaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean desvincularEscola(String professorId, String escolaId) {
        String sql = "DELETE FROM escola_professores WHERE professor_id = ?::uuid AND escola_id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            stmt.setString(2, escolaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
}