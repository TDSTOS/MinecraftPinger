# Minecraft Player Online Checker

A comprehensive Java application that monitors Minecraft server status and player activity with multi-server support, historical tracking, Discord notifications, and a web dashboard.

## Requirements

**Core Requirements:**
- Java Development Kit (JDK) 8 or higher
- A Minecraft server to connect to

**Optional Features:**
- Supabase account (for historical tracking - disabled by default)
- Discord webhook (for notifications - optional)

## Quick Start

**Important:** The application works out-of-the-box without any optional features. If you see "History tracking: DISABLED" or "Discord notifications: DISABLED", this is normal and expected. These are optional enhancements.

### 1. Configure Servers

Edit `config.properties`:

```properties
# Primary/Default Server
server.ip=localhost
server.port=25565

# Additional Servers (optional)
server.1.name=survival
server.1.ip=play.example.com
server.1.port=25565

server.2.name=creative
server.2.ip=creative.example.com
server.2.port=25566

# Dashboard
dashboard.port=8080

# History Tracking (requires Supabase)
history.enabled=true

# Discord Notifications (optional)
discord.webhook=https://discord.com/api/webhooks/...
```

### 2. Compile and Run

**Windows:**
```cmd
compile.bat
run.bat
```

**Linux/Mac:**
```bash
./compile.sh
./run.sh
```

## Console Commands

- `check <playername> [server]` - Check if a player is online on specific server
  - Example: `check Steve survival`
- `checkall <playername>` - Check player on all configured servers
- `checklist <file> [server]` - Batch check multiple players from a file
  - Example: `checklist players.txt` or `checklist players.txt survival`
- `status [server]` - Show server status (all servers if no name provided)
- `servers` - List all configured servers
- `history <playername> [days]` - Show player history (requires history tracking)
  - Example: `history Steve 7`
- `help` - Show available commands
- `exit` - Exit the program

## Web Dashboard

Access the web dashboard at `http://localhost:8080` (or your configured port) to:
- View real-time server status
- Check player status via web interface
- Monitor all servers at once
- Auto-refresh every 30 seconds

## Features

### Multi-Server Support
- Configure up to 10 servers in `config.properties`
- Check players across all servers in parallel
- Individual or batch server queries

### Advanced Protocol Detection
- **ServerListPing**: Basic player sample (limited visibility)
- **Query Protocol**: Full player list, plugins, map info, version
- **Auto-Detection**: Automatically detects and uses Query when available
- **Fallback**: Gracefully falls back to ping if Query is disabled

### Historical Tracking
- Stores player online/offline events in Supabase database
- View player activity history with timestamps
- Daily summaries showing online frequency
- Persistent across application restarts

### Discord Notifications
- Player online/offline alerts
- Server outage notifications
- Scheduled daily summaries
- Customizable webhook integration

### Batch Processing
- Create a text file with player names (one per line)
- Check multiple players at once
- Results displayed in clean table format
- Works with single or all servers

Example `players.txt`:
```
Steve
Alex
Notch
```

### Web Dashboard
- Embedded HTTP server (no external dependencies)
- Real-time server status monitoring
- Manual player checks via web interface
- Auto-refresh capability
- Responsive design

## Architecture

### Core Components
- **Main.java** - Application entry point and lifecycle management
- **ConfigLoader.java** - Multi-server configuration loader
- **ServerConfig.java** - Server configuration model

### Protocol Implementation
- **MinecraftPinger.java** - ServerListPing protocol
- **MinecraftQuery.java** - Query protocol with auto-detection
- **QueryResponse.java** - Extended server information model

### Multi-Server Management
- **MultiServerChecker.java** - Parallel server checking with ExecutorService
- **PlayerCheckResult.java** - Player check result model
- **ServerStatus.java** - Server status model

### Features
- **PlayerChecker.java** - Basic player checking (backward compatible)
- **ChecklistProcessor.java** - Batch player checking with table output
- **HistoryService.java** - Supabase integration for historical tracking
- **DiscordWebhook.java** - Discord notification plugin
- **DashboardServer.java** - Embedded HTTP server with REST API

### Database
- **SupabaseClient.java** - HTTP client for Supabase REST API
- **HistoryEntry.java** - History record model

### User Interface
- **ConsoleInterface.java** - Enhanced CLI with new commands

## Configuration Examples

### Multi-Server Setup
```properties
server.ip=hub.example.com
server.port=25565

server.1.name=survival
server.1.ip=survival.example.com
server.1.port=25565

server.2.name=creative
server.2.ip=creative.example.com
server.2.port=25566

server.3.name=minigames
server.3.ip=minigames.example.com
server.3.port=25567
```

### History Tracking Setup

**Note:** History tracking is OPTIONAL and disabled by default (`history.enabled=false`).

To enable history tracking:

1. Create a Supabase account at https://supabase.com
2. Create a new project
3. Create the required table in Supabase SQL Editor:
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

CREATE INDEX idx_player_timestamp ON player_history(player_name, timestamp DESC);
```
4. Add credentials to `.env` file:
```
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
```
5. Enable in config: `history.enabled=true`

### Discord Notifications Setup
1. Create a Discord webhook in your server settings
2. Add to `config.properties`:
```properties
discord.webhook=https://discord.com/api/webhooks/123456789/abcdefghijk
```

## Query Protocol

The application automatically detects whether a server has Query protocol enabled:

**Query Enabled:**
- Full player list (not just sample)
- Server plugins information
- Current map name
- Game version details
- More accurate player counts

**Query Disabled:**
- Falls back to standard ServerListPing
- Limited to player sample (usually 10-12 players)
- No error messages, seamless fallback

To enable Query on your Minecraft server, add to `server.properties`:
```properties
enable-query=true
query.port=25565
```

## Troubleshooting

**Command window closes immediately:**
- Scripts now include pause commands to keep window open
- Check for error messages before window closes

**Cannot connect to server:**
- Verify server IP and port in config.properties
- Ensure the Minecraft server is online
- Check firewall settings
- Try ping protocol first (Query may be disabled)

**History tracking not working:**
- Ensure Supabase credentials are in `.env` file
- Check that `history.enabled=true` in config
- Verify network connection to Supabase
- Check console for initialization messages

**Dashboard won't start:**
- Ensure port 8080 (or configured port) is available
- Check for port conflicts with other applications
- Application will continue without dashboard if it fails

**Query not working:**
- Query protocol may be disabled on the server
- Application automatically falls back to ping
- Enable Query in server's `server.properties` if you have access

**Compilation fails:**
- Ensure Java JDK 8+ is installed (not just JRE)
- Verify `javac` is in your system PATH
- Run `javac -version` to check installation
- Note: The application uses Java's built-in HTTP server (JDK 8+)

## Advanced Usage

### Monitoring Multiple Players
Create a text file with player names and use checklist command:
```
check list players.txt
```

Output shows a table with all players and their status across all servers.

### Automated Monitoring
The application can be run with scheduled tasks:
- Windows: Task Scheduler
- Linux/Mac: cron jobs

### API Endpoints

The dashboard exposes REST endpoints:

- `GET /api/servers` - Get all server statuses
- `POST /api/check` - Check player status
  - Body: `{"playerName": "Steve", "serverName": "survival"}`
- `GET /api/history?player=Steve&days=7` - Get player history

## Performance

- Multi-threaded server checking (up to 10 concurrent)
- 5-second timeout per server query
- Automatic Query detection cached per server
- Efficient parallel processing for multiple players
