package dao;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SupabaseAuthDAO {

    // Substitua pela URL base do seu projeto (aquela que tem .supabase.co)
    private static final String SUPABASE_URL = "https://[SEU_PROJETO].supabase.co";

    // Substitua pela sua chave SECRET (service_role).
    private static final String SERVICE_ROLE_KEY = "sua_chave_service_role_aqui";

    /**
     * Cria um usuário no Supabase Auth e retorna o UUID gerado (ou null se der erro).
     */
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