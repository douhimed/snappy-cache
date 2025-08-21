package org.adex.service.eviction;

import org.adex.service.Node;

public interface EvictionPolicy<T> {

    void onGet(T value);

    void onPut(T value);

    void evict();

    Node<T> head();

    Node<T> tail();

    void remove(Node<T> node);

    enum PolicyType {
        LRU, LFU,
    }
}