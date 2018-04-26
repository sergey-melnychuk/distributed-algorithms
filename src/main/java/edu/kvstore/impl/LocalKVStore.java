package edu.kvstore.impl;

import edu.kvstore.api.KVStore;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LocalKVStore implements KVStore<String, String> {
    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    @Override
    public Set<String> keys() { return map.keySet(); }

    @Override
    public boolean create(String key, String value) {
        return map.put(key, value) != null;
    }

    @Override
    public String read(String key) {
        return map.get(key);
    }

    @Override
    public boolean update(String key, String value) {
        return map.replace(key, value) != null;
    }

    @Override
    public boolean delete(String key) {
        return map.remove(key) != null;
    }
}
