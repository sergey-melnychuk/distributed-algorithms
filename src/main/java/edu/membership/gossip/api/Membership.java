package edu.membership.gossip.api;

import java.util.List;

public interface Membership<ADDRESS> {

    interface Member<ADDRESS> {
        String id();
        ADDRESS address();
        long timestamp();
        long heartbeat();
        Member<ADDRESS> updated(long timestamp);
        Member<ADDRESS> updated(long heartbeat, long timestamp);
    }

    /**
     * Add node to the membership. If node satisfies membership restrictions, it should be added to the membership.
     * @param node node to be added to the membership
     */
    void add(Membership.Member<ADDRESS> node, long now);

    /**
     * List all active members that match membership restrictions.
     * All failure detection should be performed before returning the list.
     * @return list of active members
     */
    List<Membership.Member<ADDRESS>> list(long now);

    /**
     * Reset membership list
     */
    void reset();

    /**
     * Return self address (owner of the membership list)
     * @return self address
     */
    ADDRESS self();

}
