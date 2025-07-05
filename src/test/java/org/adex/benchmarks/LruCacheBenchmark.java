package org.adex.benchmarks;


import org.adex.service.LRUCache;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class LruCacheBenchmark {

    @Param({"1000", "10000", "100000", "1000000"})
    public int cacheSize;

    private LRUCache<Integer> cache;
    private int testKey;

    @Setup(Level.Trial)
    public void setup() {
        cache = new LRUCache<>(cacheSize);
        for (int i = 0; i < cacheSize; i++) {
            cache.put(i);
        }
        testKey = cacheSize / 2;
    }

    @Benchmark
    public void testGet(Blackhole blackhole) {
        blackhole.consume(cache.get(testKey));
    }

    @Benchmark
    public void testPut() {
        int key = ThreadLocalRandom.current().nextInt(cacheSize * 2);
        cache.put(key);
    }

    @Benchmark
    public void testPutWithEviction() {
        int key = cacheSize + ThreadLocalRandom.current().nextInt(1000);
        cache.put(key);
    }

}
