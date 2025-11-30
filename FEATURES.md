# Extended Features Summary

## What's New

This extended version adds enterprise-grade features to the basic Minecraft player checker while maintaining full backward compatibility.

## 1. Multi-Server Support

**Configuration:**
```properties
# Primary server (backward compatible)
server.ip=localhost
server.port=25565

# Additional servers
server.1.name=survival
server.1.ip=survival.example.com
server.1.port=25565

server.2.name=creative
server.2.ip=creative.example.com
server.2.port=25566
```

**New Commands:**
- `checkall <player>` - Check player on all servers simultaneously
- `check <player> <server>` - Check player on specific server
- `servers` - List all configured servers
- `status <server>` - Get status of specific server

**Implementation:**
- **MultiServerChecker.java** - Manages parallel checking across servers
- **ServerConfig.java** - Server configuration model
- Uses `ExecutorService` with thread pool for parallel execution
- 10-second timeout per batch operation
- Automatic error handling per server

## 2. Advanced Protocol Support

### Query Protocol
**Automatic detection and fallback:**
1. First check: Attempts Query protocol
2. If successful: Caches that Query is available
3. If fails: Falls back to ServerListPing (no errors shown)
4. Future checks: Uses cached protocol preference

**Query Benefits:**
- Full player list (not just 10-12 sample)
- Server plugins information
- Current map name
- Game version
- More accurate player counts

**Implementation:**
- **MinecraftQuery.java** - UDP-based Query protocol
- **QueryResponse.java** - Extended server data model
- `isQueryEnabled()` static method for detection
- Seamless fallback to ping protocol

## 3. Historical Tracking with Supabase

**Features:**
- Automatic database initialization
- Player online/offline event recording
- Timestamp tracking
- Daily/weekly summaries
- Query by date range

**Setup:**
1. Add Supabase credentials to `.env`
2. Set `history.enabled=true` in config
3. Application auto-creates tables on first run

**Database Schema:**
```sql
CREATE TABLE player_history (
  id SERIAL PRIMARY KEY,
  player_name TEXT NOT NULL,
  server_name TEXT NOT NULL,
  status TEXT NOT NULL,
  timestamp TIMESTAMPTZ DEFAULT NOW(),
  online_count INTEGER,
  query_data TEXT
);
```

**New Commands:**
- `history <player> [days]` - View player activity history
- Shows recent activity and daily summaries

**Implementation:**
- **HistoryService.java** - Database operations and analytics
- **SupabaseClient.java** - HTTP client for Supabase REST API
- **HistoryEntry.java** - History record model
- Loads credentials from `.env` file or environment variables
- Non-blocking: continues if Supabase unavailable

## 4. Discord Webhook Integration

**Features:**
- Player online/offline notifications
- Server outage alerts
- Daily activity summaries
- Rich embeds with colors

**Setup:**
1. Create Discord webhook
2. Add to config: `discord.webhook=https://discord.com/api/webhooks/...`

**Notification Types:**
- **Green**: Player online
- **Red**: Player offline
- **Orange**: Server outage
- **Blue**: Daily summary

**Implementation:**
- **DiscordWebhook.java** - Discord API integration
- Async notifications (non-blocking)
- Automatic JSON formatting
- Error handling for failed webhooks

## 5. Web Dashboard

**Access:** `http://localhost:8080`

**Features:**
- Real-time server status grid
- Manual player checking
- Server selector dropdown
- Auto-refresh every 30 seconds
- Responsive design (mobile-friendly)
- Dark theme

**API Endpoints:**
- `GET /api/servers` - All server statuses (JSON)
- `POST /api/check` - Check player status
- `GET /api/history` - Query player history

**Implementation:**
- **DashboardServer.java** - Embedded HTTP server
- Uses Java's built-in `com.sun.net.httpserver.HttpServer`
- No external dependencies
- HTML/CSS/JavaScript embedded in Java
- Thread-safe with connection pooling

## 6. Batch Player Checking

**Features:**
- Check multiple players from text file
- Table-formatted results
- Works with single or all servers
- Progress indication

**Usage:**
```bash
# Create players.txt
Steve
Alex
Notch

# Run check
checklist players.txt
checklist players.txt survival
```

**Output:**
```
+---------------+----------------+----------------+
| Player Name   | default        | survival       |
+---------------+----------------+----------------+
| Steve         | ONLINE         | OFFLINE        |
| Alex          | OFFLINE        | ONLINE (Q)     |
| Notch         | OFFLINE        | OFFLINE        |
+---------------+----------------+----------------+
```

**Implementation:**
- **ChecklistProcessor.java** - File parsing and table rendering
- Supports comments (lines starting with #)
- Parallel execution across servers
- Dynamic column width adjustment

## Architecture Improvements

### Separation of Concerns
- **Core**: MinecraftPinger, PlayerChecker (backward compatible)
- **Multi-Server**: MultiServerChecker, ServerConfig
- **Protocols**: MinecraftQuery, QueryResponse
- **Plugins**: DiscordWebhook, HistoryService
- **UI**: ConsoleInterface, DashboardServer
- **Database**: SupabaseClient, HistoryEntry

### Error Handling
- Retry logic for transient failures
- Graceful degradation (features fail independently)
- Detailed error messages
- Non-blocking operations

### Performance
- Thread pool for parallel operations
- Connection timeouts (5 seconds)
- Query detection caching
- Efficient JSON parsing

### Backward Compatibility
- All original commands still work
- Original `check <player>` uses default server
- Original `status` shows default server
- Configuration backward compatible

## Configuration Reference

**Complete config.properties:**
```properties
# Primary/Default Server (required)
server.ip=localhost
server.port=25565

# Additional Servers (optional, up to 10)
server.1.name=survival
server.1.ip=survival.example.com
server.1.port=25565

server.2.name=creative
server.2.ip=creative.example.com
server.2.port=25566

# Dashboard (optional)
dashboard.port=8080

# History Tracking (optional)
history.enabled=true

# Discord Webhook (optional)
discord.webhook=https://discord.com/api/webhooks/YOUR_WEBHOOK_URL
```

**Environment Variables (.env):**
```
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key-here
```

## Migration from Basic Version

No changes needed! The basic version commands continue to work:
- `check Steve` - Uses default server
- `status` - Shows default server
- All existing behavior preserved

New features are additive and optional.

## Testing Checklist

- [ ] Single server check (backward compatibility)
- [ ] Multi-server configuration
- [ ] Parallel checking across servers
- [ ] Query protocol auto-detection
- [ ] Ping protocol fallback
- [ ] History recording (if enabled)
- [ ] Discord notifications (if configured)
- [ ] Web dashboard access
- [ ] Batch checklist processing
- [ ] Error handling (offline servers)
- [ ] Timeout handling (slow servers)

## File Count

**Total Java Files:** 17
- 5 Core files (original)
- 4 Protocol files
- 3 Multi-server files
- 5 Feature plugin files

**Lines of Code:** ~3,500+ (estimated)

## Dependencies

**Runtime:** Java JDK 8+
- `com.sun.net.httpserver.*` (built-in, JDK 6+)
- Standard Java libraries only

**Optional:**
- Supabase account (for history)
- Discord webhook (for notifications)

**Zero External JARs Required!**

## Real-Time Player Monitoring

### Overview
Continuously track specific players with automatic notifications when their status changes.

### CLI Mode (1 second interval)
**Start:** `realtime <playername> [server]`
**Stop:** `realtime stop`

Live console output showing:
- Timestamp for each check
- Online/offline status
- Player count
- Query protocol usage
- Server availability

### Dashboard Mode (60 second interval)
**Access:** Web dashboard → Real-Time Monitoring section
- Enter player name and click "Start Monitoring"
- Status updates every 5 seconds on page
- Persists across page refreshes
- Toggle on/off with single button

### Features
- **Independent Operation**: CLI and dashboard modes run simultaneously
- **Thread-Safe**: AtomicBoolean and AtomicReference for state management
- **History Integration**: All checks recorded to database
- **Discord Notifications**: Auto-alerts on status changes
- **Error Recovery**: Continues checking if server becomes unreachable
- **Configurable Intervals**: Adjust check frequency in config.properties

### Configuration
```properties
realtime.cliIntervalSeconds=1
realtime.dashboardIntervalSeconds=60
```

### Use Cases
1. **Wait for player**: Get notified when specific player comes online
2. **Server monitoring**: Track server uptime and availability
3. **Long-term tracking**: Dashboard mode for passive monitoring
4. **Active watching**: CLI mode when you're at the console

### API Endpoints
- `POST /api/realtime/start` - Start dashboard monitoring
- `POST /api/realtime/stop` - Stop dashboard monitoring
- `GET /api/realtime/status` - Get current monitoring status

