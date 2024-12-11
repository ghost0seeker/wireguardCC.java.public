package com.wireguard.cc.helper;

import java.nio.file.Paths;
//import java.io.File;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

public class Initiator {
    public static String server_name;
    public static String server_subnet;
    public static String server_address;
    public static String listen_port;
    public static String endPoint;

    static {
        System.out.println("Initating init toml file...");
        loadConfig("init.toml");
    }

    public static void loadConfig(String tomlFilePath) {

        try {
            TomlParseResult initToml = Toml.parse(Paths.get(tomlFilePath));

            Initiator.server_name = initToml.getString("server_name");
            Initiator.server_subnet = initToml.getString("server_subnet");
            Initiator.server_address = initToml.getString("server_address");
            Initiator.listen_port = initToml.getString("listen_port");
            Initiator.endPoint = initToml.getString("endpoint");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Initaitor Error: Cannot load init toml");

        }
    }

    // public static void main(String[] args) {

    //     loadConfig("init.toml");

    //     System.out.println("Server Name: " + Initiator.server_name);
    //     System.out.println("Server Subnet: " + Initiator.server_subnet);
    //     System.out.println("Server Address: " + Initiator.server_address);
    //     System.out.println("Listening Port: " + Initiator.listen_port);

    // }
}
