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

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.RemainderManager;
import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.customcommands.Remainder;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.permissions.PermissionUtilities;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command to add remainders for events
 *
 * @author Neutroni
 */
public class RemainderCommand implements ChatCommand {

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
    public String getCommand() {
        return "remainder";
    }

    @Override
    public String getDescription() {
        return "Add remainders for events";
    }

    @Override
    public String getHelpText() {
        return "Syntax: remainder <action> <name> [day] [time] [message]\n"
                + "<action> can be one of the following:\n"
                + " create - create new remainder\n"
                + " delete - delete remainder\n"
                + "<name> name of remainder\n"
                + "<day> day the remainder activates on, use 'any' to create remainder that activates daily\n"
                + "<time> time of the remainder hh:mm\n"
                + "[message] message to be sent at the remainder activation\n"
                + "Remainder will be activated on the channel it was created in";
    }

    @Override
    public MemberRank getDefaultRank() {
        return MemberRank.ADMIN;
    }

    @Override
    public void respond(CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();

        final String[] arguments = matcher.getArguments(5);
        if (arguments.length == 0) {
            textChannel.sendMessage("Provide action to perform, see help for possible actions").queue();
            return;
        }
        switch (arguments[0]) {
            case "create": {
                createRemainder(arguments, matcher, guildData);
                break;
            }
            case "delete": {
                deleteRemainder(arguments, matcher, guildData);
                break;
            }
            case "list": {
                listRemainders(matcher, guildData);
                break;
            }
            default: {
                textChannel.sendMessage("Unknown operation: " + arguments[0]).queue();
            }
        }
    }

    private void createRemainder(String[] arguments, CommandMatcher matcher, GuildDataStore guildData) {
        //remainder create test sunday 17.00 remaindertext
        final TextChannel textChannel = matcher.getTextChannel();
        final RemainderManager remainders = guildData.getRemainderManager();

        if (arguments.length < 2) {
            textChannel.sendMessage("Remainder needs a name").queue();
            return;
        }
        final String remainderName = arguments[1];

        if (arguments.length < 3) {
            textChannel.sendMessage("Remainder needs a day to activate on").queue();
            return;
        }
        final String reminderDay = arguments[2];
        final DayOfWeek activationDay;
        try {
            if ("any".equals(reminderDay.toLowerCase())) {
                activationDay = null;
            } else {
                activationDay = DayOfWeek.valueOf(reminderDay.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            textChannel.sendMessage("Day must be weekday written in full, for example, 'Sunday', or special day 'Any' for daily activation").queue();
            return;
        }

        if (arguments.length < 4) {
            textChannel.sendMessage("Remainder needs a time to activate on").queue();
            return;
        }
        final String reminderTime = arguments[3];
        final LocalTime activationTime;
        try {
            activationTime = LocalTime.parse(reminderTime);
        } catch (DateTimeParseException e) {
            textChannel.sendMessage("Unknown time: " + reminderTime + " provide time in format hh:mm").queue();
            return;
        }

        if (arguments.length < 5) {
            textChannel.sendMessage("Remainder needs a message to send at scheduled time").queue();
            return;
        }
        final String messageInput = arguments[4];
        final Remainder remainder = new Remainder(textChannel.getJDA(), guildData,
                remainderName, textChannel.getIdLong(), matcher.getMember().getIdLong(),
                messageInput, activationDay, activationTime);
        try {
            if (!remainders.addRemainder(remainder)) {
                textChannel.sendMessage("Matching remainder already exists.").queue();
                return;
            }
            final String firstActivation = dateFormat.format(remainder.getActivationDate());
            textChannel.sendMessage("Remainder succesfully created.\n First activation at: " + firstActivation).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage("Database error adding remainder, "
                    + "add again once database issue is fixed to make add persist after reboot").queue();
            LOGGER.error("Failure to create remainder");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private void deleteRemainder(String[] arguments, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        if (arguments.length < 2) {
            textChannel.sendMessage("Provide the name of the remainder you want to delete").queue();
            return;
        }
        final RemainderManager remainders = guildData.getRemainderManager();

        final String remainderName = arguments[1];
        final Optional<Remainder> oldRemainder = remainders.getRemainder(remainderName);
        if (oldRemainder.isEmpty()) {
            textChannel.sendMessage("Could not find remainder with name: " + remainderName).queue();
            return;
        }
        final Remainder remainder = oldRemainder.get();

        //Check if user has permission to remove the event
        final Member sender = matcher.getMember();
        final Member remainderOwner = textChannel.getGuild().getMemberById(remainder.getAuthor());
        final boolean hasPermission = PermissionUtilities.hasPermission(sender, remainderOwner);
        if (!hasPermission) {
            textChannel.sendMessage("You do not have permission to remove that remainder, "
                    + "only the remainder owner and admins can remove remainders").queue();
            return;
        }

        try {
            if (remainders.deleteRemainder(remainder)) {
                textChannel.sendMessage("Remainder succesfully removed").queue();
                return;
            }
            textChannel.sendMessage("Could not find such remainder").queue();
        } catch (SQLException ex) {
            textChannel.sendMessage("Database error removing remainder, "
                    + "remove again once issue is fixed to make remove persistent").queue();

            LOGGER.error("Failure to delete remainder");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private void listRemainders(CommandMatcher matcher, GuildDataStore guildData) {
        final RemainderManager remainders = guildData.getRemainderManager();
        final List<Remainder> ev = remainders.getRemainders();
        final StringBuilder sb = new StringBuilder("Remainders:\n");
        for (Remainder r : ev) {
            if (r.isValid()) {
                sb.append(r.toString()).append('\n');
            }
        }
        if (ev.isEmpty()) {
            sb.append("No remainders found.");
        }

        final TextChannel textChannel = matcher.getTextChannel();
        textChannel.sendMessage(sb.toString()).queue();
    }

}
