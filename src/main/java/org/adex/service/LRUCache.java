package org.adex.service;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache<T> implements CacheManager<T> {

    private final int capacity;
    private Map<Integer, Node<T>> map;
    private final Node<T> head = new Node<>();
    private final Node<T> tail = new Node<>();

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

    @Override
    public boolean isEmpty() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return map.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int capacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return this.capacity;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(T value) {
        Objects.requireNonNull(value, "Value cannot be null");

        final ReentrantLock lock = this.lock;
        lock.lock();

        try {
            int key = value.hashCode();

            Node<T> node = map.get(key);

            if (Objects.nonNull(node)) {
                node.value(value);
                moveToHead(node);
                return;
            }

            if (this.map.size() == this.capacity) {
                eviction();
            }

            node = new Node<>(value);
            this.map.put(key, node);
            addToHead(node);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(Collection<T> values, boolean dummy) {
        Objects.requireNonNull(values, "Collection cannot be null");
        if (values.isEmpty()) return;

        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (T value : values) {
                if (value != null)  {
                    this.put(value);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T get(T obj) {
        final ReentrantLock lock = this.lock;
        lock.lock();

        try {
            int key = obj.hashCode();
            Node<T> node = this.map.get(key);

            if (Objects.isNull(node)) {
                return null;
            }

            moveToHead(node);
            return node.value();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Collection<T> get() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return map.values()
                    .stream()
                    .map(Node::value)
                    .toList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();

        try {
            if (head.next().equals(tail)) {
                return null;
            }
            return this.head.next().value();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return map.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void purge() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            map.clear();
            head.next(tail);
            tail.previous(head);
        } finally {
            lock.unlock();
        }
    }

    private void eviction() {
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


    private void addToHead(Node<T> node) {
        node.next(head.next());
        node.previous(head);

        if (head.next() != null) {
            head.next().previous(node);
        }
        head.next(node);
    }

    private void moveToHead(Node<T> node) {
        removeNode(node);
        addToHead(node);
    }

    private void removeNode(Node<T> node) {
        Node<T> prev = node.previous();
        Node<T> next = node.next();

        if (prev != null) {
            prev.next(next);
        }

        if (next != null) {
            next.previous(prev);
        }
    }

}
