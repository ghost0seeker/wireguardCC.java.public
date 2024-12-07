package com.wireguard.cc.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConsoleMonitor {
    

    private final void monitorConnection() {

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
    }
}
