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
package eternal.lemonadebot.events;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

/**
 *
 * @author Neutroni
 */
public class Event {

    private final long guildID;
    private final String name;
    private final String description;
    private final long ownerID;
    private final Set<Long> members = Collections.synchronizedSet(new HashSet<>());

    /**
     * Constructor
     *
     * Remember to set database id for this event when adding to database
     *
     * @param name name of the event
     * @param description description for the event
     * @param owner owner of the event
     * @param guild Guild the event belongs to
     */
    public Event(String name, String description, Member owner, Guild guild) {
        this.name = name;
        this.description = description;
        this.ownerID = owner.getIdLong();
        this.guildID = guild.getIdLong();
    }

    /**
     * Constructor
     *
     * @param name Name for this event
     * @param description Description for this event
     * @param owner Owner id for this event
     * @param guild Guild id for this event
     */
    public Event(String name, String description, long owner, long guild) {
        this.name = name;
        this.description = description;
        this.ownerID = owner;
        this.guildID = guild;
    }

    /**
     * Join this event
     *
     * @param member member id who wants to join
     * @return true if succesfully joined, false otherwise
     */
    public boolean join(long member) {
        return this.members.add(member);
    }

    /**
     * Leave this event
     *
     * @param member member who wants to leave
     * @return true if left event succesfully, false otherwise
     */
    public boolean leave(long member) {
        return this.members.remove(member);
    }

    /**
     * Clears the list of joined people
     */
    public void clear() {
        this.members.clear();
        this.members.add(this.ownerID);
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

    /**
     * Get the guild this event is from
     *
     * @return guild id
     */
    public long getGuild() {
        return this.guildID;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (int) (this.guildID ^ (this.guildID >>> 32));
        hash = 89 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Event other = (Event) obj;
        if (this.guildID != other.guildID) {
            return false;
        }
        return Objects.equals(this.name, other.name);
    }

}
