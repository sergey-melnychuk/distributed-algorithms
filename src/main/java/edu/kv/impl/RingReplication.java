package edu.kv.impl;

import edu.kv.api.Replication;
import edu.kv.api.Ring;
import edu.common.api.Address;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RingReplication implements Replication<String, Address> {
    private final int replicationFactor;
    private final Ring<String> ring;

    public RingReplication(int replicationFactor, Ring<String> ring) {
        this.replicationFactor = replicationFactor;
        this.ring = ring;
    }

    @Override
    public List<Address> pick(String key, long now) {
        Address owner = ring.pick(key, now);
        List<Address> nodes = ring.ordered(now);

        if (nodes.size() < replicationFactor) {
            return Collections.emptyList();
        }

        List<Address> selected = new ArrayList<>();
        selected.add(owner);

        int index = nodes.indexOf(owner) + 1;

        while (selected.size() < replicationFactor) {
            if (index < nodes.size()) {
                selected.add(nodes.get(index));
                index += 1;
            } else {
                index = 0;
            }
        }

        return selected;
    }
}
