package dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Conexão com fallback automático de porta:
 *   1ª tentativa → porta 5432 direta (WiFi pessoal / sem firewall)
 *   2ª tentativa → porta 6543 pooler  (redes escolares que bloqueiam 5432)
 *
 * Variáveis de ambiente substituem os padrões:
 *   DB_URL, DB_USER, DB_PASS
 */
public class ConexaoBD {

    // ── Conexão direta (porta 5432) ─────────────────────────────────────────
    private static final String DIRECT_URL  =
            "jdbc:postgresql://db.iabajqkkodldjwcgvpiz.supabase.co:5432/postgres?sslmode=require";
    private static final String DIRECT_USER = "postgres";

    // ── Pooler Supabase (porta 6543 — passa em firewalls escolares) ─────────
    private static final String POOLER_URL  =
            "jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:6543/postgres?sslmode=require";
    private static final String POOLER_USER = "postgres.iabajqkkodldjwcgvpiz";

    // ── Senha (mesma para ambas) ─────────────────────────────────────────────
    private static final String DEFAULT_PASS = "2r3kZg-KFJ]-uB;";

    private static final HikariDataSource DATA_SOURCE = criarDataSource();

    private static HikariDataSource criarDataSource() {
        // Se o usuário definiu DB_URL, usa sem tentar fallback
        String envUrl  = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASS");

        if (envUrl != null && !envUrl.isBlank()) {
            System.out.println("⚙ BD: usando DB_URL de variável de ambiente.");
            return buildPool(envUrl,
                    envUser  != null ? envUser  : DIRECT_USER,
                    envPass  != null ? envPass  : DEFAULT_PASS);
        }

        String pass = envPass != null && !envPass.isBlank() ? envPass : DEFAULT_PASS;

        // 1ª tentativa: porta 5432 (rápida, sem pooler)
        if (portaAcessivel("db.iabajqkkodldjwcgvpiz.supabase.co", 5432, 3_000)) {
            System.out.println("⚙ BD: porta 5432 acessível — usando conexão direta.");
            return buildPool(DIRECT_URL, DIRECT_USER, pass);
        }

        // 2ª tentativa: porta 6543 (pooler, compatível com firewalls escolares)
        System.out.println("⚙ BD: porta 5432 bloqueada — tentando pooler 6543.");
        return buildPool(POOLER_URL, POOLER_USER, pass);
    }

    /** Testa se uma porta TCP está acessível antes de criar o pool. */
    private static boolean portaAcessivel(String host, int porta, int timeoutMs) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(host, porta), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static HikariDataSource buildPool(String url, String user, String pass) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(15_000);
        cfg.setIdleTimeout(30_000);
        cfg.setMaxLifetime(600_000);
        cfg.setKeepaliveTime(30_000);
        cfg.setConnectionTestQuery("SELECT 1");
        cfg.setPoolName("OficinaAdminPool");

        cfg.addDataSourceProperty("ssl",           "true");
        cfg.addDataSourceProperty("sslmode",       "require");
        cfg.addDataSourceProperty("socketTimeout", "30");

        return new HikariDataSource(cfg);
    }

    public static Connection conectar() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    public static void desconectar(Connection c) {
        if (c != null) try { c.close(); } catch (SQLException ignored) {}
    }
}