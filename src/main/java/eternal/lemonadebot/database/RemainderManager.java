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
import eternal.lemonadebot.events.MentionEnum;
import eternal.lemonadebot.events.Remainder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class RemainderManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Connection conn;
    private final Set<Remainder> remainders = new HashSet<>();
    private final Timer remainderTimer = new Timer();
    private final EventManager eventManager;

    /**
     * Constructor
     *
     * @param conn database connectionF
     * @param em EventManager to fetch events from
     * @throws SQLException if loading events from database failed
     */
    RemainderManager(Connection conn, EventManager em) throws SQLException {
        this.conn = conn;
        this.eventManager = em;
    }

    /**
     * Add remainder to database
     *
     * @param remainder event to add
     * @return true if event was added
     * @throws SQLException If database connection failed
     */
    public boolean addRemainder(Remainder remainder) throws SQLException {
        synchronized (this) {
            final boolean added = this.remainders.add(remainder);
            
            //If timer was just added att to 
            if (added) {
                this.remainderTimer.scheduleAtFixedRate(remainder, remainder.getActivationDate(), TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS));
            }

            //Add to database
            final String query = "INSERT INTO Remainders(event,day,time,mention,channel) VALUES(?,?,?,?,?);";
            final String eventName = remainder.getEvent().getName();
            final String remainderDay = remainder.getDay().name();
            final String remainderTime = remainder.getTime().toString();
            if (!hasRemainder(eventName,remainderDay,remainderTime)) {
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, eventName);
                    ps.setString(2, remainderDay);
                    ps.setString(3, remainderTime);
                    ps.setString(4, remainder.getMentionMode().name());
                    ps.setLong(5, remainder.getChannel().getIdLong());
                    return ps.executeUpdate() > 0;
                }
            }
            return added;
        }
    }

    /**
     * Remove event from database
     *
     * @param remainder event to remove
     * @return true if event was removed succesfully
     * @throws SQLException if database connection failed
     */
    public boolean deleteRemainder(Remainder remainder) throws SQLException {
        synchronized (this) {
            remainder.cancel();
            final boolean removed = this.remainders.remove(remainder);

            //Remove from database
            final String query = "DELETE FROM Remainders Where event = ? AND day = ? AND time = ?;";
            final String eventName = remainder.getEvent().getName();
            final String remainderDay = remainder.getDay().name();
            final String remainderTime = remainder.getTime().toString();
            if (hasRemainder(eventName, remainderDay,remainderTime)) {
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, eventName);
                    ps.setString(2, remainderDay);
                    ps.setString(3, remainderTime);
                    return ps.executeUpdate() > 0;
                }
            }
            return removed;
        }
    }

    /**
     * Load remainders from database
     *
     * @param jda JDA instance to use for initializing remainder timers
     * @throws SQLException If database connection failed
     */
    public void loadRemainders(JDA jda) throws SQLException {
        final String query = "SELECT event,day,time,mention,channel FROM Remainders;";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                final String eventName = rs.getString("event");
                final Optional<Event> optEvent = this.eventManager.getEvent(eventName);
                if (optEvent.isEmpty()) {
                    LOGGER.error("Malformed remainder with missing event" + eventName);
                    continue;
                }
                final String remainderDay = rs.getString("day");
                DayOfWeek activationDay;
                try {
                    activationDay = DayOfWeek.valueOf(remainderDay);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Malformed weekday in database: " + remainderDay);
                    continue;
                }
                final String reminderTime = rs.getString("time");
                LocalTime activationTime;
                try {
                    activationTime = LocalTime.parse(reminderTime);
                } catch (DateTimeParseException e) {
                    LOGGER.error("Malformed time for reminder in database: " + reminderTime);
                    return;
                }
                final String mentions = rs.getString("mention");
                MentionEnum me;
                try {
                    me = MentionEnum.valueOf(mentions);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Unkown mention value in database: " + mentions);
                    continue;
                }
                final long remainderChannel = rs.getLong("channel");
                final TextChannel channel = jda.getTextChannelById(remainderChannel);
                if (channel == null) {
                    LOGGER.warn("Channel for remainder in database that could not be found on server, id: " + remainderChannel);
                }
                final Remainder remainder = new Remainder(channel, optEvent.get(), me, activationDay, activationTime);

                remainders.add(remainder);
                this.remainderTimer.scheduleAtFixedRate(remainder, remainder.getActivationDate(), TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS));
            }
        }
    }

    /**
     * Get Reminder by name
     *
     * @param eventName name the remainder is for
     * @param day remainder day
     * @param time remainder time
     * @return Optional containing the remainder if found
     */
    public Optional<Remainder> getRemainder(String eventName, String day, String time) {
        synchronized (this) {
            for (Remainder r : this.remainders) {
                if (!r.getEvent().getName().equals(eventName)) {
                    continue;
                }
                if(!r.getDay().toString().equals(eventName.toUpperCase())){
                    continue;
                }
                if(!r.getTime().toString().equals(time.toUpperCase())){
                    continue;
                }
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /**
     * Check if remainder exisits in database
     *
     * @param name name of the remainder
     * @return true if remainder is in database
     * @throws SQLException if database connection failed
     */
    private boolean hasRemainder(String eventName, String remainderDay, String remainderTime) throws SQLException {
        final String query = "SELECT event FROM Remainders WHERE event=? AND day=? AND time=?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, eventName);
            ps.setString(2, remainderDay);
            ps.setString(3, remainderTime);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the list of remainders currently active
     *
     * @return List of remainders
     */
    public List<Remainder> getRemainders() {
        synchronized (this) {
            return List.copyOf(this.remainders);
        }
    }

}
