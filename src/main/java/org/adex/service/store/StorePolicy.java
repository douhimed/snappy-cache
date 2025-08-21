package org.adex.service.store;

import org.adex.service.Node;

import java.util.Collection;

public interface StorePolicy<T> {

    Node<T> get(int key);

    void put(int key, Node<T> value);

    void remove(int key);

    void clear();

    Collection<Node<T>> values();

    int size();

    boolean isEmpty();

    enum StorePolicyType {
        In_MEMORY, REDIS;
    }
}
