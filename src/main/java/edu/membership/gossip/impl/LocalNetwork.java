package edu.membership.gossip.impl;

import edu.membership.gossip.api.Address;
import edu.membership.gossip.api.Message;
import edu.membership.gossip.api.Network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LocalNetwork implements Network<Address, Message<Address>> {

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Message<Address>>> listeners = new ConcurrentHashMap<>();

    private String key(Address address) {
        return address.host() + ":" + address.port();
    }

    @Override
    public boolean send(Address target, Message<Address> message) {
        String key = key(target);
        if (listeners.containsKey(key)) {
            listeners.get(key).offer(message);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Listener<Message<Address>> listen(final Address address) {
        final ConcurrentLinkedQueue<Message<Address>> queue = new ConcurrentLinkedQueue<>();

        listeners.put(key(address), queue);

        return new Listener<Message<Address>>() {
            @Override
            public Queue<Message<Address>> queue() {
                return queue;
            }

            @Override
            public void stop() {
                listeners.remove(key(address));
                queue.clear();
            }
        };
    }

}
