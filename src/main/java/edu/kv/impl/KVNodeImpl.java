package edu.kv.impl;

import edu.common.api.Address;
import edu.common.api.Network;
import edu.common.api.Payload;
import edu.kv.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class KVNodeImpl implements KVNode {
    private static final Logger logger = LogManager.getLogger(KVNodeImpl.class);

    private final Address address;
    private final Network network;
    private final Supplier<Long> clock;
    private final Ring<String> ring;
    private final Replication<String, Address> replication;
    private final KVStore<String, String> kv;
    private final long timeoutMillis;
    private final int replicationFactor;
    private final int minimumQuorum;

    private long seq = 0L;
    private final ConcurrentHashMap<Long, Request> pendingRequests = new ConcurrentHashMap<>();

    public KVNodeImpl(Address address, Network network, Supplier<Long> clock, Ring<String> ring) {
        this(address, network, ring, clock, 100, 3);
    }

    public KVNodeImpl(Address address, Network network, Ring<String> ring, Supplier<Long> clock, long timeoutMillis, int replicationFactor) {
        this.address = address;
        this.network = network;
        this.clock = clock;
        this.ring = ring;
        this.replication = new RingReplication(replicationFactor, ring);
        this.timeoutMillis = timeoutMillis;
        this.replicationFactor = replicationFactor;
        this.minimumQuorum = replicationFactor / 2 + 1;
        this.kv = new LocalKVStore();
    }

    private long tick() {
        seq += 1;
        return clock.get();
    }

    private void send(Address address, Message message) {
        network.send(address, Payload.of(message));
    }

    private void pend(Message message, long now) {
        pendingRequests.put(message.seqNr, new Request(message, now,1));
        logger.debug("Pending request: {}", message);
    }

    private void check(Message message, long now) {
        if (message.type != Message.Type.ACK) {
            return;
        }
        if (pendingRequests.containsKey(message.seqNr)) {
            final Request req = pendingRequests.get(message.seqNr);
            if (now - req.ts >= timeoutMillis) {
                // Pending request failed due to timestamp
                pendingRequests.remove(message.seqNr);
                send(req.msg.sender, message.fail(address));
                logger.debug("Timeout for: {}", req);
            } else if (req.count + 1 >= minimumQuorum) {
                // Quorum reached for the request
                pendingRequests.remove(message.seqNr);
                send(req.msg.sender, message.ok(address));
                logger.debug("Ack received and quorum reached: {}", req);
            } else {
                pendingRequests.replace(message.seqNr, req.inc());
                logger.debug("Ack received: {}", req);
            }
        }
    }

    private void replicate(Message message, long now) {
        List<Address> targets = replication.pick(message.key, now);
        if (targets.size() < replicationFactor) {
            logger.error("Not enough replicas in the group: " + ring.ordered(now).size());
            send(message.sender, message.fail(address));
        } else if (targets.get(0).equals(address)) {
            // Replicate the key, you're the key's master
            apply(message);
            pend(message, now);
            Message replica = message.replica(address);
            for (int i=1; i<targets.size(); i++) {
                logger.debug("Send replication to {}", targets.get(i));
                send(targets.get(i), replica);
            }
        } else {
            // Forward the message to key's master node
            send(targets.get(0), message);
            logger.debug("Forwarding request to {}", targets.get(0));
        }
    }

    private Message apply(Message message) {
        switch (message.type) {
            case READ:
                String val = kv.read(message.key);
                logger.debug("read made key={} value={}", message.key, val);
                return message.value(val);
            case CREATE:
                kv.create(message.key, message.value);
                logger.debug("create saved key={} value={}", message.key, message.value);
                return message.ack();
            case UPDATE:
                kv.update(message.key, message.value);
                logger.debug("update saved key={} value={}", message.key, message.value);
                return message.ack();
            case DELETE:
                kv.delete(message.key);
                logger.debug("delete saved key={}", message.key);
                return message.ack();
            default:
                throw new IllegalStateException("Unexpected message: " + message);
        }
    }

    @Override
    public void handle(Message m) {
        logger.debug("[{}] Received: {}", address, m);
        final long now = tick();

        if (m.type == Message.Type.ACK) {
            check(m, now);
        } else {
            Message message = m.accept(seq);
            if (message.replica) {
                Message reply = apply(message);
                send(message.sender, reply);
            } else {
                replicate(message, now);
            }
        }
    }

    private static class Request {
        final Message msg;
        final long ts;
        final int count;

        private Request(Message msg, long ts, int count) {
            this.msg = msg;
            this.ts = ts;
            this.count = count;
        }

        Request inc() {
            return new Request(msg, ts, count + 1);
        }

        @Override
        public String toString() {
            return "Request{" +
                    "msg=" + msg +
                    ", ts=" + ts +
                    ", count=" + count +
                    '}';
        }
    }

}
