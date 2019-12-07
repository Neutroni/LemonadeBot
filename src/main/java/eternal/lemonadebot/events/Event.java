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
package eternal.lemonadebot.events;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Neutroni
 */
public class Event {

    private final String name;
    private final String description;
    private final long ownerID;
    private final Set<Long> members = new HashSet<>();

    /**
     * Constructor
     *
     * @param name name of the event
     * @param description description for the event
     * @param owner owner of the event
     */
    public Event(String name, String description, long owner) {
        this.name = name;
        this.description = description;
        this.ownerID = owner;
    }

    /**
     * Join this event
     *
     * @param member member id who wants to join
     * @return true if succesfully joined, false otherwise
     */
    public boolean join(long member) {
        synchronized (this) {
            return this.members.add(member);
        }
    }

    /**
     * Leave this event
     *
     * @param member member who wants to leave
     * @return true if left event succesfully, false otherwise
     */
    public boolean leave(long member) {
        synchronized (this) {
            return this.members.remove(member);
        }
    }

    /**
     * Clears the list of joined people
     */
    public void clear() {
        synchronized (this) {
            this.members.clear();
            this.members.add(this.ownerID);
        }
    }

    /**
     * Get the list of members who have joined this event
     *
     * @return list of member idsF
     */
    public List<Long> getMembers() {
        return List.copyOf(this.members);
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
     * Get the description of this event
     *
     * @return description for this event
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Get the id of the owner of this event
     *
     * @return owner id
     */
    public long getOwner() {
        return this.ownerID;
    }
    
    @Override
    public int hashCode(){
        return this.name.hashCode();
    }
    
    @Override
    public boolean equals(Object other){
        if(other instanceof Event){
            Event otherEvent = (Event) other;
            return this.name.equals(otherEvent.getName());
        }
        return false;
    }

}
