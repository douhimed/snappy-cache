package org.adex.service;

import java.util.Map;
import java.util.Objects;

public class LRUEvictionPolicy<T> implements EvictionPolicy<T> {

    private final Map<Integer, Node<T>> map;
    private final Node<T> head = new Node<>();
    private final Node<T> tail = new Node<>();

    public LRUEvictionPolicy(Map<Integer, Node<T>> map) {
        this.map = map;
        head.next(tail);
        tail.previous(head);
    }

    @Override
    public void onPut(T value) {
        int hash = Objects.hashCode(value);

        var node = map.computeIfAbsent(hash, h -> new Node<>(value));
        node.value(value);

        remove(node);
        addToFront(node);
    }

    @Override
    public void onGet(T value) {
        var node = map.get(Objects.hashCode(value));
        if (node != null) {
            remove(node);
            addToFront(node);
        }
    }

    @Override
    public void evict() {
        Node<T> toDelete = tail.previous();

        if (toDelete == null) {
            return;
        }

        Node<T> newLast = toDelete.previous();
        tail.previous(newLast);

        if (newLast != null) {
            newLast.next(tail);
        }

        map.remove(toDelete.value().hashCode());
        toDelete.next(null);
        toDelete.previous(null);
    }

    @Override
    public Node<T> head() {
        return head;
    }

    @Override
    public Node<T> tail() {
        return tail;
    }

    @Override
    public void remove(Node<T> node) {
        var prev = node.previous();
        var next = node.next();

        if (prev != null) prev.next(next);
        if (next != null) next.previous(prev);
    }

    private void addToFront(Node<T> node) {
        node.next(head.next());
        node.previous(head);

        if (head.next() != null) {
            head.next().previous(node);
        }
        head.next(node);
    }
}