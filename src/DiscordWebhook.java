import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

public record DiscordWebhook(String webhookUrl) {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }

    public void sendPlayerOnlineNotification(String playerName, String serverName) {
        if (!isEnabled()) return;

        try {
            String time = TIME_FORMAT.format(new Date());
            String message = String.format(
                    "{\"embeds\":[{\"title\":\"Player Online\",\"description\":\"**%s** is now online on **%s**\",\"color\":5763719,\"timestamp\":\"%s\",\"footer\":{\"text\":\"Minecraft Player Checker\"}}]}",
                    escapeJson(playerName),
                    escapeJson(serverName),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date.from(Instant.now(Clock.systemUTC())))
            );

            sendWebhook(message);
        } catch (Exception e) {
            System.err.println("Failed to send Discord notification: " + e.getMessage());
        }
    }

    public void sendPlayerOfflineNotification(String playerName, String serverName) {
        if (!isEnabled()) return;

        try {
            String message = String.format(
                    "{\"embeds\":[{\"title\":\"Player Offline\",\"description\":\"**%s** is now offline on **%s**\",\"color\":15158332,\"timestamp\":\"%s\",\"footer\":{\"text\":\"Minecraft Player Checker\"}}]}",
                    escapeJson(playerName),
                    escapeJson(serverName),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date())
            );

            sendWebhook(message);
        } catch (Exception e) {
            System.err.println("Failed to send Discord notification: " + e.getMessage());
        }
    }

    public void sendServerOutageNotification(String serverName, String error) {
        if (!isEnabled()) return;

        try {
            String message = String.format(
                    "{\"embeds\":[{\"title\":\"Server Outage\",\"description\":\"**%s** is currently unreachable.\\n\\nError: %s\",\"color\":15105570,\"timestamp\":\"%s\",\"footer\":{\"text\":\"Minecraft Player Checker\"}}]}",
                    escapeJson(serverName),
                    escapeJson(error),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date())
            );

            sendWebhook(message);
        } catch (Exception e) {
            System.err.println("Failed to send Discord notification: " + e.getMessage());
        }
    }

    public void sendServerOnlineNotification(String serverName) {
        if (!isEnabled()) return;

        try {
            String message = String.format(
                    "{\"embeds\":[{\"title\":\"Server Online\",\"description\":\"**%s** is now online again.\",\"color\":5763719,\"timestamp\":\"%s\",\"footer\":{\"text\":\"Minecraft Player Checker\"}}]}",
                    escapeJson(serverName),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date())
            );

            sendWebhook(message);
        } catch (Exception e) {
            System.err.println("Failed to send Discord notification: " + e.getMessage());
        }
    }

    public void sendDailySummary(String summary) {
        if (!isEnabled()) return;

        try {
            String message = String.format(
                    "{\"embeds\":[{\"title\":\"Daily Summary\",\"description\":\"%s\",\"color\":3447003,\"timestamp\":\"%s\",\"footer\":{\"text\":\"Minecraft Player Checker\"}}]}",
                    escapeJson(summary),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date())
            );

            sendWebhook(message);
        } catch (Exception e) {
            System.err.println("Failed to send Discord notification: " + e.getMessage());
        }
    }

    private void sendWebhook(String jsonPayload) throws IOException {
        URL url = URI.create(webhookUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("Discord webhook failed with code: " + responseCode);
        }
    }

    private String escapeJson(String s) {
        return Utils.escapeJson(s);
    }
}
