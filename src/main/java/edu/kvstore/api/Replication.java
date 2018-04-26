package edu.kvstore.api;

import java.util.List;

public interface Replication<KEY, ADDRESS> {

    /**
     * Get nodes responsible for storing replicas of the key. First node is always the owner, other are replicas.
     * @param key key
     * @return list of replicas' addresses
     */
    List<ADDRESS> pick(KEY key, long now);

}
