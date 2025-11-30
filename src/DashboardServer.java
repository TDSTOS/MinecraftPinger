import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class DashboardServer {
    private HttpServer server;
    private int port;
    private MultiServerChecker serverChecker;
    private HistoryService historyService;
    private ConfigLoader config;
    private UpdateManager updateManager;

    public DashboardServer(int port, MultiServerChecker serverChecker, HistoryService historyService, ConfigLoader config, UpdateManager updateManager) {
        this.port = port;
        this.serverChecker = serverChecker;
        this.historyService = historyService;
        this.config = config;
        this.updateManager = updateManager;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(5));

        server.createContext("/", this::handleIndex);
        server.createContext("/api/servers", this::handleServers);
        server.createContext("/api/check", this::handleCheck);
        server.createContext("/api/history", this::handleHistory);

        server.start();
        System.out.println("Dashboard server started on http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        String response = buildDashboardHTML();
        sendResponse(exchange, 200, response, "text/html");
    }

    private void handleServers(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        Map<ServerConfig, ServerStatus> statuses = serverChecker.getAllServerStatus();
        String json = buildServerStatusJSON(statuses);

        setCORSHeaders(exchange);
        sendResponse(exchange, 200, json, "application/json");
    }

    private void handleCheck(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        String body = readRequestBody(exchange);
        String playerName = extractJsonValue(body, "playerName");
        String serverName = extractJsonValue(body, "serverName");

        if (playerName == null || playerName.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"Player name required\"}", "application/json");
            return;
        }

        Map<ServerConfig, PlayerCheckResult> results;

        if (serverName != null && !serverName.isEmpty() && !serverName.equals("all")) {
            ServerConfig server = config.getServerByName(serverName);
            if (server == null) {
                sendResponse(exchange, 404, "{\"error\":\"Server not found\"}", "application/json");
                return;
            }
            results = new HashMap<>();
            results.put(server, serverChecker.checkPlayerOnServer(playerName, server));
        } else {
            results = serverChecker.checkPlayerOnAllServers(playerName);
        }

        String json = buildPlayerCheckJSON(results);

        setCORSHeaders(exchange);
        sendResponse(exchange, 200, json, "application/json");
    }

    private void handleHistory(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String playerName = getQueryParam(query, "player");
        String daysStr = getQueryParam(query, "days");

        if (playerName == null || playerName.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"Player name required\"}", "application/json");
            return;
        }

        int days = 7;
        if (daysStr != null) {
            try {
                days = Integer.parseInt(daysStr);
            } catch (NumberFormatException e) {
                days = 7;
            }
        }

        List<HistoryEntry> history = historyService.getPlayerHistory(playerName, days);
        String json = buildHistoryJSON(history);

        setCORSHeaders(exchange);
        sendResponse(exchange, 200, json, "application/json");
    }

    private String buildDashboardHTML() {
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Minecraft Player Checker Dashboard</title>\n" +
            "    <style>\n" +
            "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
            "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #1a1a1a; color: #e0e0e0; padding: 20px; }\n" +
            "        .container { max-width: 1200px; margin: 0 auto; }\n" +
            "        h1 { color: #4CAF50; margin-bottom: 30px; }\n" +
            "        .section { background: #2a2a2a; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.3); }\n" +
            "        .section h2 { color: #4CAF50; margin-bottom: 15px; font-size: 1.3em; }\n" +
            "        .server-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 15px; }\n" +
            "        .server-card { background: #333; border-radius: 6px; padding: 15px; border-left: 4px solid #4CAF50; }\n" +
            "        .server-card.offline { border-left-color: #f44336; }\n" +
            "        .server-name { font-weight: bold; font-size: 1.1em; margin-bottom: 8px; }\n" +
            "        .server-info { font-size: 0.9em; color: #999; margin-bottom: 5px; }\n" +
            "        .status-badge { display: inline-block; padding: 4px 12px; border-radius: 12px; font-size: 0.85em; font-weight: bold; }\n" +
            "        .status-online { background: #4CAF50; color: white; }\n" +
            "        .status-offline { background: #f44336; color: white; }\n" +
            "        .check-form { display: flex; gap: 10px; flex-wrap: wrap; }\n" +
            "        input, select, button { padding: 10px 15px; border: none; border-radius: 4px; font-size: 1em; }\n" +
            "        input, select { background: #333; color: #e0e0e0; flex: 1; min-width: 200px; }\n" +
            "        button { background: #4CAF50; color: white; cursor: pointer; font-weight: bold; transition: background 0.3s; }\n" +
            "        button:hover { background: #45a049; }\n" +
            "        .result { margin-top: 15px; padding: 15px; background: #333; border-radius: 4px; }\n" +
            "        .refresh-btn { background: #2196F3; margin-bottom: 15px; }\n" +
            "        .refresh-btn:hover { background: #0b7dda; }\n" +
            "        .loading { color: #999; font-style: italic; }\n" +
            "        .update-banner { background: #ff9800; color: #000; padding: 15px; border-radius: 4px; margin-bottom: 20px; display: none; }\n" +
            "        .update-banner strong { display: block; margin-bottom: 5px; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <h1>Minecraft Player Checker Dashboard v" + UpdateManager.getVersion() + "</h1>\n" +
            "        \n" +
            "        <div id=\"updateBanner\" class=\"update-banner\">\n" +
            "            <strong>Update Available!</strong>\n" +
            "            <span id=\"updateMessage\"></span>\n" +
            "        </div>\n" +
            "        \n" +
            "        <div class=\"section\">\n" +
            "            <h2>Server Status</h2>\n" +
            "            <button class=\"refresh-btn\" onclick=\"loadServers()\">Refresh Status</button>\n" +
            "            <div id=\"servers\" class=\"server-grid\">\n" +
            "                <div class=\"loading\">Loading servers...</div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        \n" +
            "        <div class=\"section\">\n" +
            "            <h2>Check Player Status</h2>\n" +
            "            <div class=\"check-form\">\n" +
            "                <input type=\"text\" id=\"playerName\" placeholder=\"Enter player name\">\n" +
            "                <select id=\"serverSelect\">\n" +
            "                    <option value=\"all\">All Servers</option>\n" +
            "                </select>\n" +
            "                <button onclick=\"checkPlayer()\">Check Player</button>\n" +
            "            </div>\n" +
            "            <div id=\"result\"></div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    \n" +
            "    <script>\n" +
            "        async function loadServers() {\n" +
            "            const container = document.getElementById('servers');\n" +
            "            container.innerHTML = '<div class=\"loading\">Loading...</div>';\n" +
            "            \n" +
            "            try {\n" +
            "                const response = await fetch('/api/servers');\n" +
            "                const data = await response.json();\n" +
            "                \n" +
            "                container.innerHTML = '';\n" +
            "                const select = document.getElementById('serverSelect');\n" +
            "                select.innerHTML = '<option value=\"all\">All Servers</option>';\n" +
            "                \n" +
            "                data.forEach(server => {\n" +
            "                    const card = document.createElement('div');\n" +
            "                    card.className = `server-card ${server.online ? '' : 'offline'}`;\n" +
            "                    card.innerHTML = `\n" +
            "                        <div class=\"server-name\">${server.name}</div>\n" +
            "                        <div class=\"server-info\">${server.ip}:${server.port}</div>\n" +
            "                        <div class=\"server-info\"><span class=\"status-badge status-${server.online ? 'online' : 'offline'}\">${server.online ? 'Online' : 'Offline'}</span></div>\n" +
            "                        ${server.online ? `<div class=\"server-info\">Players: ${server.onlineCount}/${server.maxPlayers || '?'}</div>` : ''}\n" +
            "                        ${server.queryEnabled ? '<div class=\"server-info\">Query: Enabled</div>' : ''}\n" +
            "                    `;\n" +
            "                    container.appendChild(card);\n" +
            "                    \n" +
            "                    const option = document.createElement('option');\n" +
            "                    option.value = server.name;\n" +
            "                    option.textContent = server.name;\n" +
            "                    select.appendChild(option);\n" +
            "                });\n" +
            "            } catch (error) {\n" +
            "                container.innerHTML = '<div style=\"color: #f44336;\">Error loading servers: ' + error.message + '</div>';\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function checkPlayer() {\n" +
            "            const playerName = document.getElementById('playerName').value;\n" +
            "            const serverName = document.getElementById('serverSelect').value;\n" +
            "            const result = document.getElementById('result');\n" +
            "            \n" +
            "            if (!playerName) {\n" +
            "                result.innerHTML = '<div class=\"result\" style=\"color: #f44336;\">Please enter a player name</div>';\n" +
            "                return;\n" +
            "            }\n" +
            "            \n" +
            "            result.innerHTML = '<div class=\"result loading\">Checking...</div>';\n" +
            "            \n" +
            "            try {\n" +
            "                const response = await fetch('/api/check', {\n" +
            "                    method: 'POST',\n" +
            "                    headers: { 'Content-Type': 'application/json' },\n" +
            "                    body: JSON.stringify({ playerName, serverName })\n" +
            "                });\n" +
            "                \n" +
            "                const data = await response.json();\n" +
            "                \n" +
            "                let html = '<div class=\"result\">';\n" +
            "                data.forEach(check => {\n" +
            "                    html += `<div style=\"margin-bottom: 10px;\">`;\n" +
            "                    html += `<strong>${check.server}</strong>: `;\n" +
            "                    if (check.success) {\n" +
            "                        html += `<span class=\"status-badge status-${check.online ? 'online' : 'offline'}\">${check.online ? 'Online' : 'Offline'}</span>`;\n" +
            "                        if (check.usingQuery) html += ` <span style=\"color: #2196F3;\">(Query)</span>`;\n" +
            "                    } else {\n" +
            "                        html += `<span style=\"color: #f44336;\">Error: ${check.error}</span>`;\n" +
            "                    }\n" +
            "                    html += `</div>`;\n" +
            "                });\n" +
            "                html += '</div>';\n" +
            "                \n" +
            "                result.innerHTML = html;\n" +
            "            } catch (error) {\n" +
            "                result.innerHTML = '<div class=\"result\" style=\"color: #f44336;\">Error: ' + error.message + '</div>';\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        loadServers();\n" +
            "        setInterval(loadServers, 30000);\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }

    private String buildServerStatusJSON(Map<ServerConfig, ServerStatus> statuses) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Map.Entry<ServerConfig, ServerStatus> entry : statuses.entrySet()) {
            if (!first) json.append(",");
            first = false;

            ServerConfig server = entry.getKey();
            ServerStatus status = entry.getValue();

            json.append("{");
            json.append("\"name\":\"").append(escapeJson(server.getName())).append("\",");
            json.append("\"ip\":\"").append(escapeJson(server.getIp())).append("\",");
            json.append("\"port\":").append(server.getPort()).append(",");
            json.append("\"online\":").append(status.isOnline()).append(",");
            json.append("\"onlineCount\":").append(status.getOnlineCount()).append(",");
            json.append("\"maxPlayers\":").append(status.getMaxPlayers()).append(",");
            json.append("\"queryEnabled\":").append(server.isQueryEnabled());
            json.append("}");
        }

        json.append("]");
        return json.toString();
    }

    private String buildPlayerCheckJSON(Map<ServerConfig, PlayerCheckResult> results) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Map.Entry<ServerConfig, PlayerCheckResult> entry : results.entrySet()) {
            if (!first) json.append(",");
            first = false;

            PlayerCheckResult result = entry.getValue();

            json.append("{");
            json.append("\"server\":\"").append(escapeJson(result.getServer().getName())).append("\",");
            json.append("\"success\":").append(result.isSuccess()).append(",");
            json.append("\"online\":").append(result.isOnline()).append(",");
            json.append("\"usingQuery\":").append(result.isUsingQuery()).append(",");
            json.append("\"error\":\"").append(escapeJson(result.getErrorMessage())).append("\"");
            json.append("}");
        }

        json.append("]");
        return json.toString();
    }

    private String buildHistoryJSON(List<HistoryEntry> history) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (HistoryEntry entry : history) {
            if (!first) json.append(",");
            first = false;

            json.append("{");
            json.append("\"playerName\":\"").append(escapeJson(entry.getPlayerName())).append("\",");
            json.append("\"serverName\":\"").append(escapeJson(entry.getServerName())).append("\",");
            json.append("\"status\":\"").append(escapeJson(entry.getStatus())).append("\",");
            json.append("\"timestamp\":\"").append(escapeJson(entry.getTimestamp())).append("\"");
            json.append("}");
        }

        json.append("]");
        return json.toString();
    }

    private void sendResponse(HttpExchange exchange, int code, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void setCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;

        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;

        return json.substring(start, end);
    }

    private String getQueryParam(String query, String param) {
        if (query == null) return null;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(param)) {
                return keyValue[1];
            }
        }
        return null;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
