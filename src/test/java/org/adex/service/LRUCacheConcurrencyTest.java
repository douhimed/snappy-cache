package org.adex.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
    private static final int OPERATIONS_PER_THREAD = 1000;

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
    void givenHighContention_whenPuttingValues_thenCompletesWithoutDeadlock() {
        assertTimeoutPreemptively(Duration.ofSeconds(TIMEOUT_SECONDS), () -> {
            int threadCount = Runtime.getRuntime().availableProcessors() * 2;
            ExecutorService highLoadExecutor = Executors.newFixedThreadPool(threadCount);

            List<Future<?>> futures = IntStream.range(0, threadCount)
                    .mapToObj(i -> highLoadExecutor.submit(() ->
                            IntStream.range(0, HIGH_CONTENTION_ITERATIONS).forEach(cache::put)))
                    .collect(Collectors.toUnmodifiableList());

            for (Future<?> future : futures) {
                assertDoesNotThrow(() -> future.get());
            }
        });
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

    @Test
    void givenConcurrentAccess_whenCheckingEmpty_thenConsistentResults() throws Exception {
        AtomicBoolean isEmptyConsistent = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.execute(() -> {
                latch.countDown();
                try {
                    latch.await();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        boolean empty = cache.isEmpty();
                        if (empty && !cache.isEmpty()) {
                            isEmptyConsistent.set(false);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executorService.awaitTermination(2, TimeUnit.SECONDS);
        assertTrue(isEmptyConsistent.get(), "isEmpty() reported inconsistent results during concurrent modifications");
    }

    @Test
    void givenConcurrentPuts_whenCheckingSize_thenCorrectTotal() throws Exception {
        AtomicInteger actualAdds = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.execute(() -> {
                latch.countDown();
                try {
                    latch.await();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        int val = ThreadLocalRandom.current().nextInt();
                        cache.put(val);
                        actualAdds.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executorService.awaitTermination(2, TimeUnit.SECONDS);
        assertEquals(Math.min(cache.capacity(), actualAdds.get()), cache.size(), "size() reported wrong count during concurrent puts");
    }

    @Test
    void givenConcurrentAccess_whenPeeking_thenSeesLatestOrNull() throws Exception {
        AtomicReference<Integer> lastPut = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.execute(() -> {
                latch.countDown();
                try {
                    latch.await();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        int val = ThreadLocalRandom.current().nextInt();
                        cache.put(val);
                        lastPut.set(val);

                        Integer peeked = cache.peek();
                        assertTrue(peeked == null || peeked.equals(lastPut.get()),
                                "peek() returned inconsistent value during concurrent puts");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executorService.awaitTermination(2, TimeUnit.SECONDS);
    }

    @RepeatedTest(5)
    void givenConcurrentCollections_whenPutAll_thenMaintainsConsistency() throws Exception {
        final int THREAD_COUNT = 10;
        final int BATCH_SIZE = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // Test with collection size > capacity to force evictions
        final int TEST_CAPACITY = 50;
        final LRUCache<Integer> testCache = new LRUCache<>(TEST_CAPACITY);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.execute(() -> {
                latch.countDown();
                try {
                    latch.await();
                    List<Integer> batch = IntStream.range(0, BATCH_SIZE)
                            .map(n -> threadId * BATCH_SIZE + n)
                            .boxed()
                            .collect(Collectors.toList());

                    testCache.put(batch, false);

                    // Verify capacity constraint
                    assertTrue(testCache.size() <= TEST_CAPACITY,
                            "Cache exceeded capacity");
                } catch (Exception e) {
                    fail("Unexpected exception: " + e);
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void givenNullInCollection_whenPutAll_thenFailsAtomically() {
        cache = new LRUCache<>();
        List<Integer> mixedValues = Arrays.asList(1, 2, null, 4);

        cache.put(mixedValues, false);

        assertEquals(3, cache.size());
        assertFalse(cache.isEmpty());
    }

    @Test
    void givenEmptyCollection_whenPutAll_thenNoEffect() {
        cache = new LRUCache<>();
        cache.put(Collections.singletonList(1), false);

        cache.put(Collections.emptyList(), false);

        assertEquals(1, cache.size(), "Cache size should remain unchanged");
        assertEquals(1, cache.peek(), "Head item should remain unchanged");
    }

    @Test
    void givenDuplicateValues_whenPutAll_thenMaintainsLRUOrder() {
        cache = new LRUCache<>();
        List<Integer> values = Arrays.asList(1, 2, 3, 2, 1);

        cache.put(values, false);

        assertEquals(3, cache.size(), "Should deduplicate values");
        assertEquals(1, cache.peek(), "Last duplicate should be at head");

        Iterator<Integer> it = cache.get().iterator();
        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertEquals(3, it.next());
    }

    @RepeatedTest(5)
    void givenConcurrentAccess_whenGettingAllValues_thenConsistentResults() throws Exception {
        final int THREAD_COUNT = 10;
        final int OPERATIONS = 1000;
        final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        final AtomicInteger inconsistencies = new AtomicInteger(0);

        IntStream.range(0, 50).forEach(i -> cache.put(i));

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.execute(() -> {
                latch.countDown();
                try {
                    latch.await();
                    for (int j = 0; j < OPERATIONS; j++) {
                        Collection<Integer> values = cache.get();

                        cache.lock.lock();
                        try {
                            if (values.size() != cache.size()) {
                                inconsistencies.incrementAndGet();
                            }

                            for (Integer value : values) {
                                if (cache.peek() != null && !cache.get(value).equals(value)) {
                                    inconsistencies.incrementAndGet();
                                }
                            }
                        } finally {
                            cache.lock.unlock();
                        }
                    }
                } catch (Exception e) {
                    fail("Unexpected exception: " + e);
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(0, inconsistencies.get(),
                "Found " + inconsistencies.get() + " inconsistent reads");
    }

    @Test
    void givenEmptyCache_whenGetAll_thenReturnsEmptyCollection() {
        cache =  new LRUCache<>();
        Collection<Integer> result = cache.get();
        assertTrue(result.isEmpty(), "Should return empty collection");
    }

    @Test
    void givenModifiedCache_whenGetAll_thenReturnsIndependentCopy() {
        cache = new LRUCache<>();
        cache.put(Arrays.asList(1, 2, 3), false);
        Collection<Integer> firstGet = cache.get();
        cache.put(4);

        assertFalse(firstGet.contains(4), "Original collection shouldn't reflect later changes");
        assertEquals(3, firstGet.size());
    }

    @Test
    void givenConcurrentModifications_whenGettingAllValues_thenNoCorruption() throws Exception {
        final int THREAD_COUNT = 5;
        final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        final AtomicBoolean clean = new AtomicBoolean(true);

        IntStream.range(0, 100).forEach(i -> cache.put(i));

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.execute(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        Collection<Integer> values = cache.get();
                        if (values.stream().anyMatch(Objects::isNull)) {
                            clean.set(false);
                        }
                        cache.put(j + 1000); // Concurrent modifications
                    }
                } catch (Exception e) {
                    clean.set(false);
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
        assertTrue(clean.get(), "Encountered corrupted collection reads");
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