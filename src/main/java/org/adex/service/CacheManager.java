package org.adex.service;

import java.util.Collection;

public interface CacheManager<T> {

    void put(T value);

    void put(Collection<T> values, boolean dummy);

    T get(T value);

    Collection<T> get();

    T peek();

    int size();

    void purge();

    boolean isEmpty();
}
