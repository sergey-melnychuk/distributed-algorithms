package edu.common.impl;

import edu.common.api.Address;
import edu.common.api.Network;
import edu.common.api.Payload;
import edu.kvstore.api.Message;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeImplTest {
    private static final int TIME_FAILED_MILLIS = 2000;
    private static final int TIME_CLEANUP_MILLIS = 3000;
    private static final int REPLICATION_FACTOR = 3;
    private static final long TIMEOUT_MILLIS = 100;

    private long globalTime = 0L;
    private Supplier<Long> clock = () -> globalTime;

    private Network network = new LocalNetwork();

    private Address makeAddress(int port) {
        return new LocalAddress(port);
    }

    private Network.Node makeNode(Address address) {
        return new NodeImpl(address, network, clock, TIME_FAILED_MILLIS, TIME_CLEANUP_MILLIS, TIMEOUT_MILLIS, REPLICATION_FACTOR);
    }

    private void handle(Network.Node node, Network.Listener listener, int count) {
        assertEquals(count, listener.queue().size(), "Must be enough messages in the queue to process");
        for (int i=0; i<count; i++) {
            node.handle(listener.queue().poll());
        }
    }

    @Test
    void createReadUpdateReadDeleteRead() {
        Address selfAddr = makeAddress(10010);
        Address rep1Addr = makeAddress(10020);
        Address rep2Addr = makeAddress(10030);
        Address clientAddr = makeAddress(9999);

        Network.Node selfNode = makeNode(selfAddr);
        Network.Node rep1Node = makeNode(rep1Addr);
        Network.Node rep2Node = makeNode(rep2Addr);

        Network.Listener self = network.listen(selfAddr);
        Network.Listener rep1 = network.listen(rep1Addr);
        Network.Listener rep2 = network.listen(rep2Addr);
        Network.Listener client = network.listen(clientAddr);

        rep1Node.join(selfAddr);
        rep2Node.join(selfAddr);

        handle(selfNode, self, 2);
        selfNode.cycle();

        handle(rep1Node, rep1, 1);
        handle(rep2Node, rep2, 1);

        List<Address> members = Arrays.asList(selfAddr, rep2Addr, rep1Addr);
        assertEquals(members, selfNode.peers());
        assertEquals(members, rep1Node.peers());
        assertEquals(members, rep2Node.peers());

        String key = "17bc3e00-9fb0-4c5a-8c54-ea9af782a678";
        String val1 = "4bbca841-6935-4e8b-96f6-a783dd41d6ae";
        String val2 = "ce751a98-daa5-4e07-b1c7-cccf3296022d";

        Message createMessage = new Message(Message.Type.CREATE, 0L, clientAddr, key, val1, false);
        assertTrue(client.queue().isEmpty());
        network.send(selfAddr, Payload.of(createMessage));
        handle(selfNode, self, 1);
        handle(rep1Node, rep1, 1);
        handle(rep2Node, rep2, 1);
        handle(selfNode, self, 2);
        assertEquals(createMessage.ok(selfAddr), client.queue().poll().keyval);

        Message readMessage = new Message(Message.Type.READ, 1L, clientAddr, key, null, false);
        assertTrue(client.queue().isEmpty());
        network.send(selfAddr, Payload.of(readMessage));
        handle(selfNode, self, 1);
        handle(rep1Node, rep1, 1);
        handle(rep2Node, rep2, 1);
        handle(selfNode, self, 2);
        assertEquals(readMessage.value(val1).ok(selfAddr), client.queue().poll().keyval);

        Message updateMessage = new Message(Message.Type.UPDATE, 3L, clientAddr, key, val2, false);
        assertTrue(client.queue().isEmpty());
        network.send(selfAddr, Payload.of(updateMessage));
        handle(selfNode, self, 1);
        handle(rep1Node, rep1, 1);
        handle(rep2Node, rep2, 1);
        handle(selfNode, self, 2);
        assertEquals(updateMessage.value(val2).ok(selfAddr), client.queue().poll().keyval);

        Message read2Message = new Message(Message.Type.READ, 4L, clientAddr, key, null, false);
        assertTrue(client.queue().isEmpty());
        network.send(selfAddr, Payload.of(read2Message));
        handle(selfNode, self, 1);
        handle(rep1Node, rep1, 1);
        handle(rep2Node, rep2, 1);
        handle(selfNode, self, 2);
        assertEquals(read2Message.value(val2).ok(selfAddr), client.queue().poll().keyval);

        Message deleteMessage = new Message(Message.Type.DELETE, 5L, clientAddr, key, null, false);
        assertTrue(client.queue().isEmpty());
        network.send(selfAddr, Payload.of(deleteMessage));
        handle(selfNode, self, 1);
        handle(rep1Node, rep1, 1);
        handle(rep2Node, rep2, 1);
        handle(selfNode, self, 2);
        assertEquals(deleteMessage.ok(selfAddr), client.queue().poll().keyval);

        Message read3Message = new Message(Message.Type.READ, 6L, clientAddr, key, null, false);
        assertTrue(client.queue().isEmpty());
        network.send(selfAddr, Payload.of(read3Message));
        handle(selfNode, self, 1);
        handle(rep1Node, rep1, 1);
        handle(rep2Node, rep2, 1);
        handle(selfNode, self, 2);
        assertEquals(read3Message.ok(selfAddr), client.queue().poll().keyval);
    }

}
