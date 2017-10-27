/**
 * comm-protocols Project.
 * com.linoagli.java.comprotocols.tcp
 *
 * @author Olubusayo K. Faye-Lino Agli, username: linoagli
 */
package com.linoagli.java.comprotocols.tcp;

import com.linoagli.java.comprotocols.DataPacket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPClient {
    public static final String LINE_PING_QUERY = "comprotocols-query-mRPrLr5t2hURfDULcReMQf7BWsazASUJ";
    public static final int HIGH_QUERY_QUEUE_SIZE_THRESHOLD = 20;

    private InetAddress serverAddress;
    private Callback callback;
    private int port;
    private int socketTimeOut = 0;
    private boolean isRunning = false;
    private boolean isNullResponseBad = false;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private List<String> queries = new ArrayList<>();

    private WorkerThread workerThread;
    private LinePingThread linePingThread;

    public TCPClient(Callback callback) {
        this.callback = callback;
    }

    public void setSocketTimeOut(int socketTimeOut) {
        this.socketTimeOut = socketTimeOut;
    }

    /**
     * When this client is not connected to a server anymore, it will receive null responses from its queries.
     * Setting isNullResponseBad to true will force this client to disconnect itself from the "dead" server
     *
     * @param isNullResponseBad
     */
    public void setNullResponseBad(boolean isNullResponseBad) {
        this.isNullResponseBad = isNullResponseBad;
    }

    public int getPort() {
        return port;
    }

    public int getQueryQueueSize() {
        return queries.size();
    }

    public InetAddress getServerAddress() {
        return serverAddress;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void connect(InetAddress serverIp, int port) {
        this.serverAddress = serverIp;
        this.port = port;

        disconnect(); // Just to make sure all is neat and clean before getting it dirty again... XD

        workerThread = new WorkerThread();
        workerThread.start();
    }

    public void disconnect() {
        if (workerThread != null) {
            workerThread.cancel();
            workerThread = null;
        }

        cleanUp();
    }

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

    public interface Callback {
        public void onConnected(InetAddress serverIp, int port);
        public void onConnectionFailed(InetAddress serverIp, int port);
        public void onDisconnected();
        public void onDataReceived(DataPacket data);
    }
}
