import java.io.IOException;
import java.util.*;

public class ConsoleInterface {
    private PlayerChecker playerChecker;
    private MultiServerChecker multiServerChecker;
    private ConfigLoader config;
    private HistoryService historyService;
    private DiscordWebhook discord;
    private ChecklistProcessor checklistProcessor;
    private UpdateManager updateManager;
    private RealTimeCheckController realTimeController;
    private Scanner scanner;
    private boolean running;

    public ConsoleInterface(PlayerChecker playerChecker, MultiServerChecker multiServerChecker, ConfigLoader config, HistoryService historyService, DiscordWebhook discord, UpdateManager updateManager, RealTimeCheckController realTimeController) {
        this.playerChecker = playerChecker;
        this.multiServerChecker = multiServerChecker;
        this.config = config;
        this.historyService = historyService;
        this.discord = discord;
        this.updateManager = updateManager;
        this.realTimeController = realTimeController;
        this.checklistProcessor = new ChecklistProcessor(multiServerChecker, config);
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    public void start() {
        printWelcome();

        while (running) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            processCommand(input);
        }

        scanner.close();
    }

    private void printWelcome() {
        System.out.println("========================================");
        System.out.println("  Minecraft Player Online Checker v" + UpdateManager.getVersion());
        System.out.println("========================================");
        System.out.println("Commands:");
        System.out.println("  check <playername> [server]    - Check if a player is online");
        System.out.println("  checkall <playername>          - Check player on all servers");
        System.out.println("  checklist <file> [server]      - Check multiple players from file");
        System.out.println("  status [server]                - Show server status");
        System.out.println("  servers                        - List all configured servers");
        System.out.println("  history <playername> [days]    - Show player history");
        System.out.println("  realtime <playername> [server] - Start real-time monitoring");
        System.out.println("  realtime stop                  - Stop real-time monitoring");
        System.out.println("  checkupdates                   - Check for application updates");
        System.out.println("  help                           - Show this help message");
        System.out.println("  exit                           - Exit the program");
        System.out.println("========================================");
    }

    private void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "check":
                if (parts.length < 2) {
                    System.out.println("Error: Please provide a player name.");
                    System.out.println("Usage: check <playername> [server]");
                } else {
                    String[] checkArgs = parts[1].split("\\s+", 2);
                    String playerName = checkArgs[0];
                    String serverName = checkArgs.length > 1 ? checkArgs[1] : null;
                    checkPlayerMultiServer(playerName, serverName);
                }
                break;

            case "checkall":
                if (parts.length < 2) {
                    System.out.println("Error: Please provide a player name.");
                    System.out.println("Usage: checkall <playername>");
                } else {
                    checkPlayerAllServers(parts[1]);
                }
                break;

            case "checklist":
                if (parts.length < 2) {
                    System.out.println("Error: Please provide a file path.");
                    System.out.println("Usage: checklist <file> [server]");
                } else {
                    String[] checklistArgs = parts[1].split("\\s+", 2);
                    String filePath = checklistArgs[0];
                    String serverName = checklistArgs.length > 1 ? checklistArgs[1] : "all";
                    checklistProcessor.processChecklist(filePath, serverName);
                }
                break;

            case "status":
                if (parts.length > 1) {
                    showServerStatus(parts[1]);
                } else {
                    showAllServersStatus();
                }
                break;

            case "servers":
                listServers();
                break;

            case "history":
                if (parts.length < 2) {
                    System.out.println("Error: Please provide a player name.");
                    System.out.println("Usage: history <playername> [days]");
                } else {
                    String[] historyArgs = parts[1].split("\\s+", 2);
                    String playerName = historyArgs[0];
                    int days = 7;
                    if (historyArgs.length > 1) {
                        try {
                            days = Integer.parseInt(historyArgs[1]);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid days value, using default: 7");
                        }
                    }
                    showHistory(playerName, days);
                }
                break;

            case "realtime":
                if (parts.length < 2) {
                    System.out.println("Error: Please provide a player name or 'stop'.");
                    System.out.println("Usage: realtime <playername> [server]");
                    System.out.println("       realtime stop");
                } else {
                    String[] realtimeArgs = parts[1].split("\\s+", 2);
                    String firstArg = realtimeArgs[0];

                    if (firstArg.equalsIgnoreCase("stop")) {
                        realTimeController.stopCliRealTimeCheck();
                    } else {
                        String playerName = firstArg;
                        String serverName = realtimeArgs.length > 1 ? realtimeArgs[1] : null;
                        realTimeController.startCliRealTimeCheck(playerName, serverName);
                    }
                }
                break;

            case "checkupdates":
                checkForUpdates();
                break;

            case "help":
                printWelcome();
                break;

            case "exit":
            case "quit":
                System.out.println("Goodbye!");
                running = false;
                break;

            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Type 'help' for available commands.");
                break;
        }
    }

    private void checkPlayer(String playerName) {
        try {
            System.out.println("Checking player: " + playerName + "...");
            boolean isOnline = playerChecker.isPlayerOnline(playerName);

            if (isOnline) {
                System.out.println("Result: " + playerName + " is ONLINE");
            } else {
                System.out.println("Result: " + playerName + " is OFFLINE");
                int onlineCount = playerChecker.getOnlinePlayerCount();
                if (onlineCount > 0) {
                    System.out.println("Note: " + onlineCount + " player(s) are online, but " + playerName + " is not in the visible sample.");
                }
            }
        } catch (IOException e) {
            handleConnectionError(e);
        }
    }

    private void checkPlayerMultiServer(String playerName, String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            checkPlayer(playerName);
            return;
        }

        ServerConfig server = config.getServerByName(serverName);
        if (server == null) {
            System.out.println("Error: Server '" + serverName + "' not found.");
            System.out.println("Use 'servers' command to list available servers.");
            return;
        }

        System.out.println("Checking player: " + playerName + " on " + server.getName() + "...");
        PlayerCheckResult result = multiServerChecker.checkPlayerOnServer(playerName, server);

        if (!result.isSuccess()) {
            System.out.println("Error: " + result.getErrorMessage());
            return;
        }

        if (result.isOnline()) {
            System.out.println("Result: " + playerName + " is ONLINE on " + server.getName());
            if (result.isUsingQuery()) {
                System.out.println("Protocol: Query (extended data available)");
            }
        } else {
            System.out.println("Result: " + playerName + " is OFFLINE on " + server.getName());
            if (result.getOnlineCount() > 0) {
                System.out.println("Note: " + result.getOnlineCount() + " player(s) are online on this server.");
            }
        }

        if (historyService.isEnabled()) {
            historyService.recordPlayerStatus(playerName, server.getName(), result.isOnline(), result.getOnlineCount());
        }
    }

    private void checkPlayerAllServers(String playerName) {
        System.out.println("Checking player: " + playerName + " on all servers...");
        System.out.println();

        Map<ServerConfig, PlayerCheckResult> results = multiServerChecker.checkPlayerOnAllServers(playerName);

        for (Map.Entry<ServerConfig, PlayerCheckResult> entry : results.entrySet()) {
            ServerConfig server = entry.getKey();
            PlayerCheckResult result = entry.getValue();

            System.out.print("[" + server.getName() + "] ");

            if (!result.isSuccess()) {
                System.out.println("ERROR: " + result.getErrorMessage());
            } else if (result.isOnline()) {
                System.out.println("ONLINE" + (result.isUsingQuery() ? " (Query)" : ""));
            } else {
                System.out.println("OFFLINE");
            }

            if (historyService.isEnabled() && result.isSuccess()) {
                historyService.recordPlayerStatus(playerName, server.getName(), result.isOnline(), result.getOnlineCount());
            }
        }

        System.out.println();
    }

    private void showAllServersStatus() {
        System.out.println("Querying all servers...");
        System.out.println();

        Map<ServerConfig, ServerStatus> statuses = multiServerChecker.getAllServerStatus();

        for (Map.Entry<ServerConfig, ServerStatus> entry : statuses.entrySet()) {
            ServerConfig server = entry.getKey();
            ServerStatus status = entry.getValue();

            System.out.println("Server: " + server.toString());
            if (status.isOnline()) {
                System.out.println("  Status: ONLINE");
                System.out.println("  Players: " + status.getOnlineCount() + "/" + status.getMaxPlayers());
                if (server.isQueryEnabled()) {
                    System.out.println("  Query: Enabled");
                    QueryResponse qr = status.getQueryResponse();
                    if (qr != null && qr.getMap() != null) {
                        System.out.println("  Map: " + qr.getMap());
                    }
                }
            } else {
                System.out.println("  Status: OFFLINE");
                if (status.getErrorMessage() != null) {
                    System.out.println("  Error: " + status.getErrorMessage());
                }
            }
            System.out.println();
        }
    }

    private void showServerStatus(String serverName) {
        ServerConfig server = config.getServerByName(serverName);
        if (server == null) {
            System.out.println("Error: Server '" + serverName + "' not found.");
            return;
        }

        System.out.println("Querying " + server.getName() + "...");
        ServerStatus status = multiServerChecker.getServerStatus(server);

        System.out.println("Server: " + server.toString());
        if (status.isOnline()) {
            System.out.println("Status: ONLINE");
            System.out.println("Players: " + status.getOnlineCount() + "/" + status.getMaxPlayers());
            if (server.isQueryEnabled()) {
                System.out.println("Query: Enabled");
                QueryResponse qr = status.getQueryResponse();
                if (qr != null) {
                    if (qr.getMap() != null) System.out.println("Map: " + qr.getMap());
                    if (qr.getVersion() != null) System.out.println("Version: " + qr.getVersion());
                    if (qr.getPlugins() != null) System.out.println("Plugins: " + qr.getPlugins());
                }
            }
        } else {
            System.out.println("Status: OFFLINE");
            if (status.getErrorMessage() != null) {
                System.out.println("Error: " + status.getErrorMessage());
            }
        }
    }

    private void listServers() {
        System.out.println("Configured servers:");
        System.out.println();

        for (ServerConfig server : config.getServers()) {
            System.out.println("  " + server.getName() + " - " + server.getIp() + ":" + server.getPort());
        }

        System.out.println();
        System.out.println("Total: " + config.getServers().size() + " server(s)");
    }

    private void showHistory(String playerName, int days) {
        if (!historyService.isEnabled()) {
            System.out.println("History tracking is not enabled.");
            return;
        }

        System.out.println("Loading history for " + playerName + " (last " + days + " days)...");
        System.out.println();

        List<HistoryEntry> history = historyService.getPlayerHistory(playerName, days);

        if (history.isEmpty()) {
            System.out.println("No history found for " + playerName);
            return;
        }

        System.out.println("Recent activity:");
        int displayCount = Math.min(history.size(), 20);
        for (int i = 0; i < displayCount; i++) {
            HistoryEntry entry = history.get(i);
            System.out.println("  " + entry.getTimestamp() + " - " + entry.getServerName() + ": " + entry.getStatus().toUpperCase());
        }

        if (history.size() > displayCount) {
            System.out.println("  ... and " + (history.size() - displayCount) + " more entries");
        }

        System.out.println();
        System.out.println("Total records: " + history.size());

        Map<String, Integer> summary = historyService.getDailySummary(playerName, days);
        if (!summary.isEmpty()) {
            System.out.println();
            System.out.println("Daily summary (online checks):");
            for (Map.Entry<String, Integer> entry : summary.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " times");
            }
        }
    }

    private void checkForUpdates() {
        if (updateManager == null) {
            System.out.println("Update manager not initialized.");
            return;
        }

        boolean updateAvailable = updateManager.checkForUpdates(false);

        if (updateAvailable) {
            System.out.println("Would you like to download and install the update now? (yes/no)");
            System.out.print("> ");

            String response = scanner.nextLine().trim().toLowerCase();

            if (response.equals("yes") || response.equals("y")) {
                System.out.println();
                boolean success = updateManager.downloadAndInstallUpdate();

                if (!success) {
                    System.out.println();
                    System.out.println("Update failed. You can try again later with the 'checkupdates' command.");
                }
            } else {
                System.out.println("Update canceled. You can check for updates anytime with 'checkupdates'.");
            }
        }
    }

    private void handleConnectionError(IOException e) {
        System.out.println("Connection Error: Unable to reach the Minecraft server.");
        System.out.println("Reason: " + e.getMessage());
        System.out.println();
        System.out.println("Possible causes:");
        System.out.println("  - The server is offline");
        System.out.println("  - The server IP or port is incorrect");
        System.out.println("  - Your network connection is down");
        System.out.println("  - The server firewall is blocking connections");
        System.out.println();
        System.out.println("Please check your config.properties file and try again.");
    }
}
