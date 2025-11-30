# Minecraft Player Online Checker

A standalone Java console application that checks if a specific player is online on a Minecraft server.

## Requirements

- Java Development Kit (JDK) 8 or higher
- A Minecraft server to connect to

## Quick Start

### 1. Configure the Server

Edit `config.properties` with your Minecraft server details:

```properties
server.ip=your.minecraft.server.ip
server.port=25565
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

## Usage

Once running, use these commands:

- `check <playername>` - Check if a player is online
  - Example: `check Steve`
- `status` - Show server status and player count
- `help` - Show available commands
- `exit` - Exit the program

## Features

- Implements Minecraft ServerListPing protocol
- Parses player list from server response
- Graceful error handling
- No external dependencies
- Clean modular architecture

## Architecture

- **Main.java** - Application entry point
- **ConfigLoader.java** - Reads configuration
- **MinecraftPinger.java** - ServerListPing protocol implementation
- **PlayerChecker.java** - Player online status checking
- **ConsoleInterface.java** - Console UI

## Troubleshooting

**Command window closes immediately:**
- The batch/shell scripts now include pause commands to keep the window open
- Check for error messages before the window closes

**Cannot connect to server:**
- Verify server IP and port in config.properties
- Ensure the Minecraft server is online
- Check firewall settings

**Compilation fails:**
- Ensure Java JDK is installed (not just JRE)
- Verify `javac` is in your system PATH
- Run `javac -version` to check installation
