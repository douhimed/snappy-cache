package org.adex.models;

import java.util.HashMap;
import java.util.Map;

public class LRUCache<T> {

    private final int capacity;
    private final Map<String, Node<T>> map;
    private Node<T> head;
    private Node<T> tail;

    public LRUCache() {
        this(16);
    }

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>(capacity);
    }

}
