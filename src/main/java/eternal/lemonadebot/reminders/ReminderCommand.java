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
import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.RuntimeStorage;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.PermissionUtilities;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
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
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
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
    private final DatabaseManager dataBase;
    private final Map<Long, ReminderManager> managers;

    /**
     * Constrocutor
     *
     * @param db Database connection
     */
    public ReminderCommand(DatabaseManager db) {
        this.dataBase = db;
        this.managers = new ConcurrentHashMap<>();
    }

    @Override
    public void initialize(final List<Guild> guilds, final RuntimeStorage rs) {
        guilds.forEach(guild -> {
            getReminderManager(guild, rs.getGuildData(guild));
        });
    }

    @Override
    public void close() {
        this.managers.values().forEach((ReminderManager t) -> {
            t.close();
        });
    }

    private ReminderManager getReminderManager(final Guild guild, final GuildDataStore guildData) {
        return this.managers.computeIfAbsent(guild.getIdLong(), (Long t) -> {
            final ReminderManager reminderManager = new ReminderManager(this.dataBase.getDataSource(), t);
            reminderManager.loadReminders(guild.getJDA(), guildData);
            return reminderManager;
        });
    }

    @Override
    public String getCommand(final ResourceBundle locale) {
        return locale.getString("COMMAND_REMINDER");
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_REMINDER");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        return locale.getString("SYNTAX_REMINDER");
    }

    @Override
    public void respond(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel textChannel = matcher.getTextChannel();
        final TranslationCache translationCache = context.getTranslation();
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
                createReminder(context);
                break;
            }
            case DELETE: {
                deleteReminder(arguments, context);
                break;
            }
            case LIST: {
                listReminders(context);
                break;
            }
            default: {
                textChannel.sendMessage(locale.getString("ERROR_UNKNOWN_OPERATION") + arguments[0]).queue();
            }
        }
    }

    private void createReminder(final CommandContext context) {
        //reminder create test 17.00 * * * reminder text
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final GuildDataStore guildData = context.getGuildData();
        final Guild guild = matcher.getGuild();
        final ReminderManager reminders = getReminderManager(guild, guildData);
        final TranslationCache translationCache = context.getTranslation();
        final ResourceBundle locale = translationCache.getResourceBundle();

        final String[] arguments = matcher.getArguments(6);
        if (arguments.length < 2) {
            channel.sendMessage(locale.getString("REMINDER_MISSING_NAME")).queue();
            return;
        }
        final String reminderName = arguments[1];

        //Parse time of day
        if (arguments.length < 3) {
            channel.sendMessage(locale.getString("REMINDER_MISSING_TIME")).queue();
            return;
        }
        final String reminderTime = arguments[2];
        final LocalTime timeOfDay;
        try {
            timeOfDay = LocalTime.parse(reminderTime, translationCache.getTimeFormatter());
        } catch (DateTimeParseException e) {
            channel.sendMessageFormat(locale.getString("REMINDER_UNKNOWN_TIME"), reminderTime).queue();
            return;
        }

        //Parse day of month
        if (arguments.length < 4) {
            channel.sendMessage(locale.getString("REMINDER_MISSING_DAY")).queue();
            return;
        }
        final String reminderDate = arguments[3];
        int dayOfMonth = 0;
        if (!"*".equals(reminderDate)) {
            try {
                dayOfMonth = Integer.parseUnsignedInt(reminderDate);
                if (dayOfMonth < 1 || dayOfMonth > 31) {
                    channel.sendMessageFormat(locale.getString("REMINDER_DAY_OF_MONTH_OUT_OF_RANGE")).queue();
                    return;
                }
            } catch (NumberFormatException e) {
                channel.sendMessageFormat(locale.getString("REMINDER_DAY_OF_MONTH_NOT_NUMBER")).queue();
                return;
            }
        }

        //Parse month of year
        if (arguments.length < 5) {
            channel.sendMessage(locale.getString("REMINDER_MISSING_MONTH")).queue();
            return;
        }
        final String reminderMonth = arguments[4];
        Month monthOfYear = null;
        if (!"*".equals(reminderMonth)) {
            try {
                final int monthNumber = Integer.parseUnsignedInt(reminderMonth);
                monthOfYear = Month.of(monthNumber);
            } catch (NumberFormatException e) {
                channel.sendMessage(locale.getString("REMINDER_MONTH_NOT_NUMBER")).queue();
                return;
            } catch (DateTimeParseException e) {
                channel.sendMessage(locale.getString("REMINDER_MONTH_OUT_OF_RANGE")).queue();
                return;
            }
        }

        //Validate monthDay if present
        if (dayOfMonth != 0 && monthOfYear != null) {
            try {
                final MonthDay monthDay = MonthDay.of(monthOfYear, dayOfMonth);
                LOGGER.debug("Found monthDay: {} in reminder creation.", monthDay.toString());
            } catch (DateTimeException e) {
                channel.sendMessage(locale.getString("REMINDER_INVALID_DATE")).queue();
                return;
            }
        }

        //Parse day of week
        if (arguments.length < 6) {
            channel.sendMessage(locale.getString("REMINDER_MISSING_DAY_OF_WEEK")).queue();
            return;
        }
        final String reminderDay = arguments[5];
        DayOfWeek dayOfWeek = null;
        if (!"*".equals(reminderDay)) {
            final Collator collator = translationCache.getCollator();
            for (final DayOfWeek day : DayOfWeek.values()) {
                final String localDayName = day.getDisplayName(TextStyle.FULL_STANDALONE, locale.getLocale());
                if (collator.equals(localDayName, reminderDay)) {
                    dayOfWeek = day;
                    break;
                }
            }
            //Check if we found the day with given name
            if (dayOfWeek == null) {
                channel.sendMessage(locale.getString("REMINDER_ERROR_UNKNOWN_DAY")).queue();
                return;
            }
        }

        //Construct reminder activation checker
        final ReminderActivationTime reminderActivationTime = new ReminderActivationTime(timeOfDay, dayOfWeek, dayOfMonth, monthOfYear);

        //Get the reminder message
        if (arguments.length < 7) {
            channel.sendMessage(locale.getString("REMINDER_MISSING_MESSAGE")).queue();
            return;
        }
        final String messageInput = arguments[6];

        //Construct reminder
        final JDA jda = channel.getJDA();
        final long channelID = channel.getIdLong();
        final long memberID = matcher.getMember().getIdLong();
        final Reminder reminder = new Reminder(jda, guildData, reminders, reminderName,
                messageInput, channelID, memberID, reminderActivationTime);

        //Add reminder to database
        try {
            if (!reminders.addReminder(reminder)) {
                channel.sendMessage(locale.getString("REMINDER_ALREADY_EXISTS")).queue();
                return;
            }
            channel.sendMessageFormat(locale.getString("REMINDER_CREATE_SUCCESS")).queue();
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("REMINDER_SQL_ERROR_ON_CREATE")).queue();
            LOGGER.error("Failure to create reminder: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private void deleteReminder(final String[] arguments, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();

        if (arguments.length < 2) {
            textChannel.sendMessage(locale.getString("REMINDER_DELETE_MISSING_NAME")).queue();
            return;
        }
        final Guild guild = matcher.getGuild();
        final GuildDataStore guildData = context.getGuildData();
        final ReminderManager reminders = getReminderManager(guild, guildData);

        final String reminderName = arguments[1];
        final Optional<Reminder> oldReminder = reminders.getReminder(reminderName);
        if (oldReminder.isEmpty()) {
            textChannel.sendMessageFormat(locale.getString("REMINDER_NOT_FOUND_NAME"), reminderName).queue();
            return;
        }
        final Reminder reminder = oldReminder.get();

        //Check if user has permission to remove the event
        final Member sender = matcher.getMember();
        textChannel.getGuild().retrieveMemberById(reminder.getAuthor()).submit().whenComplete((Member reminderOwner, Throwable e) -> {
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, reminderOwner);
            if (!hasPermission) {
                textChannel.sendMessage(locale.getString("REMINDER_DELETE_MISSING_PERMISSION")).queue();
                return;
            }

            try {
                reminders.deleteReminder(reminder);
                textChannel.sendMessage(locale.getString("REMINDER_DELETE_SUCCESS")).queue();
            } catch (SQLException ex) {
                textChannel.sendMessage(locale.getString("REMINDER_SQL_ERROR_ON_DELETE")).queue();
                LOGGER.error("Failure to delete reminder: {}", ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });
    }

    private void listReminders(final CommandContext context) {
        final TranslationCache translationCache = context.getTranslation();
        final ResourceBundle locale = translationCache.getResourceBundle();

        //Construct the embed
        final String header = locale.getString("HEADER_REMINDERS");
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(header);

        //Initialize all the futures
        final Guild guild = context.getMatcher().getGuild();
        final GuildDataStore guildData = context.getGuildData();
        final Collection<Reminder> ev = getReminderManager(guild, guildData).getReminders();
        final List<CompletableFuture<String>> futures = new ArrayList<>(ev.size());
        ev.forEach((Reminder reminder) -> {
            futures.add(reminder.toListElement(translationCache));
        });

        //After all the futures all initialized start waiting for results
        final StringBuilder contentBuilder = new StringBuilder();
        futures.forEach(desc -> {
            contentBuilder.append(desc.join());
        });
        if (ev.isEmpty()) {
            contentBuilder.append(locale.getString("REMINDER_NO_REMINDERS"));
        }
        eb.setDescription(contentBuilder);

        final TextChannel textChannel = context.getChannel();
        textChannel.sendMessage(eb.build()).queue();
    }

}
