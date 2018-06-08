package edu.member.api;

import edu.common.api.Address;

import java.util.List;

public interface MemberNode {

    /**
     * Handle specific membership-related message received by the node
     * @param message message received by the node
     */
    void handle(Message message);

    /**
     * Cycle the node: increment heartbeat and update local time
     */
    void cycle();

    /**
     * Join cluster of nodes
     * @param address master node
     */
    void join(Address address);

    /**
     * Get address of current node
     * @return address of current node
     */
    Address address();

    /**
     * Return addresses of detected peer nodes
     * @return list of addresses of detected peer nodes
     */
    List<Address> peers();

    /**
     * Fail the node and make it unresponsive
     */
    void fail();
}
