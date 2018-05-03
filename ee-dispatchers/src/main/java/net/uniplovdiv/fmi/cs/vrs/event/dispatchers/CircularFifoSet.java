package net.uniplovdiv.fmi.cs.vrs.event.dispatchers;

import java.util.*;

/**
 * CircularFifoSet is a first in first out set with a fixed size that replaces its oldest element if full.
 * Null elements are not permitted. The structure is not thread-safe.
 */
public class CircularFifoSet<E> implements Collection<E> {
    private int capacity;

    private HashSet<E> contents;
    private LinkedList<E> orderedContents;

    /**
     * Constructor.
     * @param capacity The capacity of the set.
     * @throws IllegalArgumentException If the capacity is 0 or a negative number.
     */
    public CircularFifoSet(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + capacity);
        }
        this.capacity = capacity;
        this.contents = new HashSet<>(capacity, 2.0F);
        this.orderedContents = new LinkedList<>();
    }

    /**
     * Returns the capacity of the set.
     * @return An integer greater than 0.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Adds an element to the collection. If the element's already been added, the new operation will change its
     * internal position to be on first place. This ensures that the element will be kept for longer in the set.
     * @param e The element to be added. Cannot be null.
     * @return True if the addition is on element that's unknown to the collection, otherwise false.
     */
    @Override
    public boolean add(E e) {

        //synchronized (this) {
            if (!contents.isEmpty() && contents.contains(e)) {
                // the element already exists so move it to in first place
                orderedContents.removeFirstOccurrence(e);
                orderedContents.addFirst(e);
                return false;
            } else {
                // brand new element
                if (contents.size() == this.capacity) {
                    contents.remove(orderedContents.getLast());
                    orderedContents.removeLast();
                }

                contents.add(e);
                orderedContents.addFirst(e);
            }
        //}

        return true;
    }

    @Override
    public boolean remove(Object e) {

        //synchronized (this) {
            if (!contents.isEmpty() && contents.contains(e)) {
                orderedContents.removeLastOccurrence(e);
                contents.remove(e);
                return true;
            }
        //}

        return false;
    }

    @Override
    public boolean contains(Object e) {
        return (e != null && !contents.isEmpty() && contents.contains(e));
    }

    @Override
    public int size() {
        return contents.size();
    }

    @Override
    public boolean isEmpty() {
        return contents.isEmpty();
    }

    private class CircularFifoSetIterator<V> implements Iterator<V> {
        private CircularFifoSet<V> inst;
        private Iterator<V> orderedContentsIterator;
        private V lastNext;

        public CircularFifoSetIterator(CircularFifoSet<V> inst) {
            this.inst = inst;
            this.orderedContentsIterator = inst.orderedContents.iterator();
        }

        @Override
        public boolean hasNext() {
            return this.orderedContentsIterator.hasNext();
        }

        @Override
        public V next() {
            return this.lastNext = this.orderedContentsIterator.next();
        }

        @Override
        public void remove() {
            if (this.lastNext != null) {
                orderedContentsIterator.remove();
                inst.contents.remove(this.lastNext);
            }
        }
    }

    @Override
    public Iterator<E> iterator() {
        return this.new CircularFifoSetIterator<>(this);
    }

    @Override
    public Object[] toArray() {
        return orderedContents.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return orderedContents.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return contents.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean result = false;
        for (Iterator<? extends E> it = c.iterator(); it.hasNext(); ) {
            result |= this.add(it.next());
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Iterator<?> it = c.iterator(); it.hasNext(); ) {
            result |= this.remove(it.next());
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = contents.retainAll(c);
        orderedContents.retainAll(c);
        return result;
    }

    @Override
    public void clear() {
        this.contents.clear();
        this.orderedContents.clear();
    }
}
