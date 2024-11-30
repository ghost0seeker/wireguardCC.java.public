package com.wireguard.cc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
// import java.util.concurrent.TimeUnit;

import org.tomlj.TomlParseResult;

public class Manager {
    // private String osType;
    private String packageManager;
    private boolean isFirewalldPresent;
    private String physicalInterface;
    private String activeZone;
    private TomlParseResult tomlData;
    private String interfaceName;
    private Path configPath;

    private ProcessBuilder processBuilder;
    
    public Manager(TomlParseResult tomlData) {
        checkRoot();
        detectOS();
        checkWireGuard();
        detectFirewall();
        detectNetworkInterface();

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

    private void checkRoot() {
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

    private void detectOS() {
        try {
            Process process = new ProcessBuilder("cat", "/etc/os-release").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("ID=")) {
                    String id  = line.substring(3).replace("\"", "");
                    switch(id) {
                        case "ubuntu":
                        case "debian":
                            // osType = "debian";
                            packageManager = "apt";
                            break;
                        case "fedora":
                        case "rhel":
                        case "centos":
                            // osType = "redhat";
                            packageManager = "dnf";
                            break;
                        case "arch":
                        case "endeavouros":
                            // osType = "arch";
                            packageManager = "pacman";
                            break;
                        case "alpine":
                            // osType = "alpine";
                            packageManager = "apk";
                            break;
                        default:
                        System.out.println("Unsupported OS: "+ id);
                        System.exit(1);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error detecting OS: " + e.getMessage());
            System.exit(1);
        }
    }

    private void checkWireGuard() {
        try {
            Process process = new ProcessBuilder("which", "wg").start();
            if (process.waitFor() != 0) {
                System.out.println("Wireguard not found. Installing...");
                installWireguard();
            }
        } catch (Exception e) {
            System.err.println("Error checking WireGuard: " + e.getMessage());
            System.exit(1);
        }
    }

    private void installWireguard() {
        try {
            Process process;
            switch (packageManager) {
                case "apt":
                    process = new ProcessBuilder("apt", "update").start();
                    process.waitFor();
                    process = new ProcessBuilder("apt", "install", "-y", "wireguard-tools").start();
                    break;
                case "dnf":
                    process = new ProcessBuilder("dnf", "install", "-y", "wireguard-tools").start();
                    break;
                case "pacman":
                    process = new ProcessBuilder("pacman", "-Sy", "--noconfirm", "wireguard-tools").start();
                    break;
                case "apk":
                    process = new ProcessBuilder("apk", "add", "wireguard-tools").start();
                    break;
                default:
                    throw new RuntimeException("Unsupported package manager");
            }
            process.waitFor();

        } catch (Exception e) {
            System.err.println("Error installing WireGuard: " + e.getMessage());
            System.exit(1);
        
        }
    }

private void detectFirewall() {
    try {
        Process process = new ProcessBuilder("which", "firewall-cmd").start();
        isFirewalldPresent = process.waitFor() == 0;

        if (!isFirewalldPresent) {
            process = new ProcessBuilder("which", "iptables").start();
            if (process.waitFor() != 0 ) {
                System.out.println("No firewall backend found...");
                System.out.println("Installing iptables...");
                installIptables();
            }
        }
    } catch (Exception e) {
        System.err.println("Error detecting firewall backend: " + e.getMessage());
        System.exit(1);
    }
}

private void installIptables() {
    try {
        Process process;
        switch (packageManager) {
            case "apt":
                process = new ProcessBuilder("apt", "install", "-y", "iptables").start();
                break;
            case "dnf":
                process = new ProcessBuilder("dnf", "install", "-y", "iptables").start();
                break;
            case "pacman":
                process = new ProcessBuilder("pacman", "-Sy", "--noconfirm", "iptables").start();
                break;
            case "apk":
                process = new ProcessBuilder("apk", "add", "iptables").start();
                break;
            default:
                throw new RuntimeException("Unsupported package manager");
        }
        process.waitFor();
    } catch (Exception e) {
        System.err.println("Error installing iptables: " + e.getMessage());
        System.exit(1);
    }
}

private void detectNetworkInterface() {
    try {
        Process process = new ProcessBuilder("ip", "route", "get", "8.8.8.8").start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();

        if (line != null) {
            String[] parts = line.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("dev")) {
                    physicalInterface = parts[i+1];
                    break;
                }
            }
        }

        if (isFirewalldPresent) {
            process = new ProcessBuilder("firewall-cmd", "--get-default-zone").start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            activeZone = reader.readLine();
        }
    } catch (Exception e) {
        System.err.println("Error detecting network interface: " + e.getMessage());
        System.exit(1);
    }
}

private void configureFirewall(String interfaceName,  boolean isServer) {
    try {
        if (isFirewalldPresent) {

            executeCommand("firewall-cmd", "--zone=" + activeZone, "--add-interface=" + interfaceName);

            if (isServer) {

                executeCommand("firewall-cmd", "--zone=" + activeZone, "--add-masquerade");
                executeCommand("firewall-cmd", "--zone=" + activeZone, "--add-forward");
                executeCommand("firewall-cmd", "--zone=" + activeZone, "--add-port=" + Initiator.listen_port + "/udp");

            }
        } else {
            if (isServer) {

                executeCommand("iptables", "-A", "INPUT", "-p", "udp", "--dport", Initiator.listen_port, "-j", "ACCEPT");
                executeCommand("iptables", "-A", "FORWARD", "-i", interfaceName, "-j", "ACCEPT");
                executeCommand("iptables", "-A", "FORWARD", "-o", interfaceName, "-j", "ACCEPT");
                executeCommand("iptables", "-t", "nat", "-A", "POSTROUTING", "-o", physicalInterface, "-j", "MASQUERADE");

            }
        }
    } catch (Exception e) {
        System.err.println("Error configuring firewall: " + e.getMessage());
        cleanupAndExit();
    }
}

private void cleanupFirewall(String interfaceName, boolean isServer) {
    try {
        if (isFirewalldPresent) {
            
            executeCommand("firewall-cmd", "--zone=" + activeZone, "--remove-interface=" + interfaceName);
            
            if (isServer) {

                executeCommand("firewall-cmd", "--zone=" + activeZone, "--remove-masquerade");
                executeCommand("firewall-cmd", "--zone=" + activeZone, "--remove-forward");

            }
        } else {
            if (isServer) {

                executeCommand("iptables", "-D", "FORWARD", "-i", interfaceName, "-j", "ACCEPT");
                executeCommand("iptables", "-D", "FORWARD", "-o", interfaceName, "-j", "ACCEPT");
                executeCommand("iptables", "-t", "nat", "-D", "POSTROUTING", "-o", physicalInterface, "-j", "MASQUERADE");

            }
        }
    } catch (Exception e) {
        System.err.println("Error cleaning up firewall: " + e.getMessage());
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
    }

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
        interfaceName = interfaceName.substring(0, interfaceName.length() - 5);
        System.out.println("Using conf file"+interfaceName);
    }

    try {
        // Configure firewall
        System.out.println("Configuring firewall for server");
        configureFirewall(interfaceName, isServer);

        System.out.println("Copying configuration file");
        saveAndCopyConfig(configFileName);

        System.out.println("Starting wireguard connection");
        int exitCode = executeCommand("wg-quick", "up", interfaceName);

        if (exitCode != 0) {
            throw new IOException("Failed to start WireGuard interface");
        }

        System.out.println("Connection Started");
        
        // Monitor loop
        boolean monitoring = true;
        StringBuilder screen = new StringBuilder();
        long lastUpdate = 0;
        final long UPDATE_INTERVAL = 2000; // 2 seconds
        
        while (monitoring) {
            long currentTime = System.currentTimeMillis();
            
            // Update display every UPDATE_INTERVAL milliseconds
            if (currentTime - lastUpdate >= UPDATE_INTERVAL) {
                screen.setLength(0); // Clear the StringBuilder
                
                // Move cursor to home position and clear screen
                screen.append("\033[H\033[2J");
                
                // Add header
                screen.append("WireGuard Interface Monitor - ").append(interfaceName).append("\n");
                screen.append("----------------------------------------\n\n");
                
                // Get current status
                Process showProcess = new ProcessBuilder("wg", "show", interfaceName).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(showProcess.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    screen.append(line).append("\n");
                }
                showProcess.waitFor();
                
                // Add footer with options
                screen.append("\n----------------------------------------\n");
                screen.append("Options:\n");
                screen.append("r: Reconnect\n");
                screen.append("q: Quit\n");
                
                // Print the entire screen at once
                System.out.print(screen.toString());
                System.out.flush();
                
                lastUpdate = currentTime;
            }
            
            // Check for user input
            if (System.in.available() > 0) {
                int input = System.in.read();
                switch (input) {
                    case 'r':
                    case 'R':
                        System.out.println("\nReconnecting...");
                        Process downProcess = new ProcessBuilder("wg-quick", "down", interfaceName).start();
                        downProcess.waitFor();
                        Process upProcess = new ProcessBuilder("wg-quick", "up", interfaceName).start();
                        upProcess.waitFor();
                        lastUpdate = 0; // Force immediate refresh
                        break;
                        
                    case 'q':
                    case 'Q':
                        monitoring = false;
                        break;
                }
            }
            
            // Small sleep to prevent CPU overload
            Thread.sleep(100);
        }
        
        // Restore console settings
        Process sttyCooked = new ProcessBuilder("stty", "-raw").inheritIO().start();
        sttyCooked.waitFor();
        
        // Clear screen one last time
        System.out.print("\033[H\033[2J");
        System.out.flush();
        
        System.out.println("Stopping WireGuard connection...");
        cleanupAndExit();
        
    } catch (Exception e) {
        // Ensure console is restored even if there's an error
        try {
            Process sttyCooked = new ProcessBuilder("stty", "-raw").inheritIO().start();
            sttyCooked.waitFor();
        } catch (Exception ex) {
            // Ignore cleanup errors
        }
        
        System.err.println("Error managing WireGuard connection: " + e.getMessage());
        cleanupAndExit();
    }
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