package org.adex.service;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {
    private static final int TEST_VALUE = 100;
    private static final String NULL_VALUE_MESSAGE = "Node's value cannot be null";

    @Test
    void shouldStoreValueOnInitialization() {
        Node<Integer> node = new Node<>(TEST_VALUE);
        assertEquals(TEST_VALUE, node.value());
    }

    @Test
    void shouldThrowWhenInitializedWithNullValue() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new Node<Integer>(null)
        );
        assertEquals(NULL_VALUE_MESSAGE, exception.getMessage());
    }

    @Test
    void shouldHaveNullLinksWhenNew() {
        Node<String> node = new Node<>("test");
        assertAll(
                () -> assertNull(node.next()),
                () -> assertNull(node.previous())
        );
    }

    @Test
    void shouldSetNextAndPreviousLinksProperly() {
        Node<Integer> first = new Node<>(1);
        Node<Integer> second = new Node<>(2);

        first.next(second);

        assertAll(
                () -> assertSame(second, first.next()),
                () -> assertSame(first, second.previous())
        );
    }

    @Test
    void shouldSetPreviousAndUpdateNextLink() {
        Node<Integer> first = new Node<>(1);
        Node<Integer> second = new Node<>(2);

        second.previous(first);

        assertAll(
                () -> assertSame(first, second.previous()),
                () -> assertSame(second, first.next())
        );
    }

    @Test
    void shouldReplaceNextAndCleanOldLinks() {
        Node<Integer> first = new Node<>(1);
        Node<Integer> second = new Node<>(2);
        Node<Integer> third = new Node<>(3);

        first.next(second);
        first.next(third);

        assertAll(
                () -> assertSame(third, first.next()),
                () -> assertSame(first, third.previous()),
                () -> assertNull(second.previous())
        );
    }

    @Test
    void shouldRemoveLinksWhenSettingNextToNull() {
        Node<Integer> first = new Node<>(1);
        Node<Integer> second = new Node<>(2);

        first.next(second);
        first.next(null);

        assertAll(
                () -> assertNull(first.next()),
                () -> assertNull(second.previous())
        );
    }

    @Test
    void shouldBreakCircularLinks() {
        Node<Integer> first = new Node<>(1);
        Node<Integer> second = new Node<>(2);

        first.next(second);
        second.previous(first);
        first.next(null);

        assertAll(
                () -> assertNull(first.next()),
                () -> assertNull(second.previous())
        );
    }

    @Test
    void shouldNotExpireImmediatelyWhenNotExpired() {
        Node<Integer> node = new Node<>(TEST_VALUE);
        assertFalse(node.isExpired(1_000_000_000));
    }

    @Test
    void shouldExpireImmediatelyWhenTTLExceeded() {
        Node<String> node = new Node<>("test");
        assertTrue(node.isExpired(-1));
    }

    @Test
    void shouldExtendLifetimeOnAccessUpdate() throws InterruptedException {
        Node<String> node = new Node<>("test");

        Thread.sleep(50);
        node.updateAccessTime();
        Thread.sleep(60);

        assertFalse(node.isExpired(100_000_000L));
    }

    @Test
    void zeroTTLShouldNeverExpire() {
        Node<String> node = new Node<>("test");
        assertFalse(node.isExpired(0));
    }

    @Test
    void shouldExpireAfterTTLOfInactivityDespiteUpdates() throws InterruptedException {
        Node<String> node = new Node<>("test");

        for (int i = 0; i < 5; i++) {
            node.updateAccessTime();
            assertFalse(node.isExpired(50));
            Thread.sleep(5);
        }

        Thread.sleep(60);
        assertTrue(node.isExpired(50));
    }

}