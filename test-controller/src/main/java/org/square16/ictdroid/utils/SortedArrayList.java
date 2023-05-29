package org.square16.ictdroid.utils;

import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

public class SortedArrayList<T extends Comparable<T>> extends CopyOnWriteArrayList<T> {
    @Override
    public boolean add(T t) {
        synchronized (this) {
            int index = 0;
            if (this.size() > 0) {
                index = Collections.binarySearch(this, t);
                if (index < 0) {
                    index = ~index;
                }
            }
            super.add(index, t);
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        synchronized (this) {
            return super.remove(o);
        }
    }

    @Override
    public void clear() {
        synchronized (this) {
            super.clear();
        }
    }
}
