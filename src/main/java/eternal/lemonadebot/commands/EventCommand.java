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
import eternal.lemonadebot.commandtypes.MemberCommand;
import eternal.lemonadebot.database.EventManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.events.Event;
import eternal.lemonadebot.permissions.PermissionUtilities;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class EventCommand extends MemberCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand() {
        return "event";
    }

    @Override
    public String getDescription() {
        return "Add events for people to join";
    }

    @Override
    public String getHelpText() {
        return "Syntax: event <action> <name> [description]\n"
                + "<action> can be one of the following:\n"
                + " create - create new event, you will join the event automatically\n"
                + " delete - deletes an event\n"
                + " join - join an event\n"
                + " leave - leave an event\n"
                + " members - list members for event\n"
                + " clear - clears event member list\n"
                + " list - list active events\n"
                + " ping - ping event members\n"
                + " random - pick random member from event"
                + "<name> is the name of the event\n"
                + "[description] description for the event";
    }

    @Override
    public void respond(CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();

        final String[] opts = matcher.getArguments(2);
        if (opts.length == 0) {
            textChannel.sendMessage("Provide action to perform, check help for possible actions.").queue();
            return;
        }

        switch (opts[0]) {
            case "create": {
                createEvent(opts, matcher, guildData);
                break;
            }
            case "delete": {
                deleteEvent(opts, matcher, guildData);
                break;
            }
            case "join": {
                joinEvent(opts, matcher, guildData);
                break;
            }
            case "leave": {
                leaveEvent(opts, matcher, guildData);
                break;
            }
            case "members": {
                showEventMembers(opts, matcher, guildData);
                break;
            }
            case "clear": {
                clearEventMembers(opts, matcher, guildData);
                break;
            }
            case "list": {
                listEvents(matcher, guildData);
                break;
            }
            case "ping": {
                pingEventMembers(opts, matcher, guildData);
                break;
            }
            case "random": {
                pickRandomEventMember(opts, matcher, guildData);
                break;
            }
            default: {
                textChannel.sendMessage("Unknown operation: " + opts[0]).queue();
            }
        }
    }

    private void createEvent(String[] opts, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Member sender = matcher.getMember();
        final EventManager events = guildData.getEventManager();
        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to create.").queue();
            return;
        }
        final String eventName = opts[1];
        final String description;
        if (opts.length == 3) {
            description = opts[2];
        } else {
            description = "No description";
        }
        final Event newEvent = new Event(eventName, description, sender);

        try {
            if (!events.addEvent(newEvent)) {
                textChannel.sendMessage("Event with that name alredy exists.").queue();
                return;
            }
            if (!events.joinEvent(newEvent, sender)) {
                textChannel.sendMessage("Event created but failed to join the event.").queue();
                return;
            }
            textChannel.sendMessage("Event created succesfully").queue();
        } catch (SQLException ex) {
            textChannel.sendMessage("Database error adding event, "
                    + "add again once database issue is fixed to make add persist after reboot").queue();
            LOGGER.error("Failure to create event");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private void deleteEvent(String[] opts, CommandMatcher matcher, GuildDataStore guildData) {
        final Member sender = matcher.getMember();
        final TextChannel textChannel = matcher.getTextChannel();
        final EventManager events = guildData.getEventManager();
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
        final boolean hasPermission = PermissionUtilities.hasPermission(sender, eventOwner);
        if (!hasPermission) {
            textChannel.sendMessage("You do not have permission to remove that event, " + "only the event owner and admins can remove events").queue();
            return;
        }

        try {
            if (events.removeEvent(event)) {
                textChannel.sendMessage("Event succesfully removed").queue();
                return;
            }
            textChannel.sendMessage("Could not find event with name: " + eventName).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage("Database error removing event, "
                    + "remove again once issue is fixed to make remove persistent").queue();

            LOGGER.error("Failure to remove event");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private void joinEvent(String[] opts, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Member sender = matcher.getMember();

        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to join").queue();
            return;
        }
        final String eventName = opts[1];

        final EventManager events = guildData.getEventManager();
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
    }

    private void leaveEvent(String[] opts, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Member sender = matcher.getMember();

        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to leave").queue();
            return;
        }
        final String eventName = opts[1];

        final EventManager events = guildData.getEventManager();
        final Optional<Event> oldEvent = events.getEvent(eventName);
        if (oldEvent.isEmpty()) {
            textChannel.sendMessage("Could not find event with name: " + eventName).queue();
            return;
        }
        try {
            if (events.leaveEvent(oldEvent.get(), sender.getIdLong())) {
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
    }

    private void showEventMembers(String[] opts, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();

        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to show members for").queue();
            return;
        }
        final String eventName = opts[1];

        final EventManager events = guildData.getEventManager();
        final Optional<Event> opt = events.getEvent(eventName);
        if (opt.isEmpty()) {
            textChannel.sendMessage("Could not find event with name: " + eventName).queue();
            return;
        }
        final Event event = opt.get();
        final List<Long> memberIds = event.getMembers();
        final StringBuilder sb = new StringBuilder("Members for the event " + eventName + ":\n");
        for (Long id : memberIds) {
            final Member m = textChannel.getGuild().getMemberById(id);
            if (m == null) {
                LOGGER.warn("Found user in event members who could not be found, removing from event\n");
                try {
                    events.leaveEvent(event, id);
                    LOGGER.info("Succesfully removed missing member from event\n");
                } catch (SQLException ex) {
                    LOGGER.error("Failure to remove member from event");
                    LOGGER.warn(ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                continue;
            }
            sb.append(' ').append(m.getEffectiveName()).append('\n');
        }
        if (memberIds.isEmpty()) {
            sb.append("No one has joined the event yet.");
        }
        textChannel.sendMessage(sb.toString()).queue();
    }

    private void clearEventMembers(String[] opts, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Member sender = matcher.getMember();

        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to clear").queue();
            return;
        }
        final String eventName = opts[1];

        final EventManager events = guildData.getEventManager();
        final Optional<Event> opt = events.getEvent(eventName);
        if (opt.isEmpty()) {
            textChannel.sendMessage("Could not find event with name: " + eventName).queue();
            return;
        }
        final Event event = opt.get();
        if (event.getOwner() != sender.getIdLong()) {
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
    }

    private void listEvents(CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final EventManager events = guildData.getEventManager();

        final List<Event> ev = events.getEvents();
        final StringBuilder sb = new StringBuilder("Events:\n");
        for (Event e : ev) {
            sb.append(' ').append(e.toString()).append('\n');
        }
        if (ev.isEmpty()) {
            sb.append("No events found.");
        }
        textChannel.sendMessage(sb.toString()).queue();
    }

    private void pingEventMembers(String[] opts, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();

        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to ping members for").queue();
            return;
        }
        final String eventName = opts[1];

        final EventManager events = guildData.getEventManager();
        final Optional<Event> opt = events.getEvent(eventName);
        if (opt.isEmpty()) {
            textChannel.sendMessage("Could not find event with name: " + eventName).queue();
            return;
        }
        final Event event = opt.get();
        final Member sender = matcher.getMember();
        if (event.getOwner() != sender.getIdLong()) {
            textChannel.sendMessage("Only the owner of the event can ping event members.").queue();
            return;
        }
        final List<Long> memberIds = event.getMembers();
        final MessageBuilder mb = new MessageBuilder("Ping!\n");
        for (final Long id : memberIds) {
            if (id == sender.getIdLong()) {
                continue;
            }
            final Member m = textChannel.getGuild().getMemberById(id);
            if (m != null) {
                mb.append(m);
            }
        }
        textChannel.sendMessage(mb.build()).queue();
    }

    private void pickRandomEventMember(String[] opts, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();

        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to ping members for").queue();
            return;
        }
        final String eventName = opts[1];
        final Guild guild = matcher.getGuild();
        final EventManager events = guildData.getEventManager();
        final Optional<Event> optEvent = events.getEvent(eventName);
        if (optEvent.isEmpty()) {
            textChannel.sendMessage("No such event: " + eventName).queue();
            return;
        }

        final Event event = optEvent.get();
        final List<Long> eventMemberIDs = event.getMembers();
        final List<Long> memberIDsMutable = new ArrayList<>(eventMemberIDs);
        Collections.shuffle(memberIDsMutable);
        for (final Long id : memberIDsMutable) {
            final Member m = guild.retrieveMemberById(id).complete();
            if (m == null) {
                LOGGER.warn("Found user in event members who could not be found, removing from event\n");
                try {
                    events.leaveEvent(event, id);
                    LOGGER.info("Succesfully removed missing member from event\n");
                } catch (SQLException ex) {
                    LOGGER.error("Failure to remove member from event");
                    LOGGER.warn(ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                continue;
            }
            textChannel.sendMessage("Selected " + m.getEffectiveName() + " from the event.").queue();
            return;
        }
        textChannel.sendMessage("No members in that event").queue();
    }
}
