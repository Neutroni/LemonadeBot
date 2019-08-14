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
package eternal.lemonadebot.commands;

import eternal.lemonadebot.commandtypes.UserCommand;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.Event;
import eternal.lemonadebot.database.EventManager;
import eternal.lemonadebot.messages.CommandManager;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandPermission;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
class EventCommand extends UserCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    private final CommandManager commandParser;
    private final EventManager events;

    /**
     * Constructor
     *
     * @param parser parser to use for parsing commands
     * @param db database to store events in
     */
    EventCommand(CommandManager parser, DatabaseManager db) {
        this.commandParser = parser;
        this.events = db.getEvents();
    }

    @Override
    public String getCommand() {
        return "event";
    }

    @Override
    public String getHelp() {
        return "create - create new event, you will join the event automatically\n"
                + "join - join an event\n"
                + "leave - leave an event\n"
                + "delete - deletes an event\n"
                + "members - list members for event\n"
                + "clear - clears event member list\n"
                + "list - list events";
    }

    @Override
    public void respond(Member sender, Message message, TextChannel textChannel) {
        final CommandMatcher matcher = commandParser.getCommandMatcher(message);
        final String[] opts = matcher.getArguments(2);
        if (opts.length == 0) {
            textChannel.sendMessage("Provide operation to perform, check help for possible operations").queue();
            return;
        }

        switch (opts[0]) {
            case "create": {
                if (opts.length == 1) {
                    textChannel.sendMessage("Provide name of the event to create").queue();
                    return;
                }
                final String eventName = opts[1];

                try {
                    final String description;
                    if (opts.length == 3) {
                        description = opts[2];
                    } else {
                        description = "No description";
                    }
                    final Event newEvent = new Event(eventName, description, sender.getId());
                    if (events.addEvent(newEvent)) {
                        textChannel.sendMessage("Event created succesfully").queue();
                        return;
                    }
                    textChannel.sendMessage("Event with that name alredy exists").queue();
                } catch (SQLException ex) {
                    textChannel.sendMessage("Database error adding event, "
                            + "add again once database issue is fixed to make add persist after reboot").queue();
                    LOGGER.error("Failure to create event");
                    LOGGER.warn(ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case "join": {
                if (opts.length == 1) {
                    textChannel.sendMessage("Provide name of the event to join").queue();
                    return;
                }
                final String eventName = opts[1];

                final Optional<Event> oldEvent = events.getEvent(eventName);
                if (oldEvent.isEmpty()) {
                    textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    return;
                }
                try {
                    if (events.joinEvent(oldEvent.get(), sender)) {
                        textChannel.sendMessage("Succesfully joined event").queue();
                        return;
                    }
                    textChannel.sendMessage("You have alredy joined that event").queue();
                } catch (SQLException ex) {
                    textChannel.sendMessage("Database error joining event, " + "join again once database issue is fixed to make joining persist after reboot").queue();

                    LOGGER.error("Failure to join event");
                    LOGGER.warn(ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case "leave": {
                if (opts.length == 1) {
                    textChannel.sendMessage("Provide name of the event to leave").queue();
                    return;
                }
                final String eventName = opts[1];

                final Optional<Event> oldEvent = events.getEvent(eventName);
                if (oldEvent.isEmpty()) {
                    textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    return;
                }
                try {
                    if (events.leaveEvent(oldEvent.get(), sender.getId())) {
                        textChannel.sendMessage("Succesfully left event").queue();
                        return;
                    }
                    textChannel.sendMessage("You have not joined that event").queue();
                } catch (SQLException ex) {
                    textChannel.sendMessage("Database error leaving event, "
                            + "leave again once database issue is fixed to make leave persist after reboot").queue();
                    LOGGER.error("Failure to leave event");
                    LOGGER.warn(ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case "delete": {
                if (opts.length == 1) {
                    textChannel.sendMessage("Provide name of the event to delete").queue();
                    return;
                }
                final String eventName = opts[1];

                final Optional<Event> oldEvent = events.getEvent(eventName);
                if (oldEvent.isEmpty()) {
                    textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    return;
                }
                final Event event = oldEvent.get();
                //Check if user has permission to remove the event
                final Member eventOwner = textChannel.getGuild().getMemberById(event.getOwner());
                final boolean hasPermission;
                if (eventOwner == null) {
                    hasPermission = (commandParser.getRank(sender).ordinal() >= CommandPermission.ADMIN.ordinal());
                } else {
                    hasPermission = commandParser.hasPermission(sender, eventOwner);
                }
                if (!hasPermission) {
                    textChannel.sendMessage("You do not have permission to remove that event, " + "only the event owner and admins can remove events").queue();
                    return;
                }
                try {
                    if (events.removeEvent(oldEvent.get())) {
                        textChannel.sendMessage("Event succesfully removed").queue();
                    } else {
                        textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    }
                } catch (SQLException ex) {
                    textChannel.sendMessage("Database error removing event, "
                            + "remove again once issue is fixed to make remove persistent").queue();

                    LOGGER.error("Failure to create event");
                    LOGGER.warn(ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case "members": {
                if (opts.length == 1) {
                    textChannel.sendMessage("Provide name of the event to show members for").queue();
                    return;
                }
                final String eventName = opts[1];

                final Optional<Event> opt = this.events.getEvent(eventName);
                if (opt.isEmpty()) {
                    textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    return;
                }
                final Event event = opt.get();
                final List<String> memberIds = event.getMembers();
                final StringBuilder sb = new StringBuilder("Members for the event: " + eventName + "\n");
                for (String id : memberIds) {
                    final Member m = textChannel.getGuild().getMemberById(id);
                    if (m == null) {
                        sb.append("Found user in event members who could not be found, removing from event\n");
                        try {
                            events.leaveEvent(event, id);
                            sb.append("Succesfully removed missing member from event\n");
                        } catch (SQLException ex) {
                            sb.append("Database error removing member from event\n");

                            LOGGER.error("Failure to remove member from event");
                            LOGGER.warn(ex.getMessage());
                            LOGGER.trace("Stack trace", ex);
                        }
                        continue;
                    }
                    sb.append(' ').append(m.getEffectiveName()).append('\n');
                }
                textChannel.sendMessage(sb.toString()).queue();
                break;
            }
            case "clear": {
                if (opts.length == 1) {
                    textChannel.sendMessage("Provide name of the event to clear").queue();
                    return;
                }
                final String eventName = opts[1];

                final Optional<Event> opt = this.events.getEvent(eventName);
                if (opt.isEmpty()) {
                    textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    return;
                }
                final Event event = opt.get();
                if (!event.getOwner().equals(sender.getId())) {
                    textChannel.sendMessage("Only the owner of the event can clear it.").queue();
                    return;
                }
                try {
                    events.clearEvent(event);
                    textChannel.sendMessage("Succesfully cleared the event").queue();
                } catch (SQLException ex) {
                    textChannel.sendMessage("Database error clearing event, " + "clear again once the issue is ").queue();

                    LOGGER.error("Failure to clear event");
                    LOGGER.warn(ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case "list": {
                final List<Event> ev = this.events.getEvents();
                final StringBuilder sb = new StringBuilder("Events:\n");
                for (Event e : ev) {
                    sb.append(' ').append(e.getName()).append(" - ").append(e.getDescription()).append('\n');
                }
                if (ev.isEmpty()) {
                    sb.append("No events found.");
                }
                textChannel.sendMessage(sb.toString()).queue();
                break;
            }
            default: {
                textChannel.sendMessage("Unkown operation: " + opts[0]).queue();
            }
        }
    }

}
