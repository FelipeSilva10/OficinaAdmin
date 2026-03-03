package dao;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SupabaseAuthDAO {

    private static final String SUPABASE_URL = "https://iabajqkkodldjwcgvpiz.supabase.co";
    private static final String SERVICE_ROLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlhYmFqcWtrb2RsZGp3Y2d2cGl6Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MjE0Mzk2MSwiZXhwIjoyMDg3NzE5OTYxfQ.sgVwsW6fzQIYxoGioZw59S-cgVR76UMxB3KrOi1H_dM";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    // ─── Criar usuário ────────────────────────────────────────────────────────

    public static String criarUsuarioAuth(String email, String password) {
        try {
            String jsonBody = String.format(
                    "{\"email\":\"%s\",\"password\":\"%s\",\"email_confirm\":true}",
                    email, password
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SUPABASE_URL + "/auth/v1/admin/users"))
                    .header("apikey", SERVICE_ROLE_KEY)
                    .header("Authorization", "Bearer " + SERVICE_ROLE_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String body = response.body();
                String tag = "\"id\":\"";
                int start = body.indexOf(tag) + tag.length();
                int end   = body.indexOf("\"", start);
                return body.substring(start, end);
            } else {
                System.err.println("❌ Supabase Auth erro ao criar: " + response.body());
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Erro de rede ao criar usuário: " + e.getMessage());
            return null;
        }
    }

    // ─── Deletar usuário ──────────────────────────────────────────────────────
    // Remove o usuário do Supabase Auth pelo UUID.
    // Deve ser chamado ANTES de excluir da tabela `perfis`.

    public static boolean deletarUsuarioAuth(String uuid) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SUPABASE_URL + "/auth/v1/admin/users/" + uuid))
                    .header("apikey", SERVICE_ROLE_KEY)
                    .header("Authorization", "Bearer " + SERVICE_ROLE_KEY)
                    .DELETE()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            // 200 = deletado, 404 = já não existia (ambos são ok para nós)
            boolean ok = response.statusCode() == 200 || response.statusCode() == 404;
            if (!ok) System.err.println("❌ Supabase Auth erro ao deletar: " + response.body());
            return ok;

        } catch (Exception e) {
            System.err.println("❌ Erro de rede ao deletar usuário: " + e.getMessage());
            return false;
        }
    }
}