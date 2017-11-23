# Comm Protocols
This library wraps network communication protocols into classes that provide simplified implementations for them.
Supported protocols currently include:
- TCP
- UDP
- Bluetooth RFComm (server implementation only)

## Current Release
Work in progress...

## Add as a Dependency
For maven:
```
<dependency>
  <groupId>com.linoagli.java</groupId>
  <artifactId>comm-protocols</artifactId>
  <version>1.0</version>
  <type>pom</type>
</dependency>
```

For gradle:
```
compile 'com.linoagli.java:comm-protocols:1.1'
```

## How to Use
Here are brief how-to-use for each of the major protocol implementations and their classes.

### TCP
For TCP communication, we have a `TCPServer` class to handler server side events and a `TCPClient` class that connects
 to a TCP server and make requests.
Here is an example of how to create a `TCPServer` and listen for incoming tcp client connections:
```java
TCPServer tcpServer = new TCPServer(tcpServerCallback);
tcpServer.setAllowMultipleConnectionsFromSameAddress(false);
tcpServer.start(5001);
```
Above, `tcpServerCallback` is simply an instance of the `TCPServer.Callback` interface. That is where you get a change 
to react to incoming requests from connected clients. Here is an example:
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
        System.out.println("TCP server: Data packet received from client: " + dataPacket.data);
        System.out.println("TCP server: responding...");
        connection.respond("Message received and parsed for data(" + dataPacket.data + ")");
    }
};
```

Next, let's look at how to connect to the server using a `TCPClient` instance:
```java
tcpClient = new TCPClient(tcpClientCallback);
tcpClient.connect(InetAddress.getByName("127.0.0.1"), 5001);
```
Above, `tcpClientCallback` is an instance of `TCPClient.Callback` interface which provides feedback on connection 
attempts and response received from the server. Here is an example:
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
For UDP communication, we have a `UDPListener` class that handles listening incoming packets and a `UDPSender` class
that is used for sending out UDP packets.

Here is an example of how to start a `UDPListener`:
```java
UDPListener udpListener = new UDPListener(5001, udpCallback);
udpListener.start();
```
Above, `udpCallback` is an instance of the `UDPListener.Callback` interface which provides the means to react to
incoming messages. Here is an example:
```java
UDPListener.Callback udpCallback = new UDPListener.Callback() {
    @Override
    public void onStarted(int port) {
        System.out.println("UDP Listener: started listening to port " + port);
    }

    @Override
    public void onStopping() {
        System.out.println("UDP Listener: stopped listening for packets");
    }

    @Override
    public void onDataReceived(DataPacket dataPacket) {
        System.out.println("UDP Listener: data packet received: " + dataPacket.data);
    }
};
```

Now that we know how to listen for UDP messages, we want to know how to them, and it's a simple 1 liner:
```java
UDPSender.send("127.0.0.1", 5001, "A Message in a bottle.");
```

### Bluetooth RFComm
Work in progress...