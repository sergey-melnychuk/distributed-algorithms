package edu.common.impl;

import edu.common.api.Address;

import java.util.Arrays;

public class RemoteAddress implements Address {
    private byte addr[];
    private final int port;

    RemoteAddress(byte addr[], int port) {
        this.addr = addr;
        this.port = port;
    }

    public RemoteAddress(String ip, int port) {
        this.port = port;
        this.addr = new byte[4];
        String b[] = ip.split("\\.");
        for (int i=0; i<4; i++) {
            addr[i] = Byte.valueOf(b[i]);
        }
    }

    @Override
    public String host() {
        return "" + addr[0] + "." + addr[1] + "." + addr[2] + "." + addr[3];
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemoteAddress that = (RemoteAddress) o;

        return (port == that.port) && Arrays.equals(addr, that.addr);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(addr);
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "RemoteAddress{" +
                "addr=" + Arrays.toString(addr) +
                ", port=" + port +
                '}';
    }

}
