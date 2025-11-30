MINECRAFT PLAYER ONLINE CHECKER
================================

A standalone Java console application that checks if a specific player is online on a Minecraft server.

REQUIREMENTS
------------
- Java Development Kit (JDK) 8 or higher
- A Minecraft server to connect to

SETUP
-----
1. Edit the 'config.properties' file with your target server details:
   server.ip=your.minecraft.server.ip
   server.port=25565

2. Compile the application:

   On Linux/Mac:
   ./compile.sh

   On Windows:
   compile.bat

3. Run the application:

   On Linux/Mac:
   ./run.sh

   On Windows:
   run.bat

USAGE
-----
Once the application starts, you can use the following commands:

  check <playername>  - Check if a specific player is online
                        Example: check Steve

  status             - Show server status and online player count

  help               - Show available commands

  exit               - Exit the program

FEATURES
--------
- Implements the Minecraft ServerListPing protocol (handshake + status request)
- Parses the player sample from the server response
- Checks if a given player name appears in the online player list
- Handles connection errors gracefully with user-friendly messages
- Clean, modular code structure with separate classes

ARCHITECTURE
------------
The application is organized into the following classes:

- Main.java            : Entry point that initializes and starts the application
- ConfigLoader.java    : Reads server IP and port from config.properties
- MinecraftPinger.java : Implements the Minecraft ServerListPing protocol
- PlayerChecker.java   : Checks if a player is online using the ping response
- ConsoleInterface.java: Provides the command-line user interface

NOTES
-----
- The player sample returned by the server is limited (usually shows a few players)
- If the server has many players online, not all will appear in the sample
- The application uses no external dependencies for maximum portability
- JSON parsing is done manually to avoid external library requirements

TROUBLESHOOTING
---------------
If you cannot connect to the server:
- Verify the server IP and port in config.properties
- Ensure the Minecraft server is online and accessible
- Check if your firewall allows outgoing connections on the server port
- Confirm the server accepts status queries (most servers do by default)
