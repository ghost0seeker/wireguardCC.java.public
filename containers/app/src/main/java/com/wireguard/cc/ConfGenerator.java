package com.wireguard.cc;

import org.tomlj.TomlParseResult;
import java.util.Base64;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays; 

// Curve25519 imports
import br.eti.balena.security.ecdh.curve25519.Curve25519;
import br.eti.balena.security.ecdh.curve25519.Curve25519KeyAgreement;
import br.eti.balena.security.ecdh.curve25519.Curve25519PrivateKey;
import br.eti.balena.security.ecdh.curve25519.Curve25519PublicKey;

public class ConfGenerator {
    private TomlParseResult tomlData;
    private List<String> peerNames;
    private Path configPath;

    public ConfGenerator(TomlParseResult tomlData) {
        this.tomlData = tomlData;
        this.peerNames = new ArrayList<>();
        loadPeerNames();
    }

    private void loadPeerNames() {
        Pattern peerPattern = Pattern.compile("Peer[A-Z]_Address");
        for (String key : tomlData.keySet()) {
            Matcher matcher = peerPattern.matcher(key);
            if (matcher.find()) {
                String peerName = key.substring(0, key.indexOf('_'));
                if (!peerNames.contains(peerName)) {
                    peerNames.add(peerName);
                }
            }
        }
    }

    public boolean generateFile(Scanner scanner) {
        
        System.out.println("\nGenerate configuration for:");
        System.out.println("1: Server");
        System.out.println("2: Peer");
        System.out.print("Select option (or 0 to cancel): ");

        try {
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 0:
                    System.out.println("Operation Cancelled.");
                    return false;

                case 1:
                    generateServerFile(scanner);
                    break;

                case 2:
                    generatePeerFile(scanner);
                    break;
                
                default:
                    System.out.println("Invalid selection");
                    return false;
            }
            return configPath != null;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input, please enter a number");
            return false;
        }
    }

    public boolean generateServerFile(Scanner scanner) {

        System.out.print("Enter configuration file name (e.g., wg0.conf): ");
        String fileName = scanner.nextLine();

        StringBuilder config = new StringBuilder();

        config.append("[Interface]\n");
        config.append("PrivateKey = ").append(tomlData.getString("Server_PrivateKey")).append("\n");
        config.append("Address = ").append(tomlData.getString("Server_Address")).append("\n");
        config.append("ListenPort = ").append(tomlData.getString("Listening_Port")).append("\n\n");
    
        for (String peerName : peerNames) {
            config.append("[Peer]\n");
            config.append("PublicKey = ").append(tomlData.getString(peerName + "_PublicKey")).append("\n");
            config.append("AllowedIPs = ").append(tomlData.getString(peerName + "_Server_Address")).append("\n\n");
        }

        saveToFile(fileName, config.toString());
        return true; 
    }

    public boolean generatePeerFile(Scanner scanner) {

        // Display available peers
        System.out.println("\nAvailable peers:");
        for (int i = 0; i < peerNames.size(); i++) {
            System.out.println((i + 1) + ": " + peerNames.get(i));
        }

        System.out.println("Select peer number (or 0 to cancel): ");
        try {
            int selection = Integer.parseInt(scanner.nextLine());
            if (selection == 0) {
                System.out.println("Operation cancelled");
                return false;
            }
            if (selection < 1 || selection > peerNames.size()) {
                System.out.println("Invalid selection");
                return false;
            }

            String selectedPeer = peerNames.get(selection - 1);

            try {

                String serverPrivateKeyStr = tomlData.getString("Server_PrivateKey");
                String peerPublicKeyStr = tomlData.getString(selectedPeer + "_PublicKey");
    
                if (serverPrivateKeyStr == null || peerPublicKeyStr == null) {
                    System.out.println("Error: Missing key information in TOML file.");
                    return false;
                }
    
                byte[] serverPrivateKey = Base64.getDecoder().decode(serverPrivateKeyStr);
                byte[] peerPublicKey = Base64.getDecoder().decode(peerPublicKeyStr);
    
                Curve25519PrivateKey privateKey = new Curve25519PrivateKey(serverPrivateKey);
                Curve25519PublicKey publicKey = new Curve25519PublicKey(peerPublicKey);
    
                Curve25519KeyAgreement keyAgreement = new Curve25519KeyAgreement(privateKey);
                keyAgreement.doFinal(publicKey);
                byte[] sharedSecret = keyAgreement.generateSecret();
    
                boolean isValid = !Arrays.equals(sharedSecret, Curve25519.ZERO);

                if (isValid) {
                    System.out.println("Key challenge succesful! Keys are valid.");

                    System.out.print("Enter configuration file name (e.g., wg0.conf): ");
                    String fileName = scanner.nextLine();
                    
                    StringBuilder config = new StringBuilder();
                    
                    // [Interface] section
                    config.append("[Interface]\n");
                    config.append("PrivateKey = ").append(tomlData.getString(selectedPeer + "_PrivateKey")).append("\n");
                    config.append("Address = ").append(tomlData.getString(selectedPeer + "_Address")).append("\n\n");
                    
                    // [Peer] section (Server)
                    config.append("[Peer]\n");
                    config.append("PublicKey = ").append(tomlData.getString("Server_PublicKey")).append("\n");
                    config.append("Endpoint = ").append(tomlData.getString(selectedPeer + "_Endpoint")).append("\n");
                    config.append("AllowedIPs = ").append(tomlData.getString(selectedPeer + "_AllowedIPS")).append("\n");
                    config.append("PersistentKeepalive = 25\n");
                    
                    saveToFile(fileName, config.toString());
                    return true;
                } else {
                    System.out.println("Key challenge failed! Invalid key pair.");
                    return false;
                }
            } catch (Exception e) {
                System.out.println("Error during key verification: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input, please enter a number");
            return false;
        }
    }

    
    private void saveToFile(String fileName, String content) {
        try {
            this.configPath = Paths.get(fileName);
            try (FileWriter writer = new FileWriter(configPath.toFile())) {
                writer.write(content);
                System.out.println("Configuration saved to " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Error saving configuration file: " + e.getMessage());
        }
    }

    public Path getConfigPath() {
        return configPath;
    }

}   

