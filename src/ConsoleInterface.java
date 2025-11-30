import java.io.IOException;
import java.util.Scanner;

public class ConsoleInterface {
    private PlayerChecker playerChecker;
    private Scanner scanner;
    private boolean running;

    public ConsoleInterface(PlayerChecker playerChecker) {
        this.playerChecker = playerChecker;
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    public void start() {
        printWelcome();

        while (running) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            processCommand(input);
        }

        scanner.close();
    }

    private void printWelcome() {
        System.out.println("========================================");
        System.out.println("  Minecraft Player Online Checker");
        System.out.println("========================================");
        System.out.println("Commands:");
        System.out.println("  check <playername> - Check if a player is online");
        System.out.println("  status            - Show server status");
        System.out.println("  help              - Show this help message");
        System.out.println("  exit              - Exit the program");
        System.out.println("========================================");
    }

    private void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "check":
                if (parts.length < 2) {
                    System.out.println("Error: Please provide a player name.");
                    System.out.println("Usage: check <playername>");
                } else {
                    checkPlayer(parts[1]);
                }
                break;

            case "status":
                showStatus();
                break;

            case "help":
                printWelcome();
                break;

            case "exit":
            case "quit":
                System.out.println("Goodbye!");
                running = false;
                break;

            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Type 'help' for available commands.");
                break;
        }
    }

    private void checkPlayer(String playerName) {
        try {
            System.out.println("Checking player: " + playerName + "...");
            boolean isOnline = playerChecker.isPlayerOnline(playerName);

            if (isOnline) {
                System.out.println("Result: " + playerName + " is ONLINE");
            } else {
                System.out.println("Result: " + playerName + " is OFFLINE");
                int onlineCount = playerChecker.getOnlinePlayerCount();
                if (onlineCount > 0) {
                    System.out.println("Note: " + onlineCount + " player(s) are online, but " + playerName + " is not in the visible sample.");
                }
            }
        } catch (IOException e) {
            handleConnectionError(e);
        }
    }

    private void showStatus() {
        try {
            System.out.println("Querying server status...");
            playerChecker.isPlayerOnline("");
            int onlineCount = playerChecker.getOnlinePlayerCount();

            if (onlineCount >= 0) {
                System.out.println("Server is online!");
                System.out.println("Players online: " + onlineCount);
            } else {
                System.out.println("Unable to retrieve player count.");
            }
        } catch (IOException e) {
            handleConnectionError(e);
        }
    }

    private void handleConnectionError(IOException e) {
        System.out.println("Connection Error: Unable to reach the Minecraft server.");
        System.out.println("Reason: " + e.getMessage());
        System.out.println();
        System.out.println("Possible causes:");
        System.out.println("  - The server is offline");
        System.out.println("  - The server IP or port is incorrect");
        System.out.println("  - Your network connection is down");
        System.out.println("  - The server firewall is blocking connections");
        System.out.println();
        System.out.println("Please check your config.properties file and try again.");
    }
}
