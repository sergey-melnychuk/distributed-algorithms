package edu.leadership.api;

import edu.common.api.Address;

import java.util.Optional;

public interface VotingNode {

    /**
     * Get address of current master
     * @return Optional.of(master address) if master is elected or Optional.empty() if no master is elected
     */
    Optional<Address> getMaster();

    /**
     * Check if current node is master
     * @return true if this node is currently elected master, false otherwise
     */
    boolean isMaster();

}
