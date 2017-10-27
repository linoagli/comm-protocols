/**
 * comm-protocols Project.
 * com.linoagli.java.comprotocols.udp
 *
 * @author Olubusayo K. Faye-Lino Agli, username: linoagli
 */
package com.linoagli.java.comprotocols.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class UDPSender {
    private final int HIGH_QUERY_QUEUE_SIZE_THRESHOLD = 20;

    private InetAddress address;
    private int port;

    private WorkerThread workerThread;

    public UDPSender(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public static void send(final InetAddress address, final int port, final String data) {
        new Thread() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(data.getBytes(), data.getBytes().length, address, port);
                    socket.send(packet);
                    socket.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public void start() {
        workerThread = new WorkerThread();
        workerThread.start();
    }

    public void stop() {
        if (workerThread != null) {
            workerThread.cancel();
            workerThread = null;
        }

        address = null;
        port = -1;
    }

    public void query(String query) {
        if (query == null || query.trim().isEmpty()) {
            System.out.println("Null or empty query string not allowed. Moving on...");
            return;
        }

        if (workerThread != null) workerThread.stringQueries.add(query);
    }

    public void query(byte[] bytes) {
        if (bytes == null) {
            System.out.println("Null byte array not allowed. Ignoring...");
            return;
        }

        if (workerThread != null) workerThread.bytesQueries.add(bytes);
    }

    public class WorkerThread extends Thread {
        private final long SLEEP_TIME = 100;

        private boolean runLoop = true;
        private boolean isRunning = false;

        private DatagramSocket socket;
        private List<String> stringQueries = new ArrayList<>();
        private List<byte[]> bytesQueries = new ArrayList<>();

        @Override
        public void run() {
            try {
                socket = new DatagramSocket();
            } catch (Exception e) {
                e.printStackTrace();
            }

            isRunning = true;

            while (runLoop) {
                if (stringQueries.isEmpty() && bytesQueries.isEmpty()) continue;

                if ((stringQueries.size() + bytesQueries.size()) >= HIGH_QUERY_QUEUE_SIZE_THRESHOLD) {
                    System.out.println(UDPSender.class.getName() + ": The query queue size is high! Queue size: " + String.valueOf(stringQueries.size() + bytesQueries.size()));
                }

                if (!stringQueries.isEmpty()) {
                    String query = stringQueries.remove(0);

                    try {
                        socket.send(new DatagramPacket(query.getBytes(), query.getBytes().length, address, port));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (!bytesQueries.isEmpty()) {
                    byte[] bytes = bytesQueries.remove(0);

                    try {
                        socket.send(new DatagramPacket(bytes, bytes.length, address, port));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Sleepy time...
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            isRunning = false;

            if (socket != null) {
                socket.close();
                socket = null;
            }
        }

        public void cancel() {
            runLoop = false;
        }
    }
}
