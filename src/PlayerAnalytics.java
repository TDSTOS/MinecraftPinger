import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.text.SimpleDateFormat;

public class PlayerAnalytics {
    private HistoryService historyService;
    private Map<String, PlayerActivityData> activityCache;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public PlayerAnalytics(HistoryService historyService) {
        this.historyService = historyService;
        this.activityCache = new ConcurrentHashMap<>();
    }

    public PlayerInsights getInsights(String playerName) {
        List<HistoryEntry> history = historyService.getPlayerHistory(playerName, 30);
        if (history.isEmpty()) {
            return null;
        }

        PlayerInsights insights = new PlayerInsights(playerName);

        long totalOnlineTime = 0;
        int sessionCount = 0;
        long longestSession = 0;
        Map<Integer, Integer> hourlyActivity = new HashMap<>();

        Long sessionStart = null;

        for (HistoryEntry entry : history) {
            long timestamp = parseTimestamp(entry.getTimestamp());
            if (timestamp == 0) continue;

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            hourlyActivity.put(hour, hourlyActivity.getOrDefault(hour, 0) + 1);

            boolean isOnline = "online".equals(entry.getStatus());

            if (isOnline) {
                if (sessionStart == null) {
                    sessionStart = timestamp;
                }
            } else {
                if (sessionStart != null) {
                    long sessionDuration = timestamp - sessionStart;
                    totalOnlineTime += sessionDuration;
                    sessionCount++;
                    if (sessionDuration > longestSession) {
                        longestSession = sessionDuration;
                    }
                    sessionStart = null;
                }
            }
        }

        if (sessionStart != null) {
            long currentSession = System.currentTimeMillis() - sessionStart;
            totalOnlineTime += currentSession;
            sessionCount++;
            if (currentSession > longestSession) {
                longestSession = currentSession;
            }
        }

        insights.setTotalOnlineTime(totalOnlineTime);
        insights.setSessionCount(sessionCount);
        insights.setLongestSession(longestSession);
        if (sessionCount > 0) {
            insights.setAverageSessionLength(totalOnlineTime / sessionCount);
        }
        insights.setHourlyActivity(hourlyActivity);

        long now = System.currentTimeMillis();
        long dayAgo = now - (24 * 60 * 60 * 1000);
        long weekAgo = now - (7 * 24 * 60 * 60 * 1000);

        insights.setDailyOnlineTime(calculateOnlineTime(history, dayAgo, now));
        insights.setWeeklyOnlineTime(calculateOnlineTime(history, weekAgo, now));

        return insights;
    }

    private long calculateOnlineTime(List<HistoryEntry> history, long startTime, long endTime) {
        long totalTime = 0;
        Long sessionStart = null;

        for (HistoryEntry entry : history) {
            long timestamp = parseTimestamp(entry.getTimestamp());
            if (timestamp == 0) continue;

            if (timestamp < startTime) continue;
            if (timestamp > endTime) break;

            boolean isOnline = "online".equals(entry.getStatus());

            if (isOnline) {
                if (sessionStart == null) {
                    sessionStart = timestamp;
                }
            } else {
                if (sessionStart != null) {
                    totalTime += timestamp - sessionStart;
                    sessionStart = null;
                }
            }
        }

        if (sessionStart != null) {
            totalTime += endTime - sessionStart;
        }

        return totalTime;
    }

    private long parseTimestamp(String timestampStr) {
        if (timestampStr == null) return 0;

        try {
            return DATE_FORMAT.parse(timestampStr).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getActivityHeatmap(String playerName) {
        PlayerInsights insights = getInsights(playerName);
        if (insights == null) return "No data available for " + playerName;

        StringBuilder sb = new StringBuilder();
        sb.append("\nActivity Heatmap for ").append(playerName).append(":\n");
        sb.append("Hour | Activity\n");
        sb.append("-----|").append("-".repeat(50)).append("\n");

        Map<Integer, Integer> hourlyActivity = insights.getHourlyActivity();
        int maxActivity = hourlyActivity.values().stream().max(Integer::compareTo).orElse(1);

        for (int hour = 0; hour < 24; hour++) {
            int activity = hourlyActivity.getOrDefault(hour, 0);
            int barLength = (int) ((activity / (double) maxActivity) * 40);

            sb.append(String.format("%02d:00", hour)).append(" | ");
            sb.append("█".repeat(Math.max(0, barLength)));
            sb.append(" (").append(activity).append(")\n");
        }

        return sb.toString();
    }

    public static class PlayerInsights {
        private String playerName;
        private long totalOnlineTime;
        private long dailyOnlineTime;
        private long weeklyOnlineTime;
        private long averageSessionLength;
        private long longestSession;
        private int sessionCount;
        private Map<Integer, Integer> hourlyActivity;

        public PlayerInsights(String playerName) {
            this.playerName = playerName;
            this.hourlyActivity = new HashMap<>();
        }

        public String getPlayerName() {
            return playerName;
        }

        public long getTotalOnlineTime() {
            return totalOnlineTime;
        }

        public void setTotalOnlineTime(long totalOnlineTime) {
            this.totalOnlineTime = totalOnlineTime;
        }

        public long getDailyOnlineTime() {
            return dailyOnlineTime;
        }

        public void setDailyOnlineTime(long dailyOnlineTime) {
            this.dailyOnlineTime = dailyOnlineTime;
        }

        public long getWeeklyOnlineTime() {
            return weeklyOnlineTime;
        }

        public void setWeeklyOnlineTime(long weeklyOnlineTime) {
            this.weeklyOnlineTime = weeklyOnlineTime;
        }

        public long getAverageSessionLength() {
            return averageSessionLength;
        }

        public void setAverageSessionLength(long averageSessionLength) {
            this.averageSessionLength = averageSessionLength;
        }

        public long getLongestSession() {
            return longestSession;
        }

        public void setLongestSession(long longestSession) {
            this.longestSession = longestSession;
        }

        public int getSessionCount() {
            return sessionCount;
        }

        public void setSessionCount(int sessionCount) {
            this.sessionCount = sessionCount;
        }

        public Map<Integer, Integer> getHourlyActivity() {
            return hourlyActivity;
        }

        public void setHourlyActivity(Map<Integer, Integer> hourlyActivity) {
            this.hourlyActivity = hourlyActivity;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\nPlayer Analytics for ").append(playerName).append(":\n");
            sb.append("  Total Online Time: ").append(formatDuration(totalOnlineTime)).append("\n");
            sb.append("  Daily Online Time: ").append(formatDuration(dailyOnlineTime)).append("\n");
            sb.append("  Weekly Online Time: ").append(formatDuration(weeklyOnlineTime)).append("\n");
            sb.append("  Total Sessions: ").append(sessionCount).append("\n");
            sb.append("  Average Session: ").append(formatDuration(averageSessionLength)).append("\n");
            sb.append("  Longest Session: ").append(formatDuration(longestSession)).append("\n");
            return sb.toString();
        }

        private String formatDuration(long millis) {
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
            } else if (hours > 0) {
                return String.format("%dh %dm", hours, minutes % 60);
            } else if (minutes > 0) {
                return String.format("%dm %ds", minutes, seconds % 60);
            } else {
                return String.format("%ds", seconds);
            }
        }
    }

    private static class PlayerActivityData {
        long lastSeen;
        boolean currentlyOnline;
        List<Long> sessions;

        public PlayerActivityData() {
            this.sessions = new ArrayList<>();
        }
    }
}
