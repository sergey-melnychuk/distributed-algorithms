package edu.member.api;

import edu.common.api.Address;

public class Member {
    public final String id;
    public final Address address;
    public final long timestamp;
    public final long heartbeat;

    public Member(Address address, long timestamp, long heartbeat) {
        this.id = address.host() + ":" + address.port();
        this.address = address;
        this.timestamp = timestamp;
        this.heartbeat = heartbeat;
    }

    public Member updated(long timestamp) { return new Member(address, timestamp, heartbeat); }
    public Member updated(long timestamp, long heartbeat) { return new Member(address, timestamp, heartbeat); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Member that = (Member) o;

        return (timestamp == that.timestamp)
                && (heartbeat == that.heartbeat)
                && address.equals(that.address);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + address.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (int) (heartbeat ^ (heartbeat >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Member{" +
                "addr=" + address +
                ",ts=" + timestamp +
                ",hb=" + heartbeat +
                '}';
    }
}
