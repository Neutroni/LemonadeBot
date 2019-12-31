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
package eternal.lemonadebot.database;

import eternal.lemonadebot.events.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

/**
 *
 * @author Neutroni
 */
public class EventManager {

    private final Connection conn;
    private final Set<Event> events = new HashSet<>();

    /**
     * Constructor
     *
     * @param conn database connectionF
     * @throws SQLException if loading events from database failed
     */
    EventManager(Connection conn) throws SQLException {
        this.conn = conn;
        loadEvents();
    }

    /**
     * Add event to database
     *
     * @param event event to add
     * @return true if event was added
     * @throws SQLException If database connection failed
     */
    public boolean addEvent(Event event) throws SQLException {
        synchronized (this) {
            this.events.add(event);
        }

        //Add to database
        final String query = "INSERT OR IGNORE INTO Events(guild,name,description,owner) VALUES(?,?,?,?);";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, event.getGuild());
            ps.setString(2, event.getName());
            ps.setString(3, event.getDescription());
            ps.setLong(4, event.getOwner());
            return ps.executeUpdate() > 0;
        }

    }

    /**
     * Remove event from database
     *
     * @param event event to remove
     * @return true if event was removed succesfully
     * @throws SQLException if database connection failed
     */
    public boolean removeEvent(Event event) throws SQLException {
        synchronized (this) {
            this.events.remove(event);
        }
        //Remove from database
        final String query = "DELETE FROM Events Where guild = ? AND name = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, event.getGuild());
            ps.setString(2, event.getName());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get event by name
     *
     * @param name name of the event
     * @param guild guild the event is from
     * @return optional containing the event
     */
    public Optional<Event> getEvent(String name, Guild guild) {
        return getEvent(name, guild.getIdLong());
    }

    /**
     * Get event by name
     *
     * @param name name of the event
     * @param guildID id of the guild the event is from
     * @return optional containing the event
     */
    public Optional<Event> getEvent(String name, long guildID) {
        synchronized (this) {
            for (Event e : this.events) {
                if (!name.equals(e.getName())) {
                    continue;
                }
                if (guildID != e.getGuild()) {
                    continue;
                }
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /**
     * Load event from database
     *
     * @return
     * @throws SQLException if database connection failed
     */
    private void loadEvents() throws SQLException {
        final String query = "SELECT guild,name,description,owner FROM Events;";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                final Event ev = new Event(rs.getString("name"), rs.getString("description"), rs.getLong("owner"), rs.getLong("guild"));
                loadMembers(ev);
                events.add(ev);
            }
        }
    }

    private void loadMembers(Event event) throws SQLException {
        final String query = "SELECT member FROM EventMembers WHERE guild = ? AND name = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, event.getGuild());
            ps.setString(2, event.getName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    event.join(rs.getLong("member"));
                }
            }
        }
    }

    /**
     * Add user to event
     *
     * @param event name of event
     * @param member member to add to event
     * @return true if joined event
     * @throws SQLException if database connection failed or tried to join event
     * that doesn't exist
     */
    public boolean joinEvent(Event event, Member member) throws SQLException {
        event.join(member.getIdLong());

        //Add to database
        final String query = "INSERT OR IGNORE INTO EventMembers(guild,name,member) VALUES(?,?);";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, event.getGuild());
            ps.setString(2, event.getName());
            ps.setString(3, member.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Remove user from event
     *
     * @param event event to remove user from
     * @param memberID id of the member to remove from event
     * @return true if user was removed from event
     * @throws SQLException if database connection failed
     */
    public boolean leaveEvent(Event event, long memberID) throws SQLException {
        event.leave(memberID);

        //Remove from database
        final String query = "DELETE FROM EventMembers WHERE guild = ? AND name = ? AND member = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, event.getGuild());
            ps.setString(2, event.getName());
            ps.setLong(3, memberID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Remove all members from event
     *
     * @param event event to clear
     * @throws SQLException if database connction failed
     */
    public void clearEvent(Event event) throws SQLException {
        event.clear();

        //Remove from database
        final String query = "DELETE FROM EventMembers WHERE guild = ? AND name = ?;";
        event.clear();
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, event.getGuild());
            ps.setString(2, event.getName());
            ps.executeUpdate();
        }
    }

    /**
     * Get list of events
     *
     * @param guild Guild to get event for
     * @return list of events
     */
    public List<Event> getEvents(Guild guild) {
        synchronized (this) {
            final List<Event> guildEvents = new ArrayList<>();
            for (Event e : this.events) {
                if (e.getGuild() == guild.getIdLong()) {
                    guildEvents.add(e);
                }
            }
            return guildEvents;
        }
    }
}
