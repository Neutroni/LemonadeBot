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
import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
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
    public String getCommand(final ResourceBundle locale) {
        return locale.getString("COMMAND_COOLDOWN");
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_COOLDOWN");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        return locale.getString("SYNTAX_COOLDOWN");
    }

    @Override
    public void respond(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final String[] arguments = matcher.getArguments(1);
        final TextChannel channel = matcher.getTextChannel();
        final TranslationCache translationCache = context.getTranslation();
        final ResourceBundle locale = translationCache.getResourceBundle();
        if (arguments.length == 0) {
            channel.sendMessage(locale.getString("ERROR_MISSING_OPERATION")).queue();
            return;
        }
        if (arguments.length == 1) {
            channel.sendMessage(locale.getString("COOLDOWN_MISSING_ACTION")).queue();
            return;
        }

        final String option = arguments[0];
        final ActionKey key = translationCache.getActionKey(option);
        switch (key) {
            case GET: {
                getCooldown(context, arguments[1]);
                break;
            }
            case SET: {
                final String[] setArguments = matcher.getArguments(3);
                setCooldown(context, setArguments);
                break;
            }
            case DISABLE: {
                disableCooldown(context, arguments[1]);
                break;
            }
            case LIST: {
                listCooldowns(context);
                break;
            }
            default: {
                channel.sendMessage(locale.getString("ERROR_UNKNOWN_OPERATION") + option).queue();
            }
        }
    }

    /**
     * Send message reply of the cooldown set for action
     *
     * @param channel Channel to send message on
     * @param guildData GuildData to get cooldown manager from
     * @param requestedAction action to get cooldown for
     */
    private static void getCooldown(final CommandContext context, final String requestedAction) {
        final CooldownManager cooldownManager = context.getCooldownManager();
        final ResourceBundle locale = context.getResource();
        final TextChannel channel = context.getChannel();
        final Guild guild = channel.getGuild();
        final long guildID = guild.getIdLong();
        final Optional<ActionCooldown> cd;
        try {
            cd = cooldownManager.getActionCooldown(requestedAction, guildID);
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("COOLDOWN_SQL_ERROR_ON_RETRIEVE")).queue();
            LOGGER.error("Failure to get cooldown for action from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
            return;
        }
        cd.ifPresentOrElse((ActionCooldown cooldown) -> {
            final String cooldownAction = cooldown.getAction();
            final Duration cooldownDuration = cooldown.getDuration();
            final String setCooldown = CooldownManager.formatDuration(cooldownDuration, locale);
            final String template = locale.getString("COOLDOWN_CURRENT_COOLDOWN");
            channel.sendMessageFormat(template, setCooldown, cooldownAction).queue();
        }, () -> {
            //No cooldown set for action
            channel.sendMessage(locale.getString("COOLDOWN_NO_COOLDOWN_SET") + requestedAction).queue();
        });
    }

    /**
     * Set cooldown for action
     *
     * @param channel Channel to send responses on
     * @param guildData cooldown manager to use for setting cooldown
     * @param arguments arguments time,amount,action to use for setting cooldown
     */
    private static void setCooldown(final CommandContext context, final String[] arguments) {
        final CooldownManager cooldownManager = context.getCooldownManager();
        final TranslationCache translationCache = context.getTranslation();
        final ResourceBundle locale = translationCache.getResourceBundle();
        final TextChannel channel = context.getChannel();

        //No time amount
        if (arguments.length < 2) {
            channel.sendMessage(locale.getString("COOLDOWN_MISSING_TIME")).queue();
            return;
        }

        final String timeAmountString = arguments[1];
        final int timeAmount;
        try {
            timeAmount = Integer.parseInt(timeAmountString);
        } catch (NumberFormatException e) {
            channel.sendMessage(locale.getString("COOLDOWN_UNKNOWN_TIME") + timeAmountString).queue();
            return;
        }

        //No time unit
        if (arguments.length < 3) {
            channel.sendMessage(locale.getString("COOLDOWN_MISSIGN_UNIT")).queue();
            return;
        }

        final String unitName = arguments[2].toLowerCase();
        final Optional<ChronoUnit> optUnit = translationCache.getChronoUnit(unitName);
        final ChronoUnit unit;
        if (optUnit.isPresent()) {
            unit = optUnit.get();
        } else {
            channel.sendMessage(locale.getString("COOLDOWN_UNKNOWN_UNIT") + arguments[2]).queue();
            return;
        }

        final Duration cooldownDuration = Duration.of(timeAmount, unit);
        if (arguments.length < 4) {
            channel.sendMessage(locale.getString("COOLDOWN_NO_ACTION")).queue();
            return;
        }
        final String actionString = arguments[3];

        try {
            final Guild guild = channel.getGuild();
            final long guildID = guild.getIdLong();
            cooldownManager.setCooldown(actionString, cooldownDuration, guildID);
            channel.sendMessage(locale.getString("COOLDOWN_UPDATED_SUCCESFULLY")).queue();
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("COOLDOWN_SQL_ERROR_ON_UPDATE")).queue();
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
    private static void disableCooldown(final CommandContext context, final String requestedAction) {
        final CooldownManager cooldownManager = context.getCooldownManager();
        final ResourceBundle locale = context.getResource();
        final TextChannel channel = context.getChannel();
        try {
            final Guild guild = context.getGuild();
            final long guildID = guild.getIdLong();
            if (cooldownManager.removeCooldown(requestedAction, guildID)) {
                //Found cooldown for action
                channel.sendMessage(locale.getString("COOLDOWN_DISABLE_SUCCESS")).queue();
            } else {
                //No cooldown for action
                channel.sendMessage(locale.getString("COOLDOWN_NO_COOLDOWN_SET") + requestedAction).queue();
            }
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("COOLDOWN_SQL_ERROR_ON_DISABLE")).queue();
            LOGGER.error("Failure to remove cooldown from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void listCooldowns(final CommandContext context) {
        final CooldownManager cooldownManager = context.getCooldownManager();
        final TextChannel channel = context.getChannel();
        final ResourceBundle locale = context.getResource();

        //Fetch the list of set cooldowns from database
        final Collection<ActionCooldown> cooldowns;
        try {
            final long guildID = context.getGuild().getIdLong();
            cooldowns = cooldownManager.getCooldowns(guildID);
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("COOLDOWN_SQL_ERROR_ON_LOADING")).queue();
            LOGGER.error("Failure to load cooldown from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
            return;
        }

        //Construct embed for response
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(locale.getString("HEADER_COOLDOWNS"));
        final StringBuilder description = new StringBuilder();
        for (final ActionCooldown cd : cooldowns) {
            description.append(cd.toListElement(locale));
        }
        if (cooldowns.isEmpty()) {
            description.append(locale.getString("COOLDOWN_NO_COOLDOWNS"));
        }
        eb.setDescription(description);
        channel.sendMessageEmbeds(eb.build()).queue();
    }
}
