package org.adex.service.store;

import org.adex.service.Node;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InMemoryStorePolicy<T> implements StorePolicy<T> {

    private final Map<Integer, Node<T>> map;

    public InMemoryStorePolicy(int capacity) {
        this.map = new HashMap<>(capacity);
    }

    @Override
    public Node<T> get(int key) {
        return map.get(key);
    }

    @Override
    public void put(int key, Node<T> value) {
        map.put(key, value);
    }

    @Override
    public void remove(int key) {
        map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();

    }

    @Override
    public Collection<Node<T>> values() {
        return map.values();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
}
