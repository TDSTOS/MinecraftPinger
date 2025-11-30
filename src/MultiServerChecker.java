import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class MultiServerChecker {
    private ConfigLoader config;
    private ExecutorService executorService;

    public MultiServerChecker(ConfigLoader config) {
        this.config = config;
        this.executorService = Executors.newFixedThreadPool(10);
    }

    public Map<ServerConfig, PlayerCheckResult> checkPlayerOnAllServers(String playerName) {
        Map<ServerConfig, PlayerCheckResult> results = new ConcurrentHashMap<>();
        List<Future<Void>> futures = new ArrayList<>();

        for (ServerConfig server : config.getServers()) {
            Future<Void> future = executorService.submit(() -> {
                PlayerCheckResult result = checkPlayerOnServer(playerName, server);
                results.put(server, result);
                return null;
            });
            futures.add(future);
        }

        for (Future<Void> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Error waiting for server check: " + e.getMessage());
            }
        }

        return results;
    }

    public PlayerCheckResult checkPlayerOnServer(String playerName, ServerConfig server) {
        PlayerCheckResult result = new PlayerCheckResult();
        result.setPlayerName(playerName);
        result.setServer(server);

        try {
            if (!server.isQueryEnabled()) {
                boolean queryAvailable = MinecraftQuery.isQueryEnabled(server.getIp(), server.getPort());
                server.setQueryEnabled(queryAvailable);
            }

            if (server.isQueryEnabled()) {
                result = checkWithQuery(playerName, server);
            } else {
                result = checkWithPing(playerName, server);
            }

            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private PlayerCheckResult checkWithQuery(String playerName, ServerConfig server) throws IOException {
        PlayerCheckResult result = new PlayerCheckResult();
        result.setPlayerName(playerName);
        result.setServer(server);
        result.setUsingQuery(true);

        MinecraftQuery query = new MinecraftQuery(server.getIp(), server.getPort());
        QueryResponse queryResponse = query.query();

        result.setOnline(queryResponse.hasPlayer(playerName));
        result.setOnlineCount(queryResponse.getOnlinePlayers());
        result.setMaxPlayers(queryResponse.getMaxPlayers());
        result.setQueryResponse(queryResponse);

        return result;
    }

    private PlayerCheckResult checkWithPing(String playerName, ServerConfig server) throws IOException {
        PlayerCheckResult result = new PlayerCheckResult();
        result.setPlayerName(playerName);
        result.setServer(server);
        result.setUsingQuery(false);

        MinecraftPinger pinger = new MinecraftPinger(server.getIp(), server.getPort());
        PlayerChecker checker = new PlayerChecker(pinger);

        result.setOnline(checker.isPlayerOnline(playerName));
        result.setOnlineCount(checker.getOnlinePlayerCount());

        return result;
    }

    public Map<ServerConfig, ServerStatus> getAllServerStatus() {
        Map<ServerConfig, ServerStatus> results = new ConcurrentHashMap<>();
        List<Future<Void>> futures = new ArrayList<>();

        for (ServerConfig server : config.getServers()) {
            Future<Void> future = executorService.submit(() -> {
                ServerStatus status = getServerStatus(server);
                results.put(server, status);
                return null;
            });
            futures.add(future);
        }

        for (Future<Void> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Error waiting for server status: " + e.getMessage());
            }
        }

        return results;
    }

    public ServerStatus getServerStatus(ServerConfig server) {
        ServerStatus status = new ServerStatus();
        status.setServer(server);

        try {
            if (!server.isQueryEnabled()) {
                boolean queryAvailable = MinecraftQuery.isQueryEnabled(server.getIp(), server.getPort());
                server.setQueryEnabled(queryAvailable);
            }

            if (server.isQueryEnabled()) {
                MinecraftQuery query = new MinecraftQuery(server.getIp(), server.getPort());
                QueryResponse queryResponse = query.query();
                status.setOnline(true);
                status.setOnlineCount(queryResponse.getOnlinePlayers());
                status.setMaxPlayers(queryResponse.getMaxPlayers());
                status.setQueryResponse(queryResponse);
            } else {
                MinecraftPinger pinger = new MinecraftPinger(server.getIp(), server.getPort());
                String response = pinger.ping();
                status.setOnline(true);
                PlayerChecker checker = new PlayerChecker(pinger);
                status.setOnlineCount(checker.getOnlinePlayerCount());
            }
        } catch (Exception e) {
            status.setOnline(false);
            status.setErrorMessage(e.getMessage());
        }

        return status;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
