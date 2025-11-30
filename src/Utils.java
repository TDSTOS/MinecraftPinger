import java.io.*;
import java.util.*;

public class Utils {

    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String extractJsonField(String json, String fieldName) {
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

    public static Map<String, String> loadEnvFile(String filePath) {
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

    public static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public static String listToJson(List<String> list) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        json.append("]");
        return json.toString();
    }

    public static String mapToJson(Map<String, String> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(escapeJson(entry.getKey())).append("\":");
            json.append("\"").append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String getQueryParam(String query, String param) {
        if (query == null) return null;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(param)) {
                return keyValue[1];
            }
        }
        return null;
    }

    public static void printSeparator(int length, char ch) {
        System.out.println(String.valueOf(ch).repeat(length));
    }

    public static void printHeader(String title) {
        printSeparator(60, '=');
        System.out.println("  " + title);
        printSeparator(60, '=');
    }

    public static String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    public static boolean deleteFileIfExists(File file) {
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    public static void ensureDirectoryExists(File directory) throws IOException {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
            }
        }
    }
}
