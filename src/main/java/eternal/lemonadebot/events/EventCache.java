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

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Member;

/**
 *
 * @author Neutroni
 */
public class EventCache extends EventManager {

    private boolean eventsLoaded = false;
    private final Map<String, Event> events = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> members = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param ds database connection
     * @param guildID ID of the guild to store events for
     */
    public EventCache(final DataSource ds, final long guildID) {
        super(ds, guildID);
    }

    @Override
    public Optional<Event> getEvent(final String name) throws SQLException {
        final Event event = this.events.get(name);
        if (this.eventsLoaded) {
            return Optional.ofNullable(event);
        }
        if (event == null) {
            final Optional<Event> optEvent = super.getEvent(name);
            optEvent.ifPresent((Event t) -> {
                this.events.put(t.getName(), t);
            });
            return optEvent;
        }
        return Optional.of(event);
    }

    @Override
    public boolean leaveEvent(final Event event, final long memberID) throws SQLException {
        final Set<Long> eventMembers = this.members.get(event.getName());
        if (eventMembers != null) {
            eventMembers.remove(memberID);
        }
        return super.leaveEvent(event, memberID);
    }

    @Override
    public List<Long> getMembers(final Event event) throws SQLException {
        final String eventName = event.getName();
        final Set<Long> eventMembers = this.members.get(eventName);
        if (eventMembers == null) {
            final List<Long> memberIds = super.getMembers(event);
            final Set<Long> memberSet = ConcurrentHashMap.newKeySet();
            memberSet.addAll(memberIds);
            this.members.put(eventName, memberSet);
            return memberIds;
        }
        return List.copyOf(eventMembers);
    }

    @Override
    boolean addEvent(final Event event) throws SQLException {
        this.events.putIfAbsent(event.getName(), event);
        return super.addEvent(event);
    }

    @Override
    boolean removeEvent(final Event event) throws SQLException {
        final String eventName = event.getName();
        this.events.remove(eventName);
        this.members.remove(eventName);
        return super.removeEvent(event);
    }

    @Override
    boolean joinEvent(final Event event, final Member member) throws SQLException {
        this.members.computeIfAbsent(event.getName(), (String eventName) -> {
            return ConcurrentHashMap.newKeySet();
        }).add(member.getIdLong());
        return super.joinEvent(event, member);
    }

    @Override
    boolean clearEvent(final Event event) throws SQLException {
        this.members.remove(event.getName());
        return super.clearEvent(event);
    }

    @Override
    boolean lockEvent(final Event event) throws SQLException {
        event.lock();
        return super.lockEvent(event);
    }

    @Override
    boolean unlockEvent(final Event event) throws SQLException {
        event.unlock();
        return super.unlockEvent(event);
    }

    @Override
    Collection<Event> getEvents() throws SQLException {
        if (this.eventsLoaded) {
            return Collections.unmodifiableCollection(this.events.values());
        }
        final Collection<Event> eventCollection = super.getEvents();
        eventCollection.forEach((Event ev) -> {
            this.events.putIfAbsent(ev.getName(), ev);
        });
        this.eventsLoaded = true;
        return eventCollection;
    }

}
