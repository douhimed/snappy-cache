package org.adex.models;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache<T> {

    private final int capacity;
    private Map<Integer, Node<T>> map;
    private Node<T> head = new Node<>();
    private Node<T> tail = new Node<>();

    protected ReentrantLock lock = new ReentrantLock();

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
        Objects.requireNonNull(value, "Value cannot be null");

        int key = value.hashCode();

        Node<T> node = map.get(key);

        if (Objects.nonNull(node)) {
            node.value(value);
            linkAsMostRecentlyUsed(node);
            return;
        }

        if (this.map.size() == this.capacity) {
            eviction();
        }

        node = new Node<>(value);
        this.map.put(key, node);
        linkAsMostRecentlyUsed(node);
    }

    public void put(Collection<T> values, boolean dummy) {
        Objects.requireNonNull(values, "Collection cannot be null");
        values.forEach(this::put);
    }

    public T get(T obj) {
        final ReentrantLock lock = this.lock;
        lock.lock();

        try {
            int key = obj.hashCode();
            Node<T> node = this.map.get(key);

            if (Objects.isNull(node)) {
                return null;
            }

            linkAsMostRecentlyUsed(node);
            return node.value();
        } finally {
            lock.unlock();
        }
    }

    public Collection<T> getAll() {
        return map.values()
                .stream().map(Node::value)
                .toList();
    }

    public T peek() {
        if (head.next().equals(tail)) {
            return null;
        }
        return this.head.next().value();
    }

    public int size() {
        return this.map.size();
    }

    public void purge() {
        map = new HashMap<>(this.capacity);
        head.next(tail);
        tail.previous(head);
    }

    private void linkAsMostRecentlyUsed(Node<T> node) {
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
