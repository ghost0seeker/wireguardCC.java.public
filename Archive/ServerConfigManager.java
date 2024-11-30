package com.wireguard.cc;

import java.io.*;
import java.io.ObjectInputFilter.Config;
import java.nio.file.*;
import java.util.Scanner;

public class ServerConfigManager {
    public static void createOrUpdateServerConfig(Configurator manager, Scanner scanner) throws IOException {
        createServerConfigDir();
        String jsonFilename = Configurator.SERVER_CONFIG_DIR + File.separator + "server.json";
        File jsonFile = new File(jsonFilename);

        if (jsonFile.exists()) {
            handleExistingServerConfig(manager, jsonFile, scanner);
        } else {
            createNewServerConfig(manager, scanner);
        }
    }
    
    private static void createServerConfigDir() throws IOException {
        Path path = Paths.get(Configurator.SERVER_CONFIG_DIR);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private static void handleExistingServerConfig(Configurator manager, File jsonFile, Scanner scanner) throws IOException {
        System.out.println("Existing server configuration found. Do you want to modify it? (y/n)");
        String choice = scanner.nextLine().toLowerCase();
    
        if (choice.equals("y")) {
            createNewServerConfig(manager, scanner);
        } else {
            String confFileName = manager.promptForConfFileName(scanner);
            String existingConfig = Files.readString(jsonFile.toPath());
            manager.writeConfFile(confFileName, existingConfig);
        }
    }

    private static void createNewServerConfig(Configurator manager, Scanner scanner) throws IOException {
        String privateKey = manager.getConfigData().getString("Server_PrivateKey");
        StringBuilder peerConfigs = new StringBuilder();

        for (String key : manager.getConfigData().keySet()) {
            if (key.endsWith("_PublicKey") && !key.startsWith("Server")) {
                String peerName = key.substring(0, key.indexOf("_"));
                String publicKey = manager.getConfigData().getString(key);
                String allowedIPs = manager.getConfigData().getString(peerName + "_AllowedIPS");

                peerConfigs.append(String.format("#%s\n[Peer]\nPublicKey = %s\nAllowedIPs = %s\n\n", 
                                                 peerName, publicKey, allowedIPs));
            }
        }

        String config = String.format(Configurator.getServerConfigTemplate(), privateKey, peerConfigs.toString());

        System.out.println("Enter any comments for this server configuration:");
        String comments = scanner.nextLine();

        manager.savePeerJsonConfig("Server", config, comments);

        String confFileName = manager.promptForConfFileName(scanner);
        manager.writeConfFile(confFileName, config);
    }
}
