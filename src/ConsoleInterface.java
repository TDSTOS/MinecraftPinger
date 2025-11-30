import java.io.IOException;
import java.util.*;

public class ConsoleInterface {
    private final PlayerChecker playerChecker;
    private final MultiServerChecker multiServerChecker;
    private final ConfigLoader config;
    private final HistoryService historyService;
    private final DiscordWebhook discord;
    private final ChecklistProcessor checklistProcessor;
    private final UpdateManager updateManager;
    private final RealTimeCheckController realTimeController;
    private final MultiPlayerRealTimeController multiRealTime;
    private final PlayerAnalytics analytics;
    private final LiveTimeline timeline;
    private final ServerPerformanceMonitor perfMonitor;
    private final Scanner scanner;
    private boolean running;

    public ConsoleInterface(PlayerChecker playerChecker, MultiServerChecker multiServerChecker, ConfigLoader config, HistoryService historyService, DiscordWebhook discord, UpdateManager updateManager, RealTimeCheckController realTimeController, MultiPlayerRealTimeController multiRealTime, PlayerAnalytics analytics, LiveTimeline timeline, ServerPerformanceMonitor perfMonitor) {
        this.playerChecker = playerChecker;
        this.multiServerChecker = multiServerChecker;
        this.config = config;
        this.historyService = historyService;
        this.discord = discord;
        this.updateManager = updateManager;
        this.realTimeController = realTimeController;
        this.multiRealTime = multiRealTime;
        this.analytics = analytics;
        this.timeline = timeline;
        this.perfMonitor = perfMonitor;
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
        System.out.println("  analytics <playername>         - Show player analytics & insights");
        System.out.println("  realtime <playername> [server] - Start real-time monitoring");
        System.out.println("  realtime stop                  - Stop real-time monitoring");
        System.out.println("  rtadd <player> [server]        - Add player to multi-player monitoring");
        System.out.println("  rtremove <player>              - Remove player from monitoring");
        System.out.println("  rtlist                         - List monitored players");
        System.out.println("  rtbackground <player(s)>       - Start background monitoring");
        System.out.println("  rtstop                         - Stop background monitoring");
        System.out.println("  timeline [count]               - Show recent events (default 20)");
        System.out.println("  perfstats [server]             - Show performance statistics");
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

            case "analytics":
                if (parts.length < 2) {
                    System.out.println("Error: Please provide a player name.");
                    System.out.println("Usage: analytics <playername>");
                } else {
                    showAnalytics(parts[1]);
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
                        String serverName = realtimeArgs.length > 1 ? realtimeArgs[1] : null;
                        realTimeController.startCliRealTimeCheck(firstArg, serverName);
                    }
                }
                break;

            case "rtadd":
                if (parts.length < 2) {
                    System.out.println("Error: Please provide a player name.");
                    System.out.println("Usage: rtadd <playername> [server]");
                } else {
                    String[] args = parts[1].split("\\s+", 2);
                    String playerName = args[0];
                    String serverName = args.length > 1 ? args[1] : null;
                    multiRealTime.addCliPlayer(playerName, serverName);
                }
                break;

            case "rtremove":
                if (parts.length < 2) {
                    System.out.println("Error: Please provide a player name.");
                    System.out.println("Usage: rtremove <playername>");
                } else {
                    multiRealTime.removeCliPlayer(parts[1]);
                }
                break;

            case "rtlist":
                listRealTimePlayers();
                break;

            case "rtbackground":
                if (parts.length < 2) {
                    System.out.println("Error: Please provide player names.");
                    System.out.println("Usage: rtbackground <player1> [player2] ...");
                } else {
                    String[] playerNames = parts[1].split("\\s+");
                    Set<String> players = new HashSet<>(Arrays.asList(playerNames));
                    multiRealTime.startBackgroundMonitoring(players);
                    System.out.println("Background monitoring started for " + players.size() + " player(s)");
                }
                break;

            case "rtstop":
                if (multiRealTime.isBackgroundActive()) {
                    multiRealTime.stopBackgroundMonitoring();
                    System.out.println("Background monitoring stopped");
                } else {
                    System.out.println("No background monitoring is active");
                }
                break;

            case "timeline":
                int count = 20;
                if (parts.length > 1) {
                    try {
                        count = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid count, using default: 20");
                    }
                }
                showTimeline(count);
                break;

            case "perfstats":
                if (parts.length > 1) {
                    showPerformanceStats(parts[1]);
                } else {
                    showAllPerformanceStats();
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

        System.out.println("Server: " + server);
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

    private void showAnalytics(String playerName) {
        PlayerAnalytics.PlayerInsights insights = analytics.getInsights(playerName);
        if (insights == null) {
            System.out.println("No data available for " + playerName);
            return;
        }

        System.out.println(insights);
        System.out.println(analytics.getActivityHeatmap(playerName));
    }

    private void listRealTimePlayers() {
        List<String> cliPlayers = multiRealTime.listCliPlayers();
        List<String> bgPlayers = multiRealTime.listBackgroundPlayers();

        System.out.println("\nReal-Time Monitored Players:");
        System.out.println("CLI Mode (" + cliPlayers.size() + "):");
        for (String player : cliPlayers) {
            System.out.println("  - " + player);
        }

        System.out.println("\nBackground Mode (" + bgPlayers.size() + "):");
        for (String player : bgPlayers) {
            System.out.println("  - " + player);
        }
        System.out.println();
    }

    private void showTimeline(int count) {
        List<LiveTimeline.TimelineEvent> events = timeline.getRecentEvents(count);
        System.out.println("\nRecent Events:");
        System.out.println("=".repeat(60));
        for (LiveTimeline.TimelineEvent event : events) {
            System.out.println(event.toString());
        }
        System.out.println("=".repeat(60));
        System.out.println();
    }

    private void showPerformanceStats(String serverName) {
        PerformanceMetrics latest = perfMonitor.getLatest(serverName);
        if (latest == null) {
            System.out.println("No performance data for " + serverName);
            return;
        }

        System.out.println(latest);

        ServerPerformanceMonitor.PerformanceStats stats = perfMonitor.getStats(serverName, 10);
        if (stats != null) {
            System.out.println(stats);
        }
    }

    private void showAllPerformanceStats() {
        Map<String, PerformanceMetrics> allMetrics = perfMonitor.getLatestForAllServers();
        System.out.println("\nServer Performance Overview:");
        System.out.println("=".repeat(60));
        for (Map.Entry<String, PerformanceMetrics> entry : allMetrics.entrySet()) {
            System.out.println(entry.getValue().toString());
        }
        System.out.println("=".repeat(60));
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
