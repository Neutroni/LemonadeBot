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
import eternal.lemonadebot.database.CooldownManager;
import eternal.lemonadebot.database.GuildDataStore;
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class CooldownCommand extends AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand() {
        return "cooldown";
    }

    @Override
    public String getDescription() {
        return "Set cooldown for commands";
    }

    @Override
    public String getHelpText() {
        return "Syntax: cooldown <option> [time] [unit] <action>\n"
                + "<option> is one of the following:\n"
                + " get - to get current cooldown for action\n"
                + " set - to set cooldown for action\n"
                + "[time] is the cooldown time in [unit] time units, for example: 2 minutes\n"
                + "<action> is the action to get or set the cooldown time for\n";
    }

    @Override
    public void respond(CommandMatcher message, GuildDataStore guildData) {
        final String[] arguments = message.getArguments(1);
        final TextChannel channel = message.getTextChannel();
        if (arguments.length == 0) {
            channel.sendMessage("Provide name of the option to perform.").queue();
            return;
        }
        if (arguments.length == 1) {
            channel.sendMessage("Provide name of the action to manage cooldown for.").queue();
            return;
        }

        final CooldownManager cooldownManager = guildData.getCooldownManager();
        final String option = arguments[0];
        switch (option) {
            case "get": {
                getCooldown(channel, cooldownManager, arguments[1]);
                break;
            }
            case "set": {
                final String[] setArguments = message.getArguments(3);
                setCooldown(channel, cooldownManager, setArguments);
                break;
            }
            default: {
                channel.sendMessage("Unkown option: " + option).queue();
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
    private void getCooldown(TextChannel channel, CooldownManager cooldownManager, String action) {
        final Optional<String> cd = cooldownManager.getCooldownFormatted(action);
        if (cd.isEmpty()) {
            channel.sendMessage("No cooldown set for action: " + action).queue();
        } else {
            channel.sendMessage("Current cooldown for action " + action + " : " + cd.get()).queue();
        }
    }

    /**
     * Set cooldown for action
     *
     * @param channel Channel to send responses on
     * @param cooldownManager cooldown manager to use for setting cooldown
     * @param arguments arguments time,amount,action to use for setting cooldown
     */
    private void setCooldown(TextChannel channel, CooldownManager cooldownManager, String[] arguments) {
        //No time amount
        if (arguments.length < 2) {
            channel.sendMessage("Provide the amount of cooldown to set for action.").queue();
            return;
        }

        final String timeAmountstring = arguments[1];
        int timeAmount;
        try {
            timeAmount = Integer.parseInt(timeAmountstring);
        } catch (NumberFormatException e) {
            channel.sendMessage("Unknown time amount: " + timeAmountstring).queue();
            return;
        }

        //No time unit
        if (arguments.length < 3) {
            channel.sendMessage("Provide the unit in which the coold down is to apply a cooldown to command.").queue();
            return;
        }

        final String unitName = (timeAmount == 1) ? (arguments[2] + 's').toUpperCase() : arguments[2].toUpperCase();
        ChronoUnit unit;
        try {
            unit = ChronoUnit.valueOf(unitName);
        } catch (IllegalArgumentException e) {
            channel.sendMessage("Unknown time unit: " + arguments[2]).queue();
            return;
        }
        final Duration cooldownDuration = Duration.of(timeAmount, unit);

        if (arguments.length < 4) {
            channel.sendMessage("Provide the name of the action to set cooldown for").queue();
            return;
        }
        final String actionString = arguments[3];

        try {
            cooldownManager.setCooldown(actionString, cooldownDuration);
            channel.sendMessage("Action cooldown updated succesfully.").queue();
        } catch (SQLException ex) {
            channel.sendMessage("Updating action cooldown failed, database error, will still use untill reboot.").queue();
            LOGGER.error("Failure to set cooldown in database");
            LOGGER.error(ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }
}
