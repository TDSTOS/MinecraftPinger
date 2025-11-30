import java.io.IOException;

public class Main {
    private static final String CONFIG_FILE = "config.properties";

    public static void main(String[] args) {
        try {
            ConfigLoader config = new ConfigLoader(CONFIG_FILE);

            System.out.println("Loading configuration...");
            System.out.println("Server: " + config.getServerIp() + ":" + config.getServerPort());
            System.out.println();

            MinecraftPinger pinger = new MinecraftPinger(
                config.getServerIp(),
                config.getServerPort()
            );

            PlayerChecker playerChecker = new PlayerChecker(pinger);

            ConsoleInterface console = new ConsoleInterface(playerChecker);
            console.start();

        } catch (IOException e) {
            System.err.println("Error loading configuration:");
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println("Please ensure the '" + CONFIG_FILE + "' file exists and contains:");
            System.err.println("  server.ip=your.server.ip");
            System.err.println("  server.port=25565");
            System.exit(1);
        }
    }
}
