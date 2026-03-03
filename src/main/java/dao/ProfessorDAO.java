package dao;

import core.Professor;
import core.Turma;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProfessorDAO {

    public boolean inserir(String authId, String nome, String email, String senha) {
        String sql = "INSERT INTO perfis (id, nome, email, senha, role) VALUES (?::uuid, ?, ?, ?, 'teacher')";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authId);
            stmt.setString(2, nome);
            stmt.setString(3, email);
            stmt.setString(4, senha);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir professor: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(String id, String nome, String email, String senha) {
        String sql = "UPDATE perfis SET nome = ?, email = ?, senha = ? WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, email);
            stmt.setString(3, senha);
            stmt.setString(4, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar professor: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(String id) {
        boolean authOk = SupabaseAuthDAO.deletarUsuarioAuth(id);
        if (!authOk) {
            System.err.println("Falha ao remover professor do Auth.");
            return false;
        }
        String sql = "DELETE FROM perfis WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir professor: " + e.getMessage());
            return false;
        }
    }

    // ✅ CORRIGIDO: ORDER BY nome em vez de created_at (evita erro se coluna não existir)
    public List<Professor> listarTodos() {
        List<Professor> lista = new ArrayList<>();
        String sql = "SELECT id, nome, email, senha FROM perfis WHERE role = 'teacher' ORDER BY nome ASC";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Professor(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("senha")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar professores: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    public List<Professor> listarPorEscola(String escolaId) {
        List<Professor> lista = new ArrayList<>();
        String sql = "SELECT p.id, p.nome, p.email, p.senha FROM perfis p " +
                "JOIN escola_professores ep ON p.id = ep.professor_id " +
                "WHERE ep.escola_id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, escolaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Professor(rs.getString("id"), rs.getString("nome"),
                            rs.getString("email"), rs.getString("senha")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar professores por escola: " + e.getMessage());
            e.printStackTrace();
        }
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

    public List<Turma> listarPorProfessor(String professorId) {
        return new TurmaDAO().listarPorProfessor(professorId);
    }
}