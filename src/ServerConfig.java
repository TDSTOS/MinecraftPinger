public class ServerConfig {
    private final String name;
    private final String ip;
    private final int port;
    private boolean queryEnabled;

    public ServerConfig(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.queryEnabled = false;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean isQueryEnabled() {
        return queryEnabled;
    }

    public void setQueryEnabled(boolean queryEnabled) {
        this.queryEnabled = queryEnabled;
    }

    @Override
    public String toString() {
        return name + " (" + ip + ":" + port + ")";
    }
}
