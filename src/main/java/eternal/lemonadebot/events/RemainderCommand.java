/*
 * The MIT License
 *
 * Copyright 2019 Neutroni.
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
package eternal.lemonadebot.events;

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.EventManager;
import eternal.lemonadebot.database.RemainderManager;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandPermission;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.MessageBuilder;
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

    private final RemainderManager remainderManager;
    private final EventManager eventManager;

    /**
     * Constructor
     *
     * @param db database to store remainders in
     */
    public RemainderCommand(DatabaseManager db) {
        this.remainderManager = db.getRemainders();
        this.eventManager = db.getEvents();
    }

    @Override
    public String getCommand() {
        return "remainder";
    }

    @Override
    public String getHelp() {
        return "Syntax: remainder <action> <event> <day> <time> [mention]\n"
                + "<action> can be one of the following:"
                + "  create - create new remainder\n"
                + "  delete - delete remainder\n"
                + "<event> event to use description from\n"
                + "<day> day the remainder activates on\n"
                + "<time> time of the remainder hh:mm\n"
                + "[mention] can be on of the following:\n"
                + "  none - do not mention anyone\n"
                + "  here - ping online people\n"
                + "  members - ping all members of the event\n"
                + "Remainder will be activated on the channel it was created in";
    }

    @Override
    public CommandPermission getPermission() {
        return CommandPermission.ADMIN;
    }

    @Override
    public void respond(CommandMatcher message) {
        final Optional<TextChannel> optChannel = message.getTextChannel();
        if (optChannel.isEmpty()) {
            message.getMessageChannel().sendMessage("Remainders are specific to discord servers and must be edited on one.").queue();
            return;
        }
        final TextChannel textChannel = optChannel.get();

        final String[] args = message.getArguments(5);
        if (args.length == 0) {
            textChannel.sendMessage("Provide action to perform, see help for possible actions").queue();
            return;
        }
        switch (args[0]) {
            case "create": {
                if (args.length < 5) {
                    textChannel.sendMessage("Missing arguments, see help for event").queue();
                    return;
                }
                final String eventName = args[1];
                final Optional<Event> optEvent = this.eventManager.getEvent(eventName);
                if (optEvent.isEmpty()) {
                    textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    return;
                }
                final String reminderDay = args[2];
                DayOfWeek activationDay;
                try {
                    activationDay = DayOfWeek.valueOf(reminderDay.toUpperCase());
                } catch (IllegalArgumentException e) {
                    textChannel.sendMessage("Day must be weekday written in full, for example, 'Sunday'").queue();
                    return;
                }
                final String reminderTime = args[3];
                LocalTime activationTime;
                try {
                    activationTime = LocalTime.parse(reminderTime);
                } catch (DateTimeParseException e) {
                    textChannel.sendMessage("Unkown time: " + reminderTime + " provide time in format hh:mm").queue();
                    return;
                }
                final String mentions = args[4];
                final MentionEnum me = MentionEnum.getByName(mentions);
                if (me == MentionEnum.ERROR) {
                    textChannel.sendMessage("Unkown mention value: " + mentions + "\nSee help for available values for mentions").queue();
                    return;
                }

                final Remainder remainder = new Remainder(textChannel, optEvent.get(), me, activationDay, activationTime);
                try {
                    if (!this.remainderManager.addRemainder(remainder)) {
                        textChannel.sendMessage("Matching remainder already exists.").queue();
                        return;
                    }
                    textChannel.sendMessage("Remainder succesfully created").queue();
                } catch (SQLException ex) {
                    textChannel.sendMessage("Database error adding remainder, "
                            + "add again once database issue is fixed to make add persist after reboot").queue();
                    LOGGER.error("Failure to create remainder");
                    LOGGER.warn(ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case "delete": {
                if (args.length < 4) {
                    textChannel.sendMessage("Provide the name of the remainder you want to delete").queue();
                    return;
                }
                final String eventName = args[1];
                final String remainderDay = args[2];
                final String remainderTime = args[3];
                //Get the remainder to delete
                final Optional<Remainder> optRemainder = this.remainderManager.getRemainder(eventName, remainderDay, remainderTime);
                if (optRemainder.isEmpty()) {
                    textChannel.sendMessage("Could not find such remainder").queue();
                    return;
                }
                try {
                    if (this.remainderManager.deleteRemainder(optRemainder.get())) {
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
                break;
            }
            case "list": {
                final List<Remainder> ev = this.remainderManager.getRemainders();
                final StringBuilder sb = new StringBuilder("Remainders:\n");
                for (Remainder e : ev) {
                    //gdds sunday 17:00 on channel #general
                    sb.append(e.getEvent().getName()).append(' ')
                            .append(e.getDay().toString().toLowerCase()).append(' ').append(e.getTime())
                            .append(" on channel ").append(e.getChannel().getAsMention())
                            .append('\n');

                }
                if (ev.isEmpty()) {
                    sb.append("No remainders found.");
                }
                textChannel.sendMessage(sb.toString()).queue();
                break;
            }
            default: {
                textChannel.sendMessage("Unkown operation: " + args[0]).queue();
            }
        }
    }

}
