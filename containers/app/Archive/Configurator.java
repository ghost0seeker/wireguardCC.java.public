package com.wireguard.cc;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Configurator {
    private TomlParseResult configData;
    public static final String CONFIG_DIR = "configs";
    public static final String PEER_CONFIG_DIR = CONFIG_DIR + File.separator + "peers";
    public static final String SERVER_CONFIG_DIR = CONFIG_DIR + File.separator + "server";

    private static final String PEER_CONFIG_TEMPLATE = 
        "[Interface]\n" +
        "Address = %s\n" +
        "PrivateKey = %s\n" +
        "[Peer]\n" +
        "AllowedIPs = %s\n" +
        "PersistentKeepalive = 25\n" + 
        "PublicKey = %s\n";
    
    private static final String SERVER_CONFIG_TEMPLATE = 
        "[Interface]\n" +
        "Address = 10.8.16.1/24\n" +
        "ListenPort = 6010\n" +
        "PrivateKey = %s\n" +
        "#DNS = 10.8.16.1\n\n" +
        "%s"; // This will be filled with peer configurations
        
    public Configurator() {
        configData = null;
        //Why?
    }

    public void readServerToml() throws IOException {
        File currentDir = new File(".");
        File[] files = currentDir.listFiles((dir, name) -> name.matches(".*Wireguard.*\\.toml"));

        if (files == null || files.length==0) {
            throw new FileNotFoundException("No Wireguard TOML file found in the current directory.");
        }

        configData = Toml.parse(files[0].toPath());
        if (configData.hasErrors()) {
            throw new IOException("Error parsing toml files: " + configData.errors());
        }

        System.out.println("Available peers:");
        for (String key : configData.keySet()) {
            if (key.startsWith("Peer") && key.endsWith("_PublicKey")) {
                System.out.println(key.substring(0, key.indexOf("_")));
            }
        }
    }
    
    public void selectAndConfigurePeer() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Select a peer [A-Z] or 'S' for server: ");
        String selection = scanner.nextLine().toUpperCase();

        if (selection.equals("S")) {
            ServerConfigManager.createOrUpdateServerConfig(this, scanner);
        } else { 
            String selectedPeer = "Peer" + selection;
            PeerConfigManager.createOrUpdatePeerConfig(this, selectedPeer, scanner);
        }
    }

    public void savePeerJsonConfig(String selectedPeer, String config, String comments) throws IOException {
        String jsonFileName = PEER_CONFIG_DIR + File.separator + selectedPeer + ".json";
        File jsonFile = new File(jsonFileName);

        JSONObject jsonObject;
        if (jsonFile.exists()) {
            String jsonContent = new String(Files.readAllBytes(jsonFile.toPath()));
            jsonObject = new JSONObject(jsonContent);
        } else {
            jsonObject = new JSONObject();
            jsonObject.put("configs", new JSONArray());
        }

        JSONArray configs = jsonObject.getJSONArray("configs");
        JSONObject newConfig = new JSONObject();
        newConfig.put("id", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        newConfig.put("config", config);
        newConfig.put("comments", comments);
        configs.put(newConfig);

        try (FileWriter file = new FileWriter(jsonFileName)) {
            file.write(jsonObject.toString(2));
        }
    }

    public String promptForConfFileName(Scanner scanner) {
        System.out.println("Enter a name for .conf file (without extension):");
        return scanner.nextLine() + ".conf";
    }

    public void writeConfFile(String fileName, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(content);
            System.out.println("Configuration file " + fileName + " has been created:");
        } catch (Exception e) {
                e.printStackTrace();
        }
    }

    public TomlParseResult getConfigData() {
        return configData;
    }

    public static String getPeerConfigTemplate() {
        return PEER_CONFIG_TEMPLATE;
    }

    public static String getServerConfigTemplate() {
        return SERVER_CONFIG_TEMPLATE;
    }

    public static void main(String[] args) {
        try { 
            Configurator manager = new Configurator();
            manager.readServerToml();
            manager.selectAndConfigurePeer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
