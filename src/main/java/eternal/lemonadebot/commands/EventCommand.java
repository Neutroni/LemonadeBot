/*
 * The MIT License
 *
 * Copyright 2019 joonas.
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
import eternal.lemonadebot.database.DatabaseException;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandParser;
import eternal.lemonadebot.stores.Event;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author joonas
 */
class EventCommand extends UserCommand {

    private final CommandParser commandParser;
    private final DatabaseManager DATABASE;

    /**
     * Constructor
     *
     * @param parser parser to use for parsing commands
     * @param db database to store events in
     */
    EventCommand(CommandParser parser, DatabaseManager db) {
        this.commandParser = parser;
        this.DATABASE = db;
    }

    @Override
    public String getCommand() {
        return "event";
    }

    @Override
    public String getHelp() {
        return "create - create new event, you will join the event automatically\n" + "join - join an event\n" + "leave - leave an event\n" + "delete - deletes an event\n" + "members - list members for event\n" + "clear - clears event member list\n" + "list - list events";
    }

    @Override
    public synchronized void respond(Member sender, Message message, TextChannel textChannel) {
        final CommandMatcher matcher = commandParser.getCommandMatcher(message);
        final String[] opts = matcher.getArguments(2);
        if (opts.length == 0) {
            textChannel.sendMessage("Provide operation to perform, check help for possible operations").queue();
            return;
        }
        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to operate on").queue();
            return;
        }
        final String eventName = opts[1];
        switch (opts[0]) {
            case "create": {
                try {
                    final String description;
                    if (opts.length == 3) {
                        description = opts[2];
                    } else {
                        description = "No description";
                    }
                    final Event newEvent = new Event(eventName, description, sender.getId());
                    if (DATABASE.addEvent(newEvent)) {
                        textChannel.sendMessage("Event created succesfully").queue();
                        return;
                    }
                    textChannel.sendMessage("Event with that name alredy exists").queue();
                } catch (DatabaseException ex) {
                    textChannel.sendMessage("Database error adding event, " + "add again once database issue is fixed to make add persist after reboot").queue();
                }
                break;
            }
            case "join": {
                final Optional<Event> oldEvent = DATABASE.getEventStore().getEvent(eventName);
                if (oldEvent.isEmpty()) {
                    textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    return;
                }
                try {
                    if (DATABASE.joinEvent(sender, oldEvent.get())) {
                        textChannel.sendMessage("Succesfully joined event").queue();
                        return;
                    }
                    textChannel.sendMessage("You have alredy joined that event").queue();
                } catch (DatabaseException ex) {
                    textChannel.sendMessage("Database error joining event, " + "join again once database issue is fixed to make joining persist after reboot").queue();
                }
                break;
            }
            case "leave": {
                final Optional<Event> oldEvent = DATABASE.getEventStore().getEvent(eventName);
                if (oldEvent.isEmpty()) {
                    textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    return;
                }
                try {
                    if (DATABASE.leaveEvent(sender, oldEvent.get())) {
                        textChannel.sendMessage("Succesfully left event").queue();
                        return;
                    }
                    textChannel.sendMessage("You have not joined that event").queue();
                } catch (DatabaseException ex) {
                    textChannel.sendMessage("Database error leaving event, " + "leave again once database issue is fixed to make leave persist after reboot").queue();
                }
                break;
            }
            case "delete": {
                final Optional<Event> oldEvent = DATABASE.getEventStore().getEvent(eventName);
                if (oldEvent.isEmpty()) {
                    textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    return;
                }
                final Event event = oldEvent.get();
                //Check if user has permission to remove the event
                final User eventOwner = textChannel.getJDA().getUserById(event.getOwner());
                final boolean hasPermission;
                if (eventOwner == null) {
                    hasPermission = DATABASE.isAdmin(sender.getUser()) || DATABASE.isOwner(sender.getUser());
                } else {
                    hasPermission = commandParser.hasPermission(sender.getUser(), eventOwner);
                }
                if (!hasPermission) {
                    textChannel.sendMessage("You do not have permission to remove that event, " + "only the event owner and admins can remove events").queue();
                    return;
                }
                try {
                    if (DATABASE.removeEvent(oldEvent.get())) {
                        textChannel.sendMessage("Event succesfully removed").queue();
                    } else {
                        textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    }
                } catch (DatabaseException ex) {
                    textChannel.sendMessage("Database error removing event, " + "remove again once issue is fixed to make remove persistent").queue();
                }
                break;
            }
            case "members": {
                final Optional<Event> oldEvent = DATABASE.getEventStore().getEvent(eventName);
                if (oldEvent.isEmpty()) {
                    textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    return;
                }
                final Set<String> memberIds = oldEvent.get().getMembers();
                final StringBuilder sb = new StringBuilder();
                for (String id : memberIds) {
                    final Member m = textChannel.getGuild().getMemberById(id);
                    if (m == null) {
                        sb.append("Found user in event members who could not be found, removing from event\n");
                        try {
                            DATABASE.leaveEvent(m, oldEvent.get());
                            sb.append("Succesfully removed missing member from event\n");
                        } catch (DatabaseException ex) {
                            sb.append("Database error removing member from event\n");
                        }
                        continue;
                    }
                    sb.append(' ').append(m.getNickname()).append('\n');
                }
                textChannel.sendMessage(sb.toString()).queue();
                break;
            }
            case "clear": {
                final Optional<Event> oldEvent = DATABASE.getEventStore().getEvent(eventName);
                if (oldEvent.isEmpty()) {
                    textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                    return;
                }
                if (!oldEvent.get().getOwner().equals(sender.getId())) {
                    textChannel.sendMessage("Only the owner of the event can clear it.").queue();
                    return;
                }
                try {
                    DATABASE.clearEvent(oldEvent.get());
                    textChannel.sendMessage("Succesfully cleared the event").queue();
                } catch (DatabaseException ex) {
                    textChannel.sendMessage("Database error clearing event, " + "clear again once the issue is ").queue();
                }
                break;
            }
            case "list": {
                final List<Event> events = DATABASE.getEventStore().getItems();
                if (events.isEmpty()) {
                    textChannel.sendMessage("No event defined").queue();
                    return;
                }
                final StringBuilder sb = new StringBuilder("Events:\n");
                for (Event e : events) {
                    sb.append(' ').append(e.getName()).append(" - ").append(e.getDescription()).append('\n');
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
