package dao;

import core.Escola;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EscolasDAO {

    public boolean inserir(Escola escola) {
        String sql = "INSERT INTO escolas (nome, status) VALUES (?, ?)";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, escola.getNome());
            stmt.setString(2, escola.getStatus());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir escola: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(String id, String novoNome) {
        String sql = "UPDATE escolas SET nome = ? WHERE id = ?::uuid";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoNome);
            stmt.setString(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar escola: " + e.getMessage());
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
            System.err.println("Erro ao excluir escola: " + e.getMessage());
            return false;
        }
    }

    public List<Escola> listarTodas() {
        List<Escola> lista = new ArrayList<>();
        String sql = "SELECT id, nome, status FROM escolas ORDER BY nome ASC";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Escola(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar escolas: " + e.getMessage());
        }
        return lista;
    }

    // PATCH: retorna apenas as escolas que têm turmas onde o professor é regente
    public List<Escola> listarPorProfessor(String professorId) {
        List<Escola> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT e.id, e.nome, e.status " +
                "FROM escolas e " +
                "JOIN turmas t ON t.escola_id = e.id " +
                "WHERE t.professor_id = ?::uuid " +
                "ORDER BY e.nome ASC";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, professorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Escola(
                            rs.getString("id"),
                            rs.getString("nome"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar escolas por professor: " + e.getMessage());
        }
        return lista;
    }
}