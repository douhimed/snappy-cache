package org.adex.service;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {
    private static final int TEST_VALUE = 100;
    private static final String NULL_VALUE_MESSAGE = "Node's value cannot be null";

    private Node<Integer> createNode(int value) {
        return new Node<>(value);
    }

    private Node<String> createNode(String value) {
        return new Node<>(value);
    }

    private Node<Integer> createNode(int value, long ttl) {
        return new Node<>(value, ttl);
    }

    private Node<String> createNode(String value, long ttl) {
        return new Node<>(value, ttl);
    }

    @Test
    void shouldStoreValueOnInitialization() {
        Node<Integer> node = createNode(TEST_VALUE);
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
        Node<String> node = createNode("test");
        assertAll(
                () -> assertNull(node.next()),
                () -> assertNull(node.previous())
        );
    }

    @Test
    void shouldSetNextAndPreviousLinksProperly() {
        Node<Integer> first = createNode(1);
        Node<Integer> second = createNode(2);

        first.next(second);

        assertAll(
                () -> assertSame(second, first.next()),
                () -> assertSame(first, second.previous())
        );
    }

    @Test
    void shouldSetPreviousAndUpdateNextLink() {
        Node<Integer> first = createNode(1);
        Node<Integer> second = createNode(2);

        second.previous(first);

        assertAll(
                () -> assertSame(first, second.previous()),
                () -> assertSame(second, first.next())
        );
    }

    @Test
    void shouldReplaceNextAndCleanOldLinks() {
        Node<Integer> first = createNode(1);
        Node<Integer> second = createNode(2);
        Node<Integer> third = createNode(3);

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
        Node<Integer> first = createNode(1);
        Node<Integer> second = createNode(2);

        first.next(second);
        first.next(null);

        assertAll(
                () -> assertNull(first.next()),
                () -> assertNull(second.previous())
        );
    }

    @Test
    void shouldBreakCircularLinks() {
        Node<Integer> first = createNode(1);
        Node<Integer> second = createNode(2);

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
        Node<Integer> node = createNode(TEST_VALUE, 1_000_000_000);
        assertFalse(node.isExpired());
    }

    @Test
    void shouldExpireImmediatelyWhenTTLExceeded() {
        Node<String> node = createNode("test", -1);
        assertTrue(node.isExpired());
    }

    @Test
    void shouldExtendLifetimeOnAccessUpdate() throws InterruptedException {
        Node<String> node = createNode("test", 100_000_000L);

        Thread.sleep(50);
        node.updateAccessTime();
        Thread.sleep(60);

        assertFalse(node.isExpired());
    }

    @Test
    void zeroTTLShouldNeverExpire() {
        Node<String> node = createNode("test", 0);
        assertFalse(node.isExpired());
    }

    @Test
    void shouldExpireAfterTTLOfInactivityDespiteUpdates() throws InterruptedException {
        Node<String> node = createNode("test", 50);

        for (int i = 0; i < 5; i++) {
            node.updateAccessTime();
            assertFalse(node.isExpired());
            Thread.sleep(5);
        }

        Thread.sleep(60);
        assertTrue(node.isExpired());
    }
}