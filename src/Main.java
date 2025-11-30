import java.io.IOException;

public class Main {
    private static final String CONFIG_FILE = "config.properties";

    public static void main(String[] args) {
        DashboardServer dashboardServer = null;
        MultiServerChecker multiServerChecker = null;

        try {
            ConfigLoader config = new ConfigLoader(CONFIG_FILE);

            System.out.println("========================================");
            System.out.println("  Minecraft Player Checker");
            System.out.println("========================================");
            System.out.println("Loading configuration...");
            System.out.println("Primary Server: " + config.getServerIp() + ":" + config.getServerPort());
            System.out.println("Total Servers: " + config.getServers().size());
            System.out.println();

            MinecraftPinger pinger = new MinecraftPinger(
                config.getServerIp(),
                config.getServerPort()
            );

            PlayerChecker playerChecker = new PlayerChecker(pinger);

            multiServerChecker = new MultiServerChecker(config);

            HistoryService historyService = new HistoryService(config.isHistoryEnabled());
            if (historyService.isEnabled()) {
                System.out.println("History tracking: ENABLED");
            } else {
                System.out.println("History tracking: DISABLED (optional feature)");
            }

            DiscordWebhook discord = new DiscordWebhook(config.getDiscordWebhook());
            if (discord.isEnabled()) {
                System.out.println("Discord notifications: ENABLED");
            } else {
                System.out.println("Discord notifications: DISABLED (optional feature)");
            }

            dashboardServer = new DashboardServer(
                config.getDashboardPort(),
                multiServerChecker,
                historyService,
                config
            );

            try {
                dashboardServer.start();
            } catch (Exception e) {
                System.err.println("Warning: Could not start dashboard server: " + e.getMessage());
            }

            System.out.println();

            ConsoleInterface console = new ConsoleInterface(
                playerChecker,
                multiServerChecker,
                config,
                historyService,
                discord
            );

            final DashboardServer finalDashboardServer = dashboardServer;
            final MultiServerChecker finalMultiServerChecker = multiServerChecker;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down...");
                if (finalDashboardServer != null) {
                    finalDashboardServer.stop();
                }
                if (finalMultiServerChecker != null) {
                    finalMultiServerChecker.shutdown();
                }
            }));

            console.start();

        } catch (IOException e) {
            System.err.println("Error loading configuration:");
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println("Please ensure the '" + CONFIG_FILE + "' file exists and contains:");
            System.err.println("  server.ip=your.server.ip");
            System.err.println("  server.port=25565");
            System.exit(1);
        } finally {
            if (dashboardServer != null) {
                dashboardServer.stop();
            }
            if (multiServerChecker != null) {
                multiServerChecker.shutdown();
            }
        }
    }
}
