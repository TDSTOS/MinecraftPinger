import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ConfigLoader {
    private String serverIp;
    private int serverPort;
    private List<ServerConfig> servers;
    private String discordWebhook;
    private int dashboardPort;
    private boolean historyEnabled;
    private boolean autoUpdateEnabled;
    private int autoUpdateCheckIntervalMinutes;
    private String autoUpdateRepositoryOwner;
    private String autoUpdateRepositoryName;
    private int realTimeCliIntervalSeconds;
    private int realTimeDashboardIntervalSeconds;

    public ConfigLoader(String configFilePath) throws IOException {
        loadConfig(configFilePath);
    }

    private void loadConfig(String configFilePath) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            properties.load(fis);
        }

        serverIp = properties.getProperty("server.ip", "localhost");
        String portStr = properties.getProperty("server.port", "25565");

        try {
            serverPort = Integer.parseInt(portStr);
            if (serverPort < 1 || serverPort > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
        } catch (NumberFormatException e) {
            throw new IOException("Invalid port number in config: " + portStr);
        }

        servers = new ArrayList<>();
        loadMultiServerConfig(properties);

        discordWebhook = properties.getProperty("discord.webhook", "");
        dashboardPort = Integer.parseInt(properties.getProperty("dashboard.port", "8080"));
        historyEnabled = Boolean.parseBoolean(properties.getProperty("history.enabled", "false"));

        autoUpdateEnabled = Boolean.parseBoolean(properties.getProperty("autoUpdate.enabled", "true"));
        autoUpdateCheckIntervalMinutes = Integer.parseInt(properties.getProperty("autoUpdate.checkIntervalMinutes", "60"));
        autoUpdateRepositoryOwner = properties.getProperty("autoUpdate.repositoryOwner", "TDSTOS");
        autoUpdateRepositoryName = properties.getProperty("autoUpdate.repositoryName", "MinecraftPinger");

        realTimeCliIntervalSeconds = Integer.parseInt(properties.getProperty("realtime.cliIntervalSeconds", "1"));
        realTimeDashboardIntervalSeconds = Integer.parseInt(properties.getProperty("realtime.dashboardIntervalSeconds", "60"));
    }

    private void loadMultiServerConfig(Properties properties) {
        ServerConfig defaultServer = new ServerConfig("default", serverIp, serverPort);
        servers.add(defaultServer);

        for (int i = 1; i <= 10; i++) {
            String name = properties.getProperty("server." + i + ".name");
            String ip = properties.getProperty("server." + i + ".ip");
            String portStr = properties.getProperty("server." + i + ".port");

            if (name != null && ip != null && portStr != null) {
                try {
                    int port = Integer.parseInt(portStr);
                    servers.add(new ServerConfig(name, ip, port));
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid port for server " + i + ", skipping");
                }
            }
        }
    }

    public String getServerIp() {
        return serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public List<ServerConfig> getServers() {
        return servers;
    }

    public ServerConfig getServerByName(String name) {
        for (ServerConfig server : servers) {
            if (server.getName().equalsIgnoreCase(name)) {
                return server;
            }
        }
        return null;
    }

    public String getDiscordWebhook() {
        return discordWebhook;
    }

    public int getDashboardPort() {
        return dashboardPort;
    }

    public boolean isHistoryEnabled() {
        return historyEnabled;
    }

    public boolean isAutoUpdateEnabled() {
        return autoUpdateEnabled;
    }

    public int getAutoUpdateCheckIntervalMinutes() {
        return autoUpdateCheckIntervalMinutes;
    }

    public String getAutoUpdateRepositoryOwner() {
        return autoUpdateRepositoryOwner;
    }

    public String getAutoUpdateRepositoryName() {
        return autoUpdateRepositoryName;
    }

    public int getRealTimeCliIntervalSeconds() {
        return realTimeCliIntervalSeconds;
    }

    public int getRealTimeDashboardIntervalSeconds() {
        return realTimeDashboardIntervalSeconds;
    }
}
