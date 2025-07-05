package org.adex.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LRUCacheConcurrencyTest {

    private static final int THREAD_COUNT = 10;
    private ExecutorService executorService;
    private static final int ITERATIONS = 1_000;

    private CountDownLatch latch;
    private LRUCache<Integer> cache;
    private List<Future<Integer>> futures;

    @BeforeEach
    void setUp() {
        cache = new LRUCache<>();
        IntStream.range(0, 10).forEach(value -> cache.put(value));

        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        latch = new CountDownLatch(THREAD_COUNT);
        futures = new ArrayList<>(THREAD_COUNT);
    }

    @Test
    void givenMultipleThreads_whenCallingGetSimultaneously_thenAllThreadsCompleteSuccessfully() throws InterruptedException, ExecutionException, TimeoutException {
        // given

        // when
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int value = i;
            futures.add(executorService.submit(() -> {
                latch.countDown();
                latch.await();
                return cache.get(value);
            }));
        }

        // then
        for (Future<Integer> future : futures) {
            assertNotNull(future.get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void givenLockedMethod_whenGetExecutes_thenLockAndUnlockAreCalled() {
        // given
        ReentrantLock spyLock = spy(cache.lock);
        cache.lock = spyLock;
        int value = 1;

        // when
        cache.get(value);

        verify(spyLock).lock();
        verify(spyLock, times(1)).unlock();
    }

    @Test
    public void givenExceptionThrownInCriticalSection_whenGetExecutes_thenUnlockIsCalledExactlyOnce() {
        // Given
        LRUCache<Integer> instance = new LRUCache<>() {
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

        ReentrantLock spyLock = spy(instance.lock);
        instance.lock = spyLock;

        // When & Then
        assertThrows(RuntimeException.class, () -> instance.get(1));
        verify(spyLock, times(1)).lock();
        verify(spyLock, times(1)).unlock();
        verifyNoMoreInteractions(spyLock);
    }

    @Test
    public void givenHighThreadContention_whenCallingGetRepeatedly_thenCompletesWithinReasonableTime() throws InterruptedException, ExecutionException {

        // Given
        final int iterations = 1000;

        // When
        long startTime = System.currentTimeMillis();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int ii = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    final int value = ii * j;
                    cache.get(value);
                }
            }));
        }

        // Then
        for (Future<?> future : futures) {
            future.get();
        }
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 5000);
    }

    @Test
    void givenMultipleThreads_whenPuttingValuesConcurrently_thenAllValuesAreProcessed() throws InterruptedException, ExecutionException, TimeoutException {
        // given

        // when
        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.execute(() -> {
                latch.countDown();
                try {
                    latch.await();
                    for (int j = 0; j < ITERATIONS; j++) {
                        final int value = j;
                        cache.put(value);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // then
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void givenPutOperation_whenExecuted_thenLocksAndUnlocksCorrectly() {
        // given
        ReentrantLock spyLock = spy(cache.lock);
        cache.lock = spyLock;

        // when
        cache.put(1);

        // then
        verify(spyLock).lock();
        verify(spyLock, times(1)).unlock();
        verifyNoMoreInteractions(spyLock);
    }

    @Test
    public void givenExceptionDuringPut_whenErrorOccurs_thenLockIsReleased() {
        // Given
        LRUCache<Integer> instance = new LRUCache<>() {

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

        ReentrantLock spyLock = spy(instance.lock);
        instance.lock = spyLock;

        // when & then
        assertThrows(RuntimeException.class, () -> instance.put(1));
        verify(spyLock).lock();
        verify(spyLock).unlock();
    }

    @Test
    public void givenNullValue_whenPutCalled_thenThrowsNPEWithoutLocking() {
        // Setup
        ReentrantLock spyLock = spy(cache.lock);
        cache.lock = spyLock;


        // when & then
        assertThrows(NullPointerException.class, () -> cache.put(null));
        verify(spyLock, never()).lock();
        verify(spyLock, never()).unlock();
    }

    @Test
    void givenHighContention_whenPuttingValues_thenCompletesWithoutDeadlock() throws InterruptedException, ExecutionException, TimeoutException {
        // given
        final int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        executorService = Executors.newFixedThreadPool(threadCount);
        final List<Future<?>> futures = new ArrayList<>(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < 10_000; j++) {
                    final int value = j;
                    cache.put(value);
                }
            }));
        }

        // then
        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void givenConcurrentPuts_whenOperationsComplete_thenInternalStateIsConsistent()
            throws Exception {
        // given
        final LRUCache<Integer> instance = new LRUCache<>(10_000);
        final int threadCount = 10;
        final int elementsPerThread = 1000;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.execute(() -> {
                for (int j = 0; j < elementsPerThread; j++) {
                    instance.put(threadId * elementsPerThread + j);
                }
            });
        }

        // then
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(threadCount * elementsPerThread, instance.size());
    }
}
