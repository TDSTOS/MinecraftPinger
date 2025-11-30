import java.io.*;
import java.util.*;

public class ChecklistProcessor {
    private MultiServerChecker serverChecker;
    private ConfigLoader config;

    public ChecklistProcessor(MultiServerChecker serverChecker, ConfigLoader config) {
        this.serverChecker = serverChecker;
        this.config = config;
    }

    public void processChecklist(String filePath, String serverName) {
        List<String> playerNames = loadPlayerNames(filePath);

        if (playerNames.isEmpty()) {
            System.out.println("No player names found in file: " + filePath);
            return;
        }

        System.out.println("Processing checklist with " + playerNames.size() + " players...");
        System.out.println();

        Map<String, Map<ServerConfig, PlayerCheckResult>> allResults = new LinkedHashMap<>();

        for (String playerName : playerNames) {
            if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("all")) {
                ServerConfig server = config.getServerByName(serverName);
                if (server == null) {
                    System.err.println("Server not found: " + serverName);
                    return;
                }
                Map<ServerConfig, PlayerCheckResult> results = new HashMap<>();
                results.put(server, serverChecker.checkPlayerOnServer(playerName, server));
                allResults.put(playerName, results);
            } else {
                Map<ServerConfig, PlayerCheckResult> results = serverChecker.checkPlayerOnAllServers(playerName);
                allResults.put(playerName, results);
            }
        }

        printResultsTable(allResults);
    }

    private List<String> loadPlayerNames(String filePath) {
        List<String> names = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    names.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading checklist file: " + e.getMessage());
        }

        return names;
    }

    private void printResultsTable(Map<String, Map<ServerConfig, PlayerCheckResult>> allResults) {
        if (allResults.isEmpty()) {
            System.out.println("No results to display.");
            return;
        }

        Set<ServerConfig> allServers = new LinkedHashSet<>();
        for (Map<ServerConfig, PlayerCheckResult> results : allResults.values()) {
            allServers.addAll(results.keySet());
        }

        int maxPlayerNameLength = 0;
        for (String playerName : allResults.keySet()) {
            maxPlayerNameLength = Math.max(maxPlayerNameLength, playerName.length());
        }
        maxPlayerNameLength = Math.max(maxPlayerNameLength, 12);

        int serverColumnWidth = 15;

        printTableHeader(maxPlayerNameLength, serverColumnWidth, allServers);
        printTableSeparator(maxPlayerNameLength, serverColumnWidth, allServers.size());

        for (Map.Entry<String, Map<ServerConfig, PlayerCheckResult>> entry : allResults.entrySet()) {
            String playerName = entry.getKey();
            Map<ServerConfig, PlayerCheckResult> results = entry.getValue();

            System.out.print("| ");
            System.out.print(padRight(playerName, maxPlayerNameLength));
            System.out.print(" | ");

            for (ServerConfig server : allServers) {
                PlayerCheckResult result = results.get(server);
                String status;

                if (result == null || !result.isSuccess()) {
                    status = "ERROR";
                } else if (result.isOnline()) {
                    status = result.isUsingQuery() ? "ONLINE (Q)" : "ONLINE";
                } else {
                    status = "OFFLINE";
                }

                System.out.print(padCenter(status, serverColumnWidth));
                System.out.print(" | ");
            }

            System.out.println();
        }

        printTableSeparator(maxPlayerNameLength, serverColumnWidth, allServers.size());
        System.out.println();
        System.out.println("Total players checked: " + allResults.size());
        System.out.println("Note: (Q) indicates Query protocol was used for extended data");
    }

    private void printTableHeader(int playerColumnWidth, int serverColumnWidth, Set<ServerConfig> servers) {
        System.out.print("| ");
        System.out.print(padRight("Player Name", playerColumnWidth));
        System.out.print(" | ");

        for (ServerConfig server : servers) {
            System.out.print(padCenter(server.getName(), serverColumnWidth));
            System.out.print(" | ");
        }

        System.out.println();
    }

    private void printTableSeparator(int playerColumnWidth, int serverColumnWidth, int serverCount) {
        System.out.print("+");
        System.out.print(repeat("-", playerColumnWidth + 2));
        System.out.print("+");

        for (int i = 0; i < serverCount; i++) {
            System.out.print(repeat("-", serverColumnWidth + 2));
            System.out.print("+");
        }

        System.out.println();
    }

    private String padRight(String s, int length) {
        if (s.length() >= length) {
            return s.substring(0, length);
        }
        return s + repeat(" ", length - s.length());
    }

    private String padCenter(String s, int length) {
        if (s.length() >= length) {
            return s.substring(0, length);
        }

        int totalPadding = length - s.length();
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;

        return repeat(" ", leftPadding) + s + repeat(" ", rightPadding);
    }

    private String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
