public class PerformanceMetrics {
    private long timestamp;
    private String serverName;
    private long pingLatency;
    private long responseTime;
    private int playerCount;
    private Double tps;
    private Double cpuLoad;
    private Double ramUsage;
    private Double ramMax;
    private String worldInfo;

    public PerformanceMetrics(String serverName) {
        this.serverName = serverName;
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public long getPingLatency() {
        return pingLatency;
    }

    public void setPingLatency(long pingLatency) {
        this.pingLatency = pingLatency;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public Double getTps() {
        return tps;
    }

    public void setTps(Double tps) {
        this.tps = tps;
    }

    public Double getCpuLoad() {
        return cpuLoad;
    }

    public void setCpuLoad(Double cpuLoad) {
        this.cpuLoad = cpuLoad;
    }

    public Double getRamUsage() {
        return ramUsage;
    }

    public void setRamUsage(Double ramUsage) {
        this.ramUsage = ramUsage;
    }

    public Double getRamMax() {
        return ramMax;
    }

    public void setRamMax(Double ramMax) {
        this.ramMax = ramMax;
    }

    public String getWorldInfo() {
        return worldInfo;
    }

    public void setWorldInfo(String worldInfo) {
        this.worldInfo = worldInfo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Performance Metrics [").append(serverName).append("]\n");
        sb.append("  Ping: ").append(pingLatency).append("ms\n");
        sb.append("  Response Time: ").append(responseTime).append("ms\n");
        sb.append("  Players: ").append(playerCount).append("\n");
        if (tps != null) sb.append("  TPS: ").append(String.format("%.2f", tps)).append("\n");
        if (cpuLoad != null) sb.append("  CPU Load: ").append(String.format("%.1f", cpuLoad)).append("%\n");
        if (ramUsage != null && ramMax != null) {
            sb.append("  RAM: ").append(String.format("%.1f", ramUsage)).append("/")
              .append(String.format("%.1f", ramMax)).append(" MB\n");
        }
        if (worldInfo != null) sb.append("  World: ").append(worldInfo).append("\n");
        return sb.toString();
    }
}
