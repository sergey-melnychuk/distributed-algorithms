package edu.membership.gossip.api;

import java.util.List;

public interface Node<ADDRESS> {

    /**
     * Handle specific message received by the node
     * @param message message received by the node
     */
    void handle(Message<ADDRESS> message);

    /**
     * Cycle the node: increment heartbeat and update local time
     */
    void cycle();

    /**
     * Join cluster of nodes
     * @param address master node
     */
    void join(ADDRESS address);

    /**
     * Get address of current node
     * @return address of current node
     */
    ADDRESS address();

    /**
     * Return addresses of detected peer nodes
     * @return list of addresses of detected peer nodes
     */
    List<ADDRESS> peers();

    /**
     * Fail the node and make it unresponsive
     */
    void fail();
}
