package edu.member.impl;

import edu.common.api.Address;
import edu.member.api.GossipSpreadStrategy;
import edu.member.api.Membership;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class RandomGossipSpreadStrategy implements GossipSpreadStrategy {
    private final int numberOfTargets;
    private final Random random;

    RandomGossipSpreadStrategy(int numberOfTargets) {
        this.numberOfTargets = numberOfTargets;
        this.random = new Random();
    }

    RandomGossipSpreadStrategy(int numberOfTargets, long seed) {
        this.numberOfTargets = numberOfTargets;
        this.random = new Random(seed);
    }

    @Override
    public Set<Address> targets(Membership membership, long now) {

        List<Address> candidates = membership.list(now).stream()
                .filter(m -> !m.address.equals(membership.self()))
                .map(m -> m.address)
                .collect(Collectors.toList());

        Set<Address> selected = new HashSet<>();

        if (candidates.size() <= numberOfTargets) {
            selected.addAll(candidates);
            return selected;
        } else {
            while (selected.size() < numberOfTargets) {
                int idx = random.nextInt(candidates.size());
                selected.add(candidates.get(idx));
            }
            return selected;
        }
    }
}
