package edu.common.impl;

import edu.common.api.Address;

public class LocalAddress implements Address {
    private final int port;

    public LocalAddress(int port) {
        this.port = port;
    }

    @Override
    public String host() {
        return "";
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalAddress that = (LocalAddress) o;

        return (port == that.port);
    }

    @Override
    public int hashCode() {
        return port;
    }

    @Override
    public String toString() {
        return "LocalAddress{port=" + port + "}";
    }

}
