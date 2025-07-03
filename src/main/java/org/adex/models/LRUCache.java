package org.adex.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LRUCache<T> {

    private final int capacity;
    private final Map<Integer, Node<T>> map;
    private Node<T> head = new Node<>();
    private Node<T> tail = new Node<>();

    public LRUCache() {
        this(16);
    }

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>(capacity);

        head.next(tail);
        tail.previous(head);
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public void put(T value) {
        int key = value.hashCode();
        Node<T> node;

        if (this.map.size() == this.capacity) {
            eviction();
        }

        if (this.map.containsKey(key)) {
            node = map.get(key);
        } else {
            node = new Node<>(value);
            this.map.put(key, node);
        }
        LinkAsMostRecentlyUsed(node);
    }

    public T get(T obj) {
        int key = obj.hashCode();
        Node<T> node = this.map.get(key);

        if (Objects.isNull(node)) {
            return null;
        }

        LinkAsMostRecentlyUsed(node);
        return node.value();
    }

    public T peek() {
        return this.head.next().value();
    }

    public int size() {
        return this.map.size();
    }

    private void LinkAsMostRecentlyUsed(Node<T> node) {
        detachNodeIfOld(node);
        moveToHead(node);
    }

    private void moveToHead(Node<T> node) {
        node.previous(head);
        node.next(head.next());
        head.next().previous(node);
        head.next(node);
    }

    private void detachNodeIfOld(Node<T> node) {
        Node<T> previous = node.previous();
        Node<T> next = node.next();

        if (Objects.nonNull(previous)) {
            previous.next(next);
        }

        if (Objects.nonNull(next)) {
            next.previous(previous);
        }
    }

    private void eviction() {
        Node<T> toDelete = tail.previous();

        tail.previous(toDelete.previous());
        toDelete.previous().next(tail);

        map.remove(toDelete.value().hashCode());
    }
}
