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
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE MMMM dd HH:mm zzz", Locale.ENGLISH);

    /**
     * Constructor
     *
     * @param db database to store remainders in
     */
    public RemainderCommand(DatabaseManager db) {
        this.remainderManager = db.getRemainders();
        this.eventManager = db.getEvents();
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
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

        final String[] arguments = message.getArguments(5);
        if (arguments.length == 0) {
            textChannel.sendMessage("Provide action to perform, see help for possible actions").queue();
            return;
        }
        switch (arguments[0]) {
            case "create": {
                createRemainder(arguments, textChannel);
                break;
            }
            case "delete": {
                deleteRemainder(arguments, textChannel);
                break;
            }
            case "list": {
                listRemainders(textChannel);
                break;
            }
            default: {
                textChannel.sendMessage("Unkown operation: " + arguments[0]).queue();
            }
        }
    }

    private void createRemainder(String[] arguments, TextChannel textChannel) {
        if (arguments.length < 5) {
            textChannel.sendMessage("Missing arguments, see help for event").queue();
            return;
        }
        final String eventName = arguments[1];
        final Optional<Event> optEvent = this.eventManager.getEvent(eventName);
        if (optEvent.isEmpty()) {
            textChannel.sendMessage("Could not find event with name: " + eventName).queue();
            return;
        }
        final String reminderDay = arguments[2];
        DayOfWeek activationDay;
        try {
            activationDay = DayOfWeek.valueOf(reminderDay.toUpperCase());
        } catch (IllegalArgumentException e) {
            textChannel.sendMessage("Day must be weekday written in full, for example, 'Sunday'").queue();
            return;
        }
        final String reminderTime = arguments[3];
        LocalTime activationTime;
        try {
            activationTime = LocalTime.parse(reminderTime);
        } catch (DateTimeParseException e) {
            textChannel.sendMessage("Unkown time: " + reminderTime + " provide time in format hh:mm").queue();
            return;
        }
        final String mentions = arguments[4];
        final MentionEnum me = MentionEnum.getByName(mentions);
        if (me == MentionEnum.ERROR) {
            textChannel.sendMessage("Unkown mention value: " + mentions + "\nSee help for available values for mentions").queue();
            return;
        }

        final Remainder remainder = this.remainderManager.build(textChannel.getIdLong(), optEvent.get(), me, activationDay, activationTime);
        try {
            if (!this.remainderManager.addRemainder(remainder)) {
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

    private void deleteRemainder(String[] arguments, TextChannel textChannel) {
        if (arguments.length < 4) {
            textChannel.sendMessage("Provide the name of the remainder you want to delete").queue();
            return;
        }
        final String eventName = arguments[1];
        final String remainderDay = arguments[2];
        final String remainderTime = arguments[3];
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
    }

    private void listRemainders(TextChannel textChannel) {
        final List<Remainder> ev = this.remainderManager.getRemainders();
        final StringBuilder sb = new StringBuilder("Remainders:\n");
        for (Remainder r : ev) {
            if (r.isValid()) {
                sb.append(r.toString()).append('\n');
                continue;
            }
            sb.append("Remainder in database that has no valid channel, removing");
            try {
                if (this.remainderManager.deleteRemainder(r)) {
                    sb.append("Remainder removed succesfully\n");
                } else {
                    sb.append("Remainder alreayd removed by someone else\n");
                }
            } catch (SQLException ex) {
                sb.append("Database failure in removing remainder from database\n");

                LOGGER.error("Failure to remove remainder while listing remainders");
                LOGGER.warn(ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        }
        if (ev.isEmpty()) {
            sb.append("No remainders found.");
        }
        textChannel.sendMessage(sb.toString()).queue();
    }

}
