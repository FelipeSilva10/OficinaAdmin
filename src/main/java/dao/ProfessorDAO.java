package dao;

import core.Professor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProfessorDAO {

    // Insere o perfil após o Auth ter criado a conta
    public boolean inserir(String authId, String escolaId, String nome) {
        String sql = "INSERT INTO perfis (id, escola_id, nome, role) VALUES (?::uuid, ?::uuid, ?, 'teacher')";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, authId);
            stmt.setString(2, escolaId);
            stmt.setString(3, nome);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erro ao inserir perfil de professor: " + e.getMessage());
            return false;
        }
    }

    // Lista os professores juntando com a tabela de escolas para pegar o nome da instituição
    public List<Professor> listarTodos() {
        List<Professor> lista = new ArrayList<>();
        String sql = "SELECT p.id, p.escola_id, p.nome, e.nome as escola_nome " +
                "FROM perfis p " +
                "JOIN escolas e ON p.escola_id = e.id " +
                "WHERE p.role = 'teacher' " +
                "ORDER BY p.created_at DESC";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(new Professor(
                        rs.getString("id"),
                        rs.getString("escola_id"),
                        rs.getString("nome"),
                        rs.getString("escola_nome")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erro ao listar professores: " + e.getMessage());
        }
        return lista;
    }
}