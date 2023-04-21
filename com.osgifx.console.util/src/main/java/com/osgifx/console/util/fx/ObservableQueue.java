/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.util.fx;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import javafx.collections.ObservableListBase;

public final class ObservableQueue<E> extends ObservableListBase<E> implements Queue<E> {

    private final Queue<E>      queue;
    private final ReentrantLock lock;

    /**
     * Creates an ObservableQueue backed by the supplied Queue. Note that
     * manipulations of the underlying queue will not result in notification to
     * listeners.
     *
     * @param queue
     */
    public ObservableQueue(final Queue<E> queue) {
        this.queue = queue;
        lock       = new ReentrantLock();
    }

    /**
     * Creates an ObservableQueue backed by a LinkedList.
     */
    public ObservableQueue() {
        this(new LinkedList<>());
    }

    @Override
    public boolean offer(final E e) {
        lock.lock();
        try {
            beginChange();
            final var result = queue.offer(e);
            if (result) {
                nextAdd(queue.size() - 1, queue.size());
            }
            endChange();
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean add(final E e) {
        lock.lock();
        try {
            beginChange();
            try {
                queue.add(e);
                nextAdd(queue.size() - 1, queue.size());
                return true;
            } finally {
                endChange();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E remove() {
        lock.lock();
        try {
            beginChange();
            try {
                final var e = queue.remove();
                nextRemove(0, e);
                return e;
            } finally {
                endChange();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll() {
        lock.lock();
        try {
            beginChange();
            final var e = queue.poll();
            if (e != null) {
                nextRemove(0, e);
            }
            endChange();
            return e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E element() {
        lock.lock();
        try {
            return queue.element();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E peek() {
        lock.lock();
        try {
            return queue.peek();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E get(final int index) {
        lock.lock();
        try {
            final var iterator = queue.iterator();
            for (var i = 0; i < index; i++) {
                iterator.next();
            }
            return iterator.next();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            queue.clear();
        } finally {
            lock.unlock();
        }
    }

}
