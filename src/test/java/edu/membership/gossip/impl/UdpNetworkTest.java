package edu.membership.gossip.impl;

import edu.membership.gossip.api.Address;
import edu.membership.gossip.api.Message;
import edu.membership.gossip.api.Network;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UdpNetworkTest {

    @Test
    void networkSendsAndReceivesMessages() {
        final Network<Address, Message<Address>> network = new UdpNetwork();

        Message<Address> message = new GossipMessage(
                Message.Type.MEMBER_LIST,
                new GossipMembership.MemberNode(new RemoteAddress("127.0.0.1", 10000), 1000, 20001000),
                Arrays.asList(
                        new GossipMembership.MemberNode(new RemoteAddress("127.0.0.1", 10000), 1000, 20001000),
                        new GossipMembership.MemberNode(new RemoteAddress("127.0.0.1", 10001), 1000, 20001000)
                ));

        RemoteAddress sender = new RemoteAddress("127.0.0.1", 12345);

        Network.Listener<Message<Address>> listener = network.listen(sender);

        network.send(sender, message);

        assertEquals(1, listener.queue().size());

        Message<Address> received = listener.queue().poll();

        listener.stop();

        assertEquals(message, received);
    }
}
