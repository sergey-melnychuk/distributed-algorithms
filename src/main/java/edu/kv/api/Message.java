package edu.kv.api;

import edu.common.api.Address;

public class Message {
    public enum Type {
        READ,
        CREATE,
        UPDATE,
        DELETE,
        ACK,
        OK,
        FAIL
    }

    public final Type type;
    public final long seqNr;
    public final Address sender;
    public final String key;
    public final String value;
    public final boolean replica;

    public Message(Type type, long seqNr, Address sender, String key, String value) {
        this(type, seqNr, sender, key, value, false);
    }

    public Message(Type type, long seqNr, Address sender, String key, String value, boolean replica) {
        this.type = type;
        this.seqNr = seqNr;
        this.sender = sender;
        this.key = key;
        this.value = value;
        this.replica = replica;
    }

    public Message ok(Address sender) {
        return new Message(Type.OK, seqNr, sender, key, value, replica);
    }

    public Message fail(Address sender) {
        return new Message(Type.FAIL, seqNr, sender, key, value, replica);
    }

    public Message accept(long seqNr) { return new Message(type, seqNr, sender, key, value, replica); }

    public Message ack() { return new Message(Type.ACK, seqNr, sender, key, value, replica); }

    public Message replica(Address master) {
        return new Message(type, seqNr, master, key, value, true);
    }

    public Message value(String value) {
        return new Message(Type.ACK, seqNr, sender, key, value, replica);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        return (seqNr == message.seqNr) &&
                (replica == message.replica) &&
                (type == message.type) &&
                sender.equals(message.sender) &&
                key.equals(message.key) &&
                value.equals(message.value);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (int) (seqNr ^ (seqNr >>> 32));
        result = 31 * result + sender.hashCode();
        result = 31 * result + key.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + (replica ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", seqNr=" + seqNr +
                ", sender=" + sender +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", replica=" + replica +
                '}';
    }
}
