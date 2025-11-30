import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private String serverIp;
    private int serverPort;

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
    }

    public String getServerIp() {
        return serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }
}
