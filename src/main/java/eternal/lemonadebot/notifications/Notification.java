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

import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.messageparsing.SimpleMessageMatcher;
import eternal.lemonadebot.translation.TranslationCache;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class that stores a notification, used to send a message at specified time
 *
 * @author Neutroni
 */
class Notification extends CustomCommand implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final JDA jda;
    private final long channelID;
    private final GuildDataStore guildData;
    private volatile ScheduledFuture<?> future;
    private final Instant activationTime;

    /**
     * Constructor
     *
     * @param jda JDA to use for getting channel
     * @param guildData Datastore for the guild
     * @param name Name of the notification
     * @param channelID ID of the channel the message should be sent in
     * @param author Author of the notification
     * @param input Input string to either send or execute if it is a command
     * @param activationTime Instant the notification happens
     */
    Notification(final JDA jda, final GuildDataStore guildData, final String name, final String input,
            final long channelID, final long author, final Instant activationTime) {
        super(name, input, author);
        this.jda = jda;
        this.guildData = guildData;
        this.channelID = channelID;
        this.activationTime = activationTime;
        this.future = null;
    }

    /**
     * Run this TimerTask
     */
    @Override
    public void run() {
        LOGGER.debug("Notification started: {}", getName());
        //Make sure JDA is loaded
        try {
            //This might delay activation if connecting takes extremely long
            this.jda.awaitReady();
        } catch (InterruptedException e) {
            LOGGER.error("JDA loading interrupted while trying to run a notification: {}", e.getMessage());
            LOGGER.trace("Stack trace: ", e);
            return;
        }

        //Check notification channel can be found
        final TextChannel channel = this.jda.getTextChannelById(this.channelID);
        if (channel == null) {
            deleteDueToMissingChannel();
            LOGGER.debug("Notification {}, did not activate channel could not be found", getName());
            return;
        }

        //Check notification author can be found
        channel.getGuild().retrieveMemberById(getAuthor()).queue((Member member) -> {
            //Success
            final CommandMatcher matcher = new SimpleMessageMatcher(member, channel);
            final DatabaseManager db = this.guildData.getDataBaseManager();
            final TranslationCache translation = db.getTranslationCache(channel.getGuild());
            final CommandContext context = new CommandContext(matcher, guildData, translation);
            respond(context);
            LOGGER.debug("Notification: {} successfully activated on channel: {}", getName(), channel.getName());
        }, (Throwable t) -> {
            //Failure
            deleteDueToMissingOwner();
            LOGGER.debug("Notification {}, did not activate owner could not be found", getName());
        });
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof Notification) {
            final Notification otherNotification = (Notification) other;
            return getName().equals(otherNotification.getName());
        }
        return false;
    }

    /**
     * Get the ID for text channel this notification will appear in
     *
     * @return TextChannel
     */
    long getChannel() {
        return this.channelID;
    }

    /**
     * Get time this event activates at
     *
     * @return LocalTime
     */
    Instant getTime() {
        return this.activationTime;
    }

    CompletableFuture<String> toListElement(final TranslationCache translation) {
        final CompletableFuture<String> result = new CompletableFuture<>();
        final ResourceBundle locale = translation.getResourceBundle();

        //Get the channel for notifications
        final TextChannel channel = this.jda.getTextChannelById(this.channelID);
        if (channel == null) {
            deleteDueToMissingChannel();
            final String response = locale.getString("NOTIFICATION_CHANNEL_MISSING");
            result.complete(String.format(response, getName()));
            return result;
        }

        final DateTimeFormatter timeFormatter = translation.getTimeFormatter();
        final String timeString = timeFormatter.format(this.activationTime);
        final String channelName = channel.getAsMention();
        final String template = locale.getString("NOTIFICATION_LIST_ELEMENT_TEMPLATE");
        this.jda.retrieveUserById(getAuthor()).queue((User notificationOwner) -> {
            //Found notifications owner
            final String ownerName = notificationOwner.getAsMention();
            final String response = String.format(template, getName(), getTemplate(), timeString, channelName, ownerName);
            result.complete(response);
        }, (Throwable t) -> {
            //Notification owner missing
            deleteDueToMissingOwner();
            final String response = locale.getString("NOTIFICATION_USER_MISSING");
            result.complete(String.format(response, getName()));
        });
        return result;
    }

    /**
     * Schedule this notification with the provided ExecutorService
     *
     * @param notificationTimer ScheduledExecutorService
     */
    boolean scheduleWith(final ScheduledExecutorService notificationTimer) {
        final Duration duration = Duration.between(Instant.now(), this.activationTime);
        //Check that the time is in the future
        if (duration.isNegative()) {
            return false;
        }
        final long millisecondsToActivation = duration.toMillis();
        this.future = notificationTimer.schedule(this, millisecondsToActivation, TimeUnit.MILLISECONDS);
        return true;
    }

    /**
     * Cancel the task, task will finnish if it is already running
     */
    void cancel() {
        if (this.future != null) {
            this.future.cancel(false);
        }
    }

    private void deleteDueToMissingOwner() {
        LOGGER.info("Deleting notification: {} with missing author, member id: {}", getName(), getAuthor());
        try {
            this.guildData.getNotificationManager().deleteNotification(this);
            LOGGER.info("Notification with missing author deleted");
        } catch (SQLException ex) {
            LOGGER.error("Error removing notification with missing author: {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
        }
    }

    private void deleteDueToMissingChannel() {
        LOGGER.info("Deleting notification: {} for textChannel that does not exist, channel id: {}", getName(), this.channelID);
        try {
            this.guildData.getNotificationManager().deleteNotification(this);
            LOGGER.info("Deleted notification with missing channel: {}", this.channelID);
        } catch (SQLException ex) {
            LOGGER.error("Error removing notification with missing channel: {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
        }
    }

}
