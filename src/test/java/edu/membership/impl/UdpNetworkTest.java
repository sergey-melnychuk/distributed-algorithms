package edu.membership.impl;

import edu.common.api.Payload;
import edu.common.impl.RemoteAddress;
import edu.common.impl.UdpNetwork;
import edu.membership.api.Member;
import edu.common.api.Network;
import edu.membership.api.Message;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UdpNetworkTest {

    private static final RemoteAddress sender = new RemoteAddress("127.0.0.1", 12345);

    private static final List<Payload> payloads = Arrays.asList(
            Payload.of(new Message(
                    Message.Type.MEMBER_LIST,
                    new Member(sender,  20001000, 1000),
                    Arrays.asList(
                            new Member(new RemoteAddress("127.0.0.1", 10000), 20001000, 1000),
                            new Member(new RemoteAddress("127.0.0.1", 10001), 20001000,1000)
                    ))),
            Payload.of(new edu.kv.api.Message(
                    edu.kv.api.Message.Type.CREATE,
                    123L,
                    sender,
                    "key",
                    "value",
                    false
            ))
    );

    @Test
    void networkSendsAndReceivesMessages() {
        final Network network = new UdpNetwork();

        Network.Listener listener = network.listen(sender);

        payloads.forEach(p -> {
            System.out.println("processing: " + p);
            network.send(sender, p);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                /* ignore */
            }
            assertEquals(1, listener.queue().size());
            Payload received = listener.queue().poll();
            assertEquals(p, received);
        });

        try {
            listener.close();
        } catch (Exception e) {
            /* close */
        }
    }
}
