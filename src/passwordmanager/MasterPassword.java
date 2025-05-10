package passwordmanager;

import java.io.Console;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class MasterPassword {

    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256;
    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";

    private static final Logger LOG = Logger.getLogger(MasterPassword.class.getName());
    private static final Path HASH_PATH = Storage.resolve(VaultFiles.HASH_FILE);
    private static final HexFormat HEX = HexFormat.of();

    public static byte[] loadOrInit() {
        try {
            if (Files.size(HASH_PATH) == 0) {
                char[] pw1 = prompt("Create master password: ");
                char[] pw2 = prompt("Reenter to confirm: ");
                if (!Arrays.equals(pw1, pw2)) {
                    wipe(pw1);
                    wipe(pw2);
                    throw new IllegalStateException("Passwords didn't match. Restart.");
                }

                byte[] salt = randomSalt();
                byte[] dk = pbkdf2(pw1, salt);
                writeRecord(salt, dk);

                LOG.info("Master password initialized.");
                wipe(pw1);
                wipe(pw2);
                return dk;
            }

            Record rec = readRecord();
            char[] attempt = prompt("Enter master password: ");

            byte[] candidate = pbkdf2(attempt, rec.salt, rec.iter);
            boolean ok = MessageDigest.isEqual(candidate, rec.hash);
            wipe(attempt);
            if (!ok) {
                throw new SecurityException("Master password incorrect.");
            }

            LOG.info("Master password verified.");
            return candidate;
        } catch (Exception e) {
            throw new RuntimeException("Master password flow failed", e);
        }
    }

    private static char[] prompt(String msg) {
        Console c = System.console();
        if (c == null) throw new IllegalStateException("No console. Run in a terminal.");
        return c.readPassword(msg);
    }
    
    private static byte[] randomSalt() throws NoSuchAlgorithmException {
        byte[] salt = new byte[SALT_BYTES];
        SecureRandom.getInstanceStrong().nextBytes(salt);
        return salt;
    }
    
    private static byte[] pbkdf2(char[] pw, byte[] salt) throws GeneralSecurityException {
        return pbkdf2(pw, salt, ITERATIONS);
    }
    
    private static byte[] pbkdf2(char[] pw, byte[] salt, int inter) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(pw, salt, inter, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF_ALGO);
        return skf.generateSecret(spec).getEncoded();
    }
    
    private static void writeRecord(byte[] salt, byte[] hash) throws Exception {
        String line = ITERATIONS + ":" + HEX.formatHex(salt) + ":" + HEX.formatHex(hash) + '\n';
        Files.writeString(HASH_PATH, line, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    private static Record readRecord() throws Exception {
        String[] parts = Files.readString(HASH_PATH, StandardCharsets.UTF_8).trim().split(":");
        int iter = Integer.parseInt(parts[0]);
        byte[] salt = HEX.parseHex(parts[1]);
        byte[] hash = HEX.parseHex(parts[2]);
        return new Record(iter, salt, hash);
    }
    private record Record(int iter, byte[] salt, byte[] hash) {}
    
    private static void wipe(char[] arr) { Arrays.fill(arr, '\0');}
    
    private MasterPassword() {}
    
}
