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
import eternal.lemonadebot.database.CooldownManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TimeKey;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command used to manage cooldown for commands
 *
 * @author Neutroni
 */
public class CooldownCommand extends AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(Locale locale) {
        return TranslationKey.COMMAND_COOLDOWN.getTranslation(locale);
    }

    @Override
    public String getDescription(Locale locale) {
        return TranslationKey.DESCRIPTION_COOLDOWN.getTranslation(locale);
    }

    @Override
    public String getHelpText(Locale locale) {
        return TranslationKey.SYNTAX_COOLDOWN.getTranslation(locale);
    }

    @Override
    public void respond(CommandMatcher message, GuildDataStore guildData) {
        final String[] arguments = message.getArguments(1);
        final TextChannel channel = message.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final Locale locale = guildConf.getLocale();
        if (arguments.length == 0) {
            channel.sendMessage(TranslationKey.ERROR_MISSING_OPERATION.getTranslation(locale)).queue();
            return;
        }
        if (arguments.length == 1) {
            channel.sendMessage(TranslationKey.COOLDOWN_MISSING_ACTION.getTranslation(locale)).queue();
            return;
        }

        final String option = arguments[0];
        final ActionKey key = ActionKey.getAction(option, guildConf);
        switch (key) {
            case GET: {
                getCooldown(channel, guildData, arguments[1]);
                break;
            }
            case SET: {
                final String[] setArguments = message.getArguments(3);
                setCooldown(channel, guildData, setArguments);
                break;
            }
            default: {
                channel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + option).queue();
            }
        }
    }

    /**
     * Send message reply of the cooldown set for command
     *
     * @param channel Channel to send message on
     * @param cooldownManager cooldown manager to get cooldown from
     * @param action action to get cooldown for
     */
    private void getCooldown(TextChannel channel, GuildDataStore guildData, String action) {
        final CooldownManager cooldownManager = guildData.getCooldownManager();
        final Locale locale = guildData.getConfigManager().getLocale();
        final Optional<String> cd = cooldownManager.getCooldownFormatted(action);
        if (cd.isEmpty()) {
            channel.sendMessage(TranslationKey.COOLDOWN_NO_COOLDOWN_SET.getTranslation(locale) + action).queue();
        } else {
            final String template = TranslationKey.COOLDOWN_CURRENT_COOLDOWN.getTranslation(locale);
            channel.sendMessageFormat(template, cd.get(), action).queue();
        }
    }

    /**
     * Set cooldown for action
     *
     * @param channel Channel to send responses on
     * @param cooldownManager cooldown manager to use for setting cooldown
     * @param arguments arguments time,amount,action to use for setting cooldown
     */
    private void setCooldown(TextChannel channel, GuildDataStore guildData, String[] arguments) {
        final CooldownManager cooldownManager = guildData.getCooldownManager();
        final ConfigManager guildConf = guildData.getConfigManager();
        final Locale locale = guildConf.getLocale();

        //No time amount
        if (arguments.length < 2) {
            channel.sendMessage(TranslationKey.COOLDOWN_MISSING_TIME.getTranslation(locale)).queue();
            return;
        }

        final String timeAmountstring = arguments[1];
        int timeAmount;
        try {
            timeAmount = Integer.parseInt(timeAmountstring);
        } catch (NumberFormatException e) {
            channel.sendMessage(TranslationKey.COOLDOWN_UNKNOWN_TIME.getTranslation(locale) + timeAmountstring).queue();
            return;
        }

        //No time unit
        if (arguments.length < 3) {
            channel.sendMessage(TranslationKey.COOLDOWN_MISSIGN_UNIT.getTranslation(locale)).queue();
            return;
        }

        final String unitName = arguments[2].toLowerCase();
        final ChronoUnit unit;
        try {
            unit = TimeKey.getTimeKey(unitName, guildConf).getChronoUnit();
        } catch (IllegalArgumentException e) {
            channel.sendMessage(TranslationKey.COOLDOWN_UNKNOWN_UNIT.getTranslation(locale) + arguments[2]).queue();
            return;
        }
        final Duration cooldownDuration = Duration.of(timeAmount, unit);

        if (arguments.length < 4) {
            channel.sendMessage(TranslationKey.COOLDOWN_NO_ACTION.getTranslation(locale)).queue();
            return;
        }
        final String actionString = arguments[3];

        try {
            cooldownManager.setCooldown(actionString, cooldownDuration);
            channel.sendMessage(TranslationKey.COOLDOWN_UPDATED_SUCCESFULLY.getTranslation(locale)).queue();
        } catch (SQLException ex) {
            channel.sendMessage(TranslationKey.COOLDOWN_SQL_ERROR_ON_UPDATE.getTranslation(locale)).queue();
            LOGGER.error("Failure to set cooldown in database: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }
}
