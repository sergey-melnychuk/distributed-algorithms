package edu.kvstore.api;

import edu.common.api.Address;
import edu.member.api.Membership;

import java.util.List;

public interface Ring<K> extends Membership {

    /**
     * Return ordered list of addresses in the ring.
     * @return ordered list of nodes in the ring
     */
    List<Address> ordered(long now);

    /**
     * Assign specific node to the key, using consistent hashing for key is considered
     * @param key key
     * @return address of the node
     */
    Address pick(K key, long now);

}
