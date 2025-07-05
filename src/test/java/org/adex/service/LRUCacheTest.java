package org.adex.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class LRUCacheTest {
    private static final int CACHE_CAPACITY = 5;
    private CacheManager<Dummy> cache;

    @BeforeEach
    void setUp() {
        this.cache = new LRUCache<>(CACHE_CAPACITY);
    }

    @Test
    void givenEmptyCache_whenCheckingIsEmpty_thenShouldReturnTrue() {
        assertTrue(cache.isEmpty(), "New cache should report empty");
    }

    @Test
    void givenCacheWithOneEntry_whenCheckingIsEmpty_thenShouldReturnFalse() {
        cache.put(new Dummy(100));
        assertFalse(cache.isEmpty(), "Cache with entries should report non-empty");
    }

    @Test
    void givenEmptyCache_whenPuttingThreeEntries_thenAllShouldBeRetrievable() {
        // Given
        Dummy dummy100 = new Dummy(100);
        Dummy dummy200 = new Dummy(200);
        Dummy dummy300 = new Dummy(300);

        // When
        cache.put(dummy100);
        cache.put(dummy200);
        cache.put(dummy300);

        // Then
        assertAll("All entries should be properly stored",
                () -> assertSame(dummy100, cache.get(dummy100)),
                () -> assertSame(dummy200, cache.get(dummy200)),
                () -> assertSame(dummy300, cache.get(dummy300))
        );
    }

    @Test
    void givenNullValue_whenPut_thenThrowsException() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> cache.put(null));
        assertEquals("Value cannot be null", exception.getMessage());
    }

    @Test
    void givenCache_whenGetEntry_thenShouldBeMovedToHead() {
        // Given
        Dummy dummy100 = new Dummy(100);
        Dummy dummy200 = new Dummy(200);
        Dummy dummy300 = new Dummy(300);

        cache.put(List.of(dummy100, dummy200, dummy300), false);
        accessAll(cache, dummy300, dummy200, dummy100);

        // When
        Dummy head = cache.peek();

        // Then
        assertAll("Verify head is most recently accessed item",
                () -> assertNotNull(head, "Head should not be null"),
                () -> assertEquals(dummy100.value, head.value),
                () -> assertSame(dummy100, head)
        );
    }

    @Test
    void givenCacheAtCapacity_whenAddingNewEntries_thenShouldEvictOldest() {
        // When
        IntStream.range(0, 8)
                .map(i -> (i + 1) * 10)
                .mapToObj(Dummy::new)
                .forEach(cache::put);

        // Then
        assertEquals(CACHE_CAPACITY, cache.size());
    }

    @Test
    void givenCollectionOfEntries_whenPutAll_thenShouldBeStoredAndLinked() {
        // Given
        Dummy dummy100 = new Dummy(100);
        Dummy dummy200 = new Dummy(200);
        Dummy dummy300 = new Dummy(300);

        // When
        cache.put(List.of(dummy100, dummy200, dummy300), false);
        accessAll(cache, dummy300, dummy200, dummy100);

        // Then
        assertMostRecentlyAccessed(cache, dummy100);
    }

    @Test
    void givenNullCollection_whenPutAll_thenThrowsException() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> cache.put(null, false));
        assertEquals("Collection cannot be null", exception.getMessage());
    }

    @Test
    void givenValidCache_whenGet_thenReturnsListOfValues() {
        // Given
        cache.put(List.of(new Dummy(100), new Dummy(200), new Dummy(300)), false);

        // When
        Collection<Dummy> actual = cache.get();

        // Then
        assertNotNull(actual);
        assertEquals(3, actual.size());
    }

    @Test
    void givenValidCache_whenPurge_thenResetToInitState() {
        // Given
        cache.put(List.of(new Dummy(100), new Dummy(200), new Dummy(300)), false);

        // When
        cache.purge();

        // Then
        assertAll("Cache should be completely empty after purge",
                () -> assertTrue(cache.isEmpty()),
                () -> assertEquals(0, cache.size()),
                () -> assertNull(cache.peek())
        );
    }

    // Helper methods
    private void assertMostRecentlyAccessed(CacheManager<Dummy> cache, Dummy expected) {
        Dummy head = cache.peek();
        assertAll("Verify head is most recently accessed item",
                () -> assertNotNull(head, "Head should not be null"),
                () -> assertEquals(expected.value, head.value),
                () -> assertSame(expected, head)
        );
    }

    private void accessAll(CacheManager<Dummy> cache, Dummy... items) {
        Arrays.stream(items).forEach(cache::get);
    }

    private record Dummy(int value) {}

    private static class TestLRUCache<T> extends LRUCache<T> {
        @SafeVarargs
        final void putAll(T... items) {
            Arrays.stream(items).forEach(this::put);
        }
    }
}