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
    private RealTimeCheckController realTimeController;

    public DashboardServer(int port, MultiServerChecker serverChecker, HistoryService historyService, ConfigLoader config, UpdateManager updateManager, RealTimeCheckController realTimeController) {
        this.port = port;
        this.serverChecker = serverChecker;
        this.historyService = historyService;
        this.config = config;
        this.updateManager = updateManager;
        this.realTimeController = realTimeController;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(5));

        server.createContext("/", this::handleIndex);
        server.createContext("/api/servers", this::handleServers);
        server.createContext("/api/check", this::handleCheck);
        server.createContext("/api/history", this::handleHistory);
        server.createContext("/api/realtime/start", this::handleRealtimeStart);
        server.createContext("/api/realtime/stop", this::handleRealtimeStop);
        server.createContext("/api/realtime/status", this::handleRealtimeStatus);
        server.createContext("/realtime/toggle", this::handleRealtimeToggle);

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

    private void handleRealtimeStart(HttpExchange exchange) throws IOException {
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

        boolean success = realTimeController.startDashboardRealTimeCheck(playerName, serverName);

        setCORSHeaders(exchange);
        if (success) {
            String json = "{\"success\":true,\"message\":\"Real-time monitoring started\",\"player\":\"" + playerName + "\"}";
            sendResponse(exchange, 200, json, "application/json");
        } else {
            sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Real-time monitoring already active or server not found\"}", "application/json");
        }
    }

    private void handleRealtimeStop(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        boolean success = realTimeController.stopDashboardRealTimeCheck();

        setCORSHeaders(exchange);
        String json = "{\"success\":" + success + ",\"message\":\"Real-time monitoring stopped\"}";
        sendResponse(exchange, 200, json, "application/json");
    }

    private void handleRealtimeStatus(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        boolean active = realTimeController.isDashboardActive();
        String player = realTimeController.getCurrentDashboardPlayer();
        PlayerCheckResult result = realTimeController.getLastDashboardResult();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"active\":").append(active).append(",");
        json.append("\"player\":").append(player != null ? "\"" + player + "\"" : "null").append(",");

        if (result != null) {
            json.append("\"lastCheck\":{");
            json.append("\"success\":").append(result.isSuccess()).append(",");
            json.append("\"online\":").append(result.isOnline()).append(",");
            json.append("\"onlineCount\":").append(result.getOnlineCount()).append(",");
            json.append("\"usingQuery\":").append(result.isUsingQuery());
            if (!result.isSuccess() && result.getErrorMessage() != null) {
                json.append(",\"error\":\"").append(result.getErrorMessage()).append("\"");
            }
            json.append("}");
        } else {
            json.append("\"lastCheck\":null");
        }

        json.append("}");

        setCORSHeaders(exchange);
        sendResponse(exchange, 200, json.toString(), "application/json");
    }

    private void handleRealtimeToggle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String playerName = getQueryParam(query, "player");

        if (playerName == null || playerName.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"Player parameter required\"}", "application/json");
            return;
        }

        boolean currentlyActive = realTimeController.isDashboardActive();
        boolean success;

        if (currentlyActive) {
            success = realTimeController.stopDashboardRealTimeCheck();
        } else {
            success = realTimeController.startDashboardRealTimeCheck(playerName, null);
        }

        boolean newState = realTimeController.isDashboardActive();
        String currentPlayer = realTimeController.getCurrentDashboardPlayer();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":").append(success).append(",");
        json.append("\"active\":").append(newState).append(",");
        json.append("\"player\":").append(currentPlayer != null ? "\"" + currentPlayer + "\"" : "null");
        json.append("}");

        setCORSHeaders(exchange);
        sendResponse(exchange, 200, json.toString(), "application/json");
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
            "        .realtime-toggle-btn { background: #FF9800; margin-left: 10px; }\n" +
            "        .realtime-toggle-btn:hover { background: #F57C00; }\n" +
            "        .realtime-toggle-btn.active { background: #f44336; }\n" +
            "        .realtime-toggle-btn.active:hover { background: #d32f2f; }\n" +
            "        .realtime-status { font-size: 0.9em; color: #999; margin-top: 10px; }\n" +
            "        .realtime-status.active { color: #4CAF50; font-weight: bold; }\n" +
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
            "                <button id=\"realtimeToggleBtn\" class=\"realtime-toggle-btn\" onclick=\"toggleRealtimeCheck()\">Start RealTime Check</button>\n" +
            "            </div>\n" +
            "            <div id=\"realtimeStatusIndicator\" class=\"realtime-status\">RealTime mode: Inactive</div>\n" +
            "            <div id=\"result\"></div>\n" +
            "        </div>\n" +
            "        \n" +
            "        <div class=\"section\">\n" +
            "            <h2>Real-Time Monitoring</h2>\n" +
            "            <div class=\"check-form\">\n" +
            "                <input type=\"text\" id=\"realtimePlayerName\" placeholder=\"Enter player name\">\n" +
            "                <select id=\"realtimeServerSelect\">\n" +
            "                    <option value=\"\">Default Server</option>\n" +
            "                </select>\n" +
            "                <button id=\"realtimeBtn\" onclick=\"toggleRealtime()\">Start Monitoring</button>\n" +
            "            </div>\n" +
            "            <div id=\"realtimeStatus\" style=\"margin-top: 15px;\"></div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    \n" +
            "    <script>\n" +
            "        let realtimeInterval = null;\n" +
            "        let realtimeActive = false;\n" +
            "        \n" +
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
            "        async function toggleRealtimeCheck() {\n" +
            "            const playerName = document.getElementById('playerName').value.trim();\n" +
            "            \n" +
            "            if (!playerName) {\n" +
            "                alert('Please enter a player name');\n" +
            "                return;\n" +
            "            }\n" +
            "            \n" +
            "            try {\n" +
            "                const response = await fetch('/realtime/toggle?player=' + encodeURIComponent(playerName));\n" +
            "                const data = await response.json();\n" +
            "                \n" +
            "                if (data.success) {\n" +
            "                    updateRealtimeButtonState(data.active, data.player);\n" +
            "                    \n" +
            "                    if (data.active) {\n" +
            "                        startDashboardAutoRefresh();\n" +
            "                    } else {\n" +
            "                        stopDashboardAutoRefresh();\n" +
            "                    }\n" +
            "                }\n" +
            "            } catch (error) {\n" +
            "                alert('Error toggling realtime check: ' + error.message);\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        function updateRealtimeButtonState(active, player) {\n" +
            "            const btn = document.getElementById('realtimeToggleBtn');\n" +
            "            const statusDiv = document.getElementById('realtimeStatusIndicator');\n" +
            "            \n" +
            "            if (active) {\n" +
            "                btn.textContent = 'Stop RealTime Check';\n" +
            "                btn.classList.add('active');\n" +
            "                statusDiv.textContent = 'RealTime mode: Active (monitoring ' + player + ' - updates every 60s)';\n" +
            "                statusDiv.classList.add('active');\n" +
            "            } else {\n" +
            "                btn.textContent = 'Start RealTime Check';\n" +
            "                btn.classList.remove('active');\n" +
            "                statusDiv.textContent = 'RealTime mode: Inactive';\n" +
            "                statusDiv.classList.remove('active');\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        let dashboardRefreshInterval = null;\n" +
            "        \n" +
            "        function startDashboardAutoRefresh() {\n" +
            "            if (dashboardRefreshInterval) return;\n" +
            "            \n" +
            "            dashboardRefreshInterval = setInterval(() => {\n" +
            "                loadServers();\n" +
            "                const playerName = document.getElementById('playerName').value.trim();\n" +
            "                if (playerName) {\n" +
            "                    checkPlayer();\n" +
            "                }\n" +
            "            }, 60000);\n" +
            "            \n" +
            "            console.log('Dashboard auto-refresh started (60s interval)');\n" +
            "        }\n" +
            "        \n" +
            "        function stopDashboardAutoRefresh() {\n" +
            "            if (dashboardRefreshInterval) {\n" +
            "                clearInterval(dashboardRefreshInterval);\n" +
            "                dashboardRefreshInterval = null;\n" +
            "                console.log('Dashboard auto-refresh stopped');\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function checkRealtimeState() {\n" +
            "            try {\n" +
            "                const response = await fetch('/api/realtime/status');\n" +
            "                const data = await response.json();\n" +
            "                \n" +
            "                if (data.active && data.player) {\n" +
            "                    updateRealtimeButtonState(true, data.player);\n" +
            "                    document.getElementById('playerName').value = data.player;\n" +
            "                    startDashboardAutoRefresh();\n" +
            "                }\n" +
            "            } catch (error) {\n" +
            "                console.error('Failed to check realtime state:', error);\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function toggleRealtime() {\n" +
            "            const btn = document.getElementById('realtimeBtn');\n" +
            "            const playerName = document.getElementById('realtimePlayerName').value.trim();\n" +
            "            const serverName = document.getElementById('realtimeServerSelect').value;\n" +
            "            \n" +
            "            if (!realtimeActive) {\n" +
            "                if (!playerName) {\n" +
            "                    alert('Please enter a player name');\n" +
            "                    return;\n" +
            "                }\n" +
            "                \n" +
            "                try {\n" +
            "                    const response = await fetch('/api/realtime/start', {\n" +
            "                        method: 'POST',\n" +
            "                        headers: {'Content-Type': 'application/json'},\n" +
            "                        body: JSON.stringify({playerName, serverName: serverName || null})\n" +
            "                    });\n" +
            "                    \n" +
            "                    const data = await response.json();\n" +
            "                    \n" +
            "                    if (data.success) {\n" +
            "                        realtimeActive = true;\n" +
            "                        btn.textContent = 'Stop Monitoring';\n" +
            "                        btn.style.background = '#f44336';\n" +
            "                        document.getElementById('realtimePlayerName').disabled = true;\n" +
            "                        document.getElementById('realtimeServerSelect').disabled = true;\n" +
            "                        startRealtimePolling();\n" +
            "                    } else {\n" +
            "                        alert('Failed to start monitoring: ' + (data.error || 'Unknown error'));\n" +
            "                    }\n" +
            "                } catch (error) {\n" +
            "                    alert('Error: ' + error.message);\n" +
            "                }\n" +
            "            } else {\n" +
            "                try {\n" +
            "                    await fetch('/api/realtime/stop', {method: 'POST'});\n" +
            "                    realtimeActive = false;\n" +
            "                    btn.textContent = 'Start Monitoring';\n" +
            "                    btn.style.background = '#4CAF50';\n" +
            "                    document.getElementById('realtimePlayerName').disabled = false;\n" +
            "                    document.getElementById('realtimeServerSelect').disabled = false;\n" +
            "                    stopRealtimePolling();\n" +
            "                    document.getElementById('realtimeStatus').innerHTML = '';\n" +
            "                } catch (error) {\n" +
            "                    alert('Error: ' + error.message);\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        function startRealtimePolling() {\n" +
            "            updateRealtimeStatus();\n" +
            "            realtimeInterval = setInterval(updateRealtimeStatus, 5000);\n" +
            "        }\n" +
            "        \n" +
            "        function stopRealtimePolling() {\n" +
            "            if (realtimeInterval) {\n" +
            "                clearInterval(realtimeInterval);\n" +
            "                realtimeInterval = null;\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function updateRealtimeStatus() {\n" +
            "            try {\n" +
            "                const response = await fetch('/api/realtime/status');\n" +
            "                const data = await response.json();\n" +
            "                \n" +
            "                const statusDiv = document.getElementById('realtimeStatus');\n" +
            "                \n" +
            "                if (!data.active) {\n" +
            "                    statusDiv.innerHTML = '';\n" +
            "                    return;\n" +
            "                }\n" +
            "                \n" +
            "                let html = '<div class=\"result\">';\n" +
            "                html += '<strong>Monitoring: ' + data.player + '</strong><br>';\n" +
            "                html += '<span style=\"color: #999; font-size: 0.9em;\">Check interval: 60 seconds</span><br><br>';\n" +
            "                \n" +
            "                if (data.lastCheck) {\n" +
            "                    if (data.lastCheck.success) {\n" +
            "                        const status = data.lastCheck.online ? 'ONLINE' : 'OFFLINE';\n" +
            "                        const color = data.lastCheck.online ? '#4CAF50' : '#f44336';\n" +
            "                        html += '<div style=\"font-size: 1.2em;\">Status: <span style=\"color: ' + color + '; font-weight: bold;\">' + status + '</span></div>';\n" +
            "                        html += '<div>Players online: ' + data.lastCheck.onlineCount + '</div>';\n" +
            "                        if (data.lastCheck.usingQuery) {\n" +
            "                            html += '<div style=\"color: #4CAF50;\">✓ Using Query Protocol</div>';\n" +
            "                        }\n" +
            "                    } else {\n" +
            "                        html += '<div style=\"color: #f44336;\">Server Unreachable</div>';\n" +
            "                        if (data.lastCheck.error) {\n" +
            "                            html += '<div style=\"color: #999; font-size: 0.9em;\">' + data.lastCheck.error + '</div>';\n" +
            "                        }\n" +
            "                    }\n" +
            "                } else {\n" +
            "                    html += '<div class=\"loading\">Waiting for first check...</div>';\n" +
            "                }\n" +
            "                \n" +
            "                html += '</div>';\n" +
            "                statusDiv.innerHTML = html;\n" +
            "            } catch (error) {\n" +
            "                console.error('Failed to update realtime status:', error);\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        loadServers();\n" +
            "        setInterval(loadServers, 30000);\n" +
            "        \n" +
            "        fetch('/api/realtime/status').then(r => r.json()).then(data => {\n" +
            "            if (data.active && data.player) {\n" +
            "                realtimeActive = true;\n" +
            "                document.getElementById('realtimePlayerName').value = data.player;\n" +
            "                document.getElementById('realtimePlayerName').disabled = true;\n" +
            "                document.getElementById('realtimeServerSelect').disabled = true;\n" +
            "                document.getElementById('realtimeBtn').textContent = 'Stop Monitoring';\n" +
            "                document.getElementById('realtimeBtn').style.background = '#f44336';\n" +
            "                startRealtimePolling();\n" +
            "            }\n" +
            "        });\n" +
            "        \n" +
            "        checkRealtimeState();\n" +
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
