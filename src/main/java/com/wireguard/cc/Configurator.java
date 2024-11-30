package com.wireguard.cc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

public class Configurator {
    private TomlParseResult parsedToml;
    private List<String> peerNames;
    private Scanner scanner;

    public Configurator() {
        this.peerNames = new ArrayList<>();
        this.scanner = new Scanner(System.in);
    }


    private boolean loadToml() {
        try {
            String tomlName = Initiator.server_name;
            Path tomlPath = Paths.get(tomlName+".toml");

            if (!Files.exists(tomlPath)) {
                System.out.println("\nServer configuration file not found: " + tomlPath);
                System.out.print("Would you like to create a new server configuration? (yes/no): ");
                String response = scanner.nextLine().trim().toLowerCase();

                if (response.equals("yes")) {
                    System.out.println("\nInitiating new server configuration...");
                    ServerConfig.main(new String[0]);
                    
                    // After creating new configuration, try loading it again
                    if (Files.exists(tomlPath)) {
                        this.parsedToml = Toml.parse(tomlPath);
                        return true;
                    } else {
                        System.err.println("Failed to create server configuration.");
                        return false;
                    }
                } else {
                    System.out.println("Configuration creation cancelled.");
                    return false;
                }
            }

            // Parse the TOML file
            this.parsedToml = Toml.parse(tomlPath);
            
            // Clear existing peer names before reloading
            peerNames.clear();

            Pattern peerPattern = Pattern.compile("Peer[A-Z]_Address");
            for (String key : parsedToml.keySet()) {
                Matcher matcher = peerPattern.matcher(key);
                if (matcher.find()) {
                    String peerName = key.substring(0, key.indexOf('_'));
                    if (!peerNames.contains(peerName)) {
                        peerNames.add(peerName);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            System.err.println("Error loading subnet toml file: "+ e.getMessage());
            return false;
        }
    }


    private void displayConf() {
        System.out.println("\n----Configured Peers----");

        String serverAddress = parsedToml.getString("Server_Address");
        System.out.println("Server = " + serverAddress);

        for (String peerName : peerNames) {
            String peerAddress = parsedToml.getString(peerName + "_Address");
            if (peerAddress != null) {
                System.out.println(peerName + " = " + peerAddress);
            }
        }
    }
    
    public void run() {
        boolean running = true;

        // Try to load TOML file first
        if (!loadToml()) {
            System.out.println("Unable to proceed without valid configuration.");
            return;
        }

        while (running) {
            
            displayConf();

            System.out.println("\nAvailable Options:");
            System.out.println("Q: Quit");
            System.out.println("C: Create new peer");
            System.out.println("S: Save peer data as Wireguard conf file");
            System.out.println("M: Manage Connection");
            System.out.print("\nEnter your choice: ");

            String choice = scanner.nextLine().toUpperCase();

            switch(choice) {
                case "Q":
                running = false;
                System.out.println("Exiting...");
                break;

                case "C":
                System.out.println("Creating new peer...");
                PeerConfig.main(new String[0]);
                loadToml();
                break;

                case "M":
                System.out.println("\n⚠️  WARNING ⚠️");
                System.out.println("Manager Option: This function performs system-level modifications:");
                System.out.println("- Configures system firewall rules");
                System.out.println("- Modifies network interfaces");
                System.out.println("- Starts WireGuard connection");
                System.out.println("\nThis feature is untested. Please ensure you have:");
                System.out.println("1. Created a system restore point");
                System.out.println("2. Backed up your network configurations");
                System.out.println("3. Have access to emergency console if needed");
                
                System.out.print("\nDo you want to continue? (yes/no): ");
                String confirmation = scanner.nextLine().trim().toLowerCase();
                
                if (confirmation.equals("yes")) {
                    try {
                        Manager manager = new Manager(parsedToml);
                        manager.run(scanner);
                    } catch (Exception e) {
                        System.err.println("\nError in Manager execution: " + e.getMessage());
                        System.out.println("If you experience network issues, please check your firewall settings");
                        System.out.println("and WireGuard interface status using 'wg show' command.");
                    }
                } else {
                    System.out.println("\nManager operation cancelled.");
                }
                break;

                case "S":
                    try {
                        ConfGenerator generate = new ConfGenerator(parsedToml);
                        generate.generateFile(scanner);
                    } catch (Exception e) {
                        System.out.println("Error during configuration generation: " + e.getMessage());
                        e.printStackTrace();
                    }                
                    break;

                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }

        scanner.close();
    }


    public static void main(String[] args) {
        Configurator start = new Configurator();
        start.run();
    }

}