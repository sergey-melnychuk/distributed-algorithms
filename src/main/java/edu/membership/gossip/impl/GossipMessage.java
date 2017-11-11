package edu.membership.gossip.impl;

import edu.membership.gossip.api.Address;
import edu.membership.gossip.api.Membership;
import edu.membership.gossip.api.Message;

import java.util.Collections;
import java.util.List;

public class GossipMessage implements Message<Address> {

    private final Type type;
    private final Membership.Member<Address> node;
    private final List<Membership.Member<Address>> members;

    GossipMessage(Type type, Membership.Member<Address> node, List<Membership.Member<Address>> members) {
        this.type = type;
        this.node = node;
        this.members = members;
    }

    GossipMessage(Type type, Membership.Member<Address> node) {
        this(type, node, Collections.emptyList());
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public Membership.Member<Address> sender() {
        return node;
    }

    @Override
    public List<Membership.Member<Address>> members() {
        return members;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(GossipMessage.class.getSimpleName());
        sb.append("{");
        sb.append("type=");
        sb.append(type.name());
        sb.append(",sender=");
        sb.append(node.address().host());
        sb.append(":");
        sb.append(node.address().port());
        sb.append(",members=[");
        for (Membership.Member<Address> m : members) {
            sb.append(m.address().host());
            sb.append(":");
            sb.append(m.address().port());
            sb.append(" ");
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GossipMessage that = (GossipMessage) o;

        if (type != that.type) return false;
        return (node.equals(that.node)) && members.equals(that.members);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + node.hashCode();
        result = 31 * result + members.hashCode();
        return result;
    }
}
