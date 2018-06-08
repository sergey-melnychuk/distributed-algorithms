package edu.leader.impl;

import edu.common.api.Address;
import edu.leader.api.LeaderAwareNode;
import edu.membership.api.Membership;

import java.util.Optional;
import java.util.function.Supplier;

public class LeaderVotingNode implements LeaderAwareNode {

    private final Membership membership;
    private final Supplier<Long> clock;

    public LeaderVotingNode(Membership membership, Supplier<Long> clock) {
        this.membership = membership;
        this.clock = clock;
    }

    @Override
    public Optional<Long> getValue() {
        return Optional.empty();
    }

    @Override
    public Optional<Address> getLeader() {
        return Optional.empty();
    }

    @Override
    public boolean isLeader() {
        return false;
    }

}
