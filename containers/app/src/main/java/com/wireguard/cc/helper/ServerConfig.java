package com.wireguard.cc.helper;

import java.io.IOException;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ServerConfig {


    public static void main(String[] args) {

        WgenKey serverKeys = new WgenKey();

        if (serverKeys.peerPrivateKey64 != null && serverKeys.peerPublicKey64 != null ) {
            System.out.println("------Server Keys Generated.------");
            System.out.println("Server Private key: "+serverKeys.peerPrivateKey64);
            System.out.println("Sever Public Key: "+serverKeys.peerPublicKey64);
        } else {
            System.out.println("Keys not generated!!");
        };

        String address = Initiator.server_address;
        String listenport = Initiator.listen_port;

        //Generate Toml
        String tomlName = Initiator.server_name;
        Path tomlPath = Paths.get(tomlName + ".toml");

        try {
            if (!Files.exists(tomlPath)) {
                Files.createFile(tomlPath);
            }

            TomlParseResult parsed = Toml.parse(tomlPath);

            Map<String, String> updatedValues = new HashMap<>();

            boolean privateKeyUpdated = false;
            boolean publicKeyUpdated = false;

            if (parsed.contains("Server_PrivateKey") ||
            !serverKeys.peerPrivateKey64.equals(parsed.getString("Server_PrivateKey"))) {
                updatedValues.put("Server_PrivateKey", serverKeys.peerPrivateKey64);
                privateKeyUpdated = true;
            }

            if (!parsed.contains("Server_PublicKey")||
            !serverKeys.peerPublicKey64.equals(parsed.getString("Server_PublicKey"))) {
                updatedValues.put("Server_PublicKey", serverKeys.peerPublicKey64);
                publicKeyUpdated = true;
            }

            if (privateKeyUpdated && publicKeyUpdated) {

                updatedValues.put("Server_Address", address);
                updatedValues.put("Listening_Port", listenport);
                updatedValues.put("Server_Name", tomlName);

                StringBuilder tomlContent = new StringBuilder();
                for (Map.Entry<String, String> entry : updatedValues.entrySet()) {
                    System.out.println("----Updating Toml----");
                    tomlContent.append(String.format("%s = \"%s\"\n", entry.getKey(), entry.getValue()));
                    System.out.println(String.format("%s = \"%s\"", entry.getKey(), entry.getValue()));
                }

                if (privateKeyUpdated) {
                    System.out.println("Server Private Key was updated.");
                }
                if (publicKeyUpdated) {
                    System.out.println("Server Public Key was updated.");
                }

                Files.write(tomlPath, tomlContent.toString().getBytes());
                System.out.println("Toml updated.");

            } else {
                System.out.println("No updates were necessary. Both keys are already up to date.");
            }
        
        } catch (IOException e) {
            System.err.println("Error handling TOML file: " + e.getMessage());
        }
    }
}
