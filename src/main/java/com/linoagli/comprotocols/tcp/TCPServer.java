/**
 * comm-protocols Project.
 * com.linoagli.java.comprotocols.tcp
 *
 * @author Olubusayo K. Faye-Lino Agli, username: linoagli
 */
package com.linoagli.comprotocols.tcp;

import com.linoagli.comprotocols.DataPacket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Provides a simplified server side implementation of a TCP connection.
 */
public class TCPServer {
    public static final String LINE_PING_RESPONSE = "comprotocols-response-mRPrLr5t2hURfDULcReMQf7BWsazASUJ";

    private Callback callback;

    private int port;
    private boolean allowMultipleConnectionsFromSameAddress = false;

    private ServerSocket serverSocket;
    private List<Connection> connections = new ArrayList<Connection>();
    private ActiveConnectionsThread activeConnectionsThread;
    private IncomingConnectionsThread incomingConnectionsThread;

    public TCPServer(Callback callback) {
        this.callback = callback;
    }

    /**
     * @return the port this server is currently listening to.
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets a flag that specifies how this server treats multiple incoming connections from the
     * same IP address:
     * - If set, the server will create a new connection instance every time, essentially hosting
     * multiple clients originating from the same IP address.
     * - If cleared, the server will close the "old" connection and create a new one, essentially
     * making sure only one connection exists for that IP address.
     *
     * @param allow the flag
     */
    public void setAllowMultipleConnectionsFromSameAddress(boolean allow) {
        this.allowMultipleConnectionsFromSameAddress = allow;
    }

    /**
     * @return whether or not this server instance is up and running (listening for incoming connections and data)
     */
    public boolean isRunning() {
        boolean isIncomingConnectionsManagerRunning = incomingConnectionsThread != null && incomingConnectionsThread.isRunning;
        boolean isActiveConnectionsManagerRunning = activeConnectionsThread != null && activeConnectionsThread.isRunning;

        return isIncomingConnectionsManagerRunning && isActiveConnectionsManagerRunning;
    }

    /**
     * Boots up this server instance and starts listening for incoming TCP client connections.
     *
     * @param port the port this server is listening to
     */
    public void start(int port) {
        this.port = port;

        try {
            serverSocket = new ServerSocket(port);

            activeConnectionsThread = new ActiveConnectionsThread();
            activeConnectionsThread.start();

            incomingConnectionsThread = new IncomingConnectionsThread();
            incomingConnectionsThread.start();
        }
        catch (Exception e) {
            e.printStackTrace();
            stop();
        }
    }

    /**
     * This will close all active TCP connections, power down the server instance and clean up all resources.
     */
    public void stop() {
        if (incomingConnectionsThread != null) {
            incomingConnectionsThread.cancel();
            incomingConnectionsThread = null;
        }

        if (activeConnectionsThread != null) {
            activeConnectionsThread.cancel();
            activeConnectionsThread = null;
        }

        cleanUp();
    }

    private void cleanUp() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            serverSocket = null;
        }

        if (connections != null) {
            for (Connection connection : connections) connection.close();

            connections.clear();
            connections = null;
        }
    }

    /**
     * This class represents an active connection to a TCP client. This connection will remain active
     * and listening while the client is connected and actively sending queries.
     */
    public class Connection {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private boolean isListening = false;

        public Connection(Socket socket) {
            this.socket = socket;
        }

        public boolean isListening() {
            return isListening;
        }

        public InetAddress getRemoteHostAddress() {
            return socket.getInetAddress();
        }

        public InetAddress getLocalHostAddress() {
            return socket.getLocalAddress();
        }

        private void listen() {
            new Thread() {
                @Override
                public void run() {
                    isListening = true;

                    try {
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out = new PrintWriter(socket.getOutputStream(), true);

                        if (callback != null) callback.onConnected(Connection.this);

                        String input;

                        while ((input = in.readLine()) != null) {
                            if (input.equals(TCPClient.LINE_PING_QUERY)) {
                                System.out.println("TCP client line ping received, we shall now cordially respond...");
                                respond(LINE_PING_RESPONSE);
                            }
                            else {
                                if (callback != null) callback.onDataReceived(Connection.this, new DataPacket(socket.getInetAddress(), port, input));
                            }
                        }
                    } catch (Exception e) {
                        if (e instanceof SocketException) {
                            System.out.println(e.getMessage());
                        } else {
                            e.printStackTrace();
                        }
                    }

                    isListening = false;
                }
            }.start();
        }

        /**
         * Send data as a response to the client that linked to this connection.
         *
         * @param response the response data
         */
        public void respond(String response) {
            try {
                out.println(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void close() {
            if (socket != null) {
                System.out.println("Closing connection to remote device at address: " + getRemoteHostAddress().toString());

                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                socket = null;
            }
        }
    }

    /**
     * This thread is in change of waiting for new incoming socket connections,
     * creating a new Connection instance and letting it loose to do its thing,
     * and add that new Connection instance to the list of connections
     */
    private class IncomingConnectionsThread extends Thread {
        private final long SLEEP_TIME = 1000;
        private boolean runLoop = true;
        private boolean isRunning = false;

        @Override
        public void run() {
            isRunning = true;

            while (runLoop) {
                try {
                    if (callback != null) callback.onWaitingForConnection(port);

                    Socket socket = serverSocket.accept();

                    // If we don't allow multiple connections from the same ip address,
                    // we check the current connections list and stop connection that has the same ip
                    // address as the current new socket connection
                    if (!allowMultipleConnectionsFromSameAddress) {
                        ListIterator<Connection> iterator = connections.listIterator();

                        while (iterator.hasNext()) {
                            Connection connection = iterator.next();

                            if (connection.socket.getInetAddress().equals(socket.getInetAddress())) {
                                System.out.println("TCPServer not allowing multiple connections from the same IP address. Disabling previous connection from address " + socket.getInetAddress().toString());
                                connection.close();
                            }
                        }
                    }

                    // Creating a new connection object with the new socket, starting its listening
                    // thread and adding to the list of connections
                    Connection connection = new Connection(socket);
                    connection.listen();

                    connections.add(connection);
                } catch (Exception e) {
                    if (e instanceof SocketException) {
                        System.out.println(e.getMessage());
                    } else {
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            isRunning = false;
        }

        private void cancel() {
            runLoop = false;
        }
    }

    /**
     * This thread is charge of making sure the connections list only contains active connections.
     */
    private class ActiveConnectionsThread extends Thread {
        private final long SLEEP_TIME = 1000;
        private boolean runLoop = true;
        private boolean isRunning = false;

        @Override
        public void run() {
            isRunning = true;

            while (runLoop) {
                ListIterator<Connection> iterator = connections.listIterator();

                while (iterator.hasNext()) {
                    Connection connection = iterator.next();

                    if (!connection.isListening) {
                        System.out.println("Discarding inactive connection to: " + connection.getRemoteHostAddress().toString());
                        iterator.remove();
                    }
                }

                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            isRunning = false;
        }

        private void cancel() {
            runLoop = false;
        }
    }

    /**
     * The TCP server events callback interface.
     */
    public interface Callback {
        /**
         * Notifies the object implementing this interface that the TCPServer is currently waiting for an incoming
         * TCP client connection.
         *
         * @param port the port number that the server is listening to
         */
        public void onWaitingForConnection(int port);

        /**
         * Notifies the object implementing this interface that the TCPServer has accepted a new TCP client and has
         * created a connection instance.
         *
         * @param connection the newly created TCP connection instance
         */
        public void onConnected(Connection connection);

        /**
         * Notifies the object implementing this interface that the server received data from one of the clients
         * connected to it.
         *
         * @param connection the Connection instance that received the data
         * @param dataPacket the received DataPacket
         */
        public void onDataReceived(Connection connection, DataPacket dataPacket);
    }
}
