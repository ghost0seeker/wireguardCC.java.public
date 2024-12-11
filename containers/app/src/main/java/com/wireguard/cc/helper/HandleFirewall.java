package com.wireguard.cc.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class HandleFirewall {

    public static boolean isFirewalldPresent;
    public static String packageManager;
    private static String physicalInterface;
    private static String activeZone;
    private ProcessBuilder processBuilder;


    public HandleFirewall() {

        this.processBuilder = new ProcessBuilder();
    
        this.processBuilder.redirectErrorStream(true);
        
        detectOS();

        detectFirewall();

    }


    private int executeCommand(String... command) throws IOException, InterruptedException {
        this.processBuilder.command(command);
        processBuilder.redirectErrorStream(true);
        Process process = this.processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        return process.waitFor();
    }


    private static void detectOS() {
        try {
            Process process = new ProcessBuilder("cat", "/etc/os-release").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean packageManagerFound = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("ID=")) {
                    System.out.println("Found ID line: [" + line + "]"); // Debug line
                    String id  = line.substring(3)
                                        .trim()
                                        .replace("\"", "")
                                        .toLowerCase();
                    System.out.println("Processed OS ID: [" + id + "]");
                    switch(id) {
                        case "ubuntu":
                        case "debian":
                            // osType = "debian";
                            packageManager = "apt";
                            packageManagerFound = true;
                            System.out.println("OS Type: Debian-based. Using " + packageManager + " package manager");
                            break;
                        case "fedora":
                        case "rhel":
                        case "centos":
                            // osType = "redhat";
                            packageManager = "dnf";
                            packageManagerFound = true;
                            System.out.println("OS Type: Red Hat-based. Using  " + packageManager + " package manager");
                            break;
                        case "arch":
                        case "endeavouros":
                            // osType = "arch";
                            packageManager = "pacman";
                            packageManagerFound = true;
                            System.out.println("OS Type: Arch-based. Using " + packageManager + " package manager");
                            break;
                        case "alpine":
                            // osType = "alpine";
                            packageManager = "apk";
                            packageManagerFound = true;
                            System.out.println("OS Type: Alpine. Using " + packageManager + " package manager");
                            break;
                        // default:
                        // System.out.println("Unsupported OS: "+ id);
                        // System.exit(1);
                    }
                    if (packageManagerFound) {
                    break;
                    }
                }
            }

            if (!packageManagerFound) {
                throw new RuntimeException("Unable to detect package manager from /etc/os-release");
            }
            
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new RuntimeException("Failed to read /etc/os-release");
            }
            
        } catch (Exception e) {
            System.err.println("Critical error detecting OS: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void detectNetworkInterface() {
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


    public void detectFirewall() {
        try {
            Process process = new ProcessBuilder("which", "firewall-cmd").start();
            isFirewalldPresent = process.waitFor() == 0;

            if (!isFirewalldPresent) {
                process = new ProcessBuilder("which", "iptables").start();
                if (process.waitFor() != 0 ) {
                    System.out.println("No firewall backend found...");
                    System.out.println("Installing iptables...");
                    installIptables();
                } else {
                    System.out.println("Found firewall: iptables");
                }
            } else {
                System.out.println("Found firewall: firewald");
            }
        } catch (Exception e) {
            System.err.println("Error detecting firewall backend: " + e.getMessage());
            System.exit(1);
        }
    }


    private void installIptables() {
        try {
            
            switch (packageManager) {
                case "apt":
                    System.out.println("Installing iptables using APT package manager...");
                    executeCommand("apt", "update");
                    executeCommand("apt", "install", "-y", "iptables");
                    break;
                case "dnf":
                    System.out.println("Installing iptables using DNF package manager (Fedora/RHEL)...");
                    executeCommand("dnf", "install", "-y", "iptables");
                    break;
                case "pacman":
                    System.out.println("Installing iptables using Pacman package manager (Arch Linux)...");
                    executeCommand("pacman", "-Sy", "--noconfirm", "iptables");
                    break;
                case "apk":
                    System.out.println("Installing iptables using APK package manager (Alpine Linux)...");
                    executeCommand("apk", "add", "iptables");
                    break;
                default:
                    throw new RuntimeException("Unsupported package manager");
            }
            
            System.out.println("iptables installation completed successfully.");
        } catch (Exception e) {
            System.err.println("Error installing iptables: " + e.getMessage());
            System.exit(1);
        }
    }


    public void configureFirewall(String interfaceName, boolean isServer) {
        try {
            if (isFirewalldPresent) {

                executeCommand("firewall-cmd", "--zone=" + activeZone, "--add-interface=" + interfaceName);
                executeCommand("firewall-cmd", "--zone=" + activeZone, "--add-interface=" + physicalInterface);


                if (isServer) {

                    System.out.println("Configuring firewalld...");
                    executeCommand("firewall-cmd", "--zone=" + activeZone, "--add-masquerade");
                    executeCommand("firewall-cmd", "--zone=" + activeZone, "--add-forward");
                    executeCommand("firewall-cmd", "--zone=" + activeZone, "--add-port=" + Initiator.listen_port + "/udp");

                }
            } else {
                if (isServer) {
                    try {
                        flushIptables();
                        System.out.println("Adding new iptables rules...");

                        System.out.println("Adding Handshake UDP port rule...");
                        executeCommand("iptables", "-A", "INPUT", "-p", "udp", "--dport", Initiator.listen_port, "-j", "ACCEPT");

                        System.out.println("Adding FORWARD rules...");
                        executeCommand("iptables", "-A", "FORWARD", "-i", interfaceName, "-j", "ACCEPT");
                        executeCommand("iptables", "-A", "FORWARD", "-o", interfaceName, "-j", "ACCEPT");

                        System.out.println("Adding NAT masquerade rule...");
                        executeCommand("iptables", "-t", "nat", "-A", "POSTROUTING", "-o", physicalInterface, "-j", "MASQUERADE");
                    
                        System.out.println("All iptables rules added successfully");
                    } catch (Exception e) {
                        System.err.println("Error configuring iptables: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error configuring firewall: " + e.getMessage());
            //cleanupAndExit();
            cleanupFirewall(isServer);
        }
    }

    private void cleanupFirewall(boolean isServer) {
        try {
            if (isFirewalldPresent) {
                //executeCommand("sudo", "firewall-cmd", "--zone=" + activeZone, "--remove-interface=" + interfaceName);
                if (isServer) {
                    System.out.println("Removing firewalld rules");
                    executeCommand("firewall-cmd", "--reload");
                }
            } else {
                if (isServer) {
                    flushIptables();
                }
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up firewall: " + e.getMessage());
        }
    }


    private void flushIptables() throws IOException, InterruptedException {
        System.out.println("Flushing iptables rules....");
        executeCommand("iptables", "-F");
        executeCommand("iptables", "-X");
        executeCommand("iptables", "-t", "nat", "-F");
        executeCommand("iptables", "-t", "nat", "-X");
        System.out.println("iptables rules flushed successfully");

        // Set default policies to ACCEPT
        System.out.println("Setting default policies...");
        executeCommand("iptables", "-P", "INPUT", "ACCEPT");
        executeCommand("iptables", "-P", "FORWARD", "ACCEPT");
        executeCommand("iptables", "-P", "OUTPUT", "ACCEPT");
        System.out.println("Default policies set to ACCEPT");

    }
}
