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

import eternal.lemonadebot.commands.AdminCommand;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.PermissionUtilities;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command used to manage reminders
 *
 * @author Neutroni
 */
public class NotificationCommand extends AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(final ResourceBundle locale) {
        return locale.getString("COMMAND_NOTIFY");
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_NOTIFY");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        return locale.getString("SYNTAX_NOTIFY");
    }

    @Override
    public void respond(final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final ResourceBundle locale = translationCache.getResourceBundle();

        final String[] arguments = matcher.getArguments(2);
        if (arguments.length == 0) {
            textChannel.sendMessage(locale.getString("ERROR_MISSING_OPERATION")).queue();
            return;
        }

        final String action = arguments[0];
        final ActionKey key = translationCache.getActionKey(action);
        switch (key) {
            case CREATE: {
                createNotification(matcher, guildData);
                break;
            }
            case DELETE: {
                deleteNotification(arguments, matcher, guildData);
                break;
            }
            case LIST: {
                listNotifications(matcher, guildData);
                break;
            }
            default: {
                textChannel.sendMessage(locale.getString("ERROR_UNKNOWN_OPERATION") + arguments[0]).queue();
            }
        }
    }

    private static void createNotification(final CommandMatcher matcher, final GuildDataStore guildData) {
        //notification create 17 hours text
        final TextChannel channel = matcher.getTextChannel();
        final Member member = matcher.getMember();
        final NotificationManager notifications = guildData.getNotificationManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final ResourceBundle locale = translationCache.getResourceBundle();

        final String[] arguments = matcher.getArguments(5);

        //Parse time amount to acticvation
        if (arguments.length < 2) {
            channel.sendMessage(locale.getString("NOTIFY_MISSING_TIME")).queue();
            return;
        }
        final String notificationTime = arguments[1];
        final Long timeAmount;
        try {
            timeAmount = Long.parseUnsignedLong(notificationTime);
        } catch (NumberFormatException e) {
            channel.sendMessageFormat(locale.getString("NOTIFY_UNKNOWN_TIME"), notificationTime).queue();
            return;
        }

        //Parse time unit
        if (arguments.length < 3) {
            channel.sendMessage(locale.getString("NOTIFY_MISSING_UNIT")).queue();
            return;
        }
        final String notificationUnit = arguments[2];
        final Optional<ChronoUnit> optUnit = translationCache.getChronoUnit(notificationUnit);
        if (optUnit.isEmpty()) {
            channel.sendMessageFormat(locale.getString("NOTIFY_UNKNOWN_UNIT")).queue();
            return;
        }
        final ChronoUnit timeUnit = optUnit.get();

        //Construct instant for notification actication time
        final Instant notificationActivationTime = Instant.now().plus(Duration.of(timeAmount, timeUnit));

        //Get the notification message
        if (arguments.length < 4) {
            channel.sendMessage(locale.getString("NOTIFY_MISSING_MESSAGE")).queue();
            return;
        }
        final String messageInput = arguments[3];
        final String timeStringFormatted = translationCache.getTimeFormatter().format(notificationActivationTime);
        final String notificationName = member.getUser().getAsTag() + '-' + timeStringFormatted;

        //Construct notification
        final JDA jda = channel.getJDA();
        final long channelID = channel.getIdLong();
        final long memberID = matcher.getMember().getIdLong();
        final Notification notification = new Notification(jda, guildData, notificationName,
                messageInput, channelID, memberID, notificationActivationTime);

        //Add notification to database
        try {
            if (!notifications.addNotification(notification)) {
                channel.sendMessage(locale.getString("NOTIFICATION_IN_PAST")).queue();
                return;
            }
            channel.sendMessageFormat(locale.getString("NOTIFICATION_CREATE_SUCCESS")).queue();
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("NOTIFICATION_SQL_ERROR_ON_CREATE")).queue();
            LOGGER.error("Failure to create notification: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void deleteNotification(final String[] arguments, final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = guildData.getTranslationCache().getResourceBundle();

        if (arguments.length < 2) {
            textChannel.sendMessage(locale.getString("NOTIFICATION_DELETE_MISSING_NAME")).queue();
            return;
        }
        final NotificationManager notifications = guildData.getNotificationManager();

        final String notificationName = arguments[1];
        final Optional<Notification> oldNotification = notifications.getNotification(notificationName);
        if (oldNotification.isEmpty()) {
            textChannel.sendMessageFormat(locale.getString("NOTIFICATION_NOT_FOUND_NAME"), notificationName).queue();
            return;
        }
        final Notification reminder = oldNotification.get();

        //Check if user has permission to remove the event
        final Member sender = matcher.getMember();
        textChannel.getGuild().retrieveMemberById(reminder.getAuthor()).submit().whenComplete((Member reminderOwner, Throwable e) -> {
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, reminderOwner);
            if (!hasPermission) {
                textChannel.sendMessage(locale.getString("NOTIFICATION_DELETE_MISSING_PERMISSION")).queue();
                return;
            }

            try {
                notifications.deleteNotification(reminder);
                textChannel.sendMessage(locale.getString("NOTIFICATION_DELETE_SUCCESS")).queue();
            } catch (SQLException ex) {
                textChannel.sendMessage(locale.getString("NOTIFICATION_SQL_ERROR_ON_DELETE")).queue();
                LOGGER.error("Failure to delete reminder: {}", ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });
    }

    private static void listNotifications(final CommandMatcher matcher, final GuildDataStore guildData) {
        final ResourceBundle locale = guildData.getTranslationCache().getResourceBundle();
        //Only list notifications of the user
        final Member user = matcher.getMember();

        //Construct the embed
        final String header = locale.getString("HEADER_NOTIFICATIONS");
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(header);

        //Initialize all the futures
        final Collection<Notification> ev = guildData.getNotificationManager().getNotifications();
        final List<CompletableFuture<String>> futures = new ArrayList<>(ev.size());
        ev.forEach((Notification reminder) -> {
            if (reminder.getAuthor() == user.getIdLong()) {
                futures.add(reminder.toListElement(locale));
            }
        });

        //After all the futures all initialized start waiting for results
        final StringBuilder contentBuilder = new StringBuilder();
        futures.forEach(desc -> {
            contentBuilder.append(desc.join());
        });
        if (futures.isEmpty()) {
            contentBuilder.append(locale.getString("NOTIFICATION_NO_NOTIFICATIONS"));
        }
        eb.setDescription(contentBuilder);

        final TextChannel textChannel = matcher.getTextChannel();
        textChannel.sendMessage(eb.build()).queue();
    }

}
