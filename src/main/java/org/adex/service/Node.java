package org.adex.service;

import java.util.Objects;

public class Node<T> {
    private T value;

    private Node<T> previous;
    private Node<T> next;

    private long expireTime;

    protected Node() {
    }

    public Node(T value) {
        Objects.requireNonNull(value, "Node's value cannot be null");
        this.value = value;
    }

    public T value() {
        return value;
    }

    public Node<T> value(T value) {
        this.value = value;
        return this;
    }

    public Node<T> previous() {
        return previous;
    }

    public Node<T> previous(Node<T> previous) {
        if (this.previous != null) {
            this.previous.next = null;
        }
        this.previous = previous;
        if (previous != null) {
            previous.next = this;
        }
        return this;
    }

    public Node<T> next() {
        return next;
    }

    public Node<T> next(Node<T> next) {
        if (this.next != null) {
            this.next.previous = null;
        }
        this.next = next;
        if (next != null) {
            next.previous = this;
        }
        return this;
    }
}
