package edu.common.api;

import java.util.Objects;

public class Payload {

    public final edu.member.api.Message member;
    public final edu.kvstore.api.Message keyval;
    public final edu.leader.api.Message leader;

    private Payload(
            edu.member.api.Message member,
            edu.kvstore.api.Message keyval,
            edu.leader.api.Message leader)
    {
        this.member = member;
        this.keyval = keyval;
        this.leader = leader;
    }

    public static Payload of(edu.member.api.Message member) {
        return new Payload(member, null, null);
    }

    public static Payload of(edu.kvstore.api.Message keyval) {
        return new Payload(null, keyval, null);
    }

    public static Payload of(edu.leader.api.Message leader) {
        return new Payload(null, null, leader);
    }

    @Override
    public String toString() {
        return "Payload{" +
                "member=" + member +
                ", keyval=" + keyval +
                ", leader=" + leader +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payload payload = (Payload) o;
        return Objects.equals(member, payload.member) &&
                Objects.equals(keyval, payload.keyval) &&
                Objects.equals(leader, payload.leader);
    }

    @Override
    public int hashCode() {
        return Objects.hash(member, keyval, leader);
    }
}
