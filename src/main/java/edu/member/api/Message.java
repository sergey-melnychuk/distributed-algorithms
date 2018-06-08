package edu.member.api;

import java.util.Arrays;
import java.util.List;

public class Message {
    public enum Type {
        JOIN,
        MEMBER_LIST
    }

    public final Type type;
    public final Member sender;
    public final List<Member> members;

    public Message(Type type, Member sender, List<Member> members) {
        this.type = type;
        this.sender = sender;
        this.members = members;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message");
        sb.append("{");
        sb.append("type=");
        sb.append(type.name());
        sb.append(",sender=");
        sb.append(sender.address.host());
        sb.append(":");
        sb.append(sender.address.port());
        sb.append(",members=");
        sb.append(Arrays.toString(members.toArray()));
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message that = (Message) o;

        return (type == that.type) && (sender.equals(that.sender)) && members.equals(that.members);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + sender.hashCode();
        result = 31 * result + members.hashCode();
        return result;
    }
}
