package dao;

import core.Fornecedor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FornecedorDAO {

    public List<Fornecedor> listarTodos() {
        List<Fornecedor> lista = new ArrayList<>();
        String sql = "SELECT * FROM fornecedores ORDER BY nome";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Fornecedor f = new Fornecedor();
                f.setId(rs.getInt("id"));
                f.setNome(rs.getString("nome"));
                f.setCnpjCpf(rs.getString("cnpj_cpf"));
                f.setContato(rs.getString("contato"));
                lista.add(f);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public void salvar(Fornecedor f) throws SQLException {
        String sql = "INSERT INTO fornecedores (nome, cnpj_cpf, contato) VALUES (?, ?, ?)";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, f.getNome());
            stmt.setString(2, f.getCnpjCpf());
            stmt.setString(3, f.getContato());

            stmt.executeUpdate();
            System.out.println("Fornecedor salvo com sucesso!");
        }
    }

    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM fornecedores WHERE id = ?";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("Fornecedor excluído com sucesso do banco!");
        }
    }

}