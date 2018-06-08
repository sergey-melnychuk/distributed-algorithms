package edu.leader.api;

import edu.common.api.Address;

import java.util.Optional;

public interface LeaderAwareNode {

    /**
     * Get the next value from cluster-wide ordered leader-aware sequence
     * @return Optional.of(number) if request succeeded, Optional.empty() otherwise
     */
    Optional<Long> getValue();

    /**
     * Get address of current leader
     * @return Optional.of(leader address) if leader is accepted or Optional.empty() if no master is elected
     */
    Optional<Address> getLeader();

    /**
     * Check if current node is accepted leader
     * @return true if this node is currently accepted leader, false otherwise
     */
    boolean isLeader();

}
