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

import eternal.lemonadebot.customcommands.Remainder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
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
    private final long guildID;

    private final Set<Remainder> remainders = Collections.synchronizedSet(new HashSet<>());
    private final Timer remainderTimer = new Timer();
    private final GuildDataStore guildData;

    /**
     * Constructor
     *
     * @param conn database connectionF
     * @param guildData EventManager to fetch events from
     * @param jda JDA to pass to remainders
     */
    RemainderManager(Connection conn, JDA jda, GuildDataStore guildData, long guild) {
        this.conn = conn;
        this.jda = jda;
        this.guildData = guildData;
        this.guildID = guild;
        loadRemainders();
    }

    /**
     * Add remainder to database
     *
     * @param remainder event to add
     * @return true if event was added
     * @throws SQLException If database connection failed
     */
    public boolean addRemainder(final Remainder remainder) throws SQLException {
        LOGGER.debug("Storing remainder: " + remainder.getName());
        final boolean added = this.remainders.add(remainder);

        //If timer was just added schedule the activation
        if (added) {
            this.remainderTimer.scheduleAtFixedRate(remainder, remainder.getActivationDate(), TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS));
            LOGGER.debug("Remainder scheluded for activation at " + remainder.getActivationDate().toString());
        }

        //Add to database
        final String query = "INSERT INTO Remainders(guild,author,channel,name,message,day,time) VALUES(?,?,?,?,?,?,?);";
        final String remainderName = remainder.getName();
        final String remainderMessage = remainder.getMessage();
        final Optional<DayOfWeek> optDay = remainder.getDay();
        final String remainderDay;
        if(optDay.isPresent()){
            remainderDay = optDay.get().name();
        } else {
            remainderDay = "EVERYDAY";
        }
        final String remainderTime = remainder.getTime().toString();
        final long channelID = remainder.getChannel();
        final long authorID = remainder.getAuthor();
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setLong(2, authorID);
            ps.setLong(3, channelID);
            ps.setString(4, remainderName);
            ps.setString(5, remainderMessage);
            ps.setString(6, remainderDay);
            ps.setString(7, remainderTime);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     *
     * @param remainder remainder to remove
     * @return true if remainder was removed succesfully
     * @throws SQLException if database connection failed
     */
    public boolean deleteRemainder(final Remainder remainder) throws SQLException {
        remainder.cancel();
        this.remainders.remove(remainder);

        //Remove from database
        final String query = "DELETE FROM Remainders Where guild = ? AND name = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, remainder.getName());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get Reminder by with name,day and time
     *
     * @param name name of the remainder
     * @return Optional containing the remainder if found
     */
    public Optional<Remainder> getRemainder(String name) {
        for (Remainder r : this.remainders) {
            if (!r.getName().equals(name)) {
                continue;
            }
            return Optional.of(r);
        }
        return Optional.empty();
    }

    /**
     * Get the list of remainders currently active
     *
     * @return List of remainders
     */
    public List<Remainder> getRemainders() {
        return new ArrayList<>(this.remainders);
    }

    /**
     * Load remainders from database
     *
     * @param jda JDA instance to use for initializing remainder timers
     * @throws SQLException If database connection failed
     */
    private void loadRemainders() {
        LOGGER.debug("Started loading remainders from database");
        final String query = "SELECT author,channel,name,message,day,time FROM Remainders WHERE guild = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final long remainderAuthor = rs.getLong("author");
                    final long remainderChannel = rs.getLong("channel");
                    final String remainderName = rs.getString("name");
                    final String remainderMessage = rs.getString("message");
                    final String remainderDay = rs.getString("day");
                    final DayOfWeek activationDay;
                    if ("EVERYDAY".equals(remainderDay)) {
                        activationDay = null;
                    } else {
                        try {
                            activationDay = DayOfWeek.valueOf(remainderDay);
                        } catch (IllegalArgumentException e) {
                            LOGGER.error("Malformed weekday in database: " + remainderDay);
                            continue;
                        }
                    }
                    final String reminderTime = rs.getString("time");
                    final LocalTime activationTime;
                    try {
                        activationTime = LocalTime.parse(reminderTime);
                    } catch (DateTimeParseException e) {
                        LOGGER.error("Malformed time for reminder in database: " + reminderTime);
                        return;
                    }
                    
                    //Construct and add to list of remainders
                    final Remainder remainder = new Remainder(this.jda, this.guildData,
                            remainderName, remainderChannel, remainderAuthor, remainderMessage, activationDay, activationTime);
                    remainders.add(remainder);
                    LOGGER.debug("Remainder loaded" + remainder.getName());
                    
                    if(activationDay == null){
                        this.remainderTimer.scheduleAtFixedRate(remainder, remainder.getActivationDate(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
                    } else {
                        this.remainderTimer.scheduleAtFixedRate(remainder, remainder.getActivationDate(), TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS));
                    }
                    
                    LOGGER.debug("Remainder scheluded for activation at " + remainder.getActivationDate().toString());
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading remainders from database failed");
            LOGGER.warn(e.getMessage());
            LOGGER.trace(e);
        }
    }

}
