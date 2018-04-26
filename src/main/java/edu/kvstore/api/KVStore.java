package edu.kvstore.api;

import java.util.Set;

public interface KVStore<K, V> {

    /**
     * Get all keys in the store
     * @return all keys in the store
     */
    Set<K> keys();

    /**
     * Create key-value pair
     * @param key key
     * @param value value
     * @return true if created successfully, false otherwise
     */
    boolean create(K key, V value);

    /**
     * Read value for a given key
     * @param key key
     * @return Optional.of(value) if value is present, Optional.empty() otherwise
     */
    String read(K key);

    /**
     * Update value for a given key
     * @param key key
     * @param value new value
     * @return true if updated successfully, false otherwise
     */
    boolean update(K key, V value);

    /**
     * Delete key-value pair for a given key
     * @param key key
     * @return true if key deleted successfully, false otherwise (including when there was no such key)
     */
    boolean delete(K key);

}
