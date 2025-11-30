import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerPerformanceMonitor {
    private static final int MAX_HISTORY_SIZE = 100;

    private Map<String, LinkedList<PerformanceMetrics>> metricsHistory;
    private HistoryService historyService;

    public ServerPerformanceMonitor(HistoryService historyService) {
        this.metricsHistory = new ConcurrentHashMap<>();
        this.historyService = historyService;
    }

    public PerformanceMetrics recordMetrics(String serverName, ServerStatus status, QueryResponse queryResponse) {
        PerformanceMetrics metrics = new PerformanceMetrics(serverName);

        if (status != null) {
            metrics.setPingLatency(status.getLatency());
            metrics.setResponseTime(status.getLatency());
            metrics.setPlayerCount(status.getOnlinePlayers());
        }

        if (queryResponse != null) {
            metrics.setPlayerCount(queryResponse.getNumPlayers());

            String plugins = queryResponse.getPlugins();
            if (plugins != null) {
                metrics.setTps(extractTPS(plugins));
                metrics.setCpuLoad(extractCPULoad(plugins));

                Double[] ram = extractRAM(plugins);
                if (ram != null && ram.length == 2) {
                    metrics.setRamUsage(ram[0]);
                    metrics.setRamMax(ram[1]);
                }
            }

            String map = queryResponse.getMap();
            if (map != null) {
                metrics.setWorldInfo(map);
            }
        }

        addToHistory(serverName, metrics);

        return metrics;
    }

    private void addToHistory(String serverName, PerformanceMetrics metrics) {
        metricsHistory.putIfAbsent(serverName, new LinkedList<>());
        LinkedList<PerformanceMetrics> history = metricsHistory.get(serverName);

        synchronized (history) {
            history.addLast(metrics);
            if (history.size() > MAX_HISTORY_SIZE) {
                history.removeFirst();
            }
        }
    }

    public List<PerformanceMetrics> getHistory(String serverName) {
        LinkedList<PerformanceMetrics> history = metricsHistory.get(serverName);
        if (history == null) return new ArrayList<>();

        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public PerformanceMetrics getLatest(String serverName) {
        LinkedList<PerformanceMetrics> history = metricsHistory.get(serverName);
        if (history == null || history.isEmpty()) return null;

        synchronized (history) {
            return history.getLast();
        }
    }

    public Map<String, PerformanceMetrics> getLatestForAllServers() {
        Map<String, PerformanceMetrics> latest = new HashMap<>();
        for (String serverName : metricsHistory.keySet()) {
            PerformanceMetrics metrics = getLatest(serverName);
            if (metrics != null) {
                latest.put(serverName, metrics);
            }
        }
        return latest;
    }

    public PerformanceStats getStats(String serverName, int lastNChecks) {
        List<PerformanceMetrics> history = getHistory(serverName);
        if (history.isEmpty()) return null;

        int size = Math.min(lastNChecks, history.size());
        List<PerformanceMetrics> recent = history.subList(history.size() - size, history.size());

        PerformanceStats stats = new PerformanceStats();

        long totalPing = 0;
        int playerSum = 0;
        double tpsSum = 0;
        int tpsCount = 0;

        for (PerformanceMetrics m : recent) {
            totalPing += m.getPingLatency();
            playerSum += m.getPlayerCount();
            if (m.getTps() != null) {
                tpsSum += m.getTps();
                tpsCount++;
            }
        }

        stats.avgPing = totalPing / (double) size;
        stats.avgPlayers = playerSum / (double) size;
        if (tpsCount > 0) {
            stats.avgTps = tpsSum / tpsCount;
        }

        return stats;
    }

    private Double extractTPS(String plugins) {
        if (plugins == null) return null;

        int tpsIndex = plugins.toLowerCase().indexOf("tps:");
        if (tpsIndex == -1) return null;

        try {
            String tpsStr = plugins.substring(tpsIndex + 4).trim();
            int endIndex = tpsStr.indexOf(' ');
            if (endIndex > 0) tpsStr = tpsStr.substring(0, endIndex);
            return Double.parseDouble(tpsStr);
        } catch (Exception e) {
            return null;
        }
    }

    private Double extractCPULoad(String plugins) {
        if (plugins == null) return null;

        int cpuIndex = plugins.toLowerCase().indexOf("cpu:");
        if (cpuIndex == -1) return null;

        try {
            String cpuStr = plugins.substring(cpuIndex + 4).trim();
            cpuStr = cpuStr.replace("%", "");
            int endIndex = cpuStr.indexOf(' ');
            if (endIndex > 0) cpuStr = cpuStr.substring(0, endIndex);
            return Double.parseDouble(cpuStr);
        } catch (Exception e) {
            return null;
        }
    }

    private Double[] extractRAM(String plugins) {
        if (plugins == null) return null;

        int ramIndex = plugins.toLowerCase().indexOf("ram:");
        if (ramIndex == -1) return null;

        try {
            String ramStr = plugins.substring(ramIndex + 4).trim();
            int slashIndex = ramStr.indexOf('/');
            if (slashIndex == -1) return null;

            String usedStr = ramStr.substring(0, slashIndex).trim();
            String remaining = ramStr.substring(slashIndex + 1).trim();
            int endIndex = remaining.indexOf(' ');
            if (endIndex > 0) remaining = remaining.substring(0, endIndex);

            Double used = Double.parseDouble(usedStr);
            Double max = Double.parseDouble(remaining);

            return new Double[]{used, max};
        } catch (Exception e) {
            return null;
        }
    }

    public static class PerformanceStats {
        public double avgPing;
        public double avgPlayers;
        public Double avgTps;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Average Performance Stats:\n");
            sb.append("  Avg Ping: ").append(String.format("%.1f", avgPing)).append("ms\n");
            sb.append("  Avg Players: ").append(String.format("%.1f", avgPlayers)).append("\n");
            if (avgTps != null) {
                sb.append("  Avg TPS: ").append(String.format("%.2f", avgTps)).append("\n");
            }
            return sb.toString();
        }
    }
}
