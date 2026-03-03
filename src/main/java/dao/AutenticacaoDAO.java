package dao;

import core.UsuarioSessao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AutenticacaoDAO {

    public UsuarioSessao autenticar(String loginOuEmail, String senha) {
        // 1. Tenta autenticar como Administrador
        String sqlAdmin = "SELECT id, nome FROM backoffice_admins WHERE login = ? AND senha = ?";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlAdmin)) {
            stmt.setString(1, loginOuEmail);
            stmt.setString(2, senha);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UsuarioSessao(rs.getString("id"), rs.getString("nome"), "ADMIN");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar Admin: " + e.getMessage());
        }

        // 2. Tenta autenticar como Professor (Usando email como login)
        String sqlProf = "SELECT id, nome FROM perfis WHERE role = 'teacher' AND email = ? AND senha = ?";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlProf)) {
            stmt.setString(1, loginOuEmail);
            stmt.setString(2, senha);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UsuarioSessao(rs.getString("id"), rs.getString("nome"), "TEACHER");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar Professor: " + e.getMessage());
        }

        // Se chegou aqui, usuário não existe ou senha está incorreta
        return null;
    }
}