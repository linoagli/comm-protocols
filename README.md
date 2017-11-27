# Comm Protocols
This library wraps commonly used connections/communication protocols into classes that provide simple implementations 
for them. The goal is be lightweight and provide ease of use with very little to no learning curve. 
Other libraries that are more robust, feature rich exist and provide more control over these implementations exist.
Supported protocols currently include:
- TCP
- UDP
- HTTP
- Bluetooth RFComm (server implementation only)

## Current Version
[ ![Download](https://api.bintray.com/packages/linoagli/maven-repo/comm-protocols/images/download.svg) ](https://bintray.com/linoagli/maven-repo/comm-protocols/_latestVersion)

###### Add as a maven dependency
```
<dependency>
  <groupId>com.linoagli.java</groupId>
  <artifactId>comm-protocols</artifactId>
  <version>1.1</version>
  <type>pom</type>
</dependency>
```

###### Add as a gradle dependency
```
compile 'com.linoagli.java:comm-protocols:1.1'
```

## How to Use
Here are brief how-to-use for each of the major protocol implementations and their classes.

#### For TCP connections,
we have a `TCPServer` class to handler server side events and a `TCPClient` class that connects
to a TCP server and make requests.
Here is an example of how to create a `TCPServer` and listen for incoming tcp client connections:
```java
TCPServer tcpServer = new TCPServer(tcpServerCallback);
tcpServer.setAllowMultipleConnectionsFromSameAddress(false);
tcpServer.start(5001);
```
Above, `tcpServerCallback` is an instance of the `TCPServer.Callback` interface which provides the means to process and
respond to client requests. Here is an example:
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
        
        connection.respond("Server has processed data with value(" + dataPacket.data + ")");
    }
};
```
Notice how above, we have `connection.respond("Message received and parsed for data(" + dataPacket.data + ")");`. This
is how we the server sends a response data to the client that sent it data. **You are required to send a response
back to the tcp client.** Failing to do so will result in the tcp client waiting **forever** for a response from
the server and being _stuck_ on that particular query.

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

#### For UDP communications,
we have a `UDPListener` class that handles listening incoming packets and a `UDPSender` class
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

#### For HTTP connections,
we have the `HttpRequest` class which provides pretty straightforward way of make HTTP requests (a)synchronously.
<br />
Here is an example of how to make a simple GET request in one line:
```java
String response = new HttpRequest().post(HttpRequest.Method.GET,"https://httpbin.org/get", null)
                                   .getResponseString();
```
Here is an example of how to make a POST request with web form data:
```java
HttpRequest.ParamsBuilder paramsBuilder = new HttpRequest.ParamsBuilder();
paramsBuilder.add("token", "1234567890", false); // param with raw value
paramsBuilder.add("message", "a weird message!", true); // param with url encoded value

HttpRequest request = new HttpRequest().post(HttpRequest.Method.POST, "https://httpbin.org/post", paramsBuilder.toString());
int responseCode = request.getResponseCode();
String responseString = request.getResponseString();
```
Here is an example of how to make an asynchronous request:
```java
HttpRequest.Callback requestCallback = new HttpRequest.Callback() {
    @Override
    public void onRequestSuccess(int responseCode, String responseString) {
        System.out.println(responseCode + " - " + responseString);
    }

    @Override
    public void onRequestFailure(int errorCode, String errorString) {
        System.out.println(errorCode + " - " + errorString);
    }
};

request.postAsync(method, url, paramsBuilder.toString(), requestCallback);
```

#### For Bluethooth RFComm connections,
we have a `BluetoothRFCommServer` for service creation.
The bluetooth implementations are powered by the [bluecove library](http://www.bluecove.org/)

Here is an example of how to create an RFComm server instance and start it:
```java
String serviceUUID = UUID.randomUUID().toString();
BluetoothRFCommServer rfCommServer = new BluetoothRFCommServer(rfCommCallback);
rfCommServer.start(serviceUUID);
```
Above, `rfCommCallback` is an instance of the `BluetoothRFCommServer.Callback` which provides the means to process and 
respond to client requests. Here is an example:
```java
BluetoothRFCommServer.Callback rfCommCallback = new BluetoothRFCommServer.Callback() {
    @Override
    public void onWaitingForConnection(String serviceUUID) {
        System.out.println("RFComm: waiting for connection with server id: " + serviceUUID);
    }
    
    @Override
    public void onConnected() {
        System.out.println("RFComm: connected to a bluetooth client.");
    }
    
    @Override
    public void onDataReceived(BluetoothRFCommServer bluetoothServer, String data) {
        System.out.println("RFComm: data received from client: " + data);
        System.out.println("RFComm: data responding...");
    
        bluetoothServer.respond("Server has processed data with value(" + data + ")");
    }
};
```