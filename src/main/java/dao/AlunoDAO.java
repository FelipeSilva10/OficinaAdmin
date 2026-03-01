package dao;

import core.Aluno;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlunoDAO {

    public boolean inserir(String authId, String nome, String turmaId) {
        String sql = "INSERT INTO perfis (id, nome, role, turma_id) VALUES (?::uuid, ?, 'student', ?::uuid)";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authId);
            stmt.setString(2, nome);
            stmt.setString(3, turmaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erro ao inserir aluno: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(String id) {
        String sql = "DELETE FROM perfis WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public List<Aluno> listarTodos() {
        return buscar("SELECT p.id, p.nome, p.turma_id, t.nome as turma_nome, e.nome as escola_nome " +
                "FROM perfis p JOIN turmas t ON p.turma_id = t.id " +
                "JOIN escolas e ON t.escola_id = e.id " +
                "WHERE p.role = 'student' ORDER BY p.created_at DESC", null);
    }

    public List<Aluno> listarPorTurma(String turmaId) {
        return buscar("SELECT p.id, p.nome, p.turma_id, t.nome as turma_nome, e.nome as escola_nome " +
                "FROM perfis p JOIN turmas t ON p.turma_id = t.id " +
                "JOIN escolas e ON t.escola_id = e.id " +
                "WHERE p.role = 'student' AND p.turma_id = ?::uuid ORDER BY p.created_at DESC", turmaId);
    }

    private List<Aluno> buscar(String sql, String param) {
        List<Aluno> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (param != null) stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Aluno(rs.getString("id"), rs.getString("nome"), rs.getString("turma_id"),
                            rs.getString("turma_nome"), rs.getString("escola_nome")));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }
}