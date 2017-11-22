# Comm Protocols
This library wraps network communication protocols into classes that provide simplified implementations for them.
Supported protocols currently include:
- TCP/IP
- UDP
- Bluetooth RFComm (server only)

## Current Release

## Add as a Dependency

## How to Use
Here are brief how-to-use for each of the major protocol implementations and their classes.

### TCP/IP
For TCP communication, we have a TCPServer class to handler server side events and a TCPClient class that connects to a TCP server and make requests.
To create a TCPServer and listen for incoming tcp client connections, we do this:
```java
TCPServer tcpServer = new TCPServer(tcpServerCallback);
tcpServer.setAllowMultipleConnectionsFromSameAddress(false);
tcpServer.start(5001);
```
Above, `tcpServerCallback` is simply an instance of the `TCPServer.Callback` interface. That is where you get a change to react to incoming requests from connected clients. Here is an example:
```java
TCPServer.Callback tcpServerCallback = new TCPServer.Callback() {
    @Override
    public void onWaitingForConnection(int port) {
        System.out.println("TCP server: waiting for connection on port " + port);
    }

    @Override
    public void onConnected(TCPServer.Connection connection) {
        System.out.println("TCP server: accepted connection from client at " + connection.getRemoteHostAddress().toString());
    }

    @Override
    public void onDataReceived(TCPServer.Connection connection, DataPacket dataPacket) {
        String log = String.format("TCP server: Data packet received from %s:%d. data = %s",
                                   dataPacket.address.toString(),
                                   dataPacket.port,
                                   dataPacket.data);
        System.out.println(log);
        System.out.println("TCP server: responding...");
        connection.respond("Message received and parsed for data(" + dataPacket.data + ")");
    }
};
```

Next, we connect to the server using a TCPClient instance. Here is how it's done:
```java
tcpClient = new TCPClient(tcpClientCallback);
tcpClient.setSocketTimeOut(15000);
tcpClient.setNullResponseBad(false);
tcpClient.connect(InetAddress.getByName("127.0.0.1"), 5001);
```
Above, `tcpClientCallback` is an instance of `TCPClient.Callback` and provides feedback on connection attempts and response received from the server. Here is an example:
```java
private static TCPClient.Callback tcpClientCallback = new TCPClient.Callback() {
    @Override
    public void onConnected(InetAddress serverIp, int port) {
        System.out.println("TCP client: connected to server at " + serverIp.toString() + ":" + port);
    }

    @Override
    public void onConnectionFailed(InetAddress serverIp, int port) {
        System.out.println("TCP client: failed to connect to server at " + serverIp.toString() + ":" + port);
    }

    @Override
    public void onDisconnected() {
        System.out.println("TCP client: disconnected from server.");
    }

    @Override
    public void onDataReceived(DataPacket data) {
        System.out.println("TCP client: response received from server: " + data.data);
    }
};
```
After successfully connecting to a server, you can now post requests like so:
```java
tcpClient.query("hello server...");
// Some time ellapse and some stuff got done and we need to ping server again...
tcpClient.query("say something fancy please?");
```

Closing the connections, for both the server and client, is straight forward:

```java
tcpClient.disconnect(); // Closing the client connection
tcpServer.stop(); // Retiring the server
```

### UDP

### Bluetooth RFComm
