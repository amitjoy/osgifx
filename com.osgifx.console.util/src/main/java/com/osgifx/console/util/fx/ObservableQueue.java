/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import javafx.collections.ObservableListBase;

public final class ObservableQueue<E> extends ObservableListBase<E> implements Queue<E> {

	private final Queue<E> queue;

	/**
	 * Creates an ObservableQueue backed by the supplied Queue. Note that
	 * manipulations of the underlying queue will not result in notification to
	 * listeners.
	 *
	 * @param queue
	 */
	public ObservableQueue(final Queue<E> queue) {
		this.queue = queue;
	}

	/**
	 * Creates an ObservableQueue backed by a LinkedList.
	 */
	public ObservableQueue() {
		this(new LinkedList<>());
	}

	@Override
	public synchronized boolean offer(final E e) {
		beginChange();
		final var result = queue.offer(e);
		if (result) {
			nextAdd(queue.size() - 1, queue.size());
		}
		endChange();
		return result;
	}

	@Override
	public synchronized boolean add(final E e) {
		beginChange();
		try {
			queue.add(e);
			nextAdd(queue.size() - 1, queue.size());
			return true;
		} finally {
			endChange();
		}
	}

	@Override
	public synchronized E remove() {
		beginChange();
		try {
			final var e = queue.remove();
			nextRemove(0, e);
			return e;
		} finally {
			endChange();
		}
	}

	@Override
	public synchronized E poll() {
		beginChange();
		final var e = queue.poll();
		if (e != null) {
			nextRemove(0, e);
		}
		endChange();
		return e;
	}

	@Override
	public synchronized E element() {
		return queue.element();
	}

	@Override
	public synchronized E peek() {
		return queue.peek();
	}

	@Override
	public synchronized E get(final int index) {
		final var iterator = queue.iterator();
		for (var i = 0; i < index; i++) {
			iterator.next();
		}
		return iterator.next();
	}

	@Override
	public synchronized int size() {
		return queue.size();
	}

	@Override
	public synchronized void clear() {
		queue.clear();
	}

}
