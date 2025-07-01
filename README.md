# SnappyCache

An attempt at implementing a **High-Performance, Thread-Safe LRU (Least Recently Used) Cache** in Java.

Based on the concepts from the article:
**An O(1) algorithm for implementing the LFU cache eviction scheme**

---

## 🚀 Overview

This project provides a custom implementation of an LRU cache system focused on performance, extensibility, and concurrency.

### ✨ Key Features (Planned & In Progress)

- [ ] **O(1) Get and Put Operations**  
  Efficient operations using a combination of **Doubly-Linked List** and **HashMap**.

- [ ] **Thread-Safe Concurrency Control**  
  Built-in synchronization for safe multi-threaded access.

- [ ] **TTL (Time-To-Live) Support**  
  Optional time-based expiration of cache entries.

- [ ] **Extensible Design**  
  Built with flexibility in mind to support:
    - LFU (Least Frequently Used) cache strategies
    - Distributed caching in future implementations

---

## 🛠️ Technologies Used

- Java 21+

---