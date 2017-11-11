package edu.membership.gossip.impl;

import edu.membership.gossip.api.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class GossipNodeTest {

    private Network<Address, Message<Address>> makeNetwork() {
        return new LocalNetwork();
    }

    private Address makeAddress(int port) {
        return new LocalAddress(port);
    }

    private Membership<Address> makeMembership(Address self) {
        return new GossipMembership(self, 10, 20);
    }

    private Node<Address> makeNode(int port) {
        Address self = makeAddress(port);
        return new GossipNode(self, network, makeMembership(self), clock);
    }

    private long globalTime = 0L;

    private Supplier<Long> clock = () -> globalTime;

    private Network<Address, Message<Address>> network = makeNetwork();

    @Test
    void nodeSendsJoinRequest() {
        Node<Address> master = makeNode(0);
        Node<Address> node = makeNode(1);

        assertEquals(1, master.peers().size());
        assertEquals(1, node.peers().size());
        assertEquals(master.address(), master.peers().get(0));
        assertEquals(node.address(), node.peers().get(0));

        Network.Listener<Message<Address>> masterListener = network.listen(master.address());

        node.join(master.address());

        assertEquals(1, masterListener.queue().size());
        assertEquals(node.address(), masterListener.queue().peek().sender().address());
        assertEquals(Message.Type.JOIN, masterListener.queue().peek().type());
        assertTrue(masterListener.queue().peek().members().isEmpty());

        assertEquals(1, master.peers().size());
        assertEquals(1, node.peers().size());
    }

    @Test
    void nodeSpreadsGossipToJoinedNode() {
        Node<Address> master = makeNode(0);
        Node<Address> node = makeNode(1);

        Network.Listener<Message<Address>> masterListener = network.listen(master.address());
        Network.Listener<Message<Address>> nodeListener = network.listen(node.address());

        node.join(master.address());
        master.handle(masterListener.queue().poll());
        assertEquals(Arrays.asList(master.address(), node.address()), master.peers());
        assertEquals(Collections.singletonList(node.address()), node.peers());

        master.cycle();

        assertEquals(1, nodeListener.queue().size());
        Message<Address> message = nodeListener.queue().peek();
        assertEquals(master.address(), message.sender().address());
        assertEquals(Message.Type.MEMBER_LIST, message.type());
        assertEquals(Arrays.asList(
                new GossipMembership.MemberNode(master.address(), 0, 0),
                new GossipMembership.MemberNode(node.address(), 0, 0)
        ), message.members());

        masterListener.stop();
        nodeListener.stop();
    }

    @Test
    void nodeAddsMembersFromGossipMessage() {
        Node<Address> master = makeNode(0);
        Node<Address> node = makeNode(1);

        assertEquals(Collections.singletonList(node.address()), node.peers());

        Message<Address> message = new GossipMessage(
                Message.Type.MEMBER_LIST,
                new GossipMembership.MemberNode(master.address(), 0, 0),
                Arrays.asList(
                        new GossipMembership.MemberNode(master.address(), 0, 0),
                        new GossipMembership.MemberNode(node.address(), 0, 0)
                ));

        node.handle(message);

        assertEquals(Arrays.asList(master.address(), node.address()), node.peers());
    }
}
