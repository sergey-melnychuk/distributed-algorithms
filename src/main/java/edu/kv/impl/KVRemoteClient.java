package edu.kv.impl;

import edu.common.api.Address;
import edu.common.api.Network;
import edu.common.api.Payload;
import edu.common.impl.RemoteAddress;
import edu.common.impl.UdpNetwork;
import edu.kv.api.KVStore;
import edu.kv.api.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Supplier;

public class KVRemoteClient implements KVStore<String, String> {
    private static final Logger logger = LogManager.getLogger(KVRemoteClient.class);

    private final Address self;
    private final List<Address> remote;
    private final Network network;
    private final Supplier<Long> clock;
    private long seq = 0L;

    private final Network.Listener listener;

    private static final long TIMEOUT_MILLIS = 300;

    public KVRemoteClient(Address self, List<Address> remote, Network network, Supplier<Long> clock) {
        this.self = self;
        this.remote = remote;
        this.network = network;
        this.clock = clock;
        this.listener = network.listen(self);
    }

    private void inc() {
        seq += 1;
    }

    public void close() {
        try {
            listener.close();
        } catch (Exception e) {
            /* ignore */
        }
    }

    private void send(Message.Type type, String key, String val) {
        inc();
        Message msg = new Message(type, seq, self, key, val);
        Address address = remote.get((int) (seq % remote.size()));
        network.send(address, Payload.of(msg));
        logger.debug("Sent {}", msg);
    }

    private Message recv() {
        final long now = clock.get();
        while (clock.get() - now < TIMEOUT_MILLIS) {
            Payload payload = listener.queue().poll();
            if (payload != null) {
                logger.info("Received in {} ms: {}", System.currentTimeMillis() - now, payload);
                return payload.keyval;
            }
        }
        logger.debug("Received: null in {} ms", System.currentTimeMillis() - now);
        return null;
    }

    @Override
    public Set<String> keys() {
        return Collections.emptySet();
    }

    @Override
    public boolean create(String key, String value) {
        send(Message.Type.CREATE, key, value);
        Message msg = recv();
        return (msg != null) && (msg.type == Message.Type.OK);
    }

    @Override
    public String read(String key) {
        send(Message.Type.READ, key, null);
        Message msg = recv();
        if ((msg != null) && (msg.type == Message.Type.OK)) {
            return msg.value;
        } else {
            return null;
        }
    }

    @Override
    public boolean update(String key, String value) {
        send(Message.Type.UPDATE, key, value);
        Message msg = recv();
        return (msg != null) && (msg.type == Message.Type.OK);
    }

    @Override
    public boolean delete(String key) {
        send(Message.Type.DELETE, key, null);
        Message msg = recv();
        return (msg != null) && (msg.type == Message.Type.OK);
    }

    public static void main(String args[]) throws InterruptedException {
        KVRemoteClient client = new KVRemoteClient(
                new RemoteAddress("127.0.0.1", 10000),
                Arrays.asList(
                        new RemoteAddress("127.0.0.1", 10010),
                        new RemoteAddress("127.0.0.1", 10020),
                        new RemoteAddress("127.0.0.1", 10030)
                ),
                new UdpNetwork(), System::currentTimeMillis);

        for (int i=0; i<1000; i++) {
            String key = UUID.randomUUID().toString();
            boolean cr = client.create(key, "0");
            String r1 = Optional.ofNullable(client.read(key)).orElse("null");
            boolean up = client.update(key, "1");
            String r2 = Optional.ofNullable(client.read(key)).orElse("null");
            boolean dl = client.delete(key);
            String r3 = Optional.ofNullable(client.read(key)).orElse("null");

            if (i % 10 == 0) System.out.print(".");

            if (!(cr && up && dl) || !(r1.equals("0") && r2.equals("1") && r3.equals("null"))) {
                System.out.println("i=" + i);
                System.out.println("    key: " + key);
                System.out.println("created: " + cr);
                System.out.println("  read1: " + r1);
                System.out.println("updated: " + up);
                System.out.println("  read2: " + r2);
                System.out.println("deleted: " + dl);
                System.out.println("  read3: " + r3);
                System.out.println();
            }
        }

        client.close();
    }
}
