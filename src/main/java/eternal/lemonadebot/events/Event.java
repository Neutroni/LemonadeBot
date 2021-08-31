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

import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
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
    private final long guildID;
    private volatile boolean locked;

    /**
     * Constructor
     *
     * @param name name of the event
     * @param description description for the event, null if no description
     * @param owner owner of the event
     */
    Event(final String name, final String description, final Member owner) {
        this.name = name;
        this.description = description;
        this.ownerID = owner.getIdLong();
        this.guildID = owner.getGuild().getIdLong();
        this.locked = false;
    }

    /**
     * Constructor
     *
     * @param name Name for this event
     * @param description Description for this event
     * @param owner Owner id for this event
     * @param locked whether event is locked
     */
    Event(final String name, final String description, final long owner, final long guildID, final boolean locked) {
        this.name = name;
        this.description = description;
        this.ownerID = owner;
        this.guildID = guildID;
        this.locked = locked;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof Event) {
            final Event otherEvent = (Event) other;
            return this.name.equals(otherEvent.getName());
        }
        return false;
    }

    /**
     * Get the name of this event
     *
     * @return name of this event
     */
    String getName() {
        return this.name;
    }

    /**
     * Get the description of this event
     *
     * @return description for this event
     */
    String getDescription() {
        return this.description;
    }

    /**
     * Get the id of the owner of this event
     *
     * @return owner id
     */
    long getOwner() {
        return this.ownerID;
    }

    /**
     * Get the id of the guild the event is from
     *
     * @return guild id
     */
    long getGuild() {
        return this.guildID;
    }

    /**
     * Lock the event, no members can join or leave if event is locked
     */
    void lock() {
        this.locked = true;
    }

    /**
     * Unlock the event, members can join or leave event
     */
    void unlock() {
        this.locked = false;
    }

    /**
     * Check if event is locked
     *
     * @return true if joining and leaving event is disallowed
     */
    boolean isLocked() {
        return this.locked;
    }

    /**
     * Get string representation of the event for listing events
     *
     * @param locale Locale to return the list element in
     * @param jda JDA to use to get the event owner
     * @return String
     */
    CompletableFuture<String> toListElement(final ResourceBundle locale, final JDA jda) {
        final CompletableFuture<String> result = new CompletableFuture<>();
        final String template = locale.getString("EVENT_COMMAND_LIST_ELEMENT");
        jda.retrieveUserById(this.ownerID).queue((User eventCreator) -> {
            //Found user
            final String creatorName = eventCreator.getAsMention();
            final String eventDescription;
            if (this.description == null) {
                eventDescription = locale.getString("EVENT_NO_DESCRIPTION");
            } else {
                eventDescription = this.description;
            }
            result.complete(String.format(template, this.name, eventDescription, creatorName));
        }, (Throwable t) -> {
            //User missing
            final String creatorName = locale.getString("UNKNOWN_USER");
            final String eventDescription;
            if (this.description == null) {
                eventDescription = locale.getString("EVENT_NO_DESCRIPTION");
            } else {
                eventDescription = this.description;
            }
            result.complete(String.format(template, this.name, eventDescription, creatorName));
        });
        return result;
    }

}
