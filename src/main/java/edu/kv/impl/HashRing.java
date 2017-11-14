package edu.kv.impl;

import edu.kv.api.Ring;
import edu.common.api.Address;
import edu.membership.api.Member;
import edu.membership.api.Membership;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class HashRing implements Ring<String> {
    private final int base;
    private final Membership membership;

    public HashRing(int base, Membership membership) {
        this.base = base;
        this.membership = membership;
    }

    private int hash(Address address) {
        return address.hashCode() % base;
    }

    private int hash(String key) {
        return key.hashCode() % base;
    }

    private List<Address> nodes(long now) {
        return membership.list(now).stream()
                .map((m) -> m.address)
                .collect(Collectors.toList());
    }

    @Override
    public List<Address> ordered(long now) {
        List<Address> nodes = nodes(now);
        nodes.sort(Comparator.comparing(this::hash));
        return nodes;
    }

    @Override
    public Address pick(String key, long now) {
        List<Address> nodes = nodes(now);
        TreeMap<Integer, Address> map = new TreeMap<>();
        nodes.forEach((address) -> map.put(hash(address), address));
        Map.Entry<Integer, Address> entry = map.ceilingEntry(hash(key));
        if (entry != null) {
            return entry.getValue();
        } else {
            return map.firstEntry().getValue();
        }
    }

    @Override
    public void add(Member node, long now) {
        membership.add(node, now);
    }

    @Override
    public List<Member> list(long now) {
        return membership.list(now);
    }

    @Override
    public void reset() {
        membership.reset();
    }

    @Override
    public Address self() {
        return membership.self();
    }
}
