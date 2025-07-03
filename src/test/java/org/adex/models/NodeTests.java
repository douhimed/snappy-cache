package org.adex.models;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NodeTests {

    @Test
    void givenValidValue_WhenInit_ThenSetValue() {
        // Given
        Node<Integer> node = new Node<>(100);

        // When
        int actual = node.value();

        // Then
        Assertions.assertEquals(100, actual);
    }

    @Test
    void givenInvalidValue_WhenInit_ThenThrowsException() {
        // Given

        // When
        Exception actual = Assertions.assertThrows(NullPointerException.class, () -> new Node<Integer>(null));

        // Then
        Assertions.assertEquals("Node's value cannot be null", actual.getMessage());
    }
    
}
