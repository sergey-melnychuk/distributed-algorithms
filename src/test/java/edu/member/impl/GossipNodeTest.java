package edu.member.impl;

import edu.common.api.Address;
import edu.common.impl.LocalAddress;
import edu.common.impl.LocalNetwork;
import edu.common.api.Network;
import edu.member.api.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class GossipNodeTest {

    private Network makeNetwork() {
        return new LocalNetwork();
    }

    private Address makeAddress(int port) {
        return new LocalAddress(port);
    }

    private Membership makeMembership(Address self) {
        return new GossipMembership(self, 10, 20);
    }

    private MemberNode makeNode(int port) {
        Address self = makeAddress(port);
        return new GossipNode(self, network, makeMembership(self), clock);
    }

    private long globalTime = 0L;

    private Supplier<Long> clock = () -> globalTime;

    private Network network = makeNetwork();

    @Test
    void nodeSendsJoinRequest() {
        MemberNode master = makeNode(0);
        MemberNode node = makeNode(1);

        assertEquals(1, master.peers().size());
        assertEquals(1, node.peers().size());
        assertEquals(master.address(), master.peers().get(0));
        assertEquals(node.address(), node.peers().get(0));

        Network.Listener masterListener = network.listen(master.address());

        node.join(master.address());

        assertEquals(1, masterListener.queue().size());
        assertEquals(node.address(), masterListener.queue().peek().member.sender.address);
        assertEquals(Message.Type.JOIN, masterListener.queue().peek().member.type);
        assertTrue(masterListener.queue().peek().member.members.isEmpty());

        assertEquals(1, master.peers().size());
        assertEquals(1, node.peers().size());
    }

    @Test
    void nodeSpreadsGossipToJoinedNode() throws Exception {
        MemberNode master = makeNode(0);
        MemberNode node = makeNode(1);

        Network.Listener masterListener = network.listen(master.address());
        Network.Listener nodeListener = network.listen(node.address());

        node.join(master.address());
        master.handle(masterListener.queue().poll().member);
        assertEquals(Arrays.asList(master.address(), node.address()), master.peers());
        assertEquals(Collections.singletonList(node.address()), node.peers());

        master.cycle();

        assertEquals(1, nodeListener.queue().size());
        Message message = nodeListener.queue().peek().member;
        assertEquals(master.address(), message.sender.address);
        assertEquals(Message.Type.MEMBER_LIST, message.type);
        assertEquals(Arrays.asList(
                new Member(master.address(), 0, 0),
                new Member(node.address(), 0, 0)
        ), message.members);

        masterListener.close();
        nodeListener.close();
    }

    @Test
    void nodeAddsMembersFromGossipMessage() {
        MemberNode master = makeNode(0);
        MemberNode node = makeNode(1);

        assertEquals(Collections.singletonList(node.address()), node.peers());

        Message message = new Message(
                Message.Type.MEMBER_LIST,
                new Member(master.address(), 0, 0),
                Arrays.asList(
                        new Member(master.address(), 0, 0),
                        new Member(node.address(), 0, 0)
                ));

        node.handle(message);

        assertEquals(Arrays.asList(master.address(), node.address()), node.peers());
    }
}
