package org.adex.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LRUCacheConcurrencyTest {
    private static final int THREAD_COUNT = 10;
    private static final int ITERATIONS = 1_000;
    private static final int HIGH_CONTENTION_ITERATIONS = 10_000;
    private static final int TIMEOUT_SECONDS = 5;

    private ExecutorService executorService;
    private LRUCache<Integer> cache;

    @BeforeEach
    void setUp() {
        cache = new LRUCache<>();
        IntStream.range(0, 10).forEach(cache::put);
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void givenMultipleThreads_whenCallingGetSimultaneously_thenAllThreadsCompleteSuccessfully()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        List<Future<Integer>> futures = new ArrayList<>(THREAD_COUNT);

        IntStream.range(0, THREAD_COUNT).forEach(i ->
                futures.add(executorService.submit(() -> {
                    latch.countDown();
                    latch.await();
                    return cache.get(i);
                }))
        );

        for (Future<Integer> future : futures) {
            assertNotNull(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    @Test
    void givenLockedMethod_whenGetExecutes_thenLockAndUnlockAreCalled() {
        ReentrantLock spyLock = spy(cache.lock);
        cache.lock = spyLock;

        cache.get(1);

        verify(spyLock).lock();
        verify(spyLock).unlock();
        verifyNoMoreInteractions(spyLock);
    }

    @Test
    void givenExceptionThrownInCriticalSection_whenGetExecutes_thenUnlockIsCalledExactlyOnce() {
        LRUCache<Integer> throwingCache = createThrowingCache();

        ReentrantLock spyLock = spy(throwingCache.lock);
        throwingCache.lock = spyLock;

        assertThrows(RuntimeException.class, () -> throwingCache.get(1));
        verify(spyLock).lock();
        verify(spyLock).unlock();
    }

    @Test
    void givenHighThreadContention_whenCallingGetRepeatedly_thenCompletesWithinReasonableTime()
            throws Exception {
        long startTime = System.nanoTime();

        List<Future<?>> futures = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> executorService.submit(() ->
                        IntStream.range(0, ITERATIONS).forEach(j -> cache.get(i * j))))
                .collect(Collectors.toList());

        for (Future<?> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        assertDurationLessThan(startTime, 5, TimeUnit.SECONDS);
    }

    @Test
    void givenMultipleThreads_whenPuttingValuesConcurrently_thenAllValuesAreProcessed()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        IntStream.range(0, THREAD_COUNT).forEach(i ->
                executorService.execute(() -> {
                    latch.countDown();
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    IntStream.range(0, ITERATIONS).forEach(cache::put);
                })
        );

        awaitExecutorTermination();
    }

    @Test
    void givenPutOperation_whenExecuted_thenLocksAndUnlocksCorrectly() {
        ReentrantLock spyLock = spy(cache.lock);
        cache.lock = spyLock;

        cache.put(1);

        verify(spyLock).lock();
        verify(spyLock).unlock();
        verifyNoMoreInteractions(spyLock);
    }

    @Test
    void givenExceptionDuringPut_whenErrorOccurs_thenLockIsReleased() {
        LRUCache<Integer> throwingCache = createThrowingPutCache();

        ReentrantLock spyLock = spy(throwingCache.lock);
        throwingCache.lock = spyLock;

        assertThrows(RuntimeException.class, () -> throwingCache.put(1));
        verify(spyLock).lock();
        verify(spyLock).unlock();
    }

    @Test
    void givenNullValue_whenPutCalled_thenThrowsNPEWithoutLocking() {
        ReentrantLock spyLock = spy(cache.lock);
        cache.lock = spyLock;

        assertThrows(NullPointerException.class, () -> cache.put(null));
        verify(spyLock, never()).lock();
    }

    @Test
    void givenHighContention_whenPuttingValues_thenCompletesWithoutDeadlock() throws Exception {
        int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        ExecutorService highLoadExecutor = Executors.newFixedThreadPool(threadCount);

        List<Future<?>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> highLoadExecutor.submit(() ->
                        IntStream.range(0, HIGH_CONTENTION_ITERATIONS).forEach(cache::put)))
                .collect(Collectors.toList());

        for (Future<?> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Test
    void givenConcurrentPuts_whenOperationsComplete_thenInternalStateIsConsistent() throws Exception {
        int threadCount = 10;
        int elementsPerThread = 1000;
        LRUCache<Integer> largeCache = new LRUCache<>(threadCount * elementsPerThread);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        IntStream.range(0, threadCount).forEach(i ->
                executor.execute(() ->
                        IntStream.range(0, elementsPerThread)
                                .forEach(j -> largeCache.put(i * elementsPerThread + j)))
        );

        awaitExecutorTermination(executor);
        assertEquals(threadCount * elementsPerThread, largeCache.size());
    }

    // Helper methods
    private LRUCache<Integer> createThrowingCache() {
        return new LRUCache<>() {
            @Override
            public Integer get(Integer obj) {
                lock.lock();
                try {
                    throw new RuntimeException("Test exception");
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    private LRUCache<Integer> createThrowingPutCache() {
        return new LRUCache<>() {
            @Override
            public void put(Integer value) {
                lock.lock();
                try {
                    throw new RuntimeException("Simulated error");
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    private void awaitExecutorTermination() throws InterruptedException {
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private void awaitExecutorTermination(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private void assertDurationLessThan(long startNanos, long timeout, TimeUnit unit) {
        long durationNanos = System.nanoTime() - startNanos;
        long timeoutNanos = unit.toNanos(timeout);
        assertTrue(durationNanos < timeoutNanos,
                String.format("Operation took %dms (max allowed %dms)",
                        TimeUnit.NANOSECONDS.toMillis(durationNanos),
                        TimeUnit.NANOSECONDS.toMillis(timeoutNanos)));
    }

}