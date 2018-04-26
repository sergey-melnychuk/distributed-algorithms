package edu.kvstore.impl;

import edu.common.api.Address;
import edu.common.api.Network;
import edu.common.impl.LocalAddress;
import edu.common.impl.LocalNetwork;
import edu.kvstore.api.KVNode;
import edu.kvstore.api.Message;
import edu.kvstore.api.Ring;
import edu.membership.api.Member;
import edu.membership.impl.GossipMembership;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KVNodeImplTest {
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

    private Ring<String> makeRing(Address self) {
        return makeRing(self, TIME_FAILED_MILLIS, TIME_CLEANUP_MILLIS);
    }

    private Ring<String> makeRing(Address self, long timeFailedMillis, long timeCleanupMillis) {
        return new HashRing(Byte.MAX_VALUE, new GossipMembership(self, timeFailedMillis, timeCleanupMillis));
    }

    private KVNode makeNode(Address address, Ring<String> ring) {
        return new KVNodeImpl(address, network, ring, clock, TIMEOUT_MILLIS, REPLICATION_FACTOR);
    }

    @Test
    void failWhenNotEnoughMembers() {
        Address self = makeAddress(10000);
        Ring<String> ring = makeRing(self);
        ring.add(new Member(self, 0, 0), 0);

        KVNode node = makeNode(self, ring);

        Address client = makeAddress(20000);
        Network.Listener clientListener = network.listen(client);

        Message message = new Message(Message.Type.CREATE, 0L, client, "key", "val");
        node.handle(message);

        assertEquals(message.fail(self), clientListener.queue().peek().keyval);
    }

    @Test
    void replicateWhenEnoughMembers() {
        Address self = makeAddress(10000);
        Address rep1 = makeAddress(10001);
        Address rep2 = makeAddress(10002);
        Ring<String> ring = makeRing(self);
        ring.add(new Member(self, 0, 0), 0);
        ring.add(new Member(rep1, 0, 0), 0);
        ring.add(new Member(rep2, 0, 0), 0);

        KVNode node = makeNode(self, ring);

        Network.Listener rep1Listener = network.listen(rep1);
        Network.Listener rep2Listener = network.listen(rep2);

        Address client = makeAddress(20000);
        Message message = new Message(Message.Type.CREATE, 0L, client, "a", "a");
        node.handle(message);

        assertEquals(message.replica(self).accept(1), rep1Listener.queue().peek().keyval);
        assertEquals(message.replica(self).accept(1), rep2Listener.queue().peek().keyval);
    }

    @Test
    void replicateAndRespond() {
        Address self = makeAddress(10000);
        Address rep1 = makeAddress(10001);
        Address rep2 = makeAddress(10002);
        Ring<String> ring = makeRing(self);
        ring.add(new Member(self, 0, 0), 0);
        ring.add(new Member(rep1, 0, 0), 0);
        ring.add(new Member(rep2, 0, 0), 0);

        KVNode node = makeNode(self, ring);
        KVNode rep1Node = makeNode(rep1, ring);
        KVNode rep2Node = makeNode(rep2, ring);

        Network.Listener selfListener = network.listen(self);

        Address client = makeAddress(20000);
        Network.Listener clientListener = network.listen(client);

        Message message = new Message(Message.Type.CREATE, 0L, client, "a", "a");
        node.handle(message);

        rep1Node.handle(message.replica(self).accept(1));
        rep2Node.handle(message.replica(self).accept(1));

        node.handle(selfListener.queue().poll().keyval);
        node.handle(selfListener.queue().poll().keyval);

        assertEquals(message.ok(self), clientListener.queue().peek().keyval);
    }

    @Test
    void replicateAndRespondOnTimeout() {
        Address self = makeAddress(10000);
        Address rep1 = makeAddress(10001);
        Address rep2 = makeAddress(10002);
        Ring<String> ring = makeRing(self);
        ring.add(new Member(self, 0, 0), 0);
        ring.add(new Member(rep1, 0, 0), 0);
        ring.add(new Member(rep2, 0, 0), 0);

        KVNode node = makeNode(self, ring);
        KVNode rep1Node = makeNode(rep1, ring);
        KVNode rep2Node = makeNode(rep2, ring);

        Network.Listener selfListener = network.listen(self);

        Address client = makeAddress(20000);
        Network.Listener clientListener = network.listen(client);

        Message message = new Message(Message.Type.CREATE, 0L, client, "a", "a");
        node.handle(message);

        rep1Node.handle(message.replica(self).accept(1));
        rep2Node.handle(message.replica(self).accept(1));

        globalTime = TIMEOUT_MILLIS; //simulate timeout

        node.handle(selfListener.queue().poll().keyval);
        node.handle(selfListener.queue().poll().keyval);

        assertEquals(message.fail(self), clientListener.queue().peek().keyval);
    }
}
