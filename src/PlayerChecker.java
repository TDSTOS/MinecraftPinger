import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerChecker {
    private MinecraftPinger pinger;
    private String lastResponse;

    public PlayerChecker(MinecraftPinger pinger) {
        this.pinger = pinger;
    }

    public boolean isPlayerOnline(String playerName) throws IOException {
        lastResponse = pinger.ping();
        return checkPlayerInResponse(playerName, lastResponse);
    }

    private boolean checkPlayerInResponse(String playerName, String jsonResponse) {
        List<String> players = extractPlayerNames(jsonResponse);

        for (String player : players) {
            if (player.equalsIgnoreCase(playerName)) {
                return true;
            }
        }

        return false;
    }

    private List<String> extractPlayerNames(String jsonResponse) {
        List<String> playerNames = new ArrayList<>();

        int sampleIndex = jsonResponse.indexOf("\"sample\"");
        if (sampleIndex == -1) {
            return playerNames;
        }

        int startBracket = jsonResponse.indexOf("[", sampleIndex);
        if (startBracket == -1) {
            return playerNames;
        }

        int endBracket = jsonResponse.indexOf("]", startBracket);
        if (endBracket == -1) {
            return playerNames;
        }

        String sampleSection = jsonResponse.substring(startBracket + 1, endBracket);

        String[] playerObjects = sampleSection.split("\\},\\s*\\{");

        for (String playerObj : playerObjects) {
            String name = extractFieldValue(playerObj, "name");
            if (name != null && !name.isEmpty()) {
                playerNames.add(name);
            }
        }

        return playerNames;
    }

    private String extractFieldValue(String json, String fieldName) {
        String searchPattern = "\"" + fieldName + "\"";
        int fieldIndex = json.indexOf(searchPattern);

        if (fieldIndex == -1) {
            return null;
        }

        int colonIndex = json.indexOf(":", fieldIndex);
        if (colonIndex == -1) {
            return null;
        }

        int startQuote = json.indexOf("\"", colonIndex);
        if (startQuote == -1) {
            return null;
        }

        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) {
            return null;
        }

        return json.substring(startQuote + 1, endQuote);
    }

    public int getOnlinePlayerCount() {
        if (lastResponse == null) {
            return -1;
        }

        String online = extractOnlineCount(lastResponse);
        if (online != null) {
            try {
                return Integer.parseInt(online);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        return -1;
    }

    private String extractOnlineCount(String jsonResponse) {
        int playersIndex = jsonResponse.indexOf("\"players\"");
        if (playersIndex == -1) {
            return null;
        }

        int onlineIndex = jsonResponse.indexOf("\"online\"", playersIndex);
        if (onlineIndex == -1) {
            return null;
        }

        int colonIndex = jsonResponse.indexOf(":", onlineIndex);
        if (colonIndex == -1) {
            return null;
        }

        int commaIndex = jsonResponse.indexOf(",", colonIndex);
        int braceIndex = jsonResponse.indexOf("}", colonIndex);

        int endIndex;
        if (commaIndex != -1 && braceIndex != -1) {
            endIndex = Math.min(commaIndex, braceIndex);
        } else if (commaIndex != -1) {
            endIndex = commaIndex;
        } else if (braceIndex != -1) {
            endIndex = braceIndex;
        } else {
            return null;
        }

        String numberStr = jsonResponse.substring(colonIndex + 1, endIndex).trim();
        return numberStr;
    }
}
