package org.adex.models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LRUCacheTest {

    private LRUCache<Dummy> cache;

    @BeforeEach
    void setUp() {
        this.cache = new LRUCache<>(5);
    }

    @Test
    void givenEmptyCache_whenCheckingIsEmpty_thenShouldReturnTrue() {
        // When
        boolean isEmpty = cache.isEmpty();

        // Then
        assertTrue(isEmpty, "New cache should report empty");
    }

    @Test
    void givenCacheWithOneEntry_whenCheckingIsEmpty_thenShouldReturnFalse() {
        // Given
        cache.put(new Dummy(100));

        // When
        boolean isEmpty = cache.isEmpty();

        // Then
        assertFalse(isEmpty, "Cache with entries should report non-empty");
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
                () -> assertSame(dummy300, cache.get(dummy300)));
    }

    @Test
    void givenNullValue_WhenPut_ThenThrowsException() {
        // Given

        // When
        Exception actual = Assertions.assertThrows(NullPointerException.class, () -> cache.put(null));

        // Then
        Assertions.assertEquals("Value cannot be null", actual.getMessage());
    }

    @Test
    void givenCache_WhenGetEntry_ThenShouldBeMovedToHead() {
        // Given - initialize test data
        Dummy dummy100 = new Dummy(100);
        Dummy dummy200 = new Dummy(200);
        Dummy dummy300 = new Dummy(300);

        cache.put(dummy100);
        cache.put(dummy200);
        cache.put(dummy300);

        cache.get(dummy300);
        cache.get(dummy200);
        cache.get(dummy100);

        // When
        Dummy head = cache.peek();

        // Then
        Assertions.assertAll("Verify head is most recently accessed item",
                () -> assertNotNull(head, "Head should not be null"),
                () -> assertEquals(dummy100.value, head.value, "Head should be dummy100"),
                () -> assertSame(dummy100, head, "Head should be the exact dummy100 instance")
        );
    }

    @Test
    void givenCacheWithCapacity5_WhenSizeEqualsCapacity_ThenShouldEviction() {
        // Given
        for (int i = 0; i < 8; i++) {
            cache.put(new Dummy((i + 1) * 10));
        }

        // When
        int actual = cache.size();

        assertEquals(5, actual);
    }

    @Test
    void givenCollectionOfEntries_WhenPutAll_ThenShouldBeStoredAndLinked() {
        // Given - initialize test data
        Dummy dummy100 = new Dummy(100);
        Dummy dummy200 = new Dummy(200);
        Dummy dummy300 = new Dummy(300);

        cache.put(List.of(dummy100, dummy200, dummy300), false);

        cache.get(dummy300);
        cache.get(dummy200);
        cache.get(dummy100);

        // When
        Dummy head = cache.peek();

        // Then
        Assertions.assertAll("Verify head is most recently accessed item",
                () -> assertNotNull(head, "Head should not be null"),
                () -> assertEquals(dummy100.value, head.value, "Head should be dummy100"),
                () -> assertSame(dummy100, head, "Head should be the exact dummy100 instance")
        );
    }

    @Test
    void givenNullCollection_WhenPut_ThenThrowsException() {
        // Given

        // When
        Exception actual = Assertions.assertThrows(NullPointerException.class, () -> cache.put(null, false));

        // Then
        Assertions.assertEquals("Collection cannot be null", actual.getMessage());
    }

    @Test
    void givenValidCache_WhenGetAll_ThenReturnsListOfValues() {
        // Given - initialize test data
        cache.put(List.of(new Dummy(100), new Dummy(200), new Dummy(300)), false);

        // When
        Collection<Dummy> actual = cache.getAll();

        // Then
        assertNotNull(actual);
        assertEquals(3, actual.size());
    }

    @Test
    void givenValidCache_WhenPurge_ThenResetToInitState() {
        // Given - initialize test data
        cache.put(List.of(new Dummy(100), new Dummy(200), new Dummy(300)), false);

        // When
        cache.purge();

        // Then
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertNull(cache.peek());
    }

    private static record Dummy(int value) {
    }
}
