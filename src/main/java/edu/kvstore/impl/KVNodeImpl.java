package edu.kvstore.impl;

import edu.common.api.Address;
import edu.common.api.Network;
import edu.common.api.Payload;
import edu.kvstore.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class KVNodeImpl implements KVNode {
    private static final Logger logger = LogManager.getLogger(KVNodeImpl.class);

    private final Address address;
    private final Network network;
    private final Supplier<Long> clock;
    private final Ring<String> ring;
    private final Replication<String, Address> replication;
    private final KVStore<String, String> kv;
    private final Map<String, List<Address>> copies;
    private final long timeoutMillis;
    private final int replicationFactor;
    private final int minimumQuorum;

    private long seq = 0L;
    private final ConcurrentHashMap<Long, Request> pendingRequests = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> bufferedRequests = new ConcurrentHashMap<>();

    public KVNodeImpl(Address address, Network network, Supplier<Long> clock, Ring<String> ring) {
        this(address, network, ring, clock, 100, 3);
    }

    public KVNodeImpl(Address address, Network network, Ring<String> ring, Supplier<Long> clock, long timeoutMillis, int replicationFactor) {
        this.address = address;
        this.network = network;
        this.clock = clock;
        this.ring = ring;
        this.replication = new RingReplication(replicationFactor, ring);
        this.kv = new LocalKVStore();
        this.copies = new HashMap<>();
        this.timeoutMillis = timeoutMillis;
        this.replicationFactor = replicationFactor;
        this.minimumQuorum = replicationFactor / 2 + 1;
    }

    private long tick() {
        seq += 1;
        return clock.get();
    }

    private void send(Address address, Message message) {
        network.send(address, Payload.of(message));
        logger.debug("[{}] Sent {} to {}", this.address, message, address);
    }

    private void pend(Message message, long now, long seq) {
        pendingRequests.put(seq, new Request(message, now,1));
        logger.debug("[{}] Pending request: {}", this.address, message);
    }

    private void cleanup(long now) {
        for (Map.Entry<Long, Request> req : pendingRequests.entrySet()) {
            if (now - req.getValue().ts >= timeoutMillis) {
                // Pending request failed due to timestamp
                logger.debug("[{}] Timeout for: {}", this.address, req);
                pendingRequests.remove(req.getKey());
                send(req.getValue().msg.sender, req.getValue().msg.fail(address));
            }
        }
    }

    private void check(Message message, long now) {
        if (message.type != Message.Type.ACK) {
            return;
        }
        if (pendingRequests.containsKey(message.seqNr)) {
            final Request req = pendingRequests.get(message.seqNr);
            if (req.count + 1 >= minimumQuorum) {
                // Quorum reached for the request
                logger.debug("[{}] Ack received and quorum reached: {}", this.address, req);
                send(req.msg.sender, req.msg.value(message.value).ok(address));
                pendingRequests.remove(message.seqNr);

                release(message.key, now);
            } else {
                logger.debug("[{}] Ack received: {}", this.address, req);
                pendingRequests.replace(message.seqNr, req.inc());
            }
        }
    }

    private void replicate(Message message, long now) {
        List<Address> targets = replication.pick(message.key, now);
        if (targets.size() < replicationFactor) {
            logger.error("[{}] Not enough copies in the group: {}", this.address, ring.ordered(now).size());
            send(message.sender, message.fail(address));
            return;
        }
        if (targets.get(0).equals(address)) {
            // Replicate the key, you're the key's master
            apply(message, targets);
            pend(message, now, seq);

            if (!isIdempotent(message)) {
                block(message.key);
            }

            Message replica = message.replica(address).accept(seq);
            for (int i=1; i<targets.size(); i++) {
                logger.debug("[{}] Send replication to {}", this.address, targets.get(i));
                send(targets.get(i), replica);
            }
        } else {
            // Forward the message to key's master node
            logger.debug("[{}] Forwarding request to {}", this.address, targets.get(0));
            send(targets.get(0), message);
        }
    }

    private Message apply(Message message, List<Address> replicas) {
        switch (message.type) {
            case READ:
                String val = kv.read(message.key);
                logger.debug("[{}] read made key={} value={}", this.address, message.key, val);
                return message.value(val);
            case REPLICATE:
            case CREATE:
                kv.create(message.key, message.value);
                copies.put(message.key, replicas);
                logger.debug("[{}] create saved key={} value={}", this.address, message.key, message.value);
                return message.ack();
            case UPDATE:
                kv.update(message.key, message.value);
                logger.debug("[{}] update saved key={} value={}", this.address, message.key, message.value);
                return message.ack();
            case DELETE:
                kv.delete(message.key);
                copies.remove(message.key);
                logger.debug("[{}] delete saved key={}", this.address, message.key);
                return message.ack();
            default:
                throw new IllegalStateException("Unexpected message: {}" + message);
        }
    }

    private boolean isIdempotent(Message message) {
        return message.type == Message.Type.READ;
    }

    private void block(String key) {
        bufferedRequests.put(key, new ConcurrentLinkedQueue<>());
    }

    private void buffer(Message message) {
        bufferedRequests.get(message.key).offer(message);
        logger.debug("Message buffered: {}", message);
    }

    private void release(String key, long now) {
        if (bufferedRequests.containsKey(key)) {
            final ConcurrentLinkedQueue<Message> q = bufferedRequests.get(key);
            while (!q.isEmpty()) {
                final Message m = q.poll();
                process(m, now);
            }
            bufferedRequests.remove(key);
        }
    }

    private void process(Message message, long now) {
        if (message.type == Message.Type.ACK) {
            check(message, now);
        } else {
            if (message.replica) {
                Message reply = apply(message, replication.pick(message.key, now));
                send(message.sender, reply);
            } else {
                replicate(message, now);
            }
        }
    }

    private void rebalance(long now) {
        Set<Address> failed = ring.failed(now).stream()
                .map(m -> m.address)
                .collect(Collectors.toSet());

        if (failed.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<Address>> e : copies.entrySet()) {
            final String key = e.getKey();

            Set<Address> stored = new HashSet<>(e.getValue());
            List<Address> targets = replication.pick(key, now);

            if (! stored.containsAll(targets)) {
                seq += 1;
                Message message = new Message(Message.Type.REPLICATE, seq, address, key, kv.read(key));
                replicate(message, now);
            }
        }
    }

    @Override
    synchronized public void handle(Message message) {
        logger.debug("[{}] Received: {}", address, message);
        final long now = tick();
        cleanup(now);
        rebalance(now);
        if (bufferedRequests.containsKey(message.key) && !message.replica) {
            buffer(message);
        } else {
            process(message, now);
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
