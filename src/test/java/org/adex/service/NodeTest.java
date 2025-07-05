package org.adex.service;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {
    private static final int TEST_VALUE = 100;
    private static final String NULL_VALUE_MESSAGE = "Node's value cannot be null";

    @Test
    void givenValidValue_whenInit_thenSetValue() {
        Node<Integer> node = new Node<>(TEST_VALUE);
        assertEquals(TEST_VALUE, node.value(),
                "Node should store the value it was initialized with");
    }

    @Test
    void givenNullValue_whenInit_thenThrowNullPointerException() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new Node<Integer>(null)
        );
        assertEquals(NULL_VALUE_MESSAGE, exception.getMessage());
    }

    @Test
    void givenNewNode_whenCreated_thenLinksAreNull() {
        Node<String> node = new Node<>("test");
        assertAll(
                () -> assertNull(node.next(), "Next should be null for new node"),
                () -> assertNull(node.previous(), "Previous should be null for new node")
        );
    }

    @Test
    void givenNode_whenSettingNext_thenLinksAreProperlyUpdated() {
        Node<Integer> first = new Node<>(1);
        Node<Integer> second = new Node<>(2);

        first.next(second);

        assertAll(
                () -> assertSame(second, first.next()),
                () -> assertSame(first, second.previous())
        );
    }

    @Test
    void givenNode_whenSettingPrevious_thenLinksAreProperlyUpdated() {
        Node<Integer> first = new Node<>(1);
        Node<Integer> second = new Node<>(2);

        second.previous(first);

        assertAll(
                () -> assertSame(first, second.previous()),
                () -> assertSame(second, first.next())
        );
    }

    @Test
    void givenExistingLinks_whenReplacingNext_thenOldLinksAreCleaned() {
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
    void givenExistingLinks_whenSettingNullNext_thenLinksAreRemoved() {
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
    void givenCircularLinks_whenBreaking_thenLinksAreProperlyUpdated() {
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
}