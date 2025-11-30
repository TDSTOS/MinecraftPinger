# Troubleshooting Guide

## Supabase Connection Issues

### Problem: "Cannot connect to Supabase" or "History tracking disabled"

**Solution:**

History tracking is an **OPTIONAL** feature and the application works perfectly without it.

If you see warnings about Supabase:
1. **Option 1 (Recommended):** Leave history disabled
   - Set `history.enabled=false` in `config.properties` (already the default)
   - The application will run without any issues

2. **Option 2:** Enable history tracking properly
   - Create a Supabase account at https://supabase.com
   - Create a new project
   - Run this SQL in the Supabase SQL Editor:
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
   - Create `.env` file with your credentials:
   ```
   VITE_SUPABASE_URL=https://your-project.supabase.co
   VITE_SUPABASE_ANON_KEY=your-anon-key
   ```
   - Set `history.enabled=true` in `config.properties`

### What History Tracking Does

When enabled, history tracking:
- Records when players go online/offline
- Stores timestamps in Supabase database
- Allows viewing player activity history
- Provides daily/weekly summaries

When disabled:
- **All other features work normally**
- No database connection required
- No performance impact
- The `history` command will inform you it's disabled

## Common Issues

### Application works but shows "History tracking: DISABLED"

This is **normal** and **expected** if you haven't set up Supabase. The application is working correctly.

### Dashboard won't start

**Symptoms:** Warning message about dashboard port

**Solutions:**
- Check if port 8080 is already in use
- Change `dashboard.port` in config.properties to another port (e.g., 8081)
- The application continues to work via console even if dashboard fails

### Cannot connect to Minecraft server

**Solutions:**
- Verify server IP and port in `config.properties`
- Ensure the Minecraft server is online
- Check firewall settings
- Try with Query disabled servers (automatic fallback)

### Compilation errors

**Solutions:**
- Ensure JDK 8 or higher is installed (not just JRE)
- Verify `javac` is in your PATH: `javac -version`
- Delete `bin` folder and recompile

### Command window closes immediately

**Solution:**
- The batch files now include `pause` commands
- If it still closes, run from command prompt to see errors

## Feature Status Indicators

When the application starts, you'll see:

```
History tracking: DISABLED (optional feature)
Discord notifications: DISABLED (optional feature)
Dashboard server started on http://localhost:8080
```

**This is normal!** The application is fully functional.

Only these core features are required:
- ConfigLoader ✓
- MinecraftPinger ✓
- PlayerChecker ✓
- Console Interface ✓

All other features (History, Discord, Dashboard) are optional enhancements.

## Testing Without Optional Features

Minimum working configuration (`config.properties`):
```properties
server.ip=localhost
server.port=25565
```

This is enough to:
- Check if players are online
- View server status
- Use all console commands
- Check multiple servers (if configured)

## Getting Help

If problems persist:
1. Check console output for specific error messages
2. Verify Java version: `java -version` (should be 8+)
3. Test with default configuration first
4. Add features one at a time to isolate issues
