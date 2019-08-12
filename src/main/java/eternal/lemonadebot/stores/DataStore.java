/*
 * The MIT License
 *
 * Copyright 2019 Neutroni.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package eternal.lemonadebot.stores;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Neutroni
 * @param <T> Type of data this store stores
 */
public class DataStore<T> {

    private final List<T> items = new ArrayList<>();

    /**
     * Stores item with this store
     *
     * @param item item to store
     * @return true if item was added, false otherwise
     */
    public boolean add(T item) {
        synchronized (this) {
            if (this.items.contains(item)) {
                return false;
            }
            return this.items.add(item);
        }
    }

    /**
     * Remove item from this store
     *
     * @param item item to remove
     * @return true if item was removed
     */
    public boolean remove(T item) {
        synchronized (this) {
            return this.items.remove(item);
        }
    }

    /**
     * Check if this datastore contains give item
     *
     * @param item item to find
     * @return true if found
     */
    public boolean hasItem(T item) {
        synchronized (this) {
            return this.items.contains(item);
        }
    }

    /**
     * Get the items this store has
     *
     * @return Unmodifiable list of items stored in this store
     */
    public List<T> getItems() {
        return Collections.unmodifiableList(this.items);
    }
}
