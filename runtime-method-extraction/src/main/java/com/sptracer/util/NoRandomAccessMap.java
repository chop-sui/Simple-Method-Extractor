package com.sptracer.util;

import com.sptracer.Recyclable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A map (not implementing the java.util.Map interface) that only supports key-value pair additions and iterations.
 * The map doesn't allocate during addition or iteration.
 * This map does not support any form of concurrency. It can be either be in a write mode (through its {@link #add}
 * method) or read mode (through the {@link #iterator()} API) at a given time.
 *
 * NOTE: this map does not guarantee visibility, therefore ensuring visibility when switching from read to write mode
 * (or the other way around) is under the responsibility of the map's user.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class NoRandomAccessMap<K, V> implements Recyclable, Iterable<NoRandomAccessMap.Entry<K, V>> {
    private List<K> keys = new ArrayList<>();
    private List<V> values = new ArrayList<>();
    private NoGarbageIterator iterator = new NoGarbageIterator();

    /**
     * @param key   can't be null
     * @param value can be null
     * @throws NullPointerException if provided key is null
     */
    public void add(K key, @Nullable V value) throws NullPointerException {
        //noinspection ConstantConditions
        if (key == null) {
            throw new NullPointerException("This map doesn't support null keys");
        }
        keys.add(key);
        values.add(value);
    }

    public int size() {
        return keys.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void resetState() {
        keys.clear();
        values.clear();
        iterator.reset();
    }

    @Override
    public java.util.Iterator<NoRandomAccessMap.Entry<K, V>> iterator() {
        iterator.reset();
        return iterator;
    }

    public void copyFrom(NoRandomAccessMap<K, V> other) {
        resetState();
        for (Entry<K, V> entry : other) {
            this.add(entry.getKey(), entry.getValue());
        }
    }

    public interface Entry<K, V> {
        K getKey();

        @Nullable
        V getValue();
    }

    private class EntryImpl implements Entry<K, V> {
        @Nullable
        K key;
        @Nullable
        V value;

        public K getKey() {
            if (key == null) {
                throw new IllegalStateException("Key shouldn't be null. Make sure you don't read and write to this map concurrently");
            }
            return key;
        }

        @Nullable
        public V getValue() {
            return value;
        }

        void reset() {
            key = null;
            value = null;
        }
    }

    private class NoGarbageIterator implements java.util.Iterator<NoRandomAccessMap.Entry<K, V>> {
        int index = 0;
        final EntryImpl entry = new EntryImpl();

        @Override
        public boolean hasNext() {
            return index < keys.size();
        }

        @Override
        public Entry<K, V> next() {
            entry.key = keys.get(index);
            entry.value = values.get(index);
            index++;
            return entry;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        void reset() {
            index = 0;
            entry.reset();
        }
    }
}
