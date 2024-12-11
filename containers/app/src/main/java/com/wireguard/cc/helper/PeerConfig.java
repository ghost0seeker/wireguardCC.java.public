package com.wireguard.cc.helper;

//import java.io.BufferedWriter;
//import java.io.FileWriter;
import java.io.IOException;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
//import java.util.HashMap;
import java.util.Map;
//import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class PeerConfig {
    public static void main(String[] args) {

        String tomlName = Initiator.server_name;
        Path tomlPath = Paths.get(tomlName+".toml");
        System.out.println("Using file -> "+tomlPath);

        try {

            if (Files.exists(tomlPath)){

                TomlParseResult parsed = Toml.parse(tomlPath);
                System.out.println("All Keys in TOML File");
                for (String key : parsed.keySet()) {
                    System.out.println(key + " = " + parsed.getString(key));
                }


                Pattern tomlPattern =  Pattern.compile("Peer([A-Z])");
                char nextPeerLetter = 'A';
                boolean peerExists = false;
                
                for (String key : parsed.keySet()) {
                    Matcher matcher = tomlPattern.matcher(key);
                    if (matcher.find()) {
                        peerExists = true;
                        char currentPeer = matcher.group(1).charAt(0);
                        if (currentPeer >= nextPeerLetter) {
                            nextPeerLetter = (char)(currentPeer + 1);
                        }
                    }
                }
                
                
                String peerPrefix = peerExists ? "Peer" + nextPeerLetter : "PeerA";
                String endPoint = Initiator.endPoint;
                
               
                String peerSubnet=null;
                try {
                    String serverAddress = parsed.getString("Server_Address");
                    if (serverAddress == null) {
                        throw new IllegalStateException("Server Address not found in toml file");
                    }

                    if (serverAddress.startsWith("10.")) {
                        peerSubnet = serverAddress.substring(0, serverAddress.lastIndexOf(('.')));
                    } else {
                        throw new IllegalStateException("Server Address does not start with expected prefix");
                    }

                    int slashIndex = peerSubnet.indexOf('/');
                    if (slashIndex != -1) {
                        peerSubnet = peerSubnet.substring(0, slashIndex);
                    }
                } catch (IllegalStateException e){
                        // 3. If not exists or doesn't match expected format, return error
                        System.err.println("Error: " + e.getMessage());
                        // You might want to throw this exception again if you want to halt execution
                        // throw e; 
                }

                if (peerSubnet != null){
                    if(peerConfGenerator(peerPrefix, peerSubnet, endPoint)) {
                        System.out.println("Peer Configured");
                    } else {
                        System.out.println("Peer configuration failed, Check toml/source");
                    }
                } else {
                    System.err.println("Subnet not defined or initialised.");
                }

            } else {
                System.err.println("Server Toml File Not Found.");
            }    
        } catch (IOException e) {
            System.err.println("Error handling TOML file: " + e.getMessage());
        }
    }

    public static int letterToNumber(char letter) {
        return Character.toUpperCase(letter) - 'A' + 2;
    }

    private static boolean peerConfGenerator(String peerPrefix, String peerSubnet, String endPoint) {

        WgenKey peerKeys = new WgenKey();
        if (peerKeys.peerPrivateKey64 == null || peerKeys.peerPublicKey64 == null) {
            System.err.println("failed to generate peer keys");
            return false;
        }
        
        System.out.println("-----Peer Keys Generated-----");
        System.out.println("Peer# Private Key: "+peerKeys.peerPrivateKey64);
        System.out.println("Peer# Public Key: "+peerKeys.peerPublicKey64);

        char peerLetter = peerPrefix.charAt(peerPrefix.length() - 1 );
        int peerNumber = letterToNumber(peerLetter);
        String position = String.valueOf(peerNumber);
        System.out.println("Configuring Peer: "+peerLetter+" at position: "+peerNumber);

        Map<String, String> peerAttributes = Map.of(
            peerPrefix+"_Address", peerSubnet+"."+position+"/24",
            peerPrefix+"_PrivateKey", peerKeys.peerPrivateKey64,
            peerPrefix+"_PublicKey", peerKeys.peerPublicKey64,
            peerPrefix+"_Server_Address", peerSubnet +"."+position+"/32",
            peerPrefix+"_AllowedIPS", peerSubnet +".0"+"/24",
            peerPrefix+"_Endpoint", endPoint +":"+ Initiator.listen_port
        );
        
        try {
            String tomlName = Initiator.server_name;
            Path tomlPath = Paths.get(tomlName+".toml");
            List<String> existingToml = Files.readAllLines(tomlPath);
    
            StringBuilder tomlContent = new StringBuilder();
    
            for (String line : existingToml) {
                tomlContent.append(line).append("\n");
            }
    
            for (Map.Entry<String, String> entry : peerAttributes.entrySet()){
            //Need to learn about Map.Entry<> and entrySet
                System.out.println("-----updating toml-----");
                tomlContent.append(String.format("%s=\"%s\"\n", entry.getKey(), entry.getValue()));
                System.out.println(String.format("%s=\"%s\"", entry.getKey(), entry.getValue()));
            }
            Files.write(tomlPath, tomlContent.toString().getBytes());
            System.out.println("Updated Peer Attributes in Toml File");
            return true;
        } catch (IOException e) {
            System.err.println("Error handling TOML file: " + e.getMessage());
            return false;
        }
    }
}
