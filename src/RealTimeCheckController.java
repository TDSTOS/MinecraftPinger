import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RealTimeCheckController {
    private final MultiServerChecker serverChecker;
    private final HistoryService historyService;
    private final DiscordWebhook discord;
    private final ConfigLoader config;

    private Timer cliTimer;
    private Timer dashboardTimer;

    private final AtomicBoolean cliActive;
    private final AtomicBoolean dashboardActive;

    private final AtomicReference<String> currentCliPlayer;
    private final AtomicReference<String> currentDashboardPlayer;

    private final AtomicReference<PlayerCheckResult> lastCliResult;
    private final AtomicReference<PlayerCheckResult> lastDashboardResult;

    private final int cliIntervalSeconds;
    private final int dashboardIntervalSeconds;

    public RealTimeCheckController(MultiServerChecker serverChecker, HistoryService historyService,
                                   DiscordWebhook discord, ConfigLoader config) {
        this.serverChecker = serverChecker;
        this.historyService = historyService;
        this.discord = discord;
        this.config = config;

        this.cliActive = new AtomicBoolean(false);
        this.dashboardActive = new AtomicBoolean(false);

        this.currentCliPlayer = new AtomicReference<>(null);
        this.currentDashboardPlayer = new AtomicReference<>(null);

        this.lastCliResult = new AtomicReference<>(null);
        this.lastDashboardResult = new AtomicReference<>(null);

        this.cliIntervalSeconds = config.getRealTimeCliIntervalSeconds();
        this.dashboardIntervalSeconds = config.getRealTimeDashboardIntervalSeconds();
    }

    public synchronized void startCliRealTimeCheck(String playerName, String serverName) {
        if (cliActive.get()) {
            System.out.println("Real-time check already running. Stop it first with: realtime stop");
            return;
        }

        ServerConfig server;
        if (serverName != null && !serverName.isEmpty()) {
            server = config.getServerByName(serverName);
            if (server == null) {
                System.out.println("Server not found: " + serverName);
                return;
            }
        } else {
            server = config.getServers().get(0);
        }

        currentCliPlayer.set(playerName);
        cliActive.set(true);

        System.out.println("========================================");
        System.out.println("  Real-Time Monitoring Started");
        System.out.println("========================================");
        System.out.println("Player: " + playerName);
        System.out.println("Server: " + server.getName());
        System.out.println("Check interval: " + cliIntervalSeconds + " second(s)");
        System.out.println("Type 'realtime stop' to end monitoring");
        System.out.println("========================================");
        System.out.println();

        final ServerConfig finalServer = server;

        cliTimer = new Timer("CLI-RealTimeCheck", true);
        cliTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                performCliCheck(playerName, finalServer);
            }
        }, 0, cliIntervalSeconds * 1000L);

    }

    private void performCliCheck(String playerName, ServerConfig server) {
        try {
            PlayerCheckResult result = serverChecker.checkPlayerOnServer(playerName, server);

            PlayerCheckResult previousResult = lastCliResult.get();

            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            StringBuilder status = new StringBuilder();
            status.append("[").append(timestamp).append("] ");

            if (!result.isSuccess()) {
                status.append("SERVER UNREACHABLE - ").append(result.getErrorMessage());
                System.out.println(status);

                if (previousResult != null && previousResult.isSuccess()) {
                    if (discord.isEnabled()) {
                        discord.sendServerOutageNotification(server.getName(), result.getErrorMessage());
                    }
                }
            } else {
                status.append(playerName).append(": ");
                if (result.isOnline()) {
                    status.append("ONLINE");
                } else {
                    status.append("OFFLINE");
                }

                status.append(" | Players: ").append(result.getOnlineCount());

                if (result.isUsingQuery()) {
                    status.append(" | Query: YES");
                }

                System.out.println(status);

                if (historyService.isEnabled()) {
                    historyService.recordPlayerStatus(playerName, server.getName(),
                            result.isOnline(), result.getOnlineCount());
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

            lastCliResult.set(result);

        } catch (Exception e) {
            System.err.println("[ERROR] Real-time check failed: " + e.getMessage());
        }
    }

    public synchronized void stopCliRealTimeCheck() {
        if (!cliActive.get()) {
            System.out.println("No real-time check is currently running.");
            return;
        }

        if (cliTimer != null) {
            cliTimer.cancel();
            cliTimer = null;
        }

        cliActive.set(false);
        String player = currentCliPlayer.getAndSet(null);
        lastCliResult.set(null);

        System.out.println();
        System.out.println("========================================");
        System.out.println("  Real-Time Monitoring Stopped");
        System.out.println("========================================");
        if (player != null) {
            System.out.println("Player: " + player);
        }
        System.out.println();

    }

    public synchronized boolean startDashboardRealTimeCheck(String playerName, String serverName) {
        if (dashboardActive.get()) {
            return false;
        }

        ServerConfig server;
        if (serverName != null && !serverName.isEmpty()) {
            server = config.getServerByName(serverName);
            if (server == null) {
                return false;
            }
        } else {
            server = config.getServers().get(0);
        }

        currentDashboardPlayer.set(playerName);
        dashboardActive.set(true);

        System.out.println("Dashboard real-time monitoring started for: " + playerName);

        final ServerConfig finalServer = server;

        dashboardTimer = new Timer("Dashboard-RealTimeCheck", true);
        dashboardTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                performDashboardCheck(playerName, finalServer);
            }
        }, 0, dashboardIntervalSeconds * 1000L);

        return true;
    }

    private void performDashboardCheck(String playerName, ServerConfig server) {
        try {
            PlayerCheckResult result = serverChecker.checkPlayerOnServer(playerName, server);

            PlayerCheckResult previousResult = lastDashboardResult.get();

            if (result.isSuccess()) {
                if (historyService.isEnabled()) {
                    historyService.recordPlayerStatus(playerName, server.getName(),
                            result.isOnline(), result.getOnlineCount());
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

            lastDashboardResult.set(result);

        } catch (Exception e) {
            System.err.println("Dashboard real-time check failed: " + e.getMessage());
        }
    }

    public synchronized boolean stopDashboardRealTimeCheck() {
        if (!dashboardActive.get()) {
            return false;
        }

        if (dashboardTimer != null) {
            dashboardTimer.cancel();
            dashboardTimer = null;
        }

        dashboardActive.set(false);
        String player = currentDashboardPlayer.getAndSet(null);
        lastDashboardResult.set(null);

        System.out.println("Dashboard real-time monitoring stopped" + (player != null ? " for: " + player : ""));

        return true;
    }

    public boolean isCliActive() {
        return cliActive.get();
    }

    public boolean isDashboardActive() {
        return dashboardActive.get();
    }

    public String getCurrentCliPlayer() {
        return currentCliPlayer.get();
    }

    public String getCurrentDashboardPlayer() {
        return currentDashboardPlayer.get();
    }

    public PlayerCheckResult getLastCliResult() {
        return lastCliResult.get();
    }

    public PlayerCheckResult getLastDashboardResult() {
        return lastDashboardResult.get();
    }

    public void shutdown() {
        stopCliRealTimeCheck();
        stopDashboardRealTimeCheck();
    }
}
