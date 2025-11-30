import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UpdateLogger {
    private static final String LOG_FILE = "update.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private PrintWriter logWriter;

    public UpdateLogger() {
        try {
            File logFile = new File(LOG_FILE);
            logWriter = new PrintWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            System.err.println("Warning: Could not open update log file: " + e.getMessage());
        }
    }

    public void logInfo(String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = "[" + timestamp + "] [INFO] " + message;

        System.out.println(logMessage);

        if (logWriter != null) {
            logWriter.println(logMessage);
            logWriter.flush();
        }
    }

    public void logError(String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = "[" + timestamp + "] [ERROR] " + message;

        System.err.println(logMessage);

        if (logWriter != null) {
            logWriter.println(logMessage);
            logWriter.flush();
        }
    }

    public void logWarning(String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = "[" + timestamp + "] [WARNING] " + message;

        System.out.println(logMessage);

        if (logWriter != null) {
            logWriter.println(logMessage);
            logWriter.flush();
        }
    }

    public void close() {
        if (logWriter != null) {
            logWriter.close();
        }
    }
}
