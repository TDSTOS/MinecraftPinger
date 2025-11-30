import java.util.ArrayList;
import java.util.List;

public class QueryResponse {
    private String motd;
    private String gameType;
    private String gameId;
    private String version;
    private String plugins;
    private String map;
    private int onlinePlayers;
    private int maxPlayers;
    private final List<String> playerList;

    public QueryResponse() {
        this.playerList = new ArrayList<>();
    }

    public String getMotd() {
        return motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPlugins() {
        return plugins;
    }

    public void setPlugins(String plugins) {
        this.plugins = plugins;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    public void setOnlinePlayers(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public List<String> getPlayerList() {
        return playerList;
    }

    public void addPlayer(String playerName) {
        this.playerList.add(playerName);
    }

    public boolean hasPlayer(String playerName) {
        for (String player : playerList) {
            if (player.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }
}
