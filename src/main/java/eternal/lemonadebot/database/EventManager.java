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
package eternal.lemonadebot.database;

import eternal.lemonadebot.dataobjects.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class EventManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DataSource dataSource;
    private final long guildID;
    private final Map<String, Event> events = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param ds database connection
     * @param guildID
     */
    EventManager(DataSource ds, long guildID) {
        this.dataSource = ds;
        this.guildID = guildID;
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
        this.events.putIfAbsent(event.getName(), event);

        //Add to database
        final String query = "INSERT OR IGNORE INTO Events(guild,name,description,owner,locked) VALUES(?,?,?,?,?);";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, event.getName());
            ps.setString(3, event.getDescription());
            ps.setLong(4, event.getOwner());
            ps.setBoolean(5, event.isLocked());
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
        this.events.remove(event.getName());

        //Remove from database
        final String query = "DELETE FROM Events Where guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, event.getName());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get event by name
     *
     * @param name name of the event
     * @return optional containing the event
     */
    public Optional<Event> getEvent(String name) {
        return Optional.ofNullable(this.events.get(name));
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
        final String query = "INSERT OR IGNORE INTO EventMembers(guild,name,member) VALUES(?,?,?);";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
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
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
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
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, event.getName());
            ps.executeUpdate();
        }
    }

    public void lockEvent(Event event) throws SQLException {
        event.lock();

        //Update database
        final String query = "UPDATE Events SET locked = 1 WHERE guild = ? AND name = ?;";
        event.clear();
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, event.getName());
            ps.executeUpdate();
        }
    }

    public void unlockEvent(Event event) throws SQLException {
        event.unlock();

        //Update database
        final String query = "UPDATE Events SET locked = 0 WHERE guild = ? AND name = ?;";
        event.clear();
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, event.getName());
            ps.executeUpdate();
        }
    }

    /**
     * Get list of events
     *
     * @return list of events
     */
    public Collection<Event> getEvents() {
        return Collections.unmodifiableCollection(this.events.values());
    }

    /**
     * Load event from database
     *
     * @return
     * @throws SQLException if database connection failed
     */
    private void loadEvents() {
        final String query = "SELECT name,description,owner,locked FROM Events WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String eventName = rs.getString("name");
                    final String eventDescription = rs.getString("description");
                    final long eventOwnerID = rs.getLong("owner");
                    final boolean locked = rs.getBoolean("locked");
                    final Event ev = new Event(eventName, eventDescription, eventOwnerID, locked);
                    loadMembers(ev);
                    events.put(ev.getName(), ev);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading events from database failed");
            LOGGER.warn(e.getMessage());
            LOGGER.trace(e);
        }
    }

    private void loadMembers(Event event) throws SQLException {
        final String query = "SELECT member FROM EventMembers WHERE guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, event.getName());
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    event.join(rs.getLong("member"));
                }
            }
        }
    }
}
