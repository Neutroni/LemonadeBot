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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
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

    /**
     * Constructor
     *
     * @param ds DataSource to get connections from
     * @param guildID ID of the guild to store events for
     */
    public EventManager(final DataSource ds, final long guildID) {
        this.dataSource = ds;
        this.guildID = guildID;
    }

    /**
     * Get random event member from event
     *
     * @param eventName Name of event to get member for
     * @param guild guild to retrieve member from
     * @return Optional containing event if any event member can be found
     * @throws SQLException If database connection failed
     * @throws NoSuchElementException If there is no event with provided name
     */
    public Optional<Member> getRandomMember(final String eventName, final Guild guild) throws SQLException, NoSuchElementException {
        final Event ev = getEvent(eventName).orElseThrow();
        final List<Long> eventMemberIDs = getMembersMutable(ev);
        Collections.shuffle(eventMemberIDs);
        for (final Long l : eventMemberIDs) {
            try {
                final Member m = guild.retrieveMemberById(l).complete();
                return Optional.of(m);
            } catch (ErrorResponseException ex) {
                LOGGER.info("Found user {} in event {} members who could not be found, removing from event", l, eventName);
                LOGGER.debug("Error: {}", ex.getMessage());
                try {
                    leaveEvent(ev, l);
                    LOGGER.info("Succesfully removed missing member from event");
                } catch (SQLException e) {
                    LOGGER.error("Failure to remove member from event: {}", e.getMessage());
                    LOGGER.trace("Stack trace", e);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Remove user from event
     *
     * @param event event to remove user from
     * @param memberID id of the member to remove from event
     * @return true if user was removed from event
     * @throws SQLException if database connection failed
     */
    boolean leaveEvent(final Event event, final long memberID) throws SQLException {
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
     * Get event by name
     *
     * @param name name of the event
     * @return optional containing the event
     * @throws SQLException if database connection failed
     */
    Optional<Event> getEvent(final String name) throws SQLException {
        final String query = "SELECT description,owner,locked FROM Events WHERE guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, name);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String eventName = rs.getString("name");
                    final String eventDescription = rs.getString("description");
                    final long eventOwnerID = rs.getLong("owner");
                    final boolean locked = rs.getBoolean("locked");
                    final Event ev = new Event(eventName, eventDescription, eventOwnerID, locked);
                    return Optional.of(ev);
                }
            }
        }
        //Could not find event with the provided name
        return Optional.empty();
    }

    /**
     * Get members for event
     *
     * @param event event to get members for
     * @return Collection containing id:s of the members for the event
     * @throws SQLException if database connection failed
     */
    List<Long> getMembers(final Event event) throws SQLException {
        return Collections.unmodifiableList(getMembersMutable(event));
    }

    /**
     * Add event to database
     *
     * @param event event to add
     * @return true if event was added
     * @throws SQLException If database connection failed
     */
    boolean addEvent(final Event event) throws SQLException {
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
    boolean removeEvent(final Event event) throws SQLException {
        final String query = "DELETE FROM Events Where guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, event.getName());
            return ps.executeUpdate() > 0;
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
    boolean joinEvent(final Event event, final Member member) throws SQLException {
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
     * Remove all members from event
     *
     * @param event event to clear
     * @return true if any member was removed from event
     * @throws SQLException if database connction failed
     */
    boolean clearEvent(final Event event) throws SQLException {
        final String query = "DELETE FROM EventMembers WHERE guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, event.getName());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Lock event so that people can not join it
     *
     * @param event Event to lock
     * @return true if event was locked
     * @throws SQLException if database connection failed
     */
    boolean lockEvent(final Event event) throws SQLException {
        final String query = "UPDATE Events SET locked = 1 WHERE guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, event.getName());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Unlock event allowing people to join it
     *
     * @param event event to unlock
     * @return true if event was unlocked
     * @throws SQLException if database connection failed
     */
    boolean unlockEvent(final Event event) throws SQLException {
        final String query = "UPDATE Events SET locked = 0 WHERE guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, event.getName());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get list of events
     *
     * @return list of events
     * @throws SQLException if database connection failed
     */
    Collection<Event> getEvents() throws SQLException {
        final List<Event> events = new ArrayList<>();
        final String query = "SELECT name,description,owner,locked FROM Events WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String eventName = rs.getString("name");
                    final String eventDescription = rs.getString("description");
                    final long eventOwnerID = rs.getLong("owner");
                    final boolean locked = rs.getBoolean("locked");
                    events.add(new Event(eventName, eventDescription, eventOwnerID, locked));
                }
            }
        }
        return Collections.unmodifiableCollection(events);
    }

    protected List<Long> getMembersMutable(final Event event) throws SQLException {
        final List<Long> members = new ArrayList<>();
        final String query = "SELECT member FROM EventMembers WHERE guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, event.getName());
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getLong("member"));
                }
            }
        }
        return members;
    }

}
