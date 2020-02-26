/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.brpc.utils;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

public class BoundedQueue<E> implements Queue<E> {
    private int capacity;
    private int size;
    private ArrayDeque<E> queue;

    public BoundedQueue(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        queue = new ArrayDeque<E>(capacity);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    public final boolean isFull() {
        return size == capacity;
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return queue.iterator();
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return queue.toArray(a);
    }

    @Override
    public boolean add(E e) {
        if (isFull()) {
            queue.pollFirst();
            size--;
        }
        queue.addLast(e);
        size++;
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        int inputSize = c.size();
        if (inputSize > capacity) {
            throw new IllegalArgumentException(
                    "Size of added which is " + inputSize  + " is larger " + "than capacity=" + capacity);
        }
        int toPop = Math.max(0, inputSize - remainingCapacity());
        for (int i = 0; i < toPop; ++i) {
            queue.pollFirst();
        }
        size -= toPop;
        for (E e : c) {
            queue.addLast(e);
        }
        size += inputSize;
        return true;
    }

    public boolean addAll(E[] c, int offset, int len) {
        if (len > capacity) {
            throw new IllegalArgumentException(
                    "Size of added which is " + len  + " is larger " + "than capacity=" + capacity);
        }
        int toPop = Math.max(0, len - remainingCapacity());
        for (int i = 0; i < toPop; ++i) {
            queue.pollFirst();
        }
        size -= toPop;
        int last = offset + len;
        for (int i = offset; i < last; i++) {
            queue.addLast(c[i]);
        }
        size += len;
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = queue.removeAll(c);
        if (modified) {
            size = queue.size();
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean modified = queue.retainAll(c);
        if (modified) {
            size = queue.size();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        queue.clear();
        size = 0;
    }

    @Override
    public boolean offer(E e) {
        return add(e);
    }

    @Override
    public boolean remove(Object o) {
        if (queue.remove(o)) {
            size--;
            return true;
        }
        return false;
    }

    @Override
    public E remove() {
        E e = queue.removeFirst();
        size--;
        return e;
    }

    @Override
    public E poll() {
        E e = queue.pollFirst();
        if (e != null) {
            size--;
        }
        return e;
    }

    @Override
    public E element() {
        return queue.element();
    }

    @Override
    public E peek() {
        return queue.peekFirst();
    }

    public int remainingCapacity() {
        return capacity - size;
    }

    public E pop() {
        E e = queue.pop();
        size--;
        return e;
    }
}
