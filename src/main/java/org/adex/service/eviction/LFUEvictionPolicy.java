package org.adex.service.eviction;

import org.adex.service.Node;

public class LFUEvictionPolicy<T> implements EvictionPolicy<T> {

    @Override
    public void onPut(T value) {}

    @Override
    public void onGet(T value) {
    }

    @Override
    public void evict() {
    }

    @Override
    public Node<T> head() {
        return null;
    }

    @Override
    public Node<T> tail() {
        return null;
    }

    @Override
    public void remove(Node<T> node) {

    }

}
