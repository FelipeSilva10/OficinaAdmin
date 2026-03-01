package dao;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SupabaseAuthDAO {

    private static final String SUPABASE_URL = "https://iabajqkkodldjwcgvpiz.supabase.co";

    // Substitua pela sua chave SECRET (service_role).
    private static final String SERVICE_ROLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlhYmFqcWtrb2RsZGp3Y2d2cGl6Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MjE0Mzk2MSwiZXhwIjoyMDg3NzE5OTYxfQ.sgVwsW6fzQIYxoGioZw59S-cgVR76UMxB3KrOi1H_dM";

    public static String criarUsuarioAuth(String email, String password) {
        try {
            String jsonBody = String.format(
                    "{\"email\":\"%s\",\"password\":\"%s\",\"email_confirm\":true}",
                    email, password
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SUPABASE_URL + "/auth/v1/admin/users"))
                    .header("apikey", SERVICE_ROLE_KEY)
                    .header("Authorization", "Bearer " + SERVICE_ROLE_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String responseBody = response.body();
                String idTag = "\"id\":\"";
                int startIndex = responseBody.indexOf(idTag) + idTag.length();
                int endIndex = responseBody.indexOf("\"", startIndex);

                return responseBody.substring(startIndex, endIndex); // Retorna o UUID novinho!
            } else {
                System.err.println("❌ Erro da API Supabase: " + response.body());
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Erro no Java ao conectar com Supabase API: " + e.getMessage());
            return null;
        }
    }
}