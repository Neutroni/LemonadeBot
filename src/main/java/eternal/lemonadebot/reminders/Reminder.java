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
package eternal.lemonadebot.reminders;

import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.RuntimeStorage;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.messageparsing.SimpleMessageMatcher;
import eternal.lemonadebot.translation.TranslationCache;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class that stores a reminder, used to send a message at specified time
 *
 * @author Neutroni
 */
class Reminder extends CustomCommand implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final JDA jda;
    private final long channelID;
    private final GuildDataStore guildData;
    private final ReminderManager remiderManager;
    private volatile ScheduledFuture<?> future;
    private final ReminderActivationTime activationTime;

    /**
     * Constructor
     *
     * @param jda JDA to use for getting channel
     * @param guildData Datastore for the guild
     * @param name Name of the reminder
     * @param channelID ID of the channel the message should be sent in
     * @param author Author of the reminder
     * @param input Input string to either send or execute if it is a command
     * @param activationTime Weekday the reminder happens
     */
    Reminder(final JDA jda, final GuildDataStore guildData, final ReminderManager rm, final String name, final String input,
            final long channelID, final long author, final ReminderActivationTime activationTime) {
        super(name, input, author);
        this.jda = jda;
        this.guildData = guildData;
        this.remiderManager = rm;
        this.channelID = channelID;
        this.activationTime = activationTime;
        this.future = null;
    }

    /**
     * Run this TimerTask
     */
    @Override
    public void run() {
        LOGGER.debug("Reminder check started: {}", getName());
        //Make sure JDA is loaded
        try {
            //This might delay activation if connecting takes extremely long
            this.jda.awaitReady();
        } catch (InterruptedException e) {
            LOGGER.error("JDA loading interrupted while trying to run a reminder: {}", e.getMessage());
            LOGGER.trace("Stack trace: ", e);
            return;
        }

        //Check that reminder should activate today
        final ZoneId timeZone = this.guildData.getConfigManager().getZoneId();
        if (!this.activationTime.shouldActivate(timeZone)) {
            LOGGER.debug("Reminder {}, did not activate not the correct day", getName());
            return;
        }

        //Check reminder channel can be found
        final TextChannel channel = this.jda.getTextChannelById(this.channelID);
        if (channel == null) {
            deleteDueToMissingChannel();
            LOGGER.debug("Reminder {}, did not activate channel could not be found", getName());
            return;
        }

        //Check reminder author can be found
        channel.getGuild().retrieveMemberById(getAuthor()).queue((Member member) -> {
            //Success
            final CommandMatcher matcher = new SimpleMessageMatcher(member, channel);
            final Guild guild = channel.getGuild();
            final RuntimeStorage db = this.guildData.getRuntimeStorage();
            final TranslationCache translation = db.getTranslationCache(guild);
            final CommandContext context = new CommandContext(matcher, guildData, translation);
            run(context,true);
            LOGGER.debug("Reminder: {} successfully activated on channel: {}", getName(), channel.getName());
        }, (Throwable t) -> {
            //Failure
            deleteDueToMissingOwner();
            LOGGER.debug("Reminder {}, did not activate owner could not be found", getName());
        });
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof Reminder) {
            final Reminder otherReminder = (Reminder) other;
            return getName().equals(otherReminder.getName());
        }
        return false;
    }

    /**
     * Get the ID for text channel this reminder will appear in
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
    ReminderActivationTime getTime() {
        return this.activationTime;
    }

    CompletableFuture<String> toListElement(final TranslationCache translationCache) {
        final CompletableFuture<String> result = new CompletableFuture<>();
        final ResourceBundle locale = translationCache.getResourceBundle();

        //Get the channel for reminder
        final TextChannel channel = this.jda.getTextChannelById(this.channelID);
        if (channel == null) {
            deleteDueToMissingChannel();
            final String response = locale.getString("REMINDER_CHANNEL_MISSING");
            result.complete(String.format(response, getName()));
            return result;
        }

        final DateTimeFormatter timeFormatter = translationCache.getTimeFormatter();
        final String cronString = this.activationTime.getCronString(locale.getLocale(), timeFormatter);
        final String channelName = channel.getAsMention();
        final String template = locale.getString("REMINDER_LIST_ELEMENT_TEMPLATE");
        this.jda.retrieveUserById(getAuthor()).queue((User reminderOwner) -> {
            //Found reminder owner
            final String ownerName = reminderOwner.getAsMention();
            final String response = String.format(template, getName(), getTemplate(), cronString, channelName, ownerName);
            result.complete(response);
        }, (Throwable t) -> {
            //Reminder owner missing
            deleteDueToMissingOwner();
            final String response = locale.getString("REMINDER_USER_MISSING");
            result.complete(String.format(response, getName()));
        });
        return result;
    }

    /**
     * Schedule this reminder with the provided ExecutorService
     *
     * @param reminderTimer ScheduledExecutorService
     */
    void scheduleWith(final ScheduledExecutorService reminderTimer) {
        final ZoneId timeZone = this.guildData.getConfigManager().getZoneId();
        final Duration duration = this.activationTime.getTimeToActivation(timeZone);
        final long millisecondsToActivation = duration.toMillis();
        final long reminderInterval = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
        this.future = reminderTimer.scheduleAtFixedRate(this, millisecondsToActivation, reminderInterval, TimeUnit.MILLISECONDS);
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
        LOGGER.info("Deleting reminder: {} with missing author, member id: {}", getName(), getAuthor());
        try {
            this.remiderManager.deleteReminder(this);
            LOGGER.info("Reminder with missing author deleted");
        } catch (SQLException ex) {
            LOGGER.error("Error removing reminder with missing author: {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
        }
    }

    private void deleteDueToMissingChannel() {
        LOGGER.info("Deleting reminder: {} for textChannel that does not exist, channel id: {}", getName(), this.channelID);
        try {
            this.remiderManager.deleteReminder(this);
            LOGGER.info("Deleted reminder with missing channel: {}", this.channelID);
        } catch (SQLException ex) {
            LOGGER.error("Error removing reminder with missing channel: {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
        }
    }

}
