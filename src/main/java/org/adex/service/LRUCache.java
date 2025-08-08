package org.adex.service;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache<T> implements Cache<T> {

    private final int capacity;
    private final Map<Integer, Node<T>> map;

    private long ttl;
    private EvictionPolicy<T> policy;

    protected ReentrantLock lock = new ReentrantLock();

    public LRUCache() {
        this(16, 1000 * 60 * 60 * 24);
    }

    public LRUCache(int capacity) {
        this(capacity, 1000 * 60 * 60 * 24);
    }

    public LRUCache(int capacity, long ttl) {
        this.capacity = capacity;
        this.map = new HashMap<>(capacity);
        this.ttl = ttl;

        policy = new LRUEvictionPolicy<>(map);
    }

    public Cache<T> withPolicy(EvictionPolicy.PolicyType type) {
        this.policy = type == EvictionPolicy.PolicyType.LFU
                ? new LFUEvictionPolicy<>()
                : new LRUEvictionPolicy<T>(map);
        return this;
    }

    public Cache<T> ttl(long ttl) {
        this.ttl = ttl;
        return this;
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
            putInternal(value);
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
                if (value != null) {
                    putInternal(value);
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

            if (node.isExpired(ttl)) {
                policy.remove(node);
                return null;
            }

            node.updateAccessTime();
            policy.onGet(obj);
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
                    .filter(n -> !n.isExpired(ttl))
                    .peek(Node::updateAccessTime)
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

            Node<T> head = policy.head();
            Node<T> tail = policy.tail();

            Node<T> next = head.next();
            if (next.equals(tail)) {
                return null;
            }
            next.updateAccessTime();
            return next.value();
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

            Node<T> head = policy.head();
            Node<T> tail = policy.tail();

            head.next().previous(null);
            tail.previous().next(null);

            head.next(tail);
            tail.previous(head);
        } finally {
            lock.unlock();
        }
    }

    private void putInternal(T value) {
        int key = value.hashCode();
        Node<T> node = map.get(key);

        if (node != null) {
            policy.onPut(value);
            node.updateAccessTime();
            return;
        }

        if (map.size() == capacity) {
            policy.evict();
        }

        node = new Node<>(value);
        map.put(key, node);
        policy.onPut(value);
    }
}
