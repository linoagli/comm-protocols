/**
 * comm-protocols Project.
 * com.linoagli.java.comprotocols
 *
 * @author Olubusayo K. Faye-Lino Agli, username: linoagli
 */
package com.linoagli.java.comprotocols;

import java.net.InetAddress;

public class DataPacket {
    public InetAddress address;
    public int port;
    public byte[] bytes;
    public String data;

    public DataPacket(InetAddress address, int port, String data) {
        this.address = address;
        this.port = port;
        this.data = data;
    }

    public DataPacket(InetAddress address, int port, byte[] bytes) {
        this.address = address;
        this.port = port;
        this.bytes = bytes;
        this.data = new String(bytes);
    }
}
