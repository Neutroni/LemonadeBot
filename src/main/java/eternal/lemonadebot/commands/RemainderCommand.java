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
package eternal.lemonadebot.commands;

import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.commandtypes.AdminCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.RemainderManager;
import eternal.lemonadebot.events.Remainder;
import eternal.lemonadebot.permissions.PermissionUtilities;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationKey;
import eternal.lemonadebot.translation.WeekDayKey;
import java.sql.SQLException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command used to manage remainders
 *
 * @author Neutroni
 */
public class RemainderCommand extends AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    private final SimpleDateFormat dateFormat;

    /**
     * Constructor
     *
     */
    public RemainderCommand() {
        this.dateFormat = new SimpleDateFormat("EEEE MMMM dd HH:mm zzz", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public String getCommand(Locale locale) {
        return TranslationKey.COMMAND_REMAINDER.getTranslation(locale);
    }

    @Override
    public String getDescription(Locale locale) {
        return TranslationKey.DESCRIPTION_REMAINDER.getTranslation(locale);
    }

    @Override
    public String getHelpText(Locale locale) {
        return TranslationKey.SYNTAX_REMAINDER.getTranslation(locale);
    }

    @Override
    public void respond(CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final Locale locale = guildConf.getLocale();

        final String[] arguments = matcher.getArguments(5);
        if (arguments.length == 0) {
            textChannel.sendMessage(TranslationKey.ERROR_MISSING_OPERATION.getTranslation(locale)).queue();
            return;
        }

        final String action = arguments[0];
        final ActionKey key = ActionKey.getAction(action, guildConf);
        switch (key) {
            case CREATE: {
                createRemainder(arguments, matcher, guildData);
                break;
            }
            case DELETE: {
                deleteRemainder(arguments, matcher, guildData);
                break;
            }
            case LIST: {
                listRemainders(matcher, guildData);
                break;
            }
            default: {
                textChannel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + arguments[0]).queue();
            }
        }
    }

    private void createRemainder(String[] arguments, CommandMatcher matcher, GuildDataStore guildData) {
        //remainder create test sunday 17.00 remaindertext
        final TextChannel textChannel = matcher.getTextChannel();
        final RemainderManager remainders = guildData.getRemainderManager();
        final ConfigManager guildConf = guildData.getConfigManager();
        final Locale locale = guildConf.getLocale();

        if (arguments.length < 2) {
            textChannel.sendMessage(TranslationKey.REMAINDER_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String remainderName = arguments[1];

        if (arguments.length < 3) {
            textChannel.sendMessage(TranslationKey.REMAINDER_MISSING_DAY.getTranslation(locale)).queue();
            return;
        }
        final String reminderDay = arguments[2];
        final Collator collator = guildConf.getCollator();
        final DayOfWeek activationDay;
        try {
            final String localAnyDay = TranslationKey.REMAINDER_DAY_DAILY.getTranslation(locale);
            if (collator.equals(reminderDay, localAnyDay)) {
                activationDay = null;
            } else {
                WeekDayKey activationKey = WeekDayKey.getDayFromTranslatedName(reminderDay, guildConf);
                activationDay = activationKey.getDay();
            }
        } catch (IllegalArgumentException e) {
            textChannel.sendMessage(TranslationKey.REMAINDER_ERROR_UNKNOWN_DAY.getTranslation(locale)).queue();
            return;
        }

        if (arguments.length < 4) {
            textChannel.sendMessage(TranslationKey.REMAINDER_MISSING_TIME.getTranslation(locale)).queue();
            return;
        }
        final String reminderTime = arguments[3];
        final LocalTime activationTime;
        try {
            activationTime = LocalTime.parse(reminderTime);
        } catch (DateTimeParseException e) {
            textChannel.sendMessageFormat(TranslationKey.REMAINDER_UNKNOWN_TIME.getTranslation(locale), reminderTime).queue();
            return;
        }

        if (arguments.length < 5) {
            textChannel.sendMessage(TranslationKey.REMAINDER_MISSING_MESSAGE.getTranslation(locale)).queue();
            return;
        }
        final String messageInput = arguments[4];
        final Remainder remainder = new Remainder(textChannel.getJDA(), guildData,
                remainderName, textChannel.getIdLong(), matcher.getMember().getIdLong(),
                messageInput, activationDay, activationTime);
        try {
            if (!remainders.addRemainder(remainder)) {
                textChannel.sendMessage(TranslationKey.REMAINDER_ALREADY_EXISTS.getTranslation(locale)).queue();
                return;
            }
            final String firstActivation = dateFormat.format(remainder.getTimeToActivation());
            textChannel.sendMessageFormat(TranslationKey.REMAINDER_CREATE_SUCCESS.getTranslation(locale), firstActivation).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(TranslationKey.REMAINDER_SQL_ERROR_ON_CREATE.getTranslation(locale)).queue();
            LOGGER.error("Failure to create remainder: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private void deleteRemainder(String[] arguments, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();

        if (arguments.length < 2) {
            textChannel.sendMessage(TranslationKey.REMAINDER_DELETE_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final RemainderManager remainders = guildData.getRemainderManager();

        final String remainderName = arguments[1];
        final Optional<Remainder> oldRemainder = remainders.getRemainder(remainderName);
        if (oldRemainder.isEmpty()) {
            textChannel.sendMessageFormat(TranslationKey.REMAINDER_NOT_FOUND_NAME.getTranslation(locale), remainderName).queue();
            return;
        }
        final Remainder remainder = oldRemainder.get();

        //Check if user has permission to remove the event
        final Member sender = matcher.getMember();
        textChannel.getGuild().retrieveMemberById(remainder.getAuthor()).submit().whenComplete((Member remainderOwner, Throwable e) -> {
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, remainderOwner);
            if (!hasPermission) {
                textChannel.sendMessage(TranslationKey.REMAINDER_DELETE_MISSING_PERMISSION.getTranslation(locale)).queue();
                return;
            }

            try {
                remainders.deleteRemainder(remainder);
                textChannel.sendMessage(TranslationKey.REMAINDER_DELETE_SUCCESS.getTranslation(locale)).queue();
            } catch (SQLException ex) {
                textChannel.sendMessage(TranslationKey.REMAINDER_SQL_ERROR_ON_DELETE.getTranslation(locale)).queue();
                LOGGER.error("Failure to delete remainder: {}", ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });
    }

    private void listRemainders(CommandMatcher matcher, GuildDataStore guildData) {
        final Locale locale = guildData.getConfigManager().getLocale();

        //Construct the embed
        final String header = TranslationKey.HEADER_REMAINDERS.getTranslation(locale);
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(header);

        //Initialize all the futures
        final Set<Remainder> ev = guildData.getRemainderManager().getRemainders();
        final List<CompletableFuture<String>> futures = new ArrayList<>(ev.size());
        ev.forEach((Remainder remainder) -> {
            futures.add(remainder.toListElement(locale));
        });

        //After all the futures all initialized start waiting for results
        final StringBuilder contentBuilder = new StringBuilder();
        futures.forEach(desc -> {
            contentBuilder.append(desc.join());
        });
        if (ev.isEmpty()) {
            contentBuilder.append(TranslationKey.REMAINDER_NO_REMAINDERS.getTranslation(locale));
        }
        eb.setDescription(contentBuilder);

        final TextChannel textChannel = matcher.getTextChannel();
        textChannel.sendMessage(eb.build()).queue();
    }

}
