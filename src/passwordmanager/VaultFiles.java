package passwordmanager;

import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.logging.Logger;

public final class VaultFiles {
    
    private static final Logger LOG = Logger.getLogger(VaultFiles.class.getName());
    
    public static final String HASH_FILE = "master.hash";
    public static final String VAULT_FILE = "vault.dat";
    
    public static void ensureFiles() {
        createIfMissing(Storage.resolve(HASH_FILE));
        createIfMissing(Storage.resolve(VAULT_FILE));
    }
    
    private static void createIfMissing(Path file) {
        try {
            if (Files.notExists(file)) {
                Files.createFile(file, new FileAttribute<?>[0]);
                LOG.info(() -> "Created empty file: " + file);
            } else if (!Files.isRegularFile(file)) {
                throw new IOException(file + " exists but is not a normal file.");
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException("Failed to initialize " + file, ioe);
        }
    }
    
    private VaultFiles() {};
}
