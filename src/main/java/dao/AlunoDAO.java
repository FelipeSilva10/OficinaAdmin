package dao;

import core.Aluno;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlunoDAO {

    public boolean inserir(String authId, String nome, String email, String senha, String turmaId) {
        // Usa ON CONFLICT para resolver problemas com Triggers do Supabase
        String sql = "INSERT INTO perfis (id, nome, email, senha, role, turma_id) VALUES (?::uuid, ?, ?, ?, 'student', ?::uuid) " +
                "ON CONFLICT (id) DO UPDATE SET nome = EXCLUDED.nome, email = EXCLUDED.email, senha = EXCLUDED.senha, role = EXCLUDED.role, turma_id = EXCLUDED.turma_id";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authId);
            stmt.setString(2, nome);
            stmt.setString(3, email);
            stmt.setString(4, senha);
            stmt.setString(5, turmaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir aluno: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(String id, String nome, String email, String senha, String turmaId) {
        String sql = "UPDATE perfis SET nome = ?, email = ?, senha = ?, turma_id = ?::uuid WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, email);
            stmt.setString(3, senha);
            stmt.setString(4, turmaId);
            stmt.setString(5, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar aluno: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(String id) {
        boolean authOk = SupabaseAuthDAO.deletarUsuarioAuth(id);
        if (!authOk) {
            System.err.println("Falha ao remover do Auth, abortando exclusao do perfil.");
            return false;
        }
        String sql = "DELETE FROM perfis WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate(); // Não importa se retornar 0 (já deletado por cascade no supabase)
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir aluno da tabela: " + e.getMessage());
            return false;
        }
    }

    public List<Aluno> listarTodos() {
        return buscar(
                "SELECT p.id, p.nome, p.email, p.senha, p.turma_id, " +
                        "COALESCE(t.nome, 'Sem Turma') as turma_nome, " +
                        "COALESCE(e.nome, 'Sem Escola') as escola_nome " +
                        "FROM perfis p " +
                        "LEFT JOIN turmas t ON p.turma_id = t.id " +
                        "LEFT JOIN escolas e ON t.escola_id = e.id " +
                        "WHERE p.role = 'student' ORDER BY p.nome ASC",
                null);
    }

    public List<Aluno> listarPorTurma(String turmaId) {
        return buscar(
                "SELECT p.id, p.nome, p.email, p.senha, p.turma_id, " +
                        "COALESCE(t.nome, 'Sem Turma') as turma_nome, " +
                        "COALESCE(e.nome, 'Sem Escola') as escola_nome " +
                        "FROM perfis p " +
                        "LEFT JOIN turmas t ON p.turma_id = t.id " +
                        "LEFT JOIN escolas e ON t.escola_id = e.id " +
                        "WHERE p.role = 'student' AND p.turma_id = ?::uuid ORDER BY p.nome ASC",
                turmaId);
    }

    private List<Aluno> buscar(String sql, String param) {
        List<Aluno> lista = new ArrayList<>();
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (param != null) stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Aluno(
                            rs.getString("id"),
                            rs.getString("nome"),
                            rs.getString("email"),
                            rs.getString("senha"),
                            rs.getString("turma_id"),
                            rs.getString("turma_nome"),
                            rs.getString("escola_nome")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar alunos: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }
}