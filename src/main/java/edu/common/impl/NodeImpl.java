package edu.common.impl;

import edu.common.api.Address;
import edu.common.api.Network;
import edu.common.api.Payload;
import edu.kvstore.api.KVNode;
import edu.kvstore.api.Ring;
import edu.kvstore.impl.HashRing;
import edu.kvstore.impl.KVNodeImpl;
import edu.membership.api.MemberNode;
import edu.membership.impl.GossipMembership;
import edu.membership.impl.GossipNode;

import java.util.List;
import java.util.function.Supplier;

public class NodeImpl implements Network.Node {
    private final Address address;
    private final MemberNode memberNode;
    private final KVNode keyvalNode;
    private final Ring<String> ring;

    public NodeImpl(Address address, Network network, Supplier<Long> clock, long timeFailedMillis, long timeCleanupMillis, long timeoutMillis, int replicationFactor) {
        this.ring = new HashRing(Byte.MAX_VALUE, new GossipMembership(address, timeFailedMillis, timeCleanupMillis));
        this.memberNode = new GossipNode(address, network, ring, clock);
        this.keyvalNode = new KVNodeImpl(address, network, ring, clock, timeoutMillis, replicationFactor);
        this.address = address;
    }

    @Override
    public void handle(Payload payload) {
        if (payload.keyval != null) keyvalNode.handle(payload.keyval);
        else if (payload.member != null) memberNode.handle(payload.member);
    }

    @Override
    public Address address() {
        return address;
    }

    @Override
    public void join(Address leader) {
        memberNode.join(leader);
    }

    @Override
    public List<Address> peers() {
        return memberNode.peers();
    }

    @Override
    public Ring<String> ring() {
        return ring;
    }

    @Override
    public void fail() {
        memberNode.fail();
    }

    @Override
    public void cycle() {
        memberNode.cycle();
    }
}
