# New Features Integration Guide

This document describes the new modules added to the Minecraft Player Online Checker application and how to integrate them with the existing codebase.

## Overview

The following features have been implemented as standalone modules:

1. **Server Performance Monitoring** - Track server health metrics
2. **Player Analytics & Behavior Insights** - Analyze player activity patterns
3. **Live Timeline (Event Stream)** - Real-time event tracking
4. **Multi-Player RealTime Mode** - Monitor multiple players simultaneously
5. **Silent Background RealTime Mode** - Headless monitoring mode

## New Classes Created

### Core Modules

1. **PerformanceMetrics.java**
   - Data class for storing server performance metrics
   - Fields: ping, response time, player count, TPS, CPU, RAM, world info

2. **ServerPerformanceMonitor.java**
   - Tracks and stores performance metrics
   - Maintains rolling history (last 100 checks in memory)
   - Calculates statistics (average ping, TPS, etc.)
   - Integrates with HistoryService for long-term storage

3. **PlayerAnalytics.java**
   - Analyzes player behavior from historical data
   - Calculates session lengths, online time, activity patterns
   - Generates activity heatmaps (hourly distribution)
   - Provides insights: daily/weekly online time, average sessions

4. **LiveTimeline.java**
   - Event stream for real-time activity tracking
   - Records: player join/leave, server up/down, checks
   - Maintains rolling event queue (last 200 events)
   - Persists important events to HistoryService

5. **MultiPlayerRealTimeController.java**
   - Enhanced real-time monitoring supporting multiple players
   - Separate CLI and background modes
   - Concurrent player tracking with individual state management
   - Integrates with LiveTimeline and ServerPerformanceMonitor

## Integration Steps

### Step 1: Update Main.java

Add initialization for new modules:

```java
// After initializing HistoryService
LiveTimeline timeline = new LiveTimeline(historyService);
ServerPerformanceMonitor perfMonitor = new ServerPerformanceMonitor(historyService);
PlayerAnalytics analytics = new PlayerAnalytics(historyService);

// Initialize multi-player controller
MultiPlayerRealTimeController multiRealTime = new MultiPlayerRealTimeController(
    multiServerChecker,
    historyService,
    discord,
    config,
    timeline,
    perfMonitor
);

// Pass new modules to ConsoleInterface
ConsoleInterface console = new ConsoleInterface(
    playerChecker,
    multiServerChecker,
    config,
    historyService,
    discord,
    updateManager,
    realTimeController,
    multiRealTime,  // Add this
    analytics,      // Add this
    timeline,       // Add this
    perfMonitor     // Add this
);

// Pass new modules to DashboardServer
DashboardServer dashboard = new DashboardServer(
    config,
    multiServerChecker,
    historyService,
    realTimeController,
    multiRealTime,  // Add this
    analytics,      // Add this
    timeline,       // Add this
    perfMonitor     // Add this
);
```

### Step 2: Update ConsoleInterface.java

Add new command handlers to the `processCommand()` method:

```java
case "analytics":
    if (parts.length < 2) {
        System.out.println("Error: Please provide a player name.");
        System.out.println("Usage: analytics <playername>");
    } else {
        showAnalytics(parts[1]);
    }
    break;

case "rtadd":
    if (parts.length < 2) {
        System.out.println("Error: Please provide a player name.");
        System.out.println("Usage: rtadd <playername> [server]");
    } else {
        String[] args = parts[1].split("\\s+", 2);
        String playerName = args[0];
        String serverName = args.length > 1 ? args[1] : null;
        multiRealTime.addCliPlayer(playerName, serverName);
    }
    break;

case "rtremove":
    if (parts.length < 2) {
        System.out.println("Error: Please provide a player name.");
        System.out.println("Usage: rtremove <playername>");
    } else {
        multiRealTime.removeCliPlayer(parts[1]);
    }
    break;

case "rtlist":
    listRealTimePlayers();
    break;

case "rtbackground":
    if (parts.length < 2) {
        System.out.println("Error: Please provide player names.");
        System.out.println("Usage: rtbackground <player1> [player2] ...");
    } else {
        String[] playerNames = parts[1].split("\\s+");
        Set<String> players = new HashSet<>(Arrays.asList(playerNames));
        multiRealTime.startBackgroundMonitoring(players);
        System.out.println("Background monitoring started for " + players.size() + " player(s)");
    }
    break;

case "rtstop":
    if (multiRealTime.isBackgroundActive()) {
        multiRealTime.stopBackgroundMonitoring();
        System.out.println("Background monitoring stopped");
    } else {
        System.out.println("No background monitoring is active");
    }
    break;

case "timeline":
    if (parts.length > 1) {
        int count = Integer.parseInt(parts[1]);
        showTimeline(count);
    } else {
        showTimeline(20);
    }
    break;

case "perfstats":
    if (parts.length > 1) {
        showPerformanceStats(parts[1]);
    } else {
        showAllPerformanceStats();
    }
    break;
```

Add corresponding method implementations:

```java
private void showAnalytics(String playerName) {
    PlayerAnalytics.PlayerInsights insights = analytics.getInsights(playerName);
    if (insights == null) {
        System.out.println("No data available for " + playerName);
        return;
    }

    System.out.println(insights.toString());
    System.out.println(analytics.getActivityHeatmap(playerName));
}

private void listRealTimePlayers() {
    List<String> cliPlayers = multiRealTime.listCliPlayers();
    List<String> bgPlayers = multiRealTime.listBackgroundPlayers();

    System.out.println("\nReal-Time Monitored Players:");
    System.out.println("CLI Mode (" + cliPlayers.size() + "):");
    for (String player : cliPlayers) {
        System.out.println("  - " + player);
    }

    System.out.println("\nBackground Mode (" + bgPlayers.size() + "):");
    for (String player : bgPlayers) {
        System.out.println("  - " + player);
    }
    System.out.println();
}

private void showTimeline(int count) {
    List<LiveTimeline.TimelineEvent> events = timeline.getRecentEvents(count);
    System.out.println("\nRecent Events:");
    System.out.println("=".repeat(60));
    for (LiveTimeline.TimelineEvent event : events) {
        System.out.println(event.toString());
    }
    System.out.println("=".repeat(60));
    System.out.println();
}

private void showPerformanceStats(String serverName) {
    PerformanceMetrics latest = perfMonitor.getLatest(serverName);
    if (latest == null) {
        System.out.println("No performance data for " + serverName);
        return;
    }

    System.out.println(latest.toString());

    ServerPerformanceMonitor.PerformanceStats stats = perfMonitor.getStats(serverName, 10);
    if (stats != null) {
        System.out.println(stats.toString());
    }
}

private void showAllPerformanceStats() {
    Map<String, PerformanceMetrics> allMetrics = perfMonitor.getLatestForAllServers();
    System.out.println("\nServer Performance Overview:");
    System.out.println("=".repeat(60));
    for (Map.Entry<String, PerformanceMetrics> entry : allMetrics.entrySet()) {
        System.out.println(entry.getValue().toString());
    }
    System.out.println("=".repeat(60));
}
```

Update the help/welcome message:

```java
System.out.println("  analytics <player>             - Show player activity analytics");
System.out.println("  rtadd <player> [server]        - Add player to real-time monitoring");
System.out.println("  rtremove <player>              - Remove player from monitoring");
System.out.println("  rtlist                         - List monitored players");
System.out.println("  rtbackground <player(s)>       - Start background monitoring");
System.out.println("  rtstop                         - Stop background monitoring");
System.out.println("  timeline [count]               - Show recent events");
System.out.println("  perfstats [server]             - Show performance statistics");
```

### Step 3: Update DashboardServer.java

Add new REST endpoints in the `createContext()` calls:

```java
// Analytics endpoint
server.createContext("/analytics", exchange -> {
    String query = exchange.getRequestURI().getQuery();
    String playerName = extractParam(query, "player");

    if (playerName == null) {
        sendJsonResponse(exchange, 400, "{\"error\":\"Missing player parameter\"}");
        return;
    }

    PlayerAnalytics.PlayerInsights insights = analytics.getInsights(playerName);
    if (insights == null) {
        sendJsonResponse(exchange, 404, "{\"error\":\"No data found\"}");
        return;
    }

    String json = insightsToJson(insights);
    sendJsonResponse(exchange, 200, json);
});

// Timeline endpoint
server.createContext("/timeline", exchange -> {
    List<LiveTimeline.TimelineEvent> events = timeline.getRecentEvents(50);
    String json = timelineToJson(events);
    sendJsonResponse(exchange, 200, json);
});

// Performance metrics endpoint
server.createContext("/performance", exchange -> {
    String query = exchange.getRequestURI().getQuery();
    String serverName = extractParam(query, "server");

    if (serverName == null) {
        Map<String, PerformanceMetrics> allMetrics = perfMonitor.getLatestForAllServers();
        String json = allMetricsToJson(allMetrics);
        sendJsonResponse(exchange, 200, json);
    } else {
        PerformanceMetrics metrics = perfMonitor.getLatest(serverName);
        if (metrics == null) {
            sendJsonResponse(exchange, 404, "{\"error\":\"No data found\"}");
            return;
        }
        String json = metricsToJson(metrics);
        sendJsonResponse(exchange, 200, json);
    }
});

// Multi-player realtime endpoints
server.createContext("/realtime/multi/add", exchange -> {
    String query = exchange.getRequestURI().getQuery();
    String playerName = extractParam(query, "player");
    String serverName = extractParam(query, "server");

    boolean success = multiRealTime.addCliPlayer(playerName, serverName);
    sendJsonResponse(exchange, 200, "{\"success\":" + success + "}");
});

server.createContext("/realtime/multi/remove", exchange -> {
    String query = exchange.getRequestURI().getQuery();
    String playerName = extractParam(query, "player");

    boolean success = multiRealTime.removeCliPlayer(playerName);
    sendJsonResponse(exchange, 200, "{\"success\":" + success + "}");
});

server.createContext("/realtime/multi/list", exchange -> {
    List<String> cliPlayers = multiRealTime.listCliPlayers();
    List<String> bgPlayers = multiRealTime.listBackgroundPlayers();

    String json = "{\"cli\":" + listToJson(cliPlayers) +
                  ",\"background\":" + listToJson(bgPlayers) +
                  ",\"cliActive\":" + multiRealTime.isCliActive() +
                  ",\"backgroundActive\":" + multiRealTime.isBackgroundActive() + "}";
    sendJsonResponse(exchange, 200, json);
});

server.createContext("/realtime/background/start", exchange -> {
    String query = exchange.getRequestURI().getQuery();
    String playersParam = extractParam(query, "players");

    if (playersParam == null) {
        sendJsonResponse(exchange, 400, "{\"error\":\"Missing players parameter\"}");
        return;
    }

    String[] playerNames = playersParam.split(",");
    Set<String> players = new HashSet<>(Arrays.asList(playerNames));
    boolean success = multiRealTime.startBackgroundMonitoring(players);
    sendJsonResponse(exchange, 200, "{\"success\":" + success + "}");
});

server.createContext("/realtime/background/stop", exchange -> {
    boolean success = multiRealTime.stopBackgroundMonitoring();
    sendJsonResponse(exchange, 200, "{\"success\":" + success + "}");
});
```

Add helper methods for JSON conversion:

```java
private String insightsToJson(PlayerAnalytics.PlayerInsights insights) {
    return String.format(
        "{\"playerName\":\"%s\",\"totalOnlineTime\":%d,\"dailyOnlineTime\":%d," +
        "\"weeklyOnlineTime\":%d,\"sessionCount\":%d,\"averageSessionLength\":%d," +
        "\"longestSession\":%d,\"hourlyActivity\":%s}",
        insights.getPlayerName(),
        insights.getTotalOnlineTime(),
        insights.getDailyOnlineTime(),
        insights.getWeeklyOnlineTime(),
        insights.getSessionCount(),
        insights.getAverageSessionLength(),
        insights.getLongestSession(),
        mapToJson(insights.getHourlyActivity())
    );
}

private String timelineToJson(List<LiveTimeline.TimelineEvent> events) {
    StringBuilder json = new StringBuilder("[");
    for (int i = 0; i < events.size(); i++) {
        LiveTimeline.TimelineEvent event = events.get(i);
        if (i > 0) json.append(",");
        json.append(String.format(
            "{\"timestamp\":%d,\"type\":\"%s\",\"playerName\":\"%s\"," +
            "\"serverName\":\"%s\",\"message\":\"%s\"}",
            event.getTimestamp(),
            event.getType().toString(),
            event.getPlayerName() != null ? event.getPlayerName() : "",
            event.getServerName() != null ? event.getServerName() : "",
            event.getMessage()
        ));
    }
    json.append("]");
    return json.toString();
}

private String metricsToJson(PerformanceMetrics metrics) {
    return String.format(
        "{\"serverName\":\"%s\",\"timestamp\":%d,\"pingLatency\":%d," +
        "\"responseTime\":%d,\"playerCount\":%d,\"tps\":%s,\"cpuLoad\":%s," +
        "\"ramUsage\":%s,\"ramMax\":%s,\"worldInfo\":\"%s\"}",
        metrics.getServerName(),
        metrics.getTimestamp(),
        metrics.getPingLatency(),
        metrics.getResponseTime(),
        metrics.getPlayerCount(),
        metrics.getTps() != null ? metrics.getTps() : "null",
        metrics.getCpuLoad() != null ? metrics.getCpuLoad() : "null",
        metrics.getRamUsage() != null ? metrics.getRamUsage() : "null",
        metrics.getRamMax() != null ? metrics.getRamMax() : "null",
        metrics.getWorldInfo() != null ? metrics.getWorldInfo() : ""
    );
}
```

### Step 4: Integrate Performance Monitoring into Checks

In `MultiServerChecker.java`, after each server check, record performance metrics:

```java
// After getting server status
if (perfMonitor != null && status.isOnline()) {
    QueryResponse queryResponse = status.getQueryResponse();
    perfMonitor.recordMetrics(server.getName(), status, queryResponse);
}
```

### Step 5: Integrate Timeline Events

In the check methods, add timeline events:

```java
// When a player status changes
if (timeline != null) {
    if (previousResult != null && previousResult.isSuccess()) {
        if (!previousResult.isOnline() && result.isOnline()) {
            timeline.recordPlayerJoin(playerName, serverName);
        } else if (previousResult.isOnline() && !result.isOnline()) {
            timeline.recordPlayerLeave(playerName, serverName);
        }
    }
}

// When server status changes
if (timeline != null) {
    if (previousStatus != null) {
        if (!previousStatus.isOnline() && status.isOnline()) {
            timeline.recordServerUp(serverName);
        } else if (previousStatus.isOnline() && !status.isOnline()) {
            timeline.recordServerDown(serverName);
        }
    }
}
```

## Dashboard UI Updates

### Timeline Widget

Add to the dashboard HTML:

```html
<div class="timeline-widget">
    <h2>Live Timeline</h2>
    <div id="timeline-events"></div>
</div>

<script>
async function loadTimeline() {
    const response = await fetch('/timeline');
    const events = await response.json();

    const container = document.getElementById('timeline-events');
    container.innerHTML = '';

    events.reverse().forEach(event => {
        const div = document.createElement('div');
        div.className = 'timeline-event ' + event.type.toLowerCase();
        div.innerHTML = `
            <span class="time">${formatTime(event.timestamp)}</span>
            <span class="server">${event.serverName}</span>
            <span class="message">${event.message}</span>
        `;
        container.appendChild(div);
    });
}

setInterval(loadTimeline, 5000);
loadTimeline();
</script>
```

### Analytics Page

Add analytics dashboard page with charts showing:
- Session length distribution
- Hourly activity heatmap
- Weekly online time trends
- Player comparison charts

### Multi-Player RealTime Panel

Add UI for managing multiple monitored players:
- List of active monitors
- Add/remove buttons
- Status indicators per player
- Background mode toggle

## Testing

1. **Performance Monitoring**: Run `perfstats` command after several checks
2. **Analytics**: Use `analytics <player>` after collecting history data
3. **Timeline**: Run `timeline` to see recent events
4. **Multi-Player**: Use `rtadd`, `rtremove`, `rtlist` commands
5. **Background Mode**: Test `rtbackground` and `rtstop` commands
6. **Dashboard**: Access `/analytics`, `/timeline`, `/performance` endpoints

## Non-Breaking Changes

All new features are:
- In separate class files
- Optional (system works without them)
- Integrated via dependency injection
- Thread-safe with proper concurrency controls
- Backward compatible with existing code

## Performance Considerations

- Rolling histories are memory-bound (100-200 items max)
- Background checks use timers efficiently
- All concurrent collections are thread-safe
- Performance metrics parsing is fault-tolerant

## Future Enhancements

- Export analytics to CSV/JSON
- Configurable history sizes
- Alert thresholds for performance metrics
- Advanced filtering on timeline
- Supabase integration for persistent storage of all metrics
