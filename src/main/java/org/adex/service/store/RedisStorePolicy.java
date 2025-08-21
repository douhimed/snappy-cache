package org.adex.service.store;

import org.adex.service.Node;

import java.util.Collection;
import java.util.List;

public class RedisStorePolicy<T> implements StorePolicy<T> {
    
    @Override
    public Node<T> get(int key) {
        return null;
    }

    @Override
    public void put(int key, Node<T> value) {

    }

    @Override
    public void remove(int key) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Collection<Node<T>> values() {
        return List.of();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
