import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LiveTimeline {
    private static final int MAX_TIMELINE_SIZE = 200;

    private final Queue<TimelineEvent> events;
    private final HistoryService historyService;

    public LiveTimeline(HistoryService historyService) {
        this.events = new ConcurrentLinkedQueue<>();
        this.historyService = historyService;
    }

    public void addEvent(TimelineEvent event) {
        events.add(event);

        while (events.size() > MAX_TIMELINE_SIZE) {
            events.poll();
        }

        if (historyService != null) {
            persistEvent(event);
        }
    }

    public void recordPlayerJoin(String playerName, String serverName) {
        TimelineEvent event = new TimelineEvent(
            TimelineEvent.EventType.PLAYER_JOIN,
            playerName,
            serverName,
            "Player joined the server"
        );
        addEvent(event);
    }

    public void recordPlayerLeave(String playerName, String serverName) {
        TimelineEvent event = new TimelineEvent(
            TimelineEvent.EventType.PLAYER_LEAVE,
            playerName,
            serverName,
            "Player left the server"
        );
        addEvent(event);
    }

    public void recordServerUp(String serverName) {
        TimelineEvent event = new TimelineEvent(
            TimelineEvent.EventType.SERVER_UP,
            null,
            serverName,
            "Server is now online"
        );
        addEvent(event);
    }

    public void recordServerDown(String serverName) {
        TimelineEvent event = new TimelineEvent(
            TimelineEvent.EventType.SERVER_DOWN,
            null,
            serverName,
            "Server went offline"
        );
        addEvent(event);
    }

    public void recordCheckStart(String playerName, String serverName) {
        TimelineEvent event = new TimelineEvent(
            TimelineEvent.EventType.CHECK_START,
            playerName,
            serverName,
            "Check started"
        );
        addEvent(event);
    }

    public void recordCheckComplete(String playerName, String serverName, boolean online) {
        TimelineEvent event = new TimelineEvent(
            TimelineEvent.EventType.CHECK_COMPLETE,
            playerName,
            serverName,
            "Check completed - Player is " + (online ? "online" : "offline")
        );
        addEvent(event);
    }

    public List<TimelineEvent> getRecentEvents(int count) {
        List<TimelineEvent> recent = new ArrayList<>(events);
        if (recent.size() <= count) {
            return recent;
        }
        return recent.subList(recent.size() - count, recent.size());
    }

    public List<TimelineEvent> getAllEvents() {
        return new ArrayList<>(events);
    }

    public List<TimelineEvent> getEventsByPlayer(String playerName) {
        List<TimelineEvent> playerEvents = new ArrayList<>();
        for (TimelineEvent event : events) {
            if (playerName.equals(event.getPlayerName())) {
                playerEvents.add(event);
            }
        }
        return playerEvents;
    }

    public List<TimelineEvent> getEventsByServer(String serverName) {
        List<TimelineEvent> serverEvents = new ArrayList<>();
        for (TimelineEvent event : events) {
            if (serverName.equals(event.getServerName())) {
                serverEvents.add(event);
            }
        }
        return serverEvents;
    }

    private void persistEvent(TimelineEvent event) {
        if (event.getPlayerName() != null) {
            boolean online = event.getType() == TimelineEvent.EventType.PLAYER_JOIN;
            historyService.recordPlayerStatus(
                event.getPlayerName(),
                event.getServerName(),
                online,
                0
            );
        }
    }

    public static class TimelineEvent {
        public enum EventType {
            PLAYER_JOIN,
            PLAYER_LEAVE,
            SERVER_UP,
            SERVER_DOWN,
            CHECK_START,
            CHECK_COMPLETE
        }

        private final long timestamp;
        private final EventType type;
        private final String playerName;
        private final String serverName;
        private final String message;

        public TimelineEvent(EventType type, String playerName, String serverName, String message) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.playerName = playerName;
            this.serverName = serverName;
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public EventType getType() {
            return type;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getServerName() {
            return serverName;
        }

        public String getMessage() {
            return message;
        }

        public String getFormattedTime() {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return String.format("%02d:%02d:%02d",
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND)
            );
        }

        public String getTypeIcon() {
            return switch (type) {
                case PLAYER_JOIN -> "➕";
                case PLAYER_LEAVE -> "➖";
                case SERVER_UP -> "🟢";
                case SERVER_DOWN -> "🔴";
                case CHECK_START -> "🔍";
                case CHECK_COMPLETE -> "✓";
            };
        }

        @Override
        public String toString() {
            return String.format("[%s] %s %s - %s",
                getFormattedTime(),
                getTypeIcon(),
                serverName != null ? serverName : "",
                message
            );
        }
    }
}
