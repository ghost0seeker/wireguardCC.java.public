package com.wireguard.cc;

import java.io.*;
import java.io.ObjectInputFilter.Config;
import java.nio.file.*;
import java.util.Scanner;

public class PeerConfigManager {
    public static void createOrUpdatePeerConfig(Configurator manager, String selectedPeer, Scanner scanner) throws IOException {
        createPeerConfigDir();
        String jsonFileName = Configurator.PEER_CONFIG_DIR + File.separator + selectedPeer + ".json";
        File jsonFile = new File(jsonFileName);

        if (jsonFile.exists()) {
            handleExistingPeerConfig(manager, jsonFile, selectedPeer, scanner);
        } else {
            createNewPeerConfig(manager, selectedPeer, scanner);
        }
    }

    private static void createPeerConfigDir() throws IOException {
        Path path = Paths.get(Configurator.PEER_CONFIG_DIR);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
    private static void handleExistingPeerConfig(Configurator manager, File jsonFile, String selectedPeer, Scanner scanner) throws IOException {
        System.out.println("Existing configuration found. Do you want to modify it? (y/n)");
        String choice = scanner.nextLine().toLowerCase();

        if (choice.equals("y")) {
            createNewPeerConfig(manager, selectedPeer, scanner);
        } else {
            String confFileName = manager.promptForConfFileName(scanner);
            String existingConfig = Files.readString(jsonFile.toPath());
            manager.writeConfFile(confFileName, existingConfig);
        }
    }

    private static void createNewPeerConfig(Configurator manager, String selectedPeer, Scanner scanner) throws IOException {
        String address = manager.getConfigData().getString(selectedPeer + "_Address");
        String privateKey = manager.getConfigData().getString(selectedPeer + "_PrivateKey");
        String publicKey = manager.getConfigData().getString(selectedPeer + "_PublicKey");
        String defaultAllowedIPs = manager.getConfigData().getString(selectedPeer + "_AllowedIPs");

        System.out.println("Enter AllowedIPs range (press Enter for default: " + defaultAllowedIPs + "):");
        String allowedIPs = scanner.nextLine();
        if (allowedIPs.isEmpty()) {
            allowedIPs = defaultAllowedIPs;
        } else if (!allowedIPs.matches("(\\d{1,3}\\.){3}\\d{1,3}/\\d{1,2}(,\\s*(\\d{1,3}\\.){3}\\d{1,3}/\\d{1,2})*")) {
            throw new IllegalArgumentException("Invalid AllowedIPs format");
        }

        String config = String.format(Configurator.getPeerConfigTemplate(), address, privateKey, publicKey, allowedIPs);

        System.out.println("Enter any comments for this configuration: ");
        String comments = scanner.nextLine();

        manager.savePeerJsonConfig(selectedPeer, config, comments);

        String confFileName = manager.promptForConfFileName(scanner);
        manager.writeConfFile(confFileName, config);
    }
}   
