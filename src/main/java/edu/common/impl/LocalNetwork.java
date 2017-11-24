package edu.common.impl;

import edu.common.api.Address;
import edu.common.api.Network;
import edu.common.api.Payload;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LocalNetwork implements Network {

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Payload>> listeners = new ConcurrentHashMap<>();

    private String key(Address address) {
        return address.host() + ":" + address.port();
    }

    @Override
    public boolean send(Address target, Payload payload) {
        String key = key(target);
        if (listeners.containsKey(key)) {
            listeners.get(key).offer(payload);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Listener listen(final Address address) {
        final ConcurrentLinkedQueue<Payload> queue = new ConcurrentLinkedQueue<>();

        listeners.put(key(address), queue);

        return new Listener() {
            @Override
            public Queue<Payload> queue() {
                return queue;
            }

            @Override
            public void close() {
                listeners.remove(key(address));
                queue.clear();
            }
        };
    }

}
