import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public record SupabaseClient(String supabaseUrl, String supabaseKey) {


    public void insert(String table, String jsonData) throws IOException {
        HttpURLConnection conn = getHttpURLConnection(table);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            readResponse(conn.getInputStream());
        } else {
            String error = readResponse(conn.getErrorStream());
            throw new IOException("Supabase insert failed: " + responseCode + " - " + error);
        }
    }

    private HttpURLConnection getHttpURLConnection(String table) throws IOException {
        URL url = URI.create(supabaseUrl + "/rest/v1/" + table).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("apikey", supabaseKey);
        conn.setRequestProperty("Authorization", "Bearer " + supabaseKey);
        conn.setRequestProperty("Prefer", "return=representation");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setDoOutput(true);
        return conn;
    }

    public String select(String table, String filter) throws IOException {
        HttpURLConnection conn = getHttpURLConnection(table, filter);

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            return readResponse(conn.getInputStream());
        } else {
            String error = readResponse(conn.getErrorStream());
            throw new IOException("Supabase select failed: " + responseCode + " - " + error);
        }
    }

    private HttpURLConnection getHttpURLConnection(String table, String filter) throws IOException {
        String urlStr = supabaseUrl + "/rest/v1/" + table;
        if (filter != null && !filter.isEmpty()) {
            urlStr += "?" + filter;
        }

        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", supabaseKey);
        conn.setRequestProperty("Authorization", "Bearer " + supabaseKey);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        return conn;
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
        return Utils.escapeJson(s);
    }
}
