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

import eternal.lemonadebot.dataobjects.Reminder;
import eternal.lemonadebot.dataobjects.ReminderActivationTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.dv8tion.jda.api.JDA;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class ReminderManager implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Connection conn;
    private final JDA jda;
    private final long guildID;

    private final Map<String, Reminder> reminders = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reminderTimer = Executors.newSingleThreadScheduledExecutor();
    private final GuildDataStore guildData;

    /**
     * Constructor
     *
     * @param conn database connectionF
     * @param guildData EventManager to fetch events from
     * @param jda JDA to pass to reminders
     */
    ReminderManager(Connection conn, JDA jda, GuildDataStore guildData, long guild) {
        this.conn = conn;
        this.jda = jda;
        this.guildData = guildData;
        this.guildID = guild;
        loadReminders();
    }

    /**
     * Add reminder to database
     *
     * @param reminder event to add
     * @return true if event was added
     * @throws SQLException If database connection failed
     */
    public boolean addReminder(final Reminder reminder) throws SQLException {
        LOGGER.debug("Storing reminder: {}", reminder.getName());
        final Reminder oldReminder = this.reminders.putIfAbsent(reminder.getName(), reminder);

        //If timer was just added schedule the activation
        if (oldReminder == null) {
            reminder.scheduleWith(this.reminderTimer);
            LOGGER.debug("Reminder: {} scheluded with ScheduledExecutorService", reminder.getName());
        }

        //Add to database
        final String query = "INSERT INTO Reminders(guild,name,message,author,channel,time,dayOfWeek,dayOfMonth,monthOfYear) VALUES(?,?,?,?,?,?,?,?,?);";
        final ReminderActivationTime activationTime = reminder.getTime();
        final String reminderName = reminder.getName();
        final String reminderMessage = reminder.getTemplate();
        final long authorID = reminder.getAuthor();
        final long channelID = reminder.getChannel();
        final long secondsOfDay = activationTime.getTime().toSecondOfDay();
        final DayOfWeek optDay = activationTime.getDayOfWeek();
        final long dayOfWeek;
        if (optDay == null) {
            dayOfWeek = 0;
        } else {
            dayOfWeek = optDay.getValue();
        }
        final int dayOfMonth = activationTime.getDayOfMonth();
        final Month month = activationTime.getMonthOfYear();
        final long monthOfYear;
        if (month == null) {
            monthOfYear = 0;
        } else {
            monthOfYear = month.getValue();
        }
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, reminderName);
            ps.setString(3, reminderMessage);
            ps.setLong(4, authorID);
            ps.setLong(5, channelID);
            ps.setLong(6, secondsOfDay);
            ps.setLong(7, dayOfWeek);
            ps.setLong(8, dayOfMonth);
            ps.setLong(9, monthOfYear);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     *
     * @param reminder reminder to remove
     * @return true if reminder was removed succesfully
     * @throws SQLException if database connection failed
     */
    public boolean deleteReminder(final Reminder reminder) throws SQLException {
        reminder.cancel();
        this.reminders.remove(reminder.getName());

        //Remove from database
        final String query = "DELETE FROM Reminders Where guild = ? AND name = ?;";
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, reminder.getName());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get Reminder by name
     *
     * @param name name of the reminder
     * @return Optional containing the reminder if found
     */
    public Optional<Reminder> getReminder(String name) {
        return Optional.ofNullable(this.reminders.get(name));
    }

    /**
     * Get the list of reminders currently active
     *
     * @return List of reminders
     */
    public Collection<Reminder> getReminders() {
        return Collections.unmodifiableCollection(this.reminders.values());
    }

    /**
     * Load reminders from database
     *
     * @param jda JDA instance to use for initializing reminder timers
     * @throws SQLException If database connection failed
     */
    private void loadReminders() {
        LOGGER.debug("Started loading reminders for guild: {} from database", this.guildID);
        final String query = "SELECT name,message,author,channel,time,dayOfWeek,dayOfMonth,monthOfYear FROM Reminders WHERE guild = ?;";
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String reminderName = rs.getString("name");
                    final String reminderMessage = rs.getString("message");
                    final long reminderAuthor = rs.getLong("author");
                    final long reminderChannel = rs.getLong("channel");

                    //Load time reminder activates on
                    final long reminderTime = rs.getLong("time");
                    final LocalTime activationTime;
                    try {
                        activationTime = LocalTime.ofSecondOfDay(reminderTime);
                    } catch (DateTimeException e) {
                        LOGGER.error("Malformed time for reminder: {} in database: {}", reminderName, reminderTime);
                        return;
                    }

                    //Load dayOfWeek if present
                    final int dayOfWeek = rs.getInt("dayOfWeek");
                    final DayOfWeek activationDay;
                    if (dayOfWeek == 0) {
                        activationDay = null;
                    } else {
                        try {
                            activationDay = DayOfWeek.of(dayOfWeek);
                        } catch (DateTimeException e) {
                            LOGGER.error("Malformed dayOfWeek for reminder: {} in database: {}", reminderName, dayOfWeek);
                            continue;
                        }
                    }

                    //Load dayOfMonth
                    final int dayOfMonth = rs.getInt("dayOfMonth");
                    if (dayOfMonth > 31) {
                        LOGGER.error("Malformed dayOfMonth in database: {}", dayOfMonth);
                        continue;
                    }

                    //Load monthOfYear if present
                    final int monthOfYear = rs.getInt("monthOfYear");
                    final Month reminderMonth;
                    if (monthOfYear == 0) {
                        reminderMonth = null;
                    } else {
                        try {
                            reminderMonth = Month.of(monthOfYear);
                        } catch (DateTimeParseException e) {
                            LOGGER.error("Malformed monthday in database: {}", monthOfYear);
                            continue;
                        }
                    }

                    final ReminderActivationTime reminderActivationTime = new ReminderActivationTime(activationTime, activationDay, dayOfMonth, reminderMonth);

                    //Construct and add to list of reminders
                    final Reminder reminder = new Reminder(this.jda, this.guildData,
                            reminderName, reminderMessage, reminderChannel, reminderAuthor, reminderActivationTime);
                    reminders.put(reminder.getName(), reminder);
                    LOGGER.debug("Reminder succesfully loaded: {}", reminder.getName());

                    reminder.scheduleWith(this.reminderTimer);
                    LOGGER.debug("Reminder: {} scheluded with ScheduledExecutorService", reminder.getName());
                }
            }
            LOGGER.debug("Reminders for guild: {} loaded succesfully.", this.guildID);
        } catch (SQLException e) {
            LOGGER.error("Loading reminders from database failed");
            LOGGER.warn(e.getMessage());
            LOGGER.trace(e);
        }
    }

    @Override
    public void close() {
        //Cancel all scheduled reminders
        this.reminders.values().forEach(Reminder::cancel);
        this.reminderTimer.shutdown();
    }

}
