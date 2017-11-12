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

    private Worker worker;

    public BluetoothRFCommServer(Callback callback) {
        this.callback = callback;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isBound() {
        return isBound;
    }

    public void start(String serviceUUID) {
        this.serviceUUID = serviceUUID;

        worker = new Worker();
        worker.start();

        if (callback != null) callback.onStarted(serviceUUID);
    }

    public void stop() {
        if (worker != null) {
            worker.cancel();
            worker = null;
        }

        closeAllStreamsAndConnections();

        if (callback != null) callback.onStopped(serviceUUID);
    }

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

    private class Worker extends Thread {
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

                    if (callback != null) callback.onConnected();
                    isBound = true;

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
                    if (callback != null) callback.onDisconnected();
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

        public void cancel() {
            runLoop = false;
        }
    }

    public interface Callback {
        public void onStarted(String serviceUUID);
        public void onWaitingForConnection(String serviceUUID);
        public void onConnected();
        public void onDataReceived(BluetoothRFCommServer bluetoothServer, String data);
        public void onDisconnected();
        public void onStopped(String serviceUUID);
    }
}
