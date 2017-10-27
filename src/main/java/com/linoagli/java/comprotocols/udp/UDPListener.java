/**
 * comm-protocols Project.
 * com.linoagli.java.comprotocols.udp
 *
 * @author Olubusayo K. Faye-Lino Agli, username: linoagli
 */
package com.linoagli.java.comprotocols.udp;

import com.linoagli.java.comprotocols.DataPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPListener {
    public final int DEFAULT_DATA_PACKET_SIZE = 512;

    private Callback callback;
    private int port;
    private int dataPacketSize = DEFAULT_DATA_PACKET_SIZE;
    private boolean isRunning = false;

    private UDPServerThread thread;
    private DatagramSocket serverSocket;
    private DataPacket data;

    public UDPListener(int port, Callback callback) {
        this.port = port;
        this.callback = callback;
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getDataPacketSize() {
        return dataPacketSize;
    }

    public void setDataPacketSize(int dataPacketSize) {
        this.dataPacketSize = dataPacketSize;
    }

    public void start() {
        thread = new UDPServerThread();
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            thread.cancel();
            thread = null;
        }
    }

    private class UDPServerThread extends Thread {
        private boolean runLoop = true;

        @Override
        public void run() {
            try {
                serverSocket = new DatagramSocket(port);
                if (callback != null) callback.onStarted(port);
            } catch (SocketException e) {
                e.printStackTrace();
                if (callback != null) callback.onStartFailed(port);
                cancel();
            }

            isRunning = true; // Starting the thread loop. The service is open for business

            while (runLoop) {
                byte[] buffer = new byte[dataPacketSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    serverSocket.receive(packet);

                    byte[] bytes = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, bytes, 0, packet.getLength());

                    data = new DataPacket(packet.getAddress(), port, bytes);
                    if (callback != null) callback.onDataReceived(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (callback != null) callback.onStopping(port);

            isRunning = false; // Ending the thread loop. The service has reached the end of its business hours
        }

        public void cancel() {
            runLoop = false;

            if (data != null) data = null;

            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        }
    }

    public interface Callback {
        public void onStarted(int port);
        public void onStartFailed(int port);
        public void onStopping(int port);
        public void onDataReceived(DataPacket dataPacket);
    }
}
