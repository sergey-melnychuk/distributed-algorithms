package edu.common.api;

public class Payload {

    public final edu.membership.api.Message member;
    public final edu.kvstore.api.Message keyval;

    private Payload(edu.membership.api.Message member, edu.kvstore.api.Message keyval) {
        this.member = member;
        this.keyval = keyval;
    }

    public static Payload of(edu.membership.api.Message member) {
        return new Payload(member, null);
    }

    public static Payload of(edu.kvstore.api.Message keyval) {
        return new Payload(null, keyval);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Payload payload = (Payload) o;

        return (member != null ? member.equals(payload.member) : payload.member == null) &&
                (keyval != null ? keyval.equals(payload.keyval) : payload.keyval == null);
    }

    @Override
    public int hashCode() {
        int result = member != null ? member.hashCode() : 0;
        result = 31 * result + (keyval != null ? keyval.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Payload{" +
                "member=" + member +
                ", keyval=" + keyval +
                '}';
    }
}
