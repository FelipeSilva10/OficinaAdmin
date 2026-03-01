package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDAO {

    public boolean autenticar(String login, String senha) {
        String sql = "SELECT id FROM backoffice_admins WHERE login = ? AND senha = ?";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            stmt.setString(2, senha);

            try (ResultSet rs = stmt.executeQuery()) {
                // Se o rs.next() for verdadeiro, significa que encontrou o usuário com a senha certa!
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("❌ Erro ao autenticar admin: " + e.getMessage());
            return false;
        }
    }
}