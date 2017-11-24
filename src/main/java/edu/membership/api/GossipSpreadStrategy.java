package edu.membership.api;

import edu.common.api.Address;

import java.util.Set;

public interface GossipSpreadStrategy {

    Set<Address> targets(Membership membership, long now);

}
