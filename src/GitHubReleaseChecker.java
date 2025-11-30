import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GitHubReleaseChecker {
    private final String repositoryOwner;
    private final String repositoryName;
    private final String currentVersion;
    private String latestVersion;
    private String latestDownloadUrl;
    private long lastCheckTime;
    private static final int TIMEOUT = 10000;

    public GitHubReleaseChecker(String repositoryOwner, String repositoryName, String currentVersion) {
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.currentVersion = currentVersion;
        this.lastCheckTime = 0;
    }

    public boolean checkForUpdates() {
        try {
            String apiUrl = String.format("https://api.github.com/repos/%s/%s/releases/latest",
                    repositoryOwner, repositoryName);

            URL url = URI.create( apiUrl ).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("User-Agent", "MinecraftPinger-AutoUpdate");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                String response = readResponse(conn.getInputStream());
                parseReleaseInfo(response);
                lastCheckTime = System.currentTimeMillis();
                return true;
            } else if (responseCode == 404) {
                System.err.println("Update check: Repository or release not found (404)");
                return false;
            } else {
                System.err.println("Update check failed with HTTP " + responseCode);
                return false;
            }

        } catch (Exception e) {
            System.err.println("Failed to check for updates: " + e.getMessage());
            return false;
        }
    }

    private void parseReleaseInfo(String json) {
        latestVersion = extractJsonField(json, "tag_name");

        if (latestVersion != null && latestVersion.startsWith("v")) {
            latestVersion = latestVersion.substring(1);
        }

        String assetsSection = extractJsonSection(json, "assets");
        if (assetsSection != null) {
            latestDownloadUrl = extractJsonField(assetsSection, "browser_download_url");
        }
    }

    public boolean isUpdateAvailable() {
        if (latestVersion == null || currentVersion == null) {
            return false;
        }

        return compareVersions(latestVersion, currentVersion) > 0;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestDownloadUrl() {
        return latestDownloadUrl;
    }

    public long getLastCheckTime() {
        return lastCheckTime;
    }

    private int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (v1 > v2) {
                return 1;
            } else if (v1 < v2) {
                return -1;
            }
        }

        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String extractJsonField(String json, String fieldName) {
        return Utils.extractJsonField(json, fieldName);
    }

    private String extractJsonSection(String json, String sectionName) {
        String pattern = "\"" + sectionName + "\":";
        int start = json.indexOf(pattern);

        if (start == -1) {
            return null;
        }

        start += pattern.length();

        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) {
            start++;
        }

        if (start >= json.length() || json.charAt(start) != '[') {
            return null;
        }

        int bracketCount = 0;
        int end = start;

        while (end < json.length()) {
            if (json.charAt(end) == '[') {
                bracketCount++;
            } else if (json.charAt(end) == ']') {
                bracketCount--;
                if (bracketCount == 0) {
                    return json.substring(start, end + 1);
                }
            }
            end++;
        }

        return null;
    }

    private String readResponse(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
