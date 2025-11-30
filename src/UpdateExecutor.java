import java.io.*;
import java.nio.file.*;

public class UpdateExecutor {
    private File updateZip;
    private String applicationDirectory;
    private UpdateLogger logger;

    public UpdateExecutor(File updateZip, String applicationDirectory, UpdateLogger logger) {
        this.updateZip = updateZip;
        this.applicationDirectory = applicationDirectory;
        this.logger = logger;
    }

    public boolean executeUpdate() {
        if (updateZip == null || !updateZip.exists()) {
            logger.logError("Update file does not exist: " + updateZip);
            return false;
        }

        String os = detectOS();
        logger.logInfo("Detected OS: " + os);

        File updateScript = null;

        try {
            if (os.equals("windows")) {
                updateScript = generateWindowsUpdateScript();
            } else {
                updateScript = generateUnixUpdateScript();
            }

            if (updateScript == null || !updateScript.exists()) {
                logger.logError("Failed to generate update script");
                return false;
            }

            logger.logInfo("Starting update script: " + updateScript.getAbsolutePath());
            logger.logInfo("Application will shut down and restart after update");

            startUpdateScript(updateScript, os);

            return true;

        } catch (Exception e) {
            logger.logError("Failed to execute update: " + e.getMessage());
            return false;
        }
    }

    private String detectOS() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return "windows";
        } else if (osName.contains("mac")) {
            return "unix";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return "unix";
        } else {
            return "unknown";
        }
    }

    private File generateWindowsUpdateScript() throws IOException {
        File scriptFile = new File(applicationDirectory, "update.bat");

        if (scriptFile.exists()) {
            logger.logInfo("Using existing update.bat script");
            return scriptFile;
        }

        logger.logInfo("Generating update.bat script");

        String jarName = findJarFile();
        if (jarName == null) {
            jarName = "Main.jar";
        }

        String scriptContent =
            "@echo off\n" +
            "echo Minecraft Player Checker - Update Script\n" +
            "echo ==========================================\n" +
            "echo.\n" +
            "echo Waiting for application to shut down...\n" +
            "timeout /t 3 /nobreak >nul\n" +
            "echo.\n" +
            "echo Extracting update...\n" +
            "powershell -Command \"Expand-Archive -Path 'updates\\latest.zip' -DestinationPath '.' -Force\"\n" +
            "if %ERRORLEVEL% NEQ 0 (\n" +
            "    echo ERROR: Failed to extract update!\n" +
            "    pause\n" +
            "    exit /b 1\n" +
            ")\n" +
            "echo.\n" +
            "echo Update extracted successfully!\n" +
            "echo.\n" +
            "echo Restarting application...\n" +
            "timeout /t 2 /nobreak >nul\n" +
            "start \"Minecraft Player Checker\" java -jar " + jarName + "\n" +
            "exit\n";

        try (PrintWriter writer = new PrintWriter(new FileWriter(scriptFile))) {
            writer.write(scriptContent);
        }

        logger.logInfo("Generated update.bat");
        return scriptFile;
    }

    private File generateUnixUpdateScript() throws IOException {
        File scriptFile = new File(applicationDirectory, "update.sh");

        if (scriptFile.exists()) {
            logger.logInfo("Using existing update.sh script");
            return scriptFile;
        }

        logger.logInfo("Generating update.sh script");

        String jarName = findJarFile();
        if (jarName == null) {
            jarName = "Main.jar";
        }

        String scriptContent =
            "#!/bin/bash\n" +
            "echo \"Minecraft Player Checker - Update Script\"\n" +
            "echo \"==========================================\"\n" +
            "echo \"\"\n" +
            "echo \"Waiting for application to shut down...\"\n" +
            "sleep 3\n" +
            "echo \"\"\n" +
            "echo \"Extracting update...\"\n" +
            "unzip -o updates/latest.zip -d .\n" +
            "if [ $? -ne 0 ]; then\n" +
            "    echo \"ERROR: Failed to extract update!\"\n" +
            "    exit 1\n" +
            "fi\n" +
            "echo \"\"\n" +
            "echo \"Update extracted successfully!\"\n" +
            "echo \"\"\n" +
            "echo \"Restarting application...\"\n" +
            "sleep 2\n" +
            "nohup java -jar " + jarName + " > /dev/null 2>&1 &\n" +
            "echo \"Application restarted in background\"\n" +
            "exit 0\n";

        try (PrintWriter writer = new PrintWriter(new FileWriter(scriptFile))) {
            writer.write(scriptContent);
        }

        scriptFile.setExecutable(true);

        logger.logInfo("Generated update.sh");
        return scriptFile;
    }

    private String findJarFile() {
        try {
            String classPath = System.getProperty("java.class.path");
            String[] paths = classPath.split(File.pathSeparator);

            for (String path : paths) {
                if (path.endsWith(".jar")) {
                    File jarFile = new File(path);
                    if (jarFile.exists()) {
                        return jarFile.getName();
                    }
                }
            }
        } catch (Exception e) {
            logger.logError("Failed to find JAR file: " + e.getMessage());
        }

        return null;
    }

    private void startUpdateScript(File scriptFile, String os) throws IOException {
        ProcessBuilder pb;

        if (os.equals("windows")) {
            pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"Update\"", "/wait", scriptFile.getAbsolutePath());
        } else {
            pb = new ProcessBuilder("/bin/bash", scriptFile.getAbsolutePath());
        }

        pb.directory(new File(applicationDirectory));
        pb.start();

        logger.logInfo("Update script started");

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                logger.logInfo("Shutting down application for update");
                System.exit(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public static boolean ensureUpdateScriptsExist(String applicationDirectory) {
        try {
            UpdateLogger tempLogger = new UpdateLogger();

            UpdateExecutor executor = new UpdateExecutor(null, applicationDirectory, tempLogger);

            executor.generateWindowsUpdateScript();
            executor.generateUnixUpdateScript();

            return true;
        } catch (Exception e) {
            System.err.println("Failed to ensure update scripts exist: " + e.getMessage());
            return false;
        }
    }
}
