package passwordmanager;

public class PasswordManager {

    public static void main(String[] args) {
        VaultFiles.ensureFiles();
       byte[] masterKey = MasterPassword.loadOrInit();
    }
    
}
