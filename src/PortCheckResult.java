public class PortCheckResult {
    private final String ip;
    private final int port;
    private final boolean open;
    private final long latencyMs;
    private final String errorMessage;
    private final Boolean playerOnline;
    private final String playerName;
    private final Integer onlinePlayerCount;

    public PortCheckResult(String ip, int port, boolean open, long latencyMs, String errorMessage) {
        this(ip, port, open, latencyMs, errorMessage, null, null, null);
    }

    public PortCheckResult(String ip, int port, boolean open, long latencyMs, String errorMessage,
                          Boolean playerOnline, String playerName, Integer onlinePlayerCount) {
        this.ip = ip;
        this.port = port;
        this.open = open;
        this.latencyMs = latencyMs;
        this.errorMessage = errorMessage;
        this.playerOnline = playerOnline;
        this.playerName = playerName;
        this.onlinePlayerCount = onlinePlayerCount;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean isOpen() {
        return open;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Boolean getPlayerOnline() {
        return playerOnline;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Integer getOnlinePlayerCount() {
        return onlinePlayerCount;
    }

    public boolean hasPlayerCheck() {
        return playerName != null;
    }

    public String getStatus() {
        return open ? "OPEN" : "CLOSED";
    }

    public String getPlayerStatus() {
        if (!hasPlayerCheck()) {
            return "-";
        }
        if (playerOnline == null) {
            return "CHECK_FAILED";
        }
        return playerOnline ? "ONLINE" : "OFFLINE";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (open) {
            sb.append(String.format("Port %d: OPEN (latency: %dms)", port, latencyMs));
            if (hasPlayerCheck()) {
                sb.append(String.format(" | Player %s: %s", playerName, getPlayerStatus()));
                if (onlinePlayerCount != null) {
                    sb.append(String.format(" | Server: %d online", onlinePlayerCount));
                }
            }
        } else {
            sb.append(String.format("Port %d: CLOSED", port));
            if (errorMessage != null) {
                sb.append(" (").append(errorMessage).append(")");
            }
        }

        return sb.toString();
    }

    public String toTableRow() {
        String status = open ? "✓ OPEN" : "✗ CLOSED";
        String latency = open ? latencyMs + "ms" : "-";
        String error = errorMessage != null ? errorMessage : "-";

        return String.format("  %-8d %-12s %-10s %-40s", port, status, latency, error);
    }

    public String toTableRowWithPlayer() {
        String status = open ? "✓ OPEN" : "✗ CLOSED";
        String latency = open ? latencyMs + "ms" : "-";
        String playerStatus = getPlayerStatus();
        String serverInfo = onlinePlayerCount != null ? onlinePlayerCount + " online" : "-";

        return String.format("  %-8d %-12s %-10s %-15s %-20s",
            port, status, latency, playerStatus, serverInfo);
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"ip\":\"").append(Utils.escapeJson(ip)).append("\",");
        json.append("\"port\":").append(port).append(",");
        json.append("\"open\":").append(open).append(",");
        json.append("\"latencyMs\":").append(latencyMs).append(",");
        json.append("\"status\":\"").append(getStatus()).append("\",");
        json.append("\"errorMessage\":\"").append(Utils.escapeJson(errorMessage != null ? errorMessage : "")).append("\"");

        if (hasPlayerCheck()) {
            json.append(",\"playerName\":\"").append(Utils.escapeJson(playerName)).append("\"");
            json.append(",\"playerOnline\":").append(playerOnline);
            json.append(",\"playerStatus\":\"").append(getPlayerStatus()).append("\"");
            if (onlinePlayerCount != null) {
                json.append(",\"onlinePlayerCount\":").append(onlinePlayerCount);
            }
        }

        json.append("}");
        return json.toString();
    }
}
