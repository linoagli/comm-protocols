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
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a simplified client side implementation of a TCP connection.
 */
public class TCPClient {
    public static final String LINE_PING_QUERY = "comprotocols-query-mRPrLr5t2hURfDULcReMQf7BWsazASUJ";
    public static final int HIGH_QUERY_QUEUE_SIZE_THRESHOLD = 20;

    private Callback callback;

    private InetAddress serverAddress;
    private int port;
    private int socketTimeOut = 0;
    private boolean isRunning = false;
    private boolean isNullResponseBad = true;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private List<String> queries = new ArrayList<>();

    private WorkerThread workerThread;
    private LinePingThread linePingThread;

    public TCPClient(Callback callback) {
        this.callback = callback;
    }

    /**
     * Sets the socket time out delay in milliseconds. When set and response is received from the server
     * after a query, a socket exception is raised and the client disconnects itself from the server.
     *
     * The default value is <b>0</b> which is interpreted as an <i>infinite</i> delay.
     *
     * @param socketTimeOut the time out delay in milliseconds
     */
    public void setSocketTimeOut(int socketTimeOut) {
        this.socketTimeOut = socketTimeOut;
    }

    /**
     * Sets a flag that specifies how this client should handle <i>null</i> responses from the server.
     * Usually, the client receives a <i>null</i> value when its connection to a server is severed unintentionally.
     *
     * Setting this flag will cause this client to automatically disconnect itself from the server.
     *
     * This flag is set to <b>true</b> by default
     *
     * @param isNullResponseBad the flag
     */
    public void setNullResponseBad(boolean isNullResponseBad) {
        this.isNullResponseBad = isNullResponseBad;
    }

    /**
     * @return the port number this client is expecting the server to be listening to.
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the size of the query queue.
     */
    public int getQueryQueueSize() {
        return queries.size();
    }

    /**
     * @return the IP address this client is expecting to find the server at.
     */
    public InetAddress getServerAddress() {
        return serverAddress;
    }

    /**
     * @return whether this client is currently connected to a server and set to send and receive data.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Attempt to connect to a TCP server at the specified address and port
     *
     * @param serverIp the IP address of the target server
     * @param port the port number that the server should be listening to
     */
    public void connect(InetAddress serverIp, int port) {
        this.serverAddress = serverIp;
        this.port = port;

        disconnect(); // Just to make sure all is neat and clean before getting it dirty again... XD

        workerThread = new WorkerThread();
        workerThread.start();
    }

    /**
     * Close this client's connection to the server.
     */
    public void disconnect() {
        if (workerThread != null) {
            workerThread.cancel();
            workerThread = null;
        }

        cleanUp();
    }

    /**
     * Send a request to the server this client is connected to.
     *
     * @param query the query string. <i>null</i> and <i>empty</i> strings will be ignored.
     */
    public void query(String query) {
        if (query == null || query.trim().isEmpty()) {
            System.out.println("Null or empty query string not allowed. Moving on...");
            return;
        }

        queries.add(query);
    }

    private void startLinePing() {
        linePingThread = new LinePingThread();
        linePingThread.start();
    }

    private void stopLinePing() {
        if (linePingThread != null) {
            linePingThread.cancel();
            linePingThread = null;
        }
    }

    private void cleanUp() {
        queries.clear();

        if (in != null) {
            try {
                in.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            in = null;
        }

        if (out != null) {
            out.close();
            out = null;
        }

        if (socket != null) {
            try {
                socket.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            socket = null;
        }
    }

    /**
     * This thread does all the heavy lifting: connecting to the server, sending queries, waiting for responses, etc...
     */
    private class WorkerThread extends Thread {
        private final long SLEEP_TIME = 100;
        private boolean runLoop = true;

        @Override
        public void run() {
            if (init()) {
                isRunning = true;

                startLinePing();

                try {
                    while (runLoop) {
                        doLoop();
                        Thread.sleep(SLEEP_TIME);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                stopLinePing();

                isRunning = false;
            }

            cleanUp();

            if (callback != null) callback.onDisconnected();
        }

        public void cancel() {
            runLoop = false;
        }

        private boolean init() {
            try {
                socket = new Socket(serverAddress, port);
                socket.setSoTimeout(socketTimeOut);

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                if (callback != null) callback.onConnected(serverAddress, port);

                return true;
            } catch (Exception e) {
                e.printStackTrace();

                if (callback != null) callback.onConnectionFailed(serverAddress, port);

                return false;
            }
        }

        private void doLoop() throws IOException {
            if (queries.isEmpty()) return;

            if (queries.size() >= HIGH_QUERY_QUEUE_SIZE_THRESHOLD) {
                System.out.println(TCPClient.class.getName() + ": The query queue size is high! Queue size: " + String.valueOf(queries.size()));
            }

            String query = queries.remove(0); // Retrieving the 1st query in the list. You know, 1st in, 1st out queue?
            out.println(query);
            String data = in.readLine();

            if (data == null && isNullResponseBad) {
                System.out.println("Got a null response from TCP server and this response is considered \"bad\". Disconnecting...");
                cancel();

                return; // We might as well call it quits here...
            }

            if (callback != null) callback.onDataReceived(new DataPacket(serverAddress, port, data));
        }
    }

    /**
     * This thread is sort of a heart beat system. It periodically sends a ping packet to the server
     * to make sure that the server on the other end is alive and that the current TCP connection is <i>valid</i>
     */
    private class LinePingThread extends Thread {
        private final long SLEEP_TIME = 3000;
        private boolean runLoop = true;

        @Override
        public void run() {
            while (runLoop) {
                if (getQueryQueueSize() < 1) query(LINE_PING_QUERY);

                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            runLoop = false;
        }
    }

    /**
     * The TCP client events callback interface
     */
    public interface Callback {
        /**
         * Notifies the object implementing this interface that the client has successfully connected to the server
         *
         * @param serverIp the server's IP address
         * @param port the server's port number
         */
        public void onConnected(InetAddress serverIp, int port);

        /**
         * Notifies the object implementing this interface that the connection attemp failed.
         *
         * @param serverIp the server's IP address
         * @param port the server's port number
         */
        public void onConnectionFailed(InetAddress serverIp, int port);

        /**
         * Notifies the object implementing this interface that the client has been disconnected from its server.
         */
        public void onDisconnected();

        /**
         * Notifies the object implementing this interface that a data packet was received from the server.
         *
         * @param dataPacket the data packet
         */
        public void onDataReceived(DataPacket dataPacket);
    }
}
