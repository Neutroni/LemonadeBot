/*
 * The MIT License
 *
 * Copyright 2019 joonas.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author joonas
 */
public class Event {

    private final String name;
    private final String ownerID;
    private final Set<String> members = Collections.synchronizedSet(new HashSet<>());

    /**
     * Constructor
     *
     * @param name name of the event
     * @param owner owner of the event
     */
    public Event(String name, String owner) {
        this.name = name;
        this.ownerID = owner;
        this.members.add(ownerID);
    }

    /**
     * Join this event
     *
     * @param member member id who wants to join
     * @return true if succesfully joined, false otherwise
     */
    public boolean join(String member) {
        return this.members.add(name);
    }

    /**
     * Leave this event
     *
     * @param member member who wants to leave
     * @return true if left event succesfully, false otherwise
     */
    public boolean leave(String member) {
        return this.members.remove(member);
    }

    /**
     * Clears the list of joined people
     */
    public void clear() {
        this.members.clear();
    }

    /**
     * Get the list of members who have joined this event
     *
     * @return list of member idsF
     */
    public Set<String> getMembers() {
        return Collections.unmodifiableSet(this.members);
    }

    /**
     * Get the name of this event
     *
     * @return name of this event
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the id of the owner of this event
     *
     * @return owner id
     */
    public String getOwner() {
        return this.ownerID;
    }

}
