package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoBD {

    private static final String URL = "jdbc:postgresql://db.iabajqkkodldjwcgvpiz.supabase.co:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASS = "2r3kZg-KFJ]-uB;";

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            System.err.println("❌ Erro ao conectar com o Supabase: " + e.getMessage());
            return null;
        }
    }

    public static void desconectar(Connection conexao) {
        if (conexao != null) {
            try {
                conexao.close();
            } catch (SQLException e) {
                System.err.println("Erro ao fechar a conexão: " + e.getMessage());
            }
        }
    }
}