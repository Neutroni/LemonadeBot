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

import eternal.lemonadebot.commands.AdminCommand;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.PermissionUtilities;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.text.Collator;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
public class ReminderCommand extends AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(final Locale locale) {
        return TranslationKey.COMMAND_REMINDER.getTranslation(locale);
    }

    @Override
    public String getDescription(final Locale locale) {
        return TranslationKey.DESCRIPTION_REMINDER.getTranslation(locale);
    }

    @Override
    public String getHelpText(final Locale locale) {
        return TranslationKey.SYNTAX_REMINDER.getTranslation(locale);
    }

    @Override
    public void respond(final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildConf.getLocale();

        final String[] arguments = matcher.getArguments(2);
        if (arguments.length == 0) {
            textChannel.sendMessage(TranslationKey.ERROR_MISSING_OPERATION.getTranslation(locale)).queue();
            return;
        }

        final String action = arguments[0];
        final ActionKey key = translationCache.getActionKey(action);
        switch (key) {
            case CREATE: {
                createReminder(matcher, guildData);
                break;
            }
            case DELETE: {
                deleteReminder(arguments, matcher, guildData);
                break;
            }
            case LIST: {
                listReminders(matcher, guildData);
                break;
            }
            default: {
                textChannel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + arguments[0]).queue();
            }
        }
    }

    private static void createReminder(final CommandMatcher matcher, final GuildDataStore guildData) {
        //reminder create test 17.00 * * * remindertext
        //reminder create test 17.00 * * sunday remindertext
        //reminder create test 17.00 17 9 * remindertext
        //reminder create test 17.00 30 * * remindertext
        final TextChannel channel = matcher.getTextChannel();
        final ReminderManager reminders = guildData.getReminderManager();
        final ConfigManager guildConf = guildData.getConfigManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildConf.getLocale();

        final String[] arguments = matcher.getArguments(6);
        if (arguments.length < 2) {
            channel.sendMessage(TranslationKey.REMINDER_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String reminderName = arguments[1];

        //Parse time of day
        if (arguments.length < 3) {
            channel.sendMessage(TranslationKey.REMINDER_MISSING_TIME.getTranslation(locale)).queue();
            return;
        }
        final String reminderTime = arguments[2];
        final LocalTime timeOfDay;
        try {
            timeOfDay = LocalTime.parse(reminderTime, translationCache.getTimeFormatter());
        } catch (DateTimeParseException e) {
            channel.sendMessageFormat(TranslationKey.REMINDER_UNKNOWN_TIME.getTranslation(locale), reminderTime).queue();
            return;
        }

        //Parse day of month
        if (arguments.length < 4) {
            channel.sendMessage(TranslationKey.REMINDER_MISSING_DAY.getTranslation(locale)).queue();
            return;
        }
        final String reminderDate = arguments[3];
        int dayOfMonth = 0;
        if (!"*".equals(reminderDate)) {
            try {
                dayOfMonth = Integer.parseUnsignedInt(reminderDate);
                if (dayOfMonth < 1 || dayOfMonth > 31) {
                    channel.sendMessageFormat(TranslationKey.REMINDER_DAY_OF_MONTH_OUT_OF_RANGE.getTranslation(locale)).queue();
                    return;
                }
            } catch (NumberFormatException e) {
                channel.sendMessageFormat(TranslationKey.REMINDER_DAY_OF_MONTH_NOT_NUMBER.getTranslation(locale)).queue();
                return;
            }
        }

        //Parse month of year
        if (arguments.length < 5) {
            channel.sendMessage(TranslationKey.REMINDER_MISSING_MONTH.getTranslation(locale)).queue();
            return;
        }
        final String reminderMonth = arguments[4];
        Month monthOfYear = null;
        if (!"*".equals(reminderMonth)) {
            try {
                final int monthNumber = Integer.parseUnsignedInt(reminderMonth);
                monthOfYear = Month.of(monthNumber);
            } catch (NumberFormatException e) {
                channel.sendMessage(TranslationKey.REMINDER_MONTH_NOT_NUMBER.getTranslation(locale)).queue();
                return;
            } catch (DateTimeParseException e) {
                channel.sendMessage(TranslationKey.REMINDER_MONTH_OUT_OF_RANGE.getTranslation(locale)).queue();
                return;
            }
        }

        //Validate monthDay if present
        if (dayOfMonth != 0 && monthOfYear != null) {
            try {
                final MonthDay monthDay = MonthDay.of(monthOfYear, dayOfMonth);
                LOGGER.debug("Found monthDay: {} in reminder creation.", monthDay.toString());
            } catch (DateTimeException e) {
                channel.sendMessage(TranslationKey.REMINDER_INVALID_DATE.getTranslation(locale)).queue();
                return;
            }
        }

        //Parse day of week
        if (arguments.length < 6) {
            channel.sendMessage(TranslationKey.REMINDER_MISSING_DAY_OF_WEEK.getTranslation(locale)).queue();
            return;
        }
        final String reminderDay = arguments[5];
        DayOfWeek dayOfWeek = null;
        if (!"*".equals(reminderDay)) {
            final Collator collator = guildData.getTranslationCache().getCollator();
            for (final DayOfWeek day : DayOfWeek.values()) {
                final String localDayName = day.getDisplayName(TextStyle.FULL_STANDALONE, locale);
                if (collator.equals(localDayName, reminderDay)) {
                    dayOfWeek = day;
                    break;
                }
            }
            //Check if we found the day with given name
            if (dayOfWeek == null) {
                channel.sendMessage(TranslationKey.REMINDER_ERROR_UNKNOWN_DAY.getTranslation(locale)).queue();
                return;
            }
        }

        //Construct reminder activation checker
        final ReminderActivationTime reminderActivationTime = new ReminderActivationTime(timeOfDay, dayOfWeek, dayOfMonth, monthOfYear);

        //Get the reminder message
        if (arguments.length < 7) {
            channel.sendMessage(TranslationKey.REMINDER_MISSING_MESSAGE.getTranslation(locale)).queue();
            return;
        }
        final String messageInput = arguments[6];

        //Construct reminder
        final JDA jda = channel.getJDA();
        final long channelID = channel.getIdLong();
        final long memberID = matcher.getMember().getIdLong();
        final Reminder reminder = new Reminder(jda, guildData, reminderName,
                messageInput, channelID, memberID, reminderActivationTime);

        //Add reminder to database
        try {
            if (!reminders.addReminder(reminder)) {
                channel.sendMessage(TranslationKey.REMINDER_ALREADY_EXISTS.getTranslation(locale)).queue();
                return;
            }
            channel.sendMessageFormat(TranslationKey.REMINDER_CREATE_SUCCESS.getTranslation(locale)).queue();
        } catch (SQLException ex) {
            channel.sendMessage(TranslationKey.REMINDER_SQL_ERROR_ON_CREATE.getTranslation(locale)).queue();
            LOGGER.error("Failure to create reminder: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void deleteReminder(final String[] arguments, final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();

        if (arguments.length < 2) {
            textChannel.sendMessage(TranslationKey.REMINDER_DELETE_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final ReminderManager reminders = guildData.getReminderManager();

        final String reminderName = arguments[1];
        final Optional<Reminder> oldReminder = reminders.getReminder(reminderName);
        if (oldReminder.isEmpty()) {
            textChannel.sendMessageFormat(TranslationKey.REMINDER_NOT_FOUND_NAME.getTranslation(locale), reminderName).queue();
            return;
        }
        final Reminder reminder = oldReminder.get();

        //Check if user has permission to remove the event
        final Member sender = matcher.getMember();
        textChannel.getGuild().retrieveMemberById(reminder.getAuthor()).submit().whenComplete((Member reminderOwner, Throwable e) -> {
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, reminderOwner);
            if (!hasPermission) {
                textChannel.sendMessage(TranslationKey.REMINDER_DELETE_MISSING_PERMISSION.getTranslation(locale)).queue();
                return;
            }

            try {
                reminders.deleteReminder(reminder);
                textChannel.sendMessage(TranslationKey.REMINDER_DELETE_SUCCESS.getTranslation(locale)).queue();
            } catch (SQLException ex) {
                textChannel.sendMessage(TranslationKey.REMINDER_SQL_ERROR_ON_DELETE.getTranslation(locale)).queue();
                LOGGER.error("Failure to delete reminder: {}", ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });
    }

    private static void listReminders(final CommandMatcher matcher, final GuildDataStore guildData) {
        final Locale locale = guildData.getConfigManager().getLocale();

        //Construct the embed
        final String header = TranslationKey.HEADER_REMINDERS.getTranslation(locale);
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(header);

        //Initialize all the futures
        final Collection<Reminder> ev = guildData.getReminderManager().getReminders();
        final List<CompletableFuture<String>> futures = new ArrayList<>(ev.size());
        ev.forEach((Reminder reminder) -> {
            futures.add(reminder.toListElement(locale));
        });

        //After all the futures all initialized start waiting for results
        final StringBuilder contentBuilder = new StringBuilder();
        futures.forEach(desc -> {
            contentBuilder.append(desc.join());
        });
        if (ev.isEmpty()) {
            contentBuilder.append(TranslationKey.REMINDER_NO_REMINDERS.getTranslation(locale));
        }
        eb.setDescription(contentBuilder);

        final TextChannel textChannel = matcher.getTextChannel();
        textChannel.sendMessage(eb.build()).queue();
    }

}
