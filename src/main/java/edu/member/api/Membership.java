package edu.member.api;

import edu.common.api.Address;

import java.util.List;

public interface Membership {

    /**
     * Add node to the membership. If node satisfies membership restrictions, it should be added to the membership.
     * @param now timestamp
     * @param node node to be added to the membership
     */
    void add(Member node, long now);

    /**
     * List all active members that match membership restrictions.
     * All failure detection should be performed before returning the list.
     * @param now timestamp
     * @return list of active members
     */
    List<Member> list(long now);

    /**
     * List all failed members that are currently present in membership list,
     * but don't match membership restrictions at a given moment of time.
     * @param now timestamp
     * @return list of members detected as failed
     */
    List<Member> failed(long now);

    /**
     * Reset membership list
     */
    void reset();

    /**
     * Return self address (owner of the membership list)
     * @return self address
     */
    Address self();

}
