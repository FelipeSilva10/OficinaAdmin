package dao;

import core.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    // Metodo para BUSCAR TODOS os produtos (Select)
    public List<Produto> listarTodos() {
        List<Produto> lista = new ArrayList<>();

        // Mantemos o JOIN para trazer os dados do fornecedor
        String sql = "SELECT p.*, f.nome as f_nome, f.cnpj_cpf as f_cnpj, f.contato as f_contato " +
                "FROM produtos p " +
                "INNER JOIN fornecedores f ON p.fornecedor_id = f.id " +
                "ORDER BY p.codigo";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // 1. Recuperamos o fornecedor
                Fornecedor f = new Fornecedor(
                        rs.getInt("fornecedor_id"),
                        rs.getString("f_nome"),
                        rs.getString("f_cnpj"),
                        rs.getString("f_contato")
                );

                // 2. Recuperamos os dados comuns de Produto
                int codigo = rs.getInt("codigo");
                String descricao = rs.getString("descricao");
                String categoria = rs.getString("categoria");
                int qtd = rs.getInt("quantidade");
                double valor = rs.getDouble("valor_venda");
                double lucro = rs.getDouble("percentual_lucro");
                String tipo = rs.getString("tipo_produto");

                // 3. Decidimos qual filho instanciar baseado no TIPO
                Produto p = null;

                switch (tipo) {
                    case "COSMETICO":
                        java.sql.Date validadeCos = rs.getDate("data_validade");
                        String fabricante = rs.getString("fabricante");
                        p = new Cosmetico(codigo, descricao, categoria, qtd, valor, lucro, f, validadeCos, fabricante);
                        break;

                    case "ELETRONICO":
                        int garantia = rs.getInt("meses_garantia");
                        p = new Eletronico(codigo, descricao, categoria, qtd, valor, lucro, f, garantia);
                        break;

                    case "PERECIVEL":
                        java.sql.Date validadePer = rs.getDate("data_validade");
                        p = new ProdutoPerecivel(codigo, descricao, categoria, qtd, valor, lucro, f, validadePer);
                        break;
                }

                if (p != null) {
                    lista.add(p);
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro ao listar produtos: " + e.getMessage());
            e.printStackTrace();
        }

        return lista;
    }

    // Metodo para SALVAR um novo produto (Insert)
    public void salvar(Produto p) throws SQLException {
        String sql = "INSERT INTO produtos (codigo, descricao, categoria, quantidade, valor_venda, percentual_lucro, fornecedor_id, tipo_produto, data_validade, fabricante, meses_garantia) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 1. Dados Comuns
            stmt.setInt(1, p.getCodigo());
            stmt.setString(2, p.getDescricao());
            // Se sua classe Produto não tiver getCategoria(), use uma string fixa ou crie o getter
            stmt.setString(3, "Geral"); // Ajuste se tiver p.getCategoria()
            stmt.setInt(4, p.getQntdDisp());
            stmt.setDouble(5, p.getValorUnitVenda());
            stmt.setDouble(6, p.getPercentualLucro());
            stmt.setInt(7, p.getFornecedor().getId()); // <--- Aqui usamos o ID novo!

            // 2. Dados Específicos (Polimorfismo no SQL)
            if (p instanceof Cosmetico c) {
                stmt.setString(8, "COSMETICO");
                stmt.setDate(9, new java.sql.Date(c.getDataValidade().getTime()));
                stmt.setString(10, c.getFabricante());
                stmt.setNull(11, java.sql.Types.INTEGER); // Garantia é null
            }
            else if (p instanceof Eletronico e) {
                stmt.setString(8, "ELETRONICO");
                stmt.setNull(9, java.sql.Types.DATE);     // Validade é null
                stmt.setNull(10, java.sql.Types.VARCHAR); // Fabricante é null
                stmt.setInt(11, e.getDataGarantia() != null ? 12 : 0); // Simplificação: Salvando meses aproximados
            }
            else if (p instanceof ProdutoPerecivel pp) {
                stmt.setString(8, "PERECIVEL");
                stmt.setDate(9, new java.sql.Date(pp.getDataValidade().getTime()));
                stmt.setNull(10, java.sql.Types.VARCHAR);
                stmt.setNull(11, java.sql.Types.INTEGER);
            }

            stmt.executeUpdate();
            System.out.println("Produto salvo com sucesso no banco!");
        }
    }
    // Metodo para EXCLUIR um produto do banco
    public void excluir(int codigo) throws SQLException {
        String sql = "DELETE FROM produtos WHERE codigo = ?";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, codigo);
            stmt.executeUpdate();
            System.out.println("Produto excluído com sucesso do banco!");
        }
    }
}