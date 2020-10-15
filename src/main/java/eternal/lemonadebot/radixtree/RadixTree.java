/*
 * The MIT License
 *
 * Copyright 2020 Neutroni.
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
package eternal.lemonadebot.radixtree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Neutroni
 * @param <T> Type of stored items
 */
public class RadixTree<T> {

    private final Map<String, RadixTree<T>> children = new ConcurrentHashMap<>();
    private final T value;

    /**
     * Constructor
     *
     * @param value value to store in the tree
     */
    public RadixTree(T value) {
        this.value = value;
    }

    /**
     * Get the value associated with this node
     *
     * @return value stored
     */
    public T getValue() {
        return this.value;
    }

    /**
     * Add node to this tree
     *
     * @param name name of the node to add
     * @param node value to associate with name
     * @return true if opearation overwrote a value
     */
    public boolean put(String name, T node) {
        final RadixTree<T> newNode = new RadixTree<>(node);
        int i = name.indexOf(' ');
        //name is single part, just check if we have a node with given name
        if (i == -1) {
            final RadixTree<T> oldNode = this.children.put(name, newNode);
            if (oldNode == null) {
                return false;
            }
            newNode.addChildren(oldNode.getChildren());
            return true;
        }
        //Multiple part name
        final String key = name.substring(0, i);
        final String remaining = name.substring(i + 1);
        final RadixTree<T> oldNode = this.children.computeIfAbsent(key, (String t) -> {
            //Non existent middle node, mark value as null
            return new RadixTree<>(null);
        });
        return oldNode.put(remaining, node);
    }

    /**
     * Remove node by name
     *
     * @param name Name of node to remove
     * @return true if node was removed
     */
    public boolean remove(String name) {
        int i = name.indexOf(' ');
        //name is single part, just check if we have a node with given name
        if (i == -1) {
            final RadixTree<T> oldNode = this.children.get(name);
            if (oldNode == null) {
                //Could not find node to remove
                return false;
            }
            final Map<String, RadixTree<T>> branches = oldNode.getChildren();
            if (branches.isEmpty()) {
                //Node has no children, remove the node
                final RadixTree<T> tempNode = this.children.remove(name);
                return tempNode != null;
            }
            //Node has children, set the nodes value to null
            if (oldNode.getValue() == null) {
                //Node already null
                return false;
            }
            final RadixTree<T> leaf = new RadixTree<>(null);
            leaf.addChildren(branches);
            final RadixTree<T> x = this.children.put(name, leaf);
            return x != null;
        }
        //Multiple part name
        final String key = name.substring(0, i);
        final String remaining = name.substring(i + 1);
        final RadixTree<T> oldNode = this.children.get(key);
        //No children with key, no node to remove
        if (oldNode == null) {
            return false;
        }
        final boolean removed = oldNode.remove(remaining);
        final var branches = oldNode.getChildren();
        if (branches.isEmpty() && (oldNode.getValue() == null)) {
            //Child has no more children, and has no value, remove from map
            final var x = this.children.remove(key);
            return (x != null);
        }
        return removed;
    }

    /**
     * Get the value stored for the given key
     *
     * @param name key to get value for
     * @return stored value or this nodes value if no match for key
     */
    public T get(String name) {
        int i = name.indexOf(' ');
        //name is single part, just check if we have a node with given name
        if (i == -1) {
            final RadixTree<T> oldNode = this.children.get(name);
            if (oldNode == null) {
                return getValue();
            }
            final T val = oldNode.getValue();
            if (val == null) {
                return getValue();
            }
            return val;
        }
        //Multiple part name
        final String key = name.substring(0, i);
        final String remaining = name.substring(i + 1);
        final RadixTree<T> oldNode = this.children.get(key);
        //No children with key, return current nodes value
        if (oldNode == null) {
            return this.value;
        }
        final T val = oldNode.get(remaining);
        if (val == null) {
            return getValue();
        }
        return val;
    }

    /**
     * Get immutable view of the values contained in the tree
     *
     * @return map containing values of elements
     */
    public Collection<T> getValues() {
        final List<T> values = new ArrayList<>();
        //Get every child of this node and add to map
        for (RadixTree<T> child : this.children.values()) {
            final T val = child.getValue();
            if (val != null) {
                values.add(val);
            }
            values.addAll(child.getValues());
        }
        return Collections.unmodifiableCollection(values);
    }

    /**
     * Clear the list of children this node has
     */
    public void clear() {
        this.children.clear();
    }

    protected Map<String, RadixTree<T>> getChildren() {
        return Collections.unmodifiableMap(this.children);
    }

    protected void addChildren(Map<String, RadixTree<T>> nodes) {
        this.children.putAll(nodes);
    }
}
