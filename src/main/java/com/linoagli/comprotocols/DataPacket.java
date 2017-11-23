/**
 * comm-protocols Project.
 * com.linoagli.java.comprotocols
 *
 * @author Olubusayo K. Faye-Lino Agli, username: linoagli
 */
package com.linoagli.comprotocols;

import java.net.InetAddress;

/**
 * This is a data class is used to encapsulate data received from various sources
 */
public class DataPacket {
    /**
     * The source IP address of the data packet
     */
    public InetAddress address;

    /**
     * The port number that was used to send this data packet
     */
    public int port;

    /**
     * The data bytes of this data packet
     */
    public byte[] bytes;

    /**
     * The data string of this data packet
     */
    public String data;

    public DataPacket(InetAddress address, int port, String data) {
        this.address = address;
        this.port = port;
        this.data = data;
        this.bytes = data.getBytes();
    }

    public DataPacket(InetAddress address, int port, byte[] bytes) {
        this.address = address;
        this.port = port;
        this.bytes = bytes;
        this.data = new String(bytes);
    }
}
