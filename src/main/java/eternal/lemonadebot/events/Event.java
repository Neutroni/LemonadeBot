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

import eternal.lemonadebot.translation.TranslationKey;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author Neutroni
 */
public class Event {

    private final String name;
    private final String description;
    private final long ownerID;
    private final Set<Long> members = ConcurrentHashMap.newKeySet();

    /**
     * Constructor
     *
     * Remember to set database id for this event when adding to database
     *
     * @param name name of the event
     * @param description description for the event
     * @param owner owner of the event
     */
    public Event(String name, String description, Member owner) {
        this.name = name;
        this.description = description;
        this.ownerID = owner.getIdLong();
    }

    /**
     * Constructor
     *
     * @param name Name for this event
     * @param description Description for this event
     * @param owner Owner id for this event
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
     * Get the list of members who have joined this event. This returns a list
     * to make partitioning easier when retrieving members
     *
     * @return list of members in the event
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
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Event) {
            final Event otherEvent = (Event) other;
            return this.name.equals(otherEvent.name);
        }
        return false;
    }

    /**
     * Get string representation of the event for listing events
     *
     * @param locale Locale to return the list element in
     * @param jda JDA to use to get the event owner
     * @return String
     */
    public CompletableFuture<String> toListElement(Locale locale, JDA jda) {
        final CompletableFuture<String> result = new CompletableFuture<>();
        final String template = TranslationKey.EVENT_COMMAND_LIST_ELEMENT.getTranslation(locale);
        jda.retrieveUserById(this.ownerID).queue((User eventCreator) -> {
            //Found user
            final String creatorName = eventCreator.getAsMention();
            result.complete(String.format(template, this.name, this.description, creatorName));
        }, (Throwable t) -> {
            //User missing
            final String creatorName = TranslationKey.UNKNOWN_USER.getTranslation(locale);
            result.complete(String.format(template, this.name, this.description, creatorName));
        });
        return result;
    }

}
