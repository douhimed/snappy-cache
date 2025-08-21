package org.adex.service;

import org.adex.service.eviction.EvictionPolicy;
import org.adex.service.eviction.LFUEvictionPolicy;
import org.adex.service.eviction.LRUEvictionPolicy;
import org.adex.service.store.InMemoryStorePolicy;
import org.adex.service.store.RedisStorePolicy;
import org.adex.service.store.StorePolicy;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache<T> implements Cache<T> {

    private final int capacity;

    private long ttl;
    private EvictionPolicy<T> eviction;
    private StorePolicy<T> store;

    protected ReentrantLock lock = new ReentrantLock();

    public LRUCache() {
        this(16, 1000 * 60 * 60 * 24);
    }

    public LRUCache(int capacity) {
        this(capacity, 1000 * 60 * 60 * 24);
    }

    public LRUCache(int capacity, long ttl) {
        this.capacity = capacity;
        this.ttl = ttl;

        store = new InMemoryStorePolicy<>(capacity);
        eviction = new LRUEvictionPolicy<>(store);
    }

    public Cache<T> withPolicy(EvictionPolicy.PolicyType type) {
        this.eviction = type == EvictionPolicy.PolicyType.LFU
                ? new LFUEvictionPolicy<>()
                : new LRUEvictionPolicy<T>(store);
        return this;
    }

    public Cache<T> withStore(StorePolicy.StorePolicyType type) {
        this.store = type == StorePolicy.StorePolicyType.In_MEMORY
                ? new InMemoryStorePolicy<>(capacity)
                : new RedisStorePolicy<T>();
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
            return store.isEmpty();
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
            Node<T> node = this.store.get(key);

            if (Objects.isNull(node)) {
                return null;
            }

            if (node.isExpired(ttl)) {
                eviction.remove(node);
                return null;
            }

            node.updateAccessTime();
            eviction.onGet(obj);
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
            return store.values()
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

            Node<T> head = eviction.head();
            Node<T> tail = eviction.tail();

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
            return store.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void purge() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            store.clear();

            Node<T> head = eviction.head();
            Node<T> tail = eviction.tail();

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
        Node<T> node = store.get(key);

        if (node != null) {
            eviction.onPut(value);
            node.updateAccessTime();
            return;
        }

        if (store.size() == capacity) {
            eviction.evict();
        }

        node = new Node<>(value);
        store.put(key, node);
        eviction.onPut(value);
    }
}
