package edu.membership.impl;

import edu.common.impl.LocalAddress;
import edu.common.impl.LocalNetwork;
import edu.common.api.Address;
import edu.common.api.Payload;
import edu.membership.api.Member;
import edu.common.api.Network;
import edu.membership.api.Message;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalNetworkTest {

    private static final Message MESSAGE = new Message(
            Message.Type.JOIN,
            new Member(new LocalAddress(0), 0, 0),
            Collections.emptyList());

    @Test
    void messageSentAndReceivedByListener() {
        final LocalNetwork localNetwork = new LocalNetwork();
        Address address = new LocalAddress(0);
        Network.Listener listener = localNetwork.listen(address);
        boolean sent = localNetwork.send(address, Payload.of(MESSAGE));
        assertTrue(sent);
        assertEquals(MESSAGE, listener.queue().poll().member);
    }

    @Test
    void messageNotReceivedByStoppedListener() throws Exception {
        final LocalNetwork localNetwork = new LocalNetwork();
        Address address = new LocalAddress(0);
        Network.Listener listener = localNetwork.listen(address);
        listener.close();
        boolean sent = localNetwork.send(address, Payload.of(MESSAGE));
        assertFalse(sent);
        assertTrue(listener.queue().isEmpty());
    }

}
