package org.adex.models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class LRUCacheConcurrencyTest {

    private static final int THREAD_COUNT = 10;
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(THREAD_COUNT);

    private CountDownLatch latch;
    private LRUCache<Integer> cache;
    private List<Future<Integer>> futures;

    @BeforeEach
    void setUp() {
        cache = new LRUCache<>();
        IntStream.range(0, 10).forEach(value -> cache.put(value));

        latch = new CountDownLatch(THREAD_COUNT);
        futures = new ArrayList<>(THREAD_COUNT);
    }

    @Test
    void givenMultipleThreads_whenCallingGetSimultaneously_thenAllThreadsCompleteSuccessfully() throws InterruptedException, ExecutionException, TimeoutException {
        // given

        // when
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int value = i;
            futures.add(EXECUTOR_SERVICE.submit(() -> {
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
        Assertions.assertThrows(RuntimeException.class, () -> instance.get(1));
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
            futures.add(EXECUTOR_SERVICE.submit(() -> {
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
}
