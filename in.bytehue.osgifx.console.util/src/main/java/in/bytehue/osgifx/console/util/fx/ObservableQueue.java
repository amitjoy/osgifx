package in.bytehue.osgifx.console.util.fx;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import javafx.collections.ObservableListBase;

public class ObservableQueue<E> extends ObservableListBase<E> implements Queue<E> {

    private final Queue<E> queue;

    /**
     * Creates an ObservableQueue backed by the supplied Queue.
     * Note that manipulations of the underlying queue will not result
     * in notification to listeners.
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
        final boolean result = queue.offer(e);
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
            final E e = queue.remove();
            nextRemove(0, e);
            return e;
        } finally {
            endChange();
        }
    }

    @Override
    public synchronized E poll() {
        beginChange();
        final E e = queue.poll();
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
        final Iterator<E> iterator = queue.iterator();
        for (int i = 0; i < index; i++) {
            iterator.next();
        }
        return iterator.next();
    }

    @Override
    public synchronized int size() {
        return queue.size();
    }

}
