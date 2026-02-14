# Unde Server

`unde-server` is a middleware application designed to facilitate debugging and network monitoring for Android devices. It acts as a bridge between Android devices and local development tools, utilizing ADB for device management and providing real-time data streaming.

## Features

- **ADB Device Management**: Automatically detects connected Android devices and sets up reverse port forwarding (`adb reverse`).
- **Network Traffic Monitoring**:
  - Receives network traffic data from Android devices via **Plain TCP Sockets**.
  - Broadcasts network events to local clients (e.g., UI frontends) via **WebSockets**.
- **Dual Connection Architecture**:
  - **Remote Connection**: Handles communication with the Android device.
  - **Local Connection**: Handles communication with local analysis/UI tools.
- **Cross-Platform Support**: Runs on any system with JDK and ADB installed.

## Prerequisites

Before running the server, ensure you have the following installed:

- **Java Development Kit (JDK)**: Version 11 or higher.
- **Android Debug Bridge (ADB)**: Must be installed and accessible in your system's PATH.

## Architecture & Components

The server is built with Ktor and follows a modular architecture:

### Core Components

- **`AdbManager`**: 
  - Monitors connected Android devices.
  - Automatically executes `adb reverse tcp:8081 tcp:8081` to allow devices to connect to the server.

- **`ServerSocketConnection` (Remote)**:
  - Listens on a TCP port (default: 8081) for incoming connections from Android devices.
  - Receives raw data (Network, Logcat(coming soon), Database traces(coming soon)) via plain sockets.

- **`WSLocalConnection` (Local)**:
  - Manages WebSocket connections for local clients (e.g., a desktop UI or web dashboard).
  - streams the data received from devices to the frontend for visualization.

## Building

### Server Jar

To build the executable JAR file:

```bash
./gradlew :server:buildFatJar
```

### Desktop Application

You can package the server as a standalone desktop application (installer or portable) using the provided scripts.

#### 1. Requirements

- **Linux/macOS**: Standard build tools (bash).
- **Windows Installer**: [WiX Toolset](https://wixtoolset.org) must be installed and in your PATH.

#### 2. Build Scripts

**Linux / macOS:**

| Type      | Command                         | Description                                      |
|-----------|---------------------------------|--------------------------------------------------|
| Installer | `./scripts/build-installer.sh`  | Creates a native installer (deb/rpm/pkg/dmg)     |
| Portable  | `./scripts/build-portable.sh`   | Creates a portable executable directory          |

**Windows:**

| Type      | Command                          | Description                                      |
|-----------|----------------------------------|--------------------------------------------------|
| Installer | `.\scripts\build-installer.bat`  | Creates an .msi installer (Requires WiX Toolset) |
| Portable  | `.\scripts\build-portable.bat`   | Creates a portable .exe directory                |

#### 3. Output

Build artifacts will be generated in: `server/build/jpackage/`

## Running

### From Command Line

To run the server directly:

```bash
./gradlew :server:run
```

### From Docker

To run using the local Docker image:

1. Build the image:
    ```bash
    ./gradlew :server:buildImage
    ```
2. Run the image:
    ```bash
    ./gradlew :server:runDocker
    ```
