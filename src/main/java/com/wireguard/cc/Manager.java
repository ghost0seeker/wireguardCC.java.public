package com.wireguard.cc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import org.tomlj.TomlParseResult;

import com.wireguard.cc.helper.HandleFirewall;

public class Manager {

    // private String osType;
    private String packageManager;
    private TomlParseResult tomlData;
    private String interfaceName;
    private Path configPath;

    private ProcessBuilder processBuilder;
    
    public Manager(TomlParseResult tomlData) {

        checkRoot();

        this.tomlData = tomlData;
        this.processBuilder = new ProcessBuilder();
        this.processBuilder.redirectErrorStream(true);
    }

    
    private int executeCommand(String... command) throws IOException, InterruptedException {
        processBuilder.command(command);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        return process.waitFor();
    }


    private static void checkRoot() {
        try {
            Process process = new ProcessBuilder("id", "-u").start();
            BufferedReader reader = new BufferedReader( new InputStreamReader(process.getInputStream()));
            String uid = reader.readLine();
            if (!"0".equals(uid)) {
                System.out.println("This program must be run as root (sudo)");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error checking root privileges: "+ e.getMessage());
            System.exit(1);
        }
    }


    private void checkWireGuard() {
        try {
            Process process = new ProcessBuilder("which", "wg").start();
            if (process.waitFor() != 0) {
                System.out.println("Wireguard not found. Installing...");
                installWireguard(packageManager);
            }
        } catch (Exception e) {
            System.err.println("Error checking WireGuard: " + e.getMessage());
            System.exit(1);
        }
    }


    private void installWireguard(String packageManager) {
        try {
            switch (packageManager) {
                case "apt":
                    executeCommand("apt", "update");
                    executeCommand("apt", "install", "-y", "wireguard-tools");
                    break;
                case "dnf":
                    executeCommand("dnf", "install", "-y", "wireguard-tools");
                    break;
                case "pacman":
                    executeCommand("pacman", "-Sy", "--noconfirm", "wireguard-tools");
                    break;
                case "apk":
                    executeCommand("apk", "add", "wireguard-tools");
                    break;
                default:
                    throw new RuntimeException("Unsupported package manager");
            }
        } catch (Exception e) {
            System.err.println("Error installing WireGuard: " + e.getMessage());
            System.exit(1);
        }
    }


    private void saveAndCopyConfig(String fileName) {
        try {
            // Get current working directory
            String currentDir = System.getProperty("user.dir");
            Path sourceFile = Paths.get(currentDir, fileName);
            Path targetDir = Paths.get("/etc/wireguard");

            executeCommand( "mkdir", "-p", targetDir.toString());

            executeCommand( "cp", sourceFile.toString(), targetDir.resolve(fileName).toString());

            executeCommand("chmod", "600", 
                            targetDir.resolve(fileName).toString());

            System.out.println("Configuration saved and copied to " + targetDir.resolve(fileName));
            
        } catch (Exception e) {
            System.err.println("Error handling configuration file: " + e.getMessage());
            cleanupAndExit();
        }
    }


    public void run(Scanner scanner) {
        
        System.out.println("\nChoose mode:");
        System.out.println("S: Start as Server");
        System.out.println("P: Start as Peer");
        System.out.print("Enter choice: ");
        
        String choice = scanner.nextLine().toUpperCase();

        boolean isServer = choice.equals("S");
        
        if (!choice.equals("S") && !choice.equals("P")) {
            System.out.println("Invalid choice");
            return;
        } else {
            switch (choice) {
                case "S":
                    System.out.println("Starting wireguard peer as server...");
                    // Generate configuration using ConfGenerator
                    ConfGenerator confGen = new ConfGenerator(tomlData);
                    if (!confGen.generateServerFile(scanner)) {
                        System.out.println("Configuration generation failed");
                        return;
                    }

                    this.configPath = confGen.getConfigPath(); // Default interface name
                    String configFileName = configPath.getFileName().toString();
                    System.out.println("Using conf file"+configFileName);
                    this.interfaceName = configPath.getFileName().toString();

                    if (interfaceName.endsWith(".conf")) {
                        this.interfaceName = interfaceName.substring(0, interfaceName.length() - 5);
                        System.out.println("Using conf file"+interfaceName);
                    }

                    try {

                        System.out.println("Configuring firewall for server");
                        HandleFirewall handleFirewall = new HandleFirewall();
                        System.out.println("Detecting firewall backend...");
                        handleFirewall.detectFirewall();
                        handleFirewall.configureFirewall(interfaceName, isServer);
            
                        System.out.print("Setting Wireguard...");
                        checkWireGuard();
                        System.out.println("Copying configuration file to /etc/wiregaurd");
                        saveAndCopyConfig(configFileName);

                        System.out.println("Starting wireguard connection");
                        int exitCode = executeCommand("wg-quick", "up", interfaceName);
            
                        if (exitCode != 0) {
                            throw new IOException("Failed to start WireGuard interface");
                            }
                        System.out.println("Connection Started");
                    } catch (Exception e) {
                        System.err.println("Failed to establish WireGuard connection: " + e.getMessage());
                        e.printStackTrace();
                    }
                
                    case "P":
                        System.out.println("Starting wireguard connection as peer");


        }





 
            

    private void cleanupAndExit() {
        try {
            if (configPath != null) {
                // Process process = new ProcessBuilder("wg-quick", "down", configPath.toString()).start();
                // process.waitFor();
                executeCommand("wg-quick", "down", interfaceName);
                cleanupFirewall(interfaceName, true);
                
                // Clear screen one final time
                System.out.print("\033[H\033[2J");
                System.out.flush();
                System.out.println("WireGuard connection terminated.");
            }
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
        System.exit(0);
    }

}
