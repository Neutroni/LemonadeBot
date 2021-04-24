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
package eternal.lemonadebot.notifications;

import eternal.lemonadebot.database.GuildDataStore;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.sql.DataSource;
import net.dv8tion.jda.api.JDA;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class NotificationManager implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DataSource dataSource;
    private final JDA jda;
    private final long guildID;

    private final Map<String, Notification> notifications = new ConcurrentHashMap<>();
    private final ScheduledExecutorService notificastionTimer = Executors.newSingleThreadScheduledExecutor();
    private final GuildDataStore guildData;

    /**
     * Constructor
     *
     * @param ds DataSource to get connection from
     * @param guildData GuildData to pass to notifications
     * @param jda JDA to pass to notifications
     */
    public NotificationManager(final DataSource ds, final JDA jda, final GuildDataStore guildData) {
        this.dataSource = ds;
        this.jda = jda;
        this.guildData = guildData;
        this.guildID = guildData.getGuildID();
        loadNotifications();
    }

    @Override
    public void close() {
        //Cancel all scheduled notifications
        this.notifications.values().forEach(Notification::cancel);
        this.notificastionTimer.shutdown();
    }

    /**
     * Add notification to database
     *
     * @param notification event to add
     * @return true if event was added
     * @throws SQLException If database connection failed
     */
    boolean addNotification(final Notification notification) throws SQLException {
        //Check that notification is not in the past
        if (notification.getTime().isBefore(Instant.now())) {
            return false;
        }
        LOGGER.debug("Storing notification: {}", notification.getName());
        final Notification oldNotification = this.notifications.putIfAbsent(notification.getName(), notification);

        //If timer was just added schedule the activation
        if (oldNotification == null) {
            boolean scheduled = notification.scheduleWith(this.notificastionTimer);
            if (!scheduled) {
                return false;
            }
            LOGGER.debug("Notification: {} scheduled with ScheduledExecutorService", notification.getName());
        }

        //Add to database
        final String query = "INSERT OR IGNORE INTO Notifications(guild,name,message,author,channel,time) VALUES(?,?,?,?,?,?);";
        final String notificationName = notification.getName();
        final String notificationMessage = notification.getTemplate();
        final long authorID = notification.getAuthor();
        final long channelID = notification.getChannel();
        final long activationTime = notification.getTime().toEpochMilli();

        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, notificationName);
            ps.setString(3, notificationMessage);
            ps.setLong(4, authorID);
            ps.setLong(5, channelID);
            ps.setLong(6, activationTime);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     *
     * @param notification notification to remove
     * @return true if notification was removed successfully
     * @throws SQLException if database connection failed
     */
    boolean deleteNotification(final Notification notification) throws SQLException {
        notification.cancel();
        this.notifications.remove(notification.getName());

        //Remove from database
        final String query = "DELETE FROM Notifications Where guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, notification.getName());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get Notification by name
     *
     * @param name name of the notification
     * @return Optional containing the notification if found
     */
    Optional<Notification> getNotification(final String name) {
        return Optional.ofNullable(this.notifications.get(name));
    }

    /**
     * Get the list of notifications currently active
     *
     * @return List of notifications
     */
    Collection<Notification> getNotifications() {
        return Collections.unmodifiableCollection(this.notifications.values());
    }

    /**
     * Load notifications from database
     */
    private void loadNotifications() {
        LOGGER.debug("Started loading notifications for guild: {} from database", this.guildID);
        final String query = "SELECT name,message,author,channel,time FROM Notifications WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String notificationName = rs.getString("name");
                    final String notificationMessage = rs.getString("message");
                    final long notificationAuthor = rs.getLong("author");
                    final long notificationChannel = rs.getLong("channel");

                    //Load time notification activates on
                    final long notificationsTime = rs.getLong("time");
                    final Instant notificationActivationTime = Instant.ofEpochMilli(notificationsTime);

                    //Construct and add to list of notifications
                    final Notification notification = new Notification(this.jda, this.guildData,
                            notificationName, notificationMessage, notificationChannel, notificationAuthor, notificationActivationTime);
                    this.notifications.put(notification.getName(), notification);
                    LOGGER.debug("Notification successfully loaded: {}", notification.getName());

                    notification.scheduleWith(this.notificastionTimer);
                    LOGGER.debug("Notification: {} scheduled with ScheduledExecutorService", notification.getName());
                }
            }
            LOGGER.debug("Notifications for guild: {} loaded successfully.", this.guildID);
        } catch (SQLException e) {
            LOGGER.error("Loading notifications from database failed");
            LOGGER.warn(e.getMessage());
            LOGGER.trace(e);
        }
    }

}
