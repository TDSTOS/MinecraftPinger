import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PortChecker {
    private final int scanTimeoutMs;
    private final boolean parallelChecks;
    private final List<Integer> defaultPorts;
    private MinecraftPinger minecraftPinger;

    public PortChecker(int scanTimeoutMs, boolean parallelChecks, List<Integer> defaultPorts) {
        this.scanTimeoutMs = scanTimeoutMs;
        this.parallelChecks = parallelChecks;
        this.defaultPorts = defaultPorts != null ? defaultPorts : getDefaultMinecraftPorts();
    }

    public PortChecker() {
        this(1000, true, getDefaultMinecraftPorts());
    }

    private static List<Integer> getDefaultMinecraftPorts() {
        List<Integer> ports = new ArrayList<>();
        ports.add(25565);
        ports.add(25566);
        ports.add(25567);
        ports.add(25568);
        ports.add(25569);
        return ports;
    }

    public List<PortCheckResult> checkDefaultPorts(String ip) {
        return checkPorts(ip, defaultPorts);
    }

    public List<PortCheckResult> checkDefaultPortsWithPlayer(String ip, String playerName) {
        return checkPortsWithPlayer(ip, defaultPorts, playerName);
    }

    public List<PortCheckResult> checkPortsWithPlayer(String ip, int startPort, int endPort, String playerName) {
        if (startPort < 1 || startPort > 65535) {
            throw new IllegalArgumentException("Start port must be between 1 and 65535");
        }
        if (endPort < 1 || endPort > 65535) {
            throw new IllegalArgumentException("End port must be between 1 and 65535");
        }
        if (startPort > endPort) {
            throw new IllegalArgumentException("Start port must be less than or equal to end port");
        }

        List<Integer> portsToCheck = new ArrayList<>();
        for (int port = startPort; port <= endPort; port++) {
            portsToCheck.add(port);
        }

        return checkPortsWithPlayer(ip, portsToCheck, playerName);
    }

    public List<PortCheckResult> checkPortsWithPlayer(String ip, List<Integer> ports, String playerName) {
        if (Utils.isNullOrEmpty(ip)) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }

        if (ports == null || ports.isEmpty()) {
            throw new IllegalArgumentException("Ports list cannot be null or empty");
        }

        if (Utils.isNullOrEmpty(playerName)) {
            throw new IllegalArgumentException("Player name cannot be null or empty");
        }

        if (parallelChecks && ports.size() > 1) {
            return checkPortsWithPlayerParallel(ip, ports, playerName);
        } else {
            return checkPortsWithPlayerSequential(ip, ports, playerName);
        }
    }

    private List<PortCheckResult> checkPortsWithPlayerSequential(String ip, List<Integer> ports, String playerName) {
        List<PortCheckResult> results = new ArrayList<>();

        for (int port : ports) {
            PortCheckResult result = checkSinglePortWithPlayer(ip, port, playerName);
            results.add(result);
        }

        return results;
    }

    private List<PortCheckResult> checkPortsWithPlayerParallel(String ip, List<Integer> ports, String playerName) {
        List<PortCheckResult> results = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(10, ports.size()));
        List<Future<PortCheckResult>> futures = new ArrayList<>();

        for (int port : ports) {
            Future<PortCheckResult> future = executor.submit(() -> checkSinglePortWithPlayer(ip, port, playerName));
            futures.add(future);
        }

        for (Future<PortCheckResult> future : futures) {
            try {
                PortCheckResult result = future.get(scanTimeoutMs + 2000, TimeUnit.MILLISECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                results.add(new PortCheckResult(ip, -1, false, 0, "Scan timeout exceeded"));
            } catch (Exception e) {
                results.add(new PortCheckResult(ip, -1, false, 0, "Scan error: " + e.getMessage()));
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return results;
    }

    public List<PortCheckResult> checkPorts(String ip, int startPort, int endPort) {
        if (startPort < 1 || startPort > 65535) {
            throw new IllegalArgumentException("Start port must be between 1 and 65535");
        }
        if (endPort < 1 || endPort > 65535) {
            throw new IllegalArgumentException("End port must be between 1 and 65535");
        }
        if (startPort > endPort) {
            throw new IllegalArgumentException("Start port must be less than or equal to end port");
        }

        List<Integer> portsToCheck = new ArrayList<>();
        for (int port = startPort; port <= endPort; port++) {
            portsToCheck.add(port);
        }

        return checkPorts(ip, portsToCheck);
    }

    public List<PortCheckResult> checkPorts(String ip, List<Integer> ports) {
        if (Utils.isNullOrEmpty(ip)) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }

        if (ports == null || ports.isEmpty()) {
            throw new IllegalArgumentException("Ports list cannot be null or empty");
        }

        if (parallelChecks && ports.size() > 1) {
            return checkPortsParallel(ip, ports);
        } else {
            return checkPortsSequential(ip, ports);
        }
    }

    private List<PortCheckResult> checkPortsSequential(String ip, List<Integer> ports) {
        List<PortCheckResult> results = new ArrayList<>();

        for (int port : ports) {
            PortCheckResult result = checkSinglePort(ip, port);
            results.add(result);
        }

        return results;
    }

    private List<PortCheckResult> checkPortsParallel(String ip, List<Integer> ports) {
        List<PortCheckResult> results = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(10, ports.size()));
        List<Future<PortCheckResult>> futures = new ArrayList<>();

        for (int port : ports) {
            Future<PortCheckResult> future = executor.submit(() -> checkSinglePort(ip, port));
            futures.add(future);
        }

        for (Future<PortCheckResult> future : futures) {
            try {
                PortCheckResult result = future.get(scanTimeoutMs + 1000, TimeUnit.MILLISECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                results.add(new PortCheckResult(ip, -1, false, 0, "Scan timeout exceeded"));
            } catch (Exception e) {
                results.add(new PortCheckResult(ip, -1, false, 0, "Scan error: " + e.getMessage()));
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return results;
    }

    public PortCheckResult checkSinglePort(String ip, int port) {
        if (port < 1 || port > 65535) {
            return new PortCheckResult(ip, port, false, 0, "Invalid port number");
        }

        long startTime = System.currentTimeMillis();
        Socket socket = null;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), scanTimeoutMs);
            long latency = System.currentTimeMillis() - startTime;
            return new PortCheckResult(ip, port, true, latency, null);

        } catch (SocketTimeoutException e) {
            long latency = System.currentTimeMillis() - startTime;
            return new PortCheckResult(ip, port, false, latency, "Connection timeout");

        } catch (IOException e) {
            long latency = System.currentTimeMillis() - startTime;
            String errorMsg = getErrorMessage(e);
            return new PortCheckResult(ip, port, false, latency, errorMsg);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return new PortCheckResult(ip, port, false, latency, "Error: " + e.getMessage());

        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }

    public PortCheckResult checkSinglePortWithPlayer(String ip, int port, String playerName) {
        if (port < 1 || port > 65535) {
            return new PortCheckResult(ip, port, false, 0, "Invalid port number", null, playerName, null);
        }

        long startTime = System.currentTimeMillis();
        Socket socket = null;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), scanTimeoutMs);
            long portLatency = System.currentTimeMillis() - startTime;

            socket.close();

            if (minecraftPinger == null) {
                minecraftPinger = new MinecraftPinger();
            }

            try {
                boolean isOnline = minecraftPinger.isPlayerOnline(ip, port, playerName);
                int onlineCount = minecraftPinger.getOnlinePlayerCount();
                long totalLatency = System.currentTimeMillis() - startTime;

                return new PortCheckResult(ip, port, true, totalLatency, null, isOnline, playerName, onlineCount);

            } catch (Exception e) {
                return new PortCheckResult(ip, port, true, portLatency, null, null, playerName, null);
            }

        } catch (SocketTimeoutException e) {
            long latency = System.currentTimeMillis() - startTime;
            return new PortCheckResult(ip, port, false, latency, "Connection timeout", null, playerName, null);

        } catch (IOException e) {
            long latency = System.currentTimeMillis() - startTime;
            String errorMsg = getErrorMessage(e);
            return new PortCheckResult(ip, port, false, latency, errorMsg, null, playerName, null);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return new PortCheckResult(ip, port, false, latency, "Error: " + e.getMessage(), null, playerName, null);

        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }

    private String getErrorMessage(IOException e) {
        String message = e.getMessage();

        if (message == null) {
            return "Connection failed";
        }

        if (message.contains("Connection refused")) {
            return "Connection refused";
        } else if (message.contains("No route to host")) {
            return "No route to host";
        } else if (message.contains("Host is unreachable")) {
            return "Host unreachable";
        } else if (message.contains("Network is unreachable")) {
            return "Network unreachable";
        } else if (message.contains("Operation timed out")) {
            return "Operation timed out";
        } else {
            return message.length() > 40 ? message.substring(0, 37) + "..." : message;
        }
    }

    public int getScanTimeoutMs() {
        return scanTimeoutMs;
    }

    public boolean isParallelChecks() {
        return parallelChecks;
    }

    public List<Integer> getDefaultPorts() {
        return new ArrayList<>(defaultPorts);
    }

    public void printResults(List<PortCheckResult> results) {
        if (results.isEmpty()) {
            System.out.println("No results to display.");
            return;
        }

        boolean hasPlayerCheck = results.get(0).hasPlayerCheck();

        if (hasPlayerCheck) {
            printResultsWithPlayer(results);
        } else {
            printPortOnlyResults(results);
        }
    }

    private void printPortOnlyResults(List<PortCheckResult> results) {
        String ip = results.get(0).getIp();
        System.out.println("\nPort Scan Results for: " + ip);
        Utils.printSeparator(60, '=');
        System.out.println(String.format("  %-8s %-12s %-10s %-40s", "PORT", "STATUS", "LATENCY", "MESSAGE"));
        Utils.printSeparator(60, '-');

        int openCount = 0;
        for (PortCheckResult result : results) {
            System.out.println(result.toTableRow());
            if (result.isOpen()) {
                openCount++;
            }
        }

        Utils.printSeparator(60, '=');
        System.out.println(String.format("  Total: %d ports scanned, %d open, %d closed",
            results.size(), openCount, results.size() - openCount));
        System.out.println();
    }

    private void printResultsWithPlayer(List<PortCheckResult> results) {
        String ip = results.get(0).getIp();
        String playerName = results.get(0).getPlayerName();

        System.out.println("\nPort Scan with Player Check for: " + ip);
        System.out.println("Player: " + playerName);
        Utils.printSeparator(80, '=');
        System.out.println(String.format("  %-8s %-12s %-10s %-15s %-20s",
            "PORT", "STATUS", "LATENCY", "PLAYER", "SERVER INFO"));
        Utils.printSeparator(80, '-');

        int openCount = 0;
        int playerFoundCount = 0;

        for (PortCheckResult result : results) {
            System.out.println(result.toTableRowWithPlayer());
            if (result.isOpen()) {
                openCount++;
                if (result.getPlayerOnline() != null && result.getPlayerOnline()) {
                    playerFoundCount++;
                }
            }
        }

        Utils.printSeparator(80, '=');
        System.out.println(String.format("  Ports: %d scanned, %d open, %d closed",
            results.size(), openCount, results.size() - openCount));
        System.out.println(String.format("  Player found on %d port(s)", playerFoundCount));
        System.out.println();
    }

    public String resultsToJson(List<PortCheckResult> results) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) json.append(",");
            json.append(results.get(i).toJson());
        }
        json.append("]");
        return json.toString();
    }

    public static boolean isValidIp(String ip) {
        if (Utils.isNullOrEmpty(ip)) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidHostname(String hostname) {
        if (Utils.isNullOrEmpty(hostname)) {
            return false;
        }

        if (hostname.length() > 253) {
            return false;
        }

        String[] labels = hostname.split("\\.");
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63) {
                return false;
            }
            if (!label.matches("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$")) {
                return false;
            }
        }

        return true;
    }
}
