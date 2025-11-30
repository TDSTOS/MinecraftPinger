import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;

public class UpdateDownloader {
    private static final int TIMEOUT = 30000;
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_RETRIES = 3;
    private String downloadUrl;
    private String targetDirectory;
    private File downloadedFile;

    public UpdateDownloader(String downloadUrl, String targetDirectory) {
        this.downloadUrl = downloadUrl;
        this.targetDirectory = targetDirectory;
    }

    public boolean downloadUpdate() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                System.out.println("Download attempt " + attempt + "/" + MAX_RETRIES);

                File updatesDir = new File(targetDirectory, "updates");
                if (!updatesDir.exists()) {
                    updatesDir.mkdirs();
                }

                downloadedFile = new File(updatesDir, "latest.zip");

                if (downloadedFile.exists()) {
                    System.out.println("Removing old update file...");
                    downloadedFile.delete();
                }

                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "MinecraftPinger-AutoUpdate");
                conn.setConnectTimeout(TIMEOUT);
                conn.setReadTimeout(TIMEOUT);

                conn.setInstanceFollowRedirects(true);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER) {

                    String newUrl = conn.getHeaderField("Location");
                    System.out.println("Following redirect to: " + newUrl);
                    conn.disconnect();
                    conn = (HttpURLConnection) new URL(newUrl).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "MinecraftPinger-AutoUpdate");
                    conn.setConnectTimeout(TIMEOUT);
                    conn.setReadTimeout(TIMEOUT);
                    responseCode = conn.getResponseCode();
                }

                if (responseCode != 200) {
                    System.err.println("Download failed with HTTP " + responseCode);
                    if (attempt < MAX_RETRIES) {
                        Thread.sleep(2000 * attempt);
                        continue;
                    }
                    return false;
                }

                long contentLength = conn.getContentLengthLong();
                System.out.println("Downloading update (" + formatFileSize(contentLength) + ")...");

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(downloadedFile)) {

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long totalBytesRead = 0;
                    int lastProgress = -1;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        if (contentLength > 0) {
                            int progress = (int) ((totalBytesRead * 100) / contentLength);
                            if (progress != lastProgress && progress % 10 == 0) {
                                System.out.println("Progress: " + progress + "%");
                                lastProgress = progress;
                            }
                        }
                    }

                    System.out.println("Download completed successfully!");
                    System.out.println("File saved to: " + downloadedFile.getAbsolutePath());
                    return true;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Download interrupted");
                return false;
            } catch (Exception e) {
                System.err.println("Download error: " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        System.out.println("Retrying in " + (2 * attempt) + " seconds...");
                        Thread.sleep(2000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }

        System.err.println("Download failed after " + MAX_RETRIES + " attempts");
        return false;
    }

    public File getDownloadedFile() {
        return downloadedFile;
    }

    public boolean validateDownload() {
        if (downloadedFile == null || !downloadedFile.exists()) {
            System.err.println("Validation failed: File does not exist");
            return false;
        }

        if (downloadedFile.length() < 200) {
            System.err.println("Validation failed: File too small (" + downloadedFile.length() + " bytes)");
            return false;
        }

        try (FileInputStream fis = new FileInputStream(downloadedFile)) {
            byte[] header = new byte[4];
            int read = fis.read(header);

            if (read != 4) {
                System.err.println("Validation failed: Cannot read file header");
                return false;
            }

            if (header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04) {
                System.out.println("Validation successful: Valid ZIP file");
                return true;
            } else {
                System.err.println("Validation failed: Not a valid ZIP file");
                return false;
            }

        } catch (IOException e) {
            System.err.println("Validation error: " + e.getMessage());
            return false;
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 0) {
            return "unknown";
        } else if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
