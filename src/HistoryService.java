import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public class HistoryService {
    private SupabaseClient supabase;
    private boolean enabled;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public HistoryService(boolean enabled) {
        this.enabled = enabled;

        if (enabled) {
            try {
                String supabaseUrl = System.getenv("VITE_SUPABASE_URL");
                String supabaseKey = System.getenv("VITE_SUPABASE_ANON_KEY");

                if (supabaseUrl == null || supabaseKey == null) {
                    Map<String, String> env = loadEnvFile(".env");
                    supabaseUrl = env.get("VITE_SUPABASE_URL");
                    supabaseKey = env.get("VITE_SUPABASE_ANON_KEY");
                }

                if (supabaseUrl != null && !supabaseUrl.isEmpty() &&
                    supabaseKey != null && !supabaseKey.isEmpty()) {
                    this.supabase = new SupabaseClient(supabaseUrl, supabaseKey);

                    try {
                        initializeDatabase();
                        System.out.println("History service connected successfully.");
                    } catch (Exception dbError) {
                        System.err.println("Warning: Could not initialize Supabase database.");
                        System.err.println("Reason: " + dbError.getMessage());
                        System.err.println("History tracking will be disabled.");
                        this.enabled = false;
                        this.supabase = null;
                    }
                } else {
                    this.enabled = false;
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to initialize history service: " + e.getMessage());
                this.enabled = false;
                this.supabase = null;
            }
        }
    }

    private Map<String, String> loadEnvFile(String filePath) {
        Map<String, String> env = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    env.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not read .env file: " + e.getMessage());
        }
        return env;
    }

    private void initializeDatabase() {
        try {
            supabase.select("player_history", "limit=1");
        } catch (Exception e) {
            throw new RuntimeException("""
                    Cannot connect to Supabase or table 'player_history' does not exist. \
                    Please create the table manually in Supabase dashboard:
                    CREATE TABLE player_history (
                      id SERIAL PRIMARY KEY,
                      player_name TEXT NOT NULL,
                      server_name TEXT NOT NULL,
                      status TEXT NOT NULL,
                      timestamp TIMESTAMPTZ DEFAULT NOW(),
                      online_count INTEGER,
                      query_data TEXT
                    );""", e);
        }
    }

    public void recordPlayerStatus(String playerName, String serverName, boolean online, int onlineCount) {
        if (!enabled || supabase == null) {
            return;
        }

        try {
            String status = online ? "online" : "offline";
            String timestamp = DATE_FORMAT.format(Date.from(Instant.now( Clock.systemUTC() )));

            String jsonData = String.format(
                "{\"player_name\":\"%s\",\"server_name\":\"%s\",\"status\":\"%s\",\"online_count\":%d,\"timestamp\":\"%s\"}",
                escapeJson(playerName),
                escapeJson(serverName),
                status,
                onlineCount,
                timestamp
            );

            supabase.insert("player_history", jsonData);
        } catch (Exception e) {
            System.err.println("Warning: Failed to record history: " + e.getMessage());
        }
    }

    public List<HistoryEntry> getPlayerHistory(String playerName, int days) {
        List<HistoryEntry> history = new ArrayList<>();

        if (!enabled || supabase == null) {
            return history;
        }

        try {
            String filter = String.format(
                "player_name=eq.%s&timestamp=gte.now()-interval'%d days'&order=timestamp.desc&limit=1000",
                playerName, days
            );

            String response = supabase.select("player_history", filter);
            history = parseHistoryResponse(response);
        } catch (Exception e) {
            System.err.println("Warning: Failed to fetch history: " + e.getMessage());
        }

        return history;
    }

    public Map<String, Integer> getDailySummary(String playerName, int days) {
        Map<String, Integer> summary = new LinkedHashMap<>();

        if (!enabled || supabase == null) {
            return summary;
        }

        try {
            List<HistoryEntry> history = getPlayerHistory(playerName, days);

            for (HistoryEntry entry : history) {
                String date = entry.getTimestamp().substring(0, 10);
                if ("online".equals(entry.getStatus())) {
                    summary.put(date, summary.getOrDefault(date, 0) + 1);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to generate summary: " + e.getMessage());
        }

        return summary;
    }

    private List<HistoryEntry> parseHistoryResponse(String json) {
        List<HistoryEntry> entries = new ArrayList<>();

        if (json == null || json.equals("[]")) {
            return entries;
        }

        String[] records = json.substring(1, json.length() - 1).split("\\},\\{");

        for (String record : records) {
            try {
                HistoryEntry entry = new HistoryEntry();

                String playerName = extractJsonField(record, "player_name");
                String serverName = extractJsonField(record, "server_name");
                String status = extractJsonField(record, "status");
                String timestamp = extractJsonField(record, "timestamp");
                String onlineCountStr = extractJsonField(record, "online_count");

                entry.setPlayerName(playerName);
                entry.setServerName(serverName);
                entry.setStatus(status);
                entry.setTimestamp(timestamp);

                if (onlineCountStr != null && !onlineCountStr.equals("null")) {
                    try {
                        entry.setOnlineCount(Integer.parseInt(onlineCountStr));
                    } catch (NumberFormatException e) {
                        entry.setOnlineCount(0);
                    }
                }

                entries.add(entry);
            } catch (Exception e) {
                System.err.println("Warning: Failed to parse history entry: " + e.getMessage());
            }
        }

        return entries;
    }

    private String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\":";
        int start = json.indexOf(pattern);

        if (start == -1) {
            return null;
        }

        start += pattern.length();

        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            if (end == -1) return null;
            return json.substring(start, end);
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
            return json.substring(start, end).trim();
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    public boolean isEnabled() {
        return enabled;
    }
}
