package edu.membership.gossip.impl;

import edu.membership.gossip.api.Address;
import edu.membership.gossip.api.Membership;
import edu.membership.gossip.api.Message;
import edu.membership.gossip.api.Network;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalNetworkTest {

    private static class DummyMessage implements Message<Address> {
        @Override public Type type() { return null; }
        @Override public Membership.Member<Address> sender() { return null; }
        @Override public List<Membership.Member<Address>> members() { return null; }
        @Override public String toString() { return "DummyMessage"; }
    }

    private static final Message<Address> MESSAGE = new DummyMessage();

    @Test
    void messageSentAndReceivedByListener() {
        final LocalNetwork localNetwork = new LocalNetwork();
        Address address = new LocalAddress(0);
        Network.Listener<Message<Address>> listener = localNetwork.listen(address);
        boolean sent = localNetwork.send(address, MESSAGE);
        assertTrue(sent);
        assertEquals(MESSAGE, listener.queue().poll());
    }

    @Test
    void messageNotReceivedByStoppedListener() {
        final LocalNetwork localNetwork = new LocalNetwork();
        Address address = new LocalAddress(0);
        Network.Listener<Message<Address>> listener = localNetwork.listen(address);
        listener.stop();
        boolean sent = localNetwork.send(address, MESSAGE);
        assertFalse(sent);
        assertTrue(listener.queue().isEmpty());
    }

}
