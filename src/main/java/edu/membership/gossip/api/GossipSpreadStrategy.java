package edu.membership.gossip.api;

import java.util.Set;

public interface GossipSpreadStrategy<ADDRESS> {

    Set<ADDRESS> targets(Membership<ADDRESS> membership, long now);

}
