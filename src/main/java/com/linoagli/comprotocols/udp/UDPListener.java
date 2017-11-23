/**
 * comm-protocols Project.
 * com.linoagli.java.comprotocols.udp
 *
 * @author Olubusayo K. Faye-Lino Agli, username: linoagli
 */
package com.linoagli.comprotocols.udp;

import com.linoagli.comprotocols.DataPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Provides a simplified implementation for receiving and processing UDP packets
 */
public class UDPListener {
    public final int DEFAULT_DATA_PACKET_SIZE = 512;

    private Callback callback;
    private int port;
    private int dataPacketSize = DEFAULT_DATA_PACKET_SIZE;
    private boolean isRunning = false;

    private WorkerThread thread;
    private DatagramSocket serverSocket;
    private DataPacket data;

    public UDPListener(int port, Callback callback) {
        this.port = port;
        this.callback = callback;
    }

    /**
     * @return the port number currently being watched for incoming data packets
     */
    public int getPort() {
        return port;
    }

    /**
     * @return whether this listener is active and listening for incoming data packets
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * @return the current maximum data packet size in bytes
     */
    public int getDataPacketSize() {
        return dataPacketSize;
    }

    /**
     * Sets the maximum data packet size in bytes
     *
     * @param dataPacketSize the size in bytes
     */
    public void setDataPacketSize(int dataPacketSize) {
        this.dataPacketSize = dataPacketSize;
    }

    /**
     * Initializes the UDP listener and starts the listening process.
     */
    public void start() {
        thread = new WorkerThread();
        thread.start();
    }

    /**
     * Retires the UDP listener and cleans up resources.
     */
    public void stop() {
        if (thread != null) {
            thread.cancel();
            thread = null;
        }
    }

    /**
     * The thread in charge of all the heavy lifting.
     */
    private class WorkerThread extends Thread {
        private boolean runLoop = true;

        @Override
        public void run() {
            try {
                serverSocket = new DatagramSocket(port);

                isRunning = true; // Starting the thread loop. The service is open for business

                if (callback != null) callback.onStarted(port);

                while (runLoop) {
                    byte[] buffer = new byte[dataPacketSize];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    try {
                        serverSocket.receive(packet);

                        byte[] bytes = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), 0, bytes, 0, packet.getLength());

                        data = new DataPacket(packet.getAddress(), port, bytes);
                        if (callback != null) callback.onDataReceived(data);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (callback != null) callback.onStopping();

                isRunning = false; // Ending the thread loop. The service has reached the end of its business hours
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void cancel() {
            runLoop = false;

            if (data != null) data = null;

            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        }
    }

    /**
     * The events callback interface
     */
    public interface Callback {
        /**
         * Notifies the object implementing this interface that the UDP listener has successfully
         * started and is listening for incoming data packets.
         * @param port the port number being listened to
         */
        public void onStarted(int port);

        /**
         * Notifies the object implementing this interface that the UDP listener has stopped its processes.
         */
        public void onStopping();

        /**
         * Notifies the object implementing this interface that a data packet was received.
         *
         * @param dataPacket the received data packet
         */
        public void onDataReceived(DataPacket dataPacket);
    }
}
