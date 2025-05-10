package passwordmanager;

import java.nio.file.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.logging.Logger;

public final class Storage {

    private static final Logger LOG = Logger.getLogger(Storage.class.getName());
    private static final String OVERRIDE = "pm.data.dir";
    private static final Path DATA_DIR = initDataDir();

    public static Path dataDir() {
        return DATA_DIR;
    }

    public static Path resolve(String fileName) {
        return DATA_DIR.resolve(fileName);
    }

    private static Path initDataDir() {
        String override = System.getProperty(OVERRIDE);
        if (override != null && !override.isBlank()) {
            return ensureDir(Path.of(override));
        }
        
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        Path dir;
        if (os.contains("win")) {
            String localAppData = System.getenv().getOrDefault("LOCALAPPDATA",
                    Path.of(home, "AppData", "Local").toString());
            dir = Path.of(localAppData, "PasswordManager");
        } else if (os.contains("mac")) {
            dir = Path.of(home, "Library", "Application Support", "PasswordManager");
        } else {
            String dataHome = System.getenv().getOrDefault("XDG_DATA_HOME",
                    Path.of(home, ".local", "share").toString());
            dir = Path.of(dataHome, "password-manager");
        }
        return ensureDir(dir);
    }

    private static Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
            LOG.info(() -> "Password-manager data dir: " + dir.toAbsolutePath());
            return dir.toAbsolutePath();
    } catch (IOException e) {
        throw new UncheckedIOException("Cannot create data directory " + dir, e);
    }
    }
    
    private Storage() {};
}
