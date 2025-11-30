import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SupabaseClient {
    private String supabaseUrl;
    private String supabaseKey;

    public SupabaseClient(String supabaseUrl, String supabaseKey) {
        this.supabaseUrl = supabaseUrl;
        this.supabaseKey = supabaseKey;
    }

    public String executeQuery(String query) throws IOException {
        URL url = new URL(supabaseUrl + "/rest/v1/rpc/execute_sql");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("apikey", supabaseKey);
        conn.setRequestProperty("Authorization", "Bearer " + supabaseKey);
        conn.setDoOutput(true);

        String jsonPayload = "{\"query\":\"" + escapeJson(query) + "\"}";

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            return readResponse(conn.getInputStream());
        } else {
            String error = readResponse(conn.getErrorStream());
            throw new IOException("Supabase request failed: " + responseCode + " - " + error);
        }
    }

    public String insert(String table, String jsonData) throws IOException {
        URL url = new URL(supabaseUrl + "/rest/v1/" + table);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("apikey", supabaseKey);
        conn.setRequestProperty("Authorization", "Bearer " + supabaseKey);
        conn.setRequestProperty("Prefer", "return=representation");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            return readResponse(conn.getInputStream());
        } else {
            String error = readResponse(conn.getErrorStream());
            throw new IOException("Supabase insert failed: " + responseCode + " - " + error);
        }
    }

    public String select(String table, String filter) throws IOException {
        String urlStr = supabaseUrl + "/rest/v1/" + table;
        if (filter != null && !filter.isEmpty()) {
            urlStr += "?" + filter;
        }

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", supabaseKey);
        conn.setRequestProperty("Authorization", "Bearer " + supabaseKey);

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            return readResponse(conn.getInputStream());
        } else {
            String error = readResponse(conn.getErrorStream());
            throw new IOException("Supabase select failed: " + responseCode + " - " + error);
        }
    }

    private String readResponse(InputStream is) throws IOException {
        if (is == null) return "";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
