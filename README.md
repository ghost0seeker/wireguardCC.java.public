# wireguardCC.java

# Wireguard Control Center (wireguardCC)

**wireguardCC** is a Java-based command-line tool designed to simplify the process of setting up and managing WireGuard VPN connections. It provides an intuitive interface for creating and configuring servers, peers, and firewall rules, making WireGuard VPN deployment accessible for all users.

---

## Features

1. **Server Management**:
   - Initialize a new server configuration from `init.toml`.
   - Generate and save WireGuard server configuration files.

2. **Peer Management**:
   - Create new peers with unique IPs within the server subnet.
   - Validate peer keys against the server keys before generating configuration files.

3. **Firewall and Network Configuration**:
   - Detect OS, package manager, and physical network interfaces.
   - Configure `firewalld` or `iptables` rules for NAT, masquerading, and forwarding.
   - Automatically install missing dependencies like `iptables` or `wireguard-tools`.

4. **Connection Manager (Beta)**:
   - Establish WireGuard connections as a server or peer.
   - Save and copy configuration files to `/etc/wireguard`.
   - Start, reconnect, or terminate WireGuard connections.
   - Manage active connections with comprehensive status checks and dynamic updates to firewall rules.

---

## Installation

### Prerequisites

1. **Java Runtime Environment**:
   - Install OpenJDK.
     ```bash
     sudo apt install openjdk-11-jdk  # For Debian-based systems
     sudo dnf install java-11-openjdk # For Fedora-based systems
     sudo pacman -S jdk11-openjdk     # For Arch-based systems
     sudo apk add openjdk11          # For Alpine Linux
     ```

2. **Apache Maven**:
   - Install Maven for building the project.
     ```bash
     sudo apt install maven          # For Debian-based systems
     sudo dnf install maven          # For Fedora-based systems
     sudo pacman -S maven            # For Arch-based systems
     sudo apk add maven              # For Alpine Linux
     sudo apt install openjdk-11-jdk  # For Debian-based systems
     ```


---

## Usage

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/ghost0seeker/wireguardCC.java.public
   cd wireguardCC.java.public
   ```

2. **Build the Project**:
   ```bash
   mvn clean package
   ```

3. **Run the Program**:
   ```bash
   sudo java -jar target/wireguardCC*.jar #Replace * with server name you defined in init.toml
   ```

4. **Follow On-Screen Instructions**:
   - The CLI will guide you through setting up servers, creating peers, and managing connections.

---

## Configuration File: `init.toml`

The `init.toml` file provides initial values for server configuration. Below is an example:

```toml
server_name = "Wireguard-03.alpha"
server_subnet = "10.1.1.0/24"
server_address = "10.1.1.1/24"
listen_port = "54789"
endpoint = "172.2.0.101"
dns = "10.1.1.3" #(optional)
```

- **server_name**: Unique identifier for the server.
- **server_subnet**: Subnet for server and peers.
- **server_address**: Address for the WireGuard server.
- **listen_port**: Port for WireGuard communication.
- **endpoint**: Public or reachable endpoint of the server.

---

## Features in Detail

### 1. Server Management
- Automatically create a server configuration if it doesn't exist.
- Validate and update existing configurations using `ServerConfig`.

### 2. Peer Management
- Automatically assign unique IPs to new peers (e.g., PeerA, PeerB).
- Verify peer keys using Curve25519 encryption.
- Update the `init.toml` file with new peer configurations.

### 3. Firewall Integration
- Detect existing firewall backends (`firewalld` or `iptables`).
- Apply necessary rules for NAT and masquerading.
- Install missing dependencies if required.

### 4. Connection Manager
- Seamlessly start WireGuard interfaces.
- Handle reconnection and cleanup of firewall rules upon termination.

---

## Third-Party Libraries

This project uses the following third-party library:

- **ecdh-curve25519-java** ([GitHub Repository](https://github.com/balena/ecdh-curve25519-java))
  - Custom Diffie-Hellman key pair generator and key agreement for the Curve25519 algorithm by Dan Bernstein.
  - Licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0). See the repository for more details.

---

## Known Issues

- **Beta Features**:
  - The Connection Manager feature is in beta and may cause unexpected behavior. Use caution and backup your network configuration before enabling this feature.

- **Firewall Rules**:
  - Ensure existing firewall configurations are compatible with the rules applied by wireguardCC.

---

## Future Features

1. **Database Integration**:
   - Enable peer updates and management from any connection by integrating with a database.

2. **Improved Testing and Exception Handling**:
   - Add extensive tests to improve reliability.
   - Enhance exception handling for smoother user experience.

---

## Contribution

I welcome contributions! Feel free to open issues or submit pull requests to enhance wireguardCC.

---

## License

This project is licensed under the [MIT License](LICENSE).

---

## Acknowledgments

- [WireGuard](https://www.wireguard.com/) for their amazing VPN technology.
- Java TOML parser library for configuration parsing.
- Curve25519 for encryption key generation.
- [ecdh-curve25519-java](https://github.com/balena/ecdh-curve25519-java) for the Curve25519 key generation library.


