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
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.CooldownManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.permissions.MemberRank;
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
        return "Syntax: cooldown <command> [time] [unit]\n"
                + "<command> is the name of the command to set or get cooldown for\n"
                + "[time] is the cooldown time in [unit] time units\n"
                + "example: 2 minutes\n";
    }

    @Override
    public void respond(CommandMatcher message, GuildDataStore guildData) {
        final String[] options = message.getArguments(3);
        final TextChannel channel = message.getTextChannel();
        if (options.length == 0) {
            channel.sendMessage("Provide name of the command to get or set cooldown for.").queue();
            return;
        }
        final String commandName = options[0];
        final Optional<? extends ChatCommand> optCommand = CommandProvider.getCommand(commandName, guildData);
        if (optCommand.isEmpty()) {
            channel.sendMessage("No such command:" + commandName).queue();
            return;
        }
        final ChatCommand command = optCommand.get();

        //Do not allow setting cooldown for admin commands
        if (MemberRank.ADMIN == command.getDefaultRank()) {
            channel.sendMessage("Can't set a cooldown for admin commands.").queue();
            return;
        }

        //No time
        final CooldownManager cooldownManager = guildData.getCooldownManager();
        if (options.length < 2) {
            final Optional<String> optCooldown = cooldownManager.getCooldownFormatted(command);
            if (optCooldown.isEmpty()) {
                channel.sendMessage("No cooldown set for command: " + commandName).queue();
            } else {
                channel.sendMessage("Current cooldown for command " + commandName + " : " + optCooldown).queue();
            }
            return;
        }

        //No time unit
        if (options.length < 3) {
            channel.sendMessage("Provide the unit in which the coold down is to apply a cooldown to command.").queue();
            return;
        }

        String timeAmountstring = options[1];
        int timeAmount;
        try {
            timeAmount = Integer.parseInt(timeAmountstring);
        } catch (NumberFormatException e) {
            channel.sendMessage("Unknown time amount: " + timeAmountstring).queue();
            return;
        }

        final String unitName = (timeAmount == 1) ? (options[2] + 's').toUpperCase() : options[2].toUpperCase();
        ChronoUnit unit;
        try {
            unit = ChronoUnit.valueOf(unitName);
        } catch (IllegalArgumentException e) {
            channel.sendMessage("Unknown time unit: " + options[2]).queue();
            return;
        }
        final Duration cooldownDuration = Duration.of(timeAmount, unit);
        try {
            cooldownManager.setCooldown(command, cooldownDuration);
            channel.sendMessage("Command cooldown updated succesfully.").queue();
        } catch (SQLException ex) {
            channel.sendMessage("Updating command cooldown failed, database error, will still use untill reboot.").queue();
            LOGGER.error("Failure to add custom command to database");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }
}
