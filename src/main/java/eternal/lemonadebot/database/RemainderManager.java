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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class RemainderManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Connection conn;
    private final JDA jda;

    private final Set<Remainder> remainders = Collections.synchronizedSet(new HashSet<>());
    private final Timer remainderTimer = new Timer();
    private final EventManager eventManager;

    /**
     * Constructor
     *
     * @param conn database connectionF
     * @param em EventManager to fetch events from
     * @param jda JDA to pass to remainders
     * @throws SQLException if loading events from database failed
     */
    RemainderManager(Connection conn, EventManager em, JDA jda) throws SQLException {
        this.conn = conn;
        this.jda = jda;
        this.eventManager = em;
        loadRemainders();
    }

    /**
     * Builds remainder
     *
     * @param textChannel channel for remainder
     * @param event event the remainder is for
     * @param mentions who the remainder notifies
     * @param day day for remainder
     * @param time time for remainder
     * @return new remainder
     */
    public Remainder build(long textChannel, Event event, MentionEnum mentions, DayOfWeek day, LocalTime time) {
        return new Remainder(this.jda, textChannel, event, mentions, day, time);
    }

    /**
     * Add remainder to database
     *
     * @param remainder event to add
     * @return true if event was added
     * @throws SQLException If database connection failed
     */
    public boolean addRemainder(Remainder remainder) throws SQLException {
        LOGGER.debug("Storing remainder for event" + remainder.getEvent().getName());
        final boolean added = this.remainders.add(remainder);

        //If timer was just added schedule the activation
        if (added) {
            this.remainderTimer.scheduleAtFixedRate(remainder, remainder.getActivationDate(), TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS));
            LOGGER.debug("Remainder scheluded for activation at " + remainder.getActivationDate().toString());
        }

        //Add to database
        final String query = "INSERT INTO Remainders(event,day,time,mention,channel) VALUES(?,?,?,?,?);";
        final long guildID = remainder.getEvent().getGuild();
        final String eventName = remainder.getEvent().getName();
        final String remainderDay = remainder.getDay().name();
        final String remainderTime = remainder.getTime().toString();
        final String mention = remainder.getMentionMode().name();
        final long channel = remainder.getChannel();
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, guildID);
            ps.setString(2, eventName);
            ps.setString(3, remainderDay);
            ps.setString(4, remainderTime);
            ps.setString(5, mention);
            ps.setLong(6, channel);
            return ps.executeUpdate() > 0;
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
        remainder.cancel();
        this.remainders.remove(remainder);

        //Remove from database
        final String query = "DELETE FROM Remainders Where guild = ? AND name = ? AND day = ? AND time = ?;";
        final long guildID = remainder.getEvent().getGuild();
        final String eventName = remainder.getEvent().getName();
        final String remainderDay = remainder.getDay().name();
        final String remainderTime = remainder.getTime().toString();

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, guildID);
            ps.setString(2, eventName);
            ps.setString(3, remainderDay);
            ps.setString(4, remainderTime);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Load remainders from database
     *
     * @param jda JDA instance to use for initializing remainder timers
     * @throws SQLException If database connection failed
     */
    private void loadRemainders() throws SQLException {
        LOGGER.debug("Started loading remainders from database");
        final String query = "SELECT guild,name,day,time,mention,channel FROM Remainders;";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                final long guildID = rs.getLong("guild");
                final String eventName = rs.getString("name");
                final Optional<Event> optEvent = this.eventManager.getEvent(eventName, guildID);
                if (optEvent.isEmpty()) {
                    LOGGER.error("Malformed remainder with missing event" + guildID + ", " + eventName);
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
                final Remainder remainder = build(remainderChannel, optEvent.get(), me, activationDay, activationTime);

                remainders.add(remainder);
                LOGGER.debug("Remainder loaded for event " + remainder.getEvent().getName());
                this.remainderTimer.scheduleAtFixedRate(remainder, remainder.getActivationDate(), TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS));
                LOGGER.debug("Remainder scheluded for activation at " + remainder.getActivationDate().toString());
            }
        }
    }

    /**
     * Get Reminder by with name,day and time
     *
     * @param eventName name the remainder is for
     * @param day remainder day
     * @param time remainder time
     * @param guild guild to search remainders from
     * @return Optional containing the remainder if found
     */
    public Optional<Remainder> getRemainder(String eventName, String day, String time, Guild guild) {
        for (Remainder r : this.remainders) {
            if (!r.getEvent().getName().equals(eventName)) {
                continue;
            }
            if (!r.getDay().toString().equals(day.toUpperCase())) {
                continue;
            }
            if (!r.getTime().toString().equals(time.toUpperCase())) {
                continue;
            }
            if (r.getEvent().getGuild() != guild.getIdLong()) {
                continue;
            }
            return Optional.of(r);
        }
        return Optional.empty();
    }

    /**
     * Get the list of remainders currently active
     *
     * @param guild Guild to get remainder for
     * @return List of remainders
     */
    public List<Remainder> getRemainders(Guild guild) {
        final List<Remainder> guildRemainders = new ArrayList<>();
        for (Remainder r : this.remainders) {
            if (r.getEvent().getGuild() == guild.getIdLong()) {
                guildRemainders.add(r);
            }
        }
        return guildRemainders;
    }

}
