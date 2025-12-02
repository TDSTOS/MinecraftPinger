public class PortCheckResult {
    private final String ip;
    private final int port;
    private final boolean open;
    private final long latencyMs;
    private final String errorMessage;

    public PortCheckResult(String ip, int port, boolean open, long latencyMs, String errorMessage) {
        this.ip = ip;
        this.port = port;
        this.open = open;
        this.latencyMs = latencyMs;
        this.errorMessage = errorMessage;
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

    public String getStatus() {
        return open ? "OPEN" : "CLOSED";
    }

    @Override
    public String toString() {
        if (open) {
            return String.format("Port %d: OPEN (latency: %dms)", port, latencyMs);
        } else {
            return String.format("Port %d: CLOSED%s", port,
                errorMessage != null ? " (" + errorMessage + ")" : "");
        }
    }

    public String toTableRow() {
        String status = open ? "✓ OPEN" : "✗ CLOSED";
        String latency = open ? latencyMs + "ms" : "-";
        String error = errorMessage != null ? errorMessage : "-";

        return String.format("  %-8d %-12s %-10s %-40s", port, status, latency, error);
    }

    public String toJson() {
        return String.format(
            "{\"ip\":\"%s\",\"port\":%d,\"open\":%b,\"latencyMs\":%d,\"status\":\"%s\",\"errorMessage\":\"%s\"}",
            Utils.escapeJson(ip),
            port,
            open,
            latencyMs,
            getStatus(),
            Utils.escapeJson(errorMessage != null ? errorMessage : "")
        );
    }
}
