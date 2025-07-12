import common.FTPConfig;

public class Main {
    public static void main(String[] args) {
        System.out.println("FTP Client-Server Application");
        System.out.println("Phase 2: Common Components Test");

        FTPConfig config = FTPConfig.getInstance();
        config.printConfiguration();

        System.out.println("\nConfiguration loaded successfully!");
    }
}