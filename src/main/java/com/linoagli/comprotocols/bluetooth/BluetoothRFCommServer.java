/**
 * comm-protocols Project.
 * com.linoagli.java.comprotocols.bluetooth
 *
 * @author Olubusayo K. Faye-Lino Agli, username: linoagli
 */
package com.linoagli.comprotocols.bluetooth;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class BluetoothRFCommServer {
    private Callback callback;

    private String serviceUUID;
    private boolean isRunning = false;
    private boolean isBound = false;
    private StreamConnectionNotifier notifier;
    private StreamConnection connection;
    private BufferedReader in;
    private PrintWriter out;
    private WorkerThread workerThread;

    public BluetoothRFCommServer(Callback callback) {
        this.callback = callback;
    }

    /**
     * @return whether or not this server instance is up and running.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * @return whether or not this server instance is connected to bluetooth client.
     */
    public boolean isBound() {
        return isBound;
    }

    /**
     * Boots up this server instance and starts listening for incoming connections.
     *
     * @param serviceUUID the UUID this server will use as its service UUID
     */
    public void start(String serviceUUID) {
        this.serviceUUID = serviceUUID;

        workerThread = new WorkerThread();
        workerThread.start();
    }

    /**
     * Powers down the server instance and cleans up all resources.
     */
    public void stop() {
        if (workerThread != null) {
            workerThread.cancel();
            workerThread = null;
        }

        closeAllStreamsAndConnections();
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

    private void closeAllStreamsAndConnections() {
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            in = null;
        }

        if (out != null) {
            out.flush();
            out.close();
            out = null;
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            connection = null;
        }

        if (notifier != null) {
            try {
                notifier.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            notifier = null;
        }
    }

    /**
     * This thread does all the heavy lifting.
     */
    private class WorkerThread extends Thread {
        private boolean runLoop = true;

        @Override
        public void run() {
            isRunning = true;

            while (runLoop) {
                try {
                    String uri = "btspp://localhost:" + serviceUUID.replaceAll("-", "") + ";name=" + getClass().getSimpleName();
                    notifier = (StreamConnectionNotifier) Connector.open(uri);

                    if (callback != null) callback.onWaitingForConnection(serviceUUID);

                    connection = notifier.acceptAndOpen();

                    isBound = true;

                    if (callback != null) callback.onConnected();

                    try {
                        in = new BufferedReader(new InputStreamReader(connection.openInputStream()));
                        out = new PrintWriter(connection.openOutputStream(), true);

                        String input;

                        while ((input = in.readLine()) != null) {
                            if (callback != null) callback.onDataReceived(BluetoothRFCommServer.this, input);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    isBound = false;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    closeAllStreamsAndConnections();
                }

                try {
                    Thread.sleep(3000);
                }
                catch (Exception e) {
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
     * The RFComm server events callback interface.
     */
    public interface Callback {
        /**
         * Notifies the object implementing this interface that the RFComm server is currently waiting for an
         * incoming TCP client connection.
         *
         * @param serviceUUID the UUID used to initiate the server
         */
        public void onWaitingForConnection(String serviceUUID);

        /**
         * Notifies the object implementing this interface that the RFComm server has accepted a Bluetooth connection.
         */
        public void onConnected();

        /**
         * Notifies the object implementing this interface that the server received data from the connected BT client.
         *
         * @param rfCommServer the server instance that received the data
         * @param data the received data string
         */
        public void onDataReceived(BluetoothRFCommServer rfCommServer, String data);
    }
}
