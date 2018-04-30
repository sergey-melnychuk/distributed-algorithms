package edu.leadership.api;

import edu.common.api.Address;

public class Message {

    enum Type {
        PREPARE,    // Leader prepares the request with some number N
        PROMISE,    // Acceptor promises not to accept PREPARE requests <N
        ACCEPT,     // Leader requests to ACCEPT selected number N
        ACCEPTED,   // Acceptor accepts Leader as a leader and
        REJECTED    // Acceptor rejects PREPARE request
    }

    public final Type type;
    public final long number;
    public final Address sender;

    public Message(Type type, long number, Address sender) {
        this.type = type;
        this.number = number;
        this.sender = sender;
    }
}
