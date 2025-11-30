# Quick Reference Guide - New Features

## New CLI Commands

### Player Analytics
```bash
analytics <playername>
# Shows: total/daily/weekly online time, session stats, activity heatmap
# Example: analytics Steve
```

### Multi-Player Real-Time Monitoring
```bash
rtadd <playername> [server]      # Add player to monitoring
rtremove <playername>            # Remove player from monitoring
rtlist                           # List all monitored players
# Example: rtadd Steve MyServer
```

### Background Monitoring (Silent Mode)
```bash
rtbackground <player1> [player2] [player3] ...    # Start background mode
rtstop                                             # Stop background mode
# Example: rtbackground Steve Alex Notch
```

### Live Timeline
```bash
timeline            # Show last 20 events
timeline <count>    # Show last N events
# Example: timeline 50
```

### Performance Statistics
```bash
perfstats              # Show all servers performance
perfstats <server>     # Show specific server
# Example: perfstats MyServer
```

---

## New Dashboard API Endpoints

### Player Analytics
```
GET /analytics?player=<playername>
Returns: JSON with session stats, online times, hourly activity
```

### Live Timeline
```
GET /timeline
Returns: JSON array of recent events (last 50)
```

### Performance Metrics
```
GET /performance
Returns: JSON with all servers' latest metrics

GET /performance?server=<servername>
Returns: JSON with specific server metrics
```

### Multi-Player Real-Time
```
POST /realtime/multi/add?player=<name>&server=<server>
POST /realtime/multi/remove?player=<name>
GET  /realtime/multi/list
Returns: {cli: [...], background: [...], cliActive: bool, backgroundActive: bool}
```

### Background Monitoring
```
POST /realtime/background/start?players=<name1>,<name2>,<name3>
POST /realtime/background/stop
```

---

## New Classes Overview

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `PerformanceMetrics` | Data class for metrics | Getters/setters for ping, TPS, CPU, RAM |
| `ServerPerformanceMonitor` | Track server health | `recordMetrics()`, `getHistory()`, `getStats()` |
| `PlayerAnalytics` | Analyze player behavior | `getInsights()`, `getActivityHeatmap()` |
| `LiveTimeline` | Event stream manager | `recordPlayerJoin()`, `getRecentEvents()` |
| `MultiPlayerRealTimeController` | Multi-player monitoring | `addCliPlayer()`, `startBackgroundMonitoring()` |

---

## Integration Checklist

### Main.java
- [ ] Initialize `LiveTimeline`
- [ ] Initialize `ServerPerformanceMonitor`
- [ ] Initialize `PlayerAnalytics`
- [ ] Initialize `MultiPlayerRealTimeController`
- [ ] Pass to `ConsoleInterface` constructor
- [ ] Pass to `DashboardServer` constructor

### ConsoleInterface.java
- [ ] Add fields for new modules
- [ ] Add cases in `processCommand()` switch
- [ ] Implement handler methods
- [ ] Update help text in `printWelcome()`

### DashboardServer.java
- [ ] Add fields for new modules
- [ ] Create contexts for new endpoints
- [ ] Implement JSON conversion helpers
- [ ] Update dashboard HTML with new widgets

### MultiServerChecker.java
- [ ] Call `perfMonitor.recordMetrics()` after checks
- [ ] Call `timeline.recordPlayerJoin/Leave()` on state changes
- [ ] Call `timeline.recordServerUp/Down()` on server changes

---

## Code Snippets

### Initialize in Main.java
```java
// After historyService initialization
LiveTimeline timeline = new LiveTimeline(historyService);
ServerPerformanceMonitor perfMonitor = new ServerPerformanceMonitor(historyService);
PlayerAnalytics analytics = new PlayerAnalytics(historyService);

MultiPlayerRealTimeController multiRealTime = new MultiPlayerRealTimeController(
    multiServerChecker,
    historyService,
    discord,
    config,
    timeline,
    perfMonitor
);

// Pass to ConsoleInterface
ConsoleInterface console = new ConsoleInterface(
    playerChecker,
    multiServerChecker,
    config,
    historyService,
    discord,
    updateManager,
    realTimeController,
    multiRealTime,    // Add
    analytics,        // Add
    timeline,         // Add
    perfMonitor       // Add
);

// Pass to DashboardServer
DashboardServer dashboard = new DashboardServer(
    config,
    multiServerChecker,
    historyService,
    realTimeController,
    multiRealTime,    // Add
    analytics,        // Add
    timeline,         // Add
    perfMonitor       // Add
);
```

### Record Performance Metrics
```java
// In MultiServerChecker after getting status
if (perfMonitor != null && status.isOnline()) {
    QueryResponse qr = status.getQueryResponse();
    perfMonitor.recordMetrics(server.getName(), status, qr);
}
```

### Record Timeline Events
```java
// When player status changes
if (timeline != null) {
    if (previousResult != null && previousResult.isSuccess()) {
        if (!previousResult.isOnline() && result.isOnline()) {
            timeline.recordPlayerJoin(playerName, serverName);
        } else if (previousResult.isOnline() && !result.isOnline()) {
            timeline.recordPlayerLeave(playerName, serverName);
        }
    }
}
```

### Add Console Commands
```java
case "analytics":
    if (parts.length < 2) {
        System.out.println("Usage: analytics <playername>");
    } else {
        showAnalytics(parts[1]);
    }
    break;

case "rtadd":
    if (parts.length < 2) {
        System.out.println("Usage: rtadd <playername> [server]");
    } else {
        String[] args = parts[1].split("\\s+", 2);
        multiRealTime.addCliPlayer(args[0], args.length > 1 ? args[1] : null);
    }
    break;

case "rtremove":
    if (parts.length < 2) {
        System.out.println("Usage: rtremove <playername>");
    } else {
        multiRealTime.removeCliPlayer(parts[1]);
    }
    break;

case "rtlist":
    listRealTimePlayers();
    break;

case "timeline":
    int count = 20;
    if (parts.length > 1) {
        try {
            count = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {}
    }
    showTimeline(count);
    break;

case "perfstats":
    if (parts.length > 1) {
        showPerformanceStats(parts[1]);
    } else {
        showAllPerformanceStats();
    }
    break;
```

---

## Testing Commands Sequence

```bash
# 1. Start application
java Main

# 2. Test performance monitoring
> perfstats
# Should show empty or initial data

# 3. Run some checks to generate data
> check Steve
> status

# 4. Check performance again
> perfstats MyServer
# Should show metrics from recent checks

# 5. Test multi-player monitoring
> rtadd Steve
> rtadd Alex
> rtlist
# Should show both players

# 6. Wait a few seconds, then check timeline
> timeline
# Should show check events

# 7. Remove one player
> rtremove Alex
> rtlist
# Should only show Steve

# 8. Test background mode
> rtbackground Bob Charlie
# Should start silently

> rtlist
# Should show background players

> rtstop
# Should stop background mode

# 9. Test analytics (requires historical data)
> analytics Steve
# Shows session stats and heatmap

# 10. Clean shutdown
> exit
```

---

## Dashboard Testing URLs

```
http://localhost:8080/                          # Main dashboard
http://localhost:8080/analytics?player=Steve    # Player analytics
http://localhost:8080/timeline                  # Recent events
http://localhost:8080/performance               # All servers metrics
http://localhost:8080/performance?server=MyServer  # Specific server
http://localhost:8080/realtime/multi/list       # RealTime status
```

---

## Common Issues & Solutions

### Issue: "Module not initialized"
**Solution**: Ensure all new modules are initialized in `Main.java` before use

### Issue: Analytics shows "No data available"
**Solution**: Need historical data first. Run checks and wait, or use existing history

### Issue: Performance metrics show null values
**Solution**: Server must support Query protocol for TPS/CPU/RAM data

### Issue: Timeline is empty
**Solution**: Events are only recorded during active monitoring or checks

### Issue: Multi-player mode not working
**Solution**: Verify `MultiPlayerRealTimeController` is initialized and passed correctly

---

## Feature Flags / Configuration

Currently, all features are automatically enabled if modules are initialized. Future enhancement could add to `config.properties`:

```properties
# Future configuration options
features.performance_monitoring=true
features.player_analytics=true
features.live_timeline=true
features.multi_player_realtime=true
features.background_monitoring=true

# Performance settings
performance.history_size=100
timeline.max_events=200
analytics.cache_enabled=true
```

---

## Performance Characteristics

| Feature | Memory Usage | CPU Usage | Storage |
|---------|-------------|-----------|---------|
| Performance Monitor | ~10KB per server (100 metrics) | Low | Via HistoryService |
| Player Analytics | ~1KB per player (cached) | Low (on-demand) | Reads from History |
| Live Timeline | ~20KB (200 events) | Very Low | Events to History |
| Multi-Player RealTime | ~1KB per player | Medium (active checks) | Via HistoryService |
| Background Monitoring | ~1KB per player | Low (60s interval) | Via HistoryService |

**Total Additional Memory**: ~50-100KB for typical usage (5 players, 3 servers)

---

## Architecture Summary

```
┌─────────────────────────────────────────────────────┐
│                    Main.java                         │
│  (Initializes and wires all components)             │
└─────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Console    │  │   Dashboard  │  │   Checkers   │
│  Interface   │  │    Server    │  │              │
└──────────────┘  └──────────────┘  └──────────────┘
        │                 │                 │
        └─────────────────┼─────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Performance │  │   Analytics  │  │   Timeline   │
│   Monitor    │  │              │  │              │
└──────────────┘  └──────────────┘  └──────────────┘
        │                 │                 │
        └─────────────────┼─────────────────┘
                          │
                          ▼
                ┌──────────────────┐
                │  HistoryService  │
                │   (Persistent)   │
                └──────────────────┘
```

---

## Support & Documentation

- **Full Integration Guide**: `NEW_FEATURES_INTEGRATION.md`
- **Feature Summary**: `FEATURE_SUMMARY.md`
- **This Quick Reference**: `QUICK_REFERENCE.md`
- **Main README**: `README.md`
- **Troubleshooting**: `TROUBLESHOOTING.md`

---

## Version Information

These features were added in version **v1.0.2** and are backward compatible with all v1.0.x releases.

For the latest updates and documentation, visit the project repository.
