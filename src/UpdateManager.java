import java.util.Timer;
import java.util.TimerTask;

public class UpdateManager {
    private static final String VERSION = "1.2.0";
    private GitHubReleaseChecker releaseChecker;
    private final ConfigLoader config;
    private final UpdateLogger logger;
    private Timer updateTimer;
    private boolean updateAvailable;

    public UpdateManager(ConfigLoader config) {
        this.config = config;
        this.logger = new UpdateLogger();
        this.updateAvailable = false;

        String repoOwner = config.getAutoUpdateRepositoryOwner();
        String repoName = config.getAutoUpdateRepositoryName();

        if (repoOwner != null && !repoOwner.isEmpty() && repoName != null && !repoName.isEmpty()) {
            this.releaseChecker = new GitHubReleaseChecker(repoOwner, repoName, VERSION);
        }
    }

    public void startAutoUpdateCheck() {
        if (!config.isAutoUpdateEnabled() || releaseChecker == null) {
            return;
        }

        int intervalMinutes = config.getAutoUpdateCheckIntervalMinutes();
        long intervalMillis = intervalMinutes * 60 * 1000L;

        logger.logInfo("Auto-update check enabled (interval: " + intervalMinutes + " minutes)");

        updateTimer = new Timer("UpdateChecker", true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForUpdates(true);
            }
        }, 60000, intervalMillis);
    }

    public boolean checkForUpdates(boolean silent) {
        if (releaseChecker == null) {
            if (!silent) {
                System.out.println("Update checking is not configured.");
                System.out.println("Please set autoUpdate.repositoryOwner and autoUpdate.repositoryName in config.properties");
            }
            return false;
        }

        if (!silent) {
            System.out.println("Checking for updates...");
        }

        logger.logInfo("Checking for updates from GitHub");

        boolean success = releaseChecker.checkForUpdates();

        if (!success) {
            if (!silent) {
                System.out.println("Failed to check for updates. Check your internet connection.");
            }
            logger.logError("Update check failed");
            return false;
        }

        updateAvailable = releaseChecker.isUpdateAvailable();

        if (updateAvailable) {
            String currentVersion = releaseChecker.getCurrentVersion();
            String latestVersion = releaseChecker.getLatestVersion();

            System.out.println();
            System.out.println("========================================");
            System.out.println("  UPDATE AVAILABLE!");
            System.out.println("========================================");
            System.out.println("  Current version: " + currentVersion);
            System.out.println("  Latest version:  " + latestVersion);
            System.out.println("========================================");
            System.out.println();

            logger.logInfo("Update available: " + currentVersion + " -> " + latestVersion);
        } else {
            if (!silent) {
                System.out.println("You are running the latest version (" + releaseChecker.getCurrentVersion() + ")");
            }
            logger.logInfo("No updates available (current: " + releaseChecker.getCurrentVersion() + ")");
        }

        return updateAvailable;
    }

    public boolean downloadAndInstallUpdate() {
        if (releaseChecker == null || !releaseChecker.isUpdateAvailable()) {
            System.out.println("No update available to download.");
            return false;
        }

        String downloadUrl = releaseChecker.getLatestDownloadUrl();

        if (downloadUrl == null || downloadUrl.isEmpty()) {
            System.err.println("No download URL found for the latest release.");
            logger.logError("No download URL available");
            return false;
        }

        System.out.println("Starting download...");
        logger.logInfo("Downloading update from: " + downloadUrl);

        UpdateDownloader downloader = new UpdateDownloader(downloadUrl, ".");

        if (!downloader.downloadUpdate()) {
            System.err.println("Failed to download update.");
            logger.logError("Download failed");
            return false;
        }

        System.out.println("Validating download...");
        if (!downloader.validateDownload()) {
            System.err.println("Downloaded file is not valid.");
            logger.logError("Validation failed");
            return false;
        }

        System.out.println();
        System.out.println("Download complete! Preparing to install update...");
        logger.logInfo("Update downloaded and validated successfully");

        UpdateExecutor executor = new UpdateExecutor(downloader.getDownloadedFile(), ".", logger);

        System.out.println("Starting update process...");
        System.out.println("The application will shut down and restart automatically.");
        System.out.println();

        boolean success = executor.executeUpdate();

        if (!success) {
            System.err.println("Failed to start update process.");
            logger.logError("Update execution failed");
            return false;
        }

        logger.logInfo("Update process started successfully");
        return true;
    }

    public void stopAutoUpdateCheck() {
        if (updateTimer != null) {
            updateTimer.cancel();
            logger.logInfo("Auto-update check stopped");
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getCurrentVersion() {
        return VERSION;
    }

    public String getLatestVersion() {
        return releaseChecker != null ? releaseChecker.getLatestVersion() : null;
    }

    public void shutdown() {
        stopAutoUpdateCheck();
        logger.close();
    }

    public static String getVersion() {
        return VERSION;
    }
}
