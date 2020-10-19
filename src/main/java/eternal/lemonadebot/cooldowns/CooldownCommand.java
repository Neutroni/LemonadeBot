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
package eternal.lemonadebot.cooldowns;

import eternal.lemonadebot.commands.AdminCommand;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import net.dv8tion.jda.api.EmbedBuilder;
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
    public String getCommand(final Locale locale) {
        return TranslationKey.COMMAND_COOLDOWN.getTranslation(locale);
    }

    @Override
    public String getDescription(final Locale locale) {
        return TranslationKey.DESCRIPTION_COOLDOWN.getTranslation(locale);
    }

    @Override
    public String getHelpText(final Locale locale) {
        return TranslationKey.SYNTAX_COOLDOWN.getTranslation(locale);
    }

    @Override
    public void respond(final CommandMatcher matcher, final GuildDataStore guildData) {
        final String[] arguments = matcher.getArguments(1);
        final TextChannel channel = matcher.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
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
        final ActionKey key = translationCache.getActionKey(option);
        switch (key) {
            case GET: {
                getCooldown(channel, guildData, arguments[1]);
                break;
            }
            case SET: {
                final String[] setArguments = matcher.getArguments(3);
                setCooldown(channel, guildData, setArguments);
                break;
            }
            case DISABLE: {
                disableCooldown(channel, guildData, arguments[1]);
                break;
            }
            case LIST: {
                listCooldowns(matcher, guildData);
                break;
            }
            default: {
                channel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + option).queue();
            }
        }
    }

    /**
     * Send message reply of the cooldown set for action
     *
     * @param channel Channel to send message on
     * @param guildData GuildData to get  cooldown manager from
     * @param requestedAction action to get cooldown for
     */
    private static void getCooldown(final TextChannel channel, final GuildDataStore guildData, final String requestedAction) {
        final CooldownManager cooldownManager = guildData.getCooldownManager();
        final Locale locale = guildData.getConfigManager().getLocale();
        final Optional<ActionCooldown> cd;
        try {
            cd = cooldownManager.getActionCooldown(requestedAction);
        } catch (SQLException ex) {
            channel.sendMessage(TranslationKey.COOLDOWN_SQL_ERROR_ON_RETRIEVE.getTranslation(locale)).queue();
            LOGGER.error("Failure to get cooldown for action from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
            return;
        }
        cd.ifPresentOrElse((ActionCooldown cooldown) -> {
            final String cooldownAction = cooldown.getAction();
            final Duration cooldownDuration = cooldown.getDuration();
            final String setCooldown = CooldownManager.formatDuration(cooldownDuration, locale);
            final String template = TranslationKey.COOLDOWN_CURRENT_COOLDOWN.getTranslation(locale);
            channel.sendMessageFormat(template, setCooldown, cooldownAction).queue();
        }, () -> {
            //No cooldown set for action
            channel.sendMessage(TranslationKey.COOLDOWN_NO_COOLDOWN_SET.getTranslation(locale) + requestedAction).queue();
        });
    }

    /**
     * Set cooldown for action
     *
     * @param channel Channel to send responses on
     * @param guildData cooldown manager to use for setting cooldown
     * @param arguments arguments time,amount,action to use for setting cooldown
     */
    private static void setCooldown(final TextChannel channel, final GuildDataStore guildData, final String[] arguments) {
        final CooldownManager cooldownManager = guildData.getCooldownManager();
        final ConfigManager guildConf = guildData.getConfigManager();
        final Locale locale = guildConf.getLocale();

        //No time amount
        if (arguments.length < 2) {
            channel.sendMessage(TranslationKey.COOLDOWN_MISSING_TIME.getTranslation(locale)).queue();
            return;
        }

        final String timeAmountstring = arguments[1];
        final int timeAmount;
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
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Optional<ChronoUnit> optUnit = translationCache.getChronoUnit(unitName);
        final ChronoUnit unit;
        if (optUnit.isPresent()) {
            unit = optUnit.get();
        } else {
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

    /**
     * Disable cooldown for command
     *
     * @param channel Channel to send messages on
     * @param guildData CooldownManager to remove cooldown in
     * @param requestedAction Action which to remove cooldown from
     */
    private static void disableCooldown(final TextChannel channel, final GuildDataStore guildData, final String requestedAction) {
        final CooldownManager cooldownManager = guildData.getCooldownManager();
        final Locale locale = guildData.getConfigManager().getLocale();
        try {
            if (cooldownManager.removeCooldown(requestedAction)) {
                //Found cooldown for action
                channel.sendMessage(TranslationKey.COOLDOWN_DISABLE_SUCCESS.getTranslation(locale)).queue();
            } else {
                //No cooldown for action
                channel.sendMessage(TranslationKey.COOLDOWN_NO_COOLDOWN_SET.getTranslation(locale) + requestedAction).queue();
            }
        } catch (SQLException ex) {
            channel.sendMessage(TranslationKey.COOLDOWN_SQL_ERROR_ON_DISABLE.getTranslation(locale)).queue();
            LOGGER.error("Failure to remove cooldown from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void listCooldowns(final CommandMatcher matcher, final GuildDataStore guildData) {
        final CooldownManager cooldownManager = guildData.getCooldownManager();
        final TextChannel channel = matcher.getTextChannel();
        final Locale locale = matcher.getLocale();

        //Fetch the list of set cooldowns from database
        final Collection<ActionCooldown> cooldowns;
        try {
            cooldowns = cooldownManager.getCooldowns();
        } catch (SQLException ex) {
            channel.sendMessage(TranslationKey.COOLDOWN_SQL_ERROR_ON_LOADING.getTranslation(locale)).queue();
            LOGGER.error("Failure to load cooldown from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
            return;
        }

        //Construct embed for response
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(TranslationKey.HEADER_COOLDOWNS.getTranslation(locale));
        final StringBuilder description = new StringBuilder();
        for (final ActionCooldown cd : cooldowns) {
            description.append(cd.toListElement(locale));
        }
        if (cooldowns.isEmpty()) {
            description.append(TranslationKey.COOLDOWN_NO_COOLDOWNS.getTranslation(locale));
        }
        eb.setDescription(description);
        channel.sendMessage(eb.build()).queue();
    }
}
