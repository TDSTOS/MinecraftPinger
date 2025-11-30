import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiPlayerRealTimeController {
    private final MultiServerChecker serverChecker;
    private final HistoryService historyService;
    private final DiscordWebhook discord;
    private final ConfigLoader config;
    private final LiveTimeline timeline;
    private final ServerPerformanceMonitor perfMonitor;

    private Timer cliTimer;
    private Timer backgroundTimer;

    private final AtomicBoolean cliActive;
    private final AtomicBoolean backgroundActive;

    private final Set<String> cliPlayers;
    private final Set<String> backgroundPlayers;

    private final Map<String, PlayerCheckResult> lastCliResults;
    private final Map<String, PlayerCheckResult> lastBackgroundResults;

    private final int cliIntervalSeconds;
    private final int backgroundIntervalSeconds;

    public MultiPlayerRealTimeController(MultiServerChecker serverChecker, HistoryService historyService,
                                         DiscordWebhook discord, ConfigLoader config,
                                         LiveTimeline timeline, ServerPerformanceMonitor perfMonitor) {
        this.serverChecker = serverChecker;
        this.historyService = historyService;
        this.discord = discord;
        this.config = config;
        this.timeline = timeline;
        this.perfMonitor = perfMonitor;

        this.cliActive = new AtomicBoolean(false);
        this.backgroundActive = new AtomicBoolean(false);

        this.cliPlayers = ConcurrentHashMap.newKeySet();
        this.backgroundPlayers = ConcurrentHashMap.newKeySet();

        this.lastCliResults = new ConcurrentHashMap<>();
        this.lastBackgroundResults = new ConcurrentHashMap<>();

        this.cliIntervalSeconds = config.getRealTimeCliIntervalSeconds();
        this.backgroundIntervalSeconds = 60;
    }

    public synchronized boolean addCliPlayer(String playerName, String serverName) {
        if (cliPlayers.contains(playerName)) {
            System.out.println("Player " + playerName + " is already being monitored.");
            return false;
        }

        ServerConfig server = resolveServer(serverName);
        if (server == null) {
            System.out.println("Server not found: " + serverName);
            return false;
        }

        cliPlayers.add(playerName);
        System.out.println("Added " + playerName + " to real-time monitoring on " + server.getName());

        if (!cliActive.get()) {
            startCliMonitoring();
        }

        return true;
    }

    public synchronized boolean removeCliPlayer(String playerName) {
        if (!cliPlayers.contains(playerName)) {
            System.out.println("Player " + playerName + " is not being monitored.");
            return false;
        }

        cliPlayers.remove(playerName);
        lastCliResults.remove(playerName);
        System.out.println("Removed " + playerName + " from real-time monitoring.");

        if (cliPlayers.isEmpty() && cliActive.get()) {
            stopCliMonitoring();
        }

        return true;
    }

    public List<String> listCliPlayers() {
        return new ArrayList<>(cliPlayers);
    }

    public synchronized boolean startBackgroundMonitoring(Set<String> playerNames) {
        if (backgroundActive.get()) {
            System.out.println("Background monitoring already running.");
            return false;
        }

        backgroundPlayers.addAll(playerNames);
        backgroundActive.set(true);

        backgroundTimer = new Timer("Background-RealTimeCheck", true);
        backgroundTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                performBackgroundChecks();
            }
        }, 0, backgroundIntervalSeconds * 1000L);

        return true;
    }

    public synchronized boolean stopBackgroundMonitoring() {
        if (!backgroundActive.get()) {
            return false;
        }

        if (backgroundTimer != null) {
            backgroundTimer.cancel();
            backgroundTimer = null;
        }

        backgroundActive.set(false);
        backgroundPlayers.clear();
        lastBackgroundResults.clear();

        return true;
    }

    public synchronized boolean addBackgroundPlayer(String playerName) {
        if (!backgroundActive.get()) {
            Set<String> players = new HashSet<>();
            players.add(playerName);
            return startBackgroundMonitoring(players);
        }

        backgroundPlayers.add(playerName);
        return true;
    }

    public synchronized boolean removeBackgroundPlayer(String playerName) {
        if (!backgroundPlayers.contains(playerName)) {
            return false;
        }

        backgroundPlayers.remove(playerName);
        lastBackgroundResults.remove(playerName);

        if (backgroundPlayers.isEmpty()) {
            stopBackgroundMonitoring();
        }

        return true;
    }

    public List<String> listBackgroundPlayers() {
        return new ArrayList<>(backgroundPlayers);
    }

    private synchronized void startCliMonitoring() {
        if (cliActive.get()) return;

        cliActive.set(true);
        System.out.println("========================================");
        System.out.println("  Multi-Player Real-Time Monitoring Started");
        System.out.println("========================================");
        System.out.println("Monitoring " + cliPlayers.size() + " player(s)");
        System.out.println("Check interval: " + cliIntervalSeconds + " second(s)");
        System.out.println("========================================");
        System.out.println();

        cliTimer = new Timer("CLI-MultiPlayer-RealTimeCheck", true);
        cliTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                performCliChecks();
            }
        }, 0, cliIntervalSeconds * 1000L);
    }

    private synchronized void stopCliMonitoring() {
        if (!cliActive.get()) return;

        if (cliTimer != null) {
            cliTimer.cancel();
            cliTimer = null;
        }

        cliActive.set(false);
        cliPlayers.clear();
        lastCliResults.clear();

        System.out.println();
        System.out.println("========================================");
        System.out.println("  Real-Time Monitoring Stopped");
        System.out.println("========================================");
        System.out.println();
    }

    private void performCliChecks() {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

        for (String playerName : cliPlayers) {
            try {
                ServerConfig server = config.getServers().getFirst();
                PlayerCheckResult result = serverChecker.checkPlayerOnServer(playerName, server);

                PlayerCheckResult previousResult = lastCliResults.get(playerName);

                StringBuilder status = new StringBuilder();
                status.append("[").append(timestamp).append("] ");
                status.append(playerName).append(": ");

                if (!result.isSuccess()) {
                    status.append("SERVER UNREACHABLE - ").append(result.getErrorMessage());
                } else {
                    status.append(result.isOnline() ? "ONLINE" : "OFFLINE");
                    status.append(" | Players: ").append(result.getOnlineCount());

                    if (historyService.isEnabled()) {
                        historyService.recordPlayerStatus(playerName, server.getName(),
                                result.isOnline(), result.getOnlineCount());
                    }

                    if (timeline != null) {
                        if (previousResult != null && previousResult.isSuccess()) {
                            if (!previousResult.isOnline() && result.isOnline()) {
                                timeline.recordPlayerJoin(playerName, server.getName());
                            } else if (previousResult.isOnline() && !result.isOnline()) {
                                timeline.recordPlayerLeave(playerName, server.getName());
                            }
                        }
                    }

                    if (previousResult != null && previousResult.isSuccess()) {
                        if (previousResult.isOnline() != result.isOnline()) {
                            if (result.isOnline() && discord.isEnabled()) {
                                discord.sendPlayerOnlineNotification(playerName, server.getName());
                            } else if (!result.isOnline() && discord.isEnabled()) {
                                discord.sendPlayerOfflineNotification(playerName, server.getName());
                            }
                        }
                    }
                }

                System.out.println(status);
                lastCliResults.put(playerName, result);

            } catch (Exception e) {
                System.err.println("[ERROR] Check failed for " + playerName + ": " + e.getMessage());
            }
        }
    }

    private void performBackgroundChecks() {
        for (String playerName : backgroundPlayers) {
            try {
                ServerConfig server = config.getServers().getFirst();
                PlayerCheckResult result = serverChecker.checkPlayerOnServer(playerName, server);

                PlayerCheckResult previousResult = lastBackgroundResults.get(playerName);

                if (result.isSuccess()) {
                    if (historyService.isEnabled()) {
                        historyService.recordPlayerStatus(playerName, server.getName(),
                                result.isOnline(), result.getOnlineCount());
                    }

                    if (timeline != null) {
                        if (previousResult != null && previousResult.isSuccess()) {
                            if (!previousResult.isOnline() && result.isOnline()) {
                                timeline.recordPlayerJoin(playerName, server.getName());
                            } else if (previousResult.isOnline() && !result.isOnline()) {
                                timeline.recordPlayerLeave(playerName, server.getName());
                            }
                        }
                    }

                    if (previousResult != null && previousResult.isSuccess()) {
                        if (previousResult.isOnline() != result.isOnline()) {
                            if (result.isOnline() && discord.isEnabled()) {
                                discord.sendPlayerOnlineNotification(playerName, server.getName());
                            } else if (!result.isOnline() && discord.isEnabled()) {
                                discord.sendPlayerOfflineNotification(playerName, server.getName());
                            }
                        }
                    }
                }

                lastBackgroundResults.put(playerName, result);

            } catch (Exception _) {}
        }
    }

    private ServerConfig resolveServer(String serverName) {
        if (serverName != null && !serverName.isEmpty()) {
            return config.getServerByName(serverName);
        }
        return config.getServers().isEmpty() ? null : config.getServers().getFirst();
    }

    public boolean isCliActive() {
        return cliActive.get();
    }

    public boolean isBackgroundActive() {
        return backgroundActive.get();
    }

    public Map<String, PlayerCheckResult> getLastCliResults() {
        return new HashMap<>(lastCliResults);
    }

    public Map<String, PlayerCheckResult> getLastBackgroundResults() {
        return new HashMap<>(lastBackgroundResults);
    }

    public void shutdown() {
        stopCliMonitoring();
        stopBackgroundMonitoring();
    }
}
