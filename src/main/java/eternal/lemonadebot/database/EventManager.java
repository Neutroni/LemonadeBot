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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
            final boolean added = this.events.add(event);

            //Add to database
            final String query = "INSERT INTO Events(name,description,owner) VALUES(?,?,?);";
            if (hasEvent(event.getName())) {
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, event.getName());
                    ps.setString(2, event.getDescription());
                    ps.setString(3, event.getOwner());
                    return ps.executeUpdate() > 0;
                }
            }

            return added;
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
            final boolean removed = this.events.remove(event);

            //Remove from database
            final String query = "DELETE FROM Events Where name = ?;";
            if (hasEvent(event.getName())) {
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, event.getName());
                    return ps.executeUpdate() > 0;
                }
            }

            return removed;
        }
    }

    /**
     * Get event by name
     *
     * @param name name of the event
     * @return optional containing the event
     */
    public Optional<Event> getEvent(String name) {
        synchronized (this) {
            for (Event e : this.events) {
                if (name.equals(e.getName())) {
                    return Optional.of(e);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Check if event exists in database
     *
     * @param name name of the event
     * @return true if event was found
     * @throws SQLException if database connection failed
     */
    private boolean hasEvent(String name) throws SQLException {
        final String query = "SELECT name FROM Events WHERE name=?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Load event from database
     *
     * @return
     * @throws SQLException if database connection failed
     */
    private void loadEvents() throws SQLException {
        final String query = "SELECT name,description,owner FROM Events;";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                events.add(new Event(rs.getString("name"), rs.getString("description"), rs.getString("owner")));
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
        synchronized (this) {
            final boolean joined = event.join(member.getId());

            //Add to database
            final String query = "INSERT INTO EventMembers(event,member) VALUES(?,?);";
            if (!hasAttended(event.getName(), member.getId())) {
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, event.getName());
                    ps.setString(2, member.getId());
                    return ps.executeUpdate() > 0;
                }
            }

            return joined;
        }
    }

    /**
     * Check if user is a member of event
     *
     * @param name name of event
     * @param id id of the user
     * @return true if user is a mamber of the event
     * @throws SQLException
     */
    private boolean hasAttended(String name, String id) throws SQLException {
        final String query = "SELECT event FROM EventMembers WHERE event = ? AND member = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setString(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove user from event
     *
     * @param event event to remove user from
     * @param id id of the member to remove from event
     * @return true if user was removed from event
     * @throws SQLException if database connection failed
     */
    public boolean leaveEvent(Event event, String id) throws SQLException {
        synchronized (this) {
            final boolean left = event.leave(id);

            //Remove from database
            final String query = "DELETE FROM EventMembers WHERE event = ? AND member = ?;";
            if (hasAttended(event.getName(), id)) {
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, event.getName());
                    ps.setString(2, id);
                    return ps.executeUpdate() > 0;
                }
            }

            return left;
        }
    }

    /**
     * Remove all members from event
     *
     * @param event event to clear
     * @throws SQLException if database connction failed
     */
    public void clearEvent(Event event) throws SQLException {
        synchronized (this) {
            event.clear();

            //Remove from database
            final String query = "DELETE FROM Events WHERE name = ?;";
            event.clear();
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, event.getName());
                ps.executeUpdate();
            }
        }
    }

    /**
     * Get list of events
     *
     * @return list of events
     */
    public List<Event> getEvents() {
        synchronized (this) {
            return List.copyOf(this.events);
        }
    }
}
