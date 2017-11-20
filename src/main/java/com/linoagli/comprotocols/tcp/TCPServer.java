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

    public int getPort() {
        return port;
    }

    public void setAllowMultipleConnectionsFromSameAddress(boolean allow) {
        this.allowMultipleConnectionsFromSameAddress = allow;
    }

    public boolean isRunning() {
        boolean isIncomingConnectionsManagerRunning = incomingConnectionsThread != null && incomingConnectionsThread.isRunning;
        boolean isActiveConnectionsManagerRunning = activeConnectionsThread != null && activeConnectionsThread.isRunning;

        return isIncomingConnectionsManagerRunning && isActiveConnectionsManagerRunning;
    }

    public ListIterator<Connection> getConnectionListIterator() {
        return connections.listIterator();
    }

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
     * This essentially represents a single active TCP server instance. TODO: i should write a better description for this
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

        public void respond(String response) {
            try {
                out.println(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void close() {
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

        public void cancel() {
            runLoop = false;
        }
    }

    /**
     * This thread is charge of making sure the connections list only contains active connections
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

        public void cancel() {
            runLoop = false;
        }
    }

    public interface Callback {
        /**
         * Notifies the object implementing this interface that the TCPServer is currently waiting
         * for an incoming TCP client connection.
         *
         * @param port the port number that the server is listening to
         */
        public void onWaitingForConnection(int port);

        /**
         * Notifies the object implementing this interface that the TCPServer has accepted a
         * new TCP client and has created a connection instance.
         *
         * @param connection the newly created TCP connection instance
         */
        public void onConnected(Connection connection);

        /**
         * This method is called when the server receives data from one of the clients connected to it and gives
         * the object implementing this interface the opportunity to act upon said data.
         *
         * @param connection the Connection instance that received the data
         * @param dataPacket the DataPacket received by the TCPClient
         */
        public void onDataReceived(Connection connection, DataPacket dataPacket);
    }
}
