package dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class ConexaoBD {

    private static final String DEFAULT_URL = "jdbc:postgresql://db.iabajqkkodldjwcgvpiz.supabase.co:5432/postgres";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASS = "2r3kZg-KFJ]-uB;";

    private static final HikariDataSource DATA_SOURCE = criarDataSource();

    private static HikariDataSource criarDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getEnvOrDefault("DB_URL", DEFAULT_URL));
        config.setUsername(getEnvOrDefault("DB_USER", DEFAULT_USER));
        config.setPassword(getEnvOrDefault("DB_PASS", DEFAULT_PASS));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(8000);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(600000);
        config.setPoolName("OficinaAdminPool");
        return new HikariDataSource(config);
    }

    private static String getEnvOrDefault(String envName, String defaultValue) {
        String value = System.getenv(envName);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static Connection conectar() throws SQLException {
        return DATA_SOURCE.getConnection();
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
