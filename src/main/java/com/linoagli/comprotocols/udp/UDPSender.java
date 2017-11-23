/**
 * comm-protocols Project.
 * com.linoagli.java.comprotocols.udp
 *
 * @author Olubusayo K. Faye-Lino Agli, username: linoagli
 */
package com.linoagli.comprotocols.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple way of sending UDP packets
 */
public class UDPSender {
//    private final int HIGH_QUERY_QUEUE_SIZE_THRESHOLD = 20;
//
//    private InetAddress address;
//    private int port;
//    private List<String> stringQueries = new ArrayList<>();
//    private List<byte[]> bytesQueries = new ArrayList<>();
//
//    private WorkerThread workerThread;

    private UDPSender() {}

//    public UDPSender(InetAddress address, int port) {
//        this.address = address;
//        this.port = port;
//    }

    /**
     * Sends UDP data packet asynchronously to the specified address and port number.
     *
     * @param addressName the recipient's IP address or known address name
     * @param port the recipient's port number
     * @param data the data to be sent
     */
    public static void send(final String addressName, final int port, final String data) {
        try {
            InetAddress address = InetAddress.getByName(addressName);
            send(address, port, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends UDP data packet asynchronously to the specified address and port number.
     *
     * @param address the recipient's IP address
     * @param port the recipient's port number
     * @param data the data to be sent
     */
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

//    /**
//     * @return the current target IP address for all outgoing data packets
//     */
//    public InetAddress getAddress() {
//        return address;
//    }
//
//    /**
//     * @return the current target port number
//     */
//    public int getPort() {
//        return port;
//    }
//
//    /**
//     * Initialized and prepares the UDP sender to send batches of data packets.
//     */
//    public void start() {
//        workerThread = new WorkerThread();
//        workerThread.start();
//    }
//
//    /**
//     * This essentially powers down the UDP sender and cleans up all resources.
//     */
//    public void stop() {
//        if (workerThread != null) {
//            workerThread.cancel();
//            workerThread = null;
//        }
//
//        address = null;
//        port = -1;
//    }
//
//    /**
//     * Sends a UDP data packet.
//     *
//     * @param data the data string to be sent
//     */
//    public void send(String data) {
//        if (data == null || data.trim().isEmpty()) {
//            System.out.println("Null or empty query string not allowed. Moving on...");
//            return;
//        }
//
//        stringQueries.add(data);
//
//        System.out.println("added data: " + data);
//        System.out.println("query size: " + stringQueries.size());
//
//        System.out.println("---");
//        for (String item : stringQueries) System.out.println(item);
//        System.out.println("---");
//    }
//
//    /**
//     * Sends a UDP data packet
//     *
//     * @param bytes the bytes of data to be sent
//     */
//    public void send(byte[] bytes) {
//        if (bytes == null) {
//            System.out.println("Null byte array not allowed. Ignoring...");
//            return;
//        }
//
//        bytesQueries.add(bytes);
//    }
//
//    /**
//     * This thread handles all of the heavy lifting
//     */
//    private class WorkerThread extends Thread {
//        private final long SLEEP_TIME = 300;
//
//        private boolean runLoop = true;
//        private boolean isRunning = false;
//
//        private DatagramSocket socket;
//
//        @Override
//        public void run() {
//            try {
//                socket = new DatagramSocket();
//
//                isRunning = true;
//
//                while (runLoop) {
//                    if (stringQueries.isEmpty() && bytesQueries.isEmpty()) continue;
//
//                    if ((stringQueries.size() + bytesQueries.size()) >= HIGH_QUERY_QUEUE_SIZE_THRESHOLD) {
//                        System.out.println(UDPSender.class.getName() + ": The query queue size is high! Queue size: " + String.valueOf(stringQueries.size() + bytesQueries.size()));
//                    }
//
//                    if (!stringQueries.isEmpty()) {
//                        String query = stringQueries.remove(0);
//
//                        try {
//                            socket.send(new DatagramPacket(query.getBytes(), query.getBytes().length, address, port));
//                        }
//                        catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    if (!bytesQueries.isEmpty()) {
//                        byte[] bytes = bytesQueries.remove(0);
//
//                        try {
//                            socket.send(new DatagramPacket(bytes, bytes.length, address, port));
//                        }
//                        catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    // Sleepy time...
//                    try {
//                        Thread.sleep(SLEEP_TIME);
//                    }
//                    catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                isRunning = false;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            if (socket != null) {
//                socket.close();
//                socket = null;
//            }
//        }
//
//        private void cancel() {
//            runLoop = false;
//        }
//    }
}
