package org.adex.service.eviction;

import org.adex.service.Node;
import org.adex.service.store.StorePolicy;

import java.util.Objects;

public class LRUEvictionPolicy<T> implements EvictionPolicy<T> {

    private StorePolicy<T> store;
    private final Node<T> head = new Node<>();
    private final Node<T> tail = new Node<>();

    public LRUEvictionPolicy(StorePolicy<T> store) {
        this.store = store;
        head.next(tail);
        tail.previous(head);
    }

    @Override
    public void onPut(T value) {
        int hash = Objects.hashCode(value);

        Node<T> node = store.get(hash);
        if (node == null) {
            node = new Node<>(value);
            store.put(hash, node);
        } else {
            node.value(value);
        }

        remove(node);
        addToFront(node);
    }

    @Override
    public void onGet(T value) {
        var node = store.get(Objects.hashCode(value));
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

        store.remove(toDelete.value().hashCode());
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