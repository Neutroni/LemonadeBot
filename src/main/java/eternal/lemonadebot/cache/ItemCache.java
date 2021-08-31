/*
 * The MIT License
 *
 * Copyright 2021 Neutroni.
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
package eternal.lemonadebot.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class used to store items limited
 *
 * @author Neutroni
 * @param <Key> Type of the keys to store
 * @param <Value> Type of the values to store
 */
public class ItemCache<Key, Value> extends LinkedHashMap<Key, Value> {

    /**
     * Limit of the items to store
     */
    private final int itemLimit;

    /**
     * Constructor that initializes the backing LinkedHashMap with current
     * default loadfactor and sets ordering to access order
     *
     * @param size Max amount of items to store
     */
    public ItemCache(final int size) throws IllegalArgumentException {
        super(1, 0.75f, true);
        if (size < 0) {
            throw new IllegalArgumentException("Cache size can not be negative");
        }
        this.itemLimit = size;
    }

    @Override
    protected boolean removeEldestEntry(final Map.Entry eldest) {
        return size() > itemLimit;
    }
}
