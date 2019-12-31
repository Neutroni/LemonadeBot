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

import eternal.lemonadebot.commandtypes.UserCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.EventManager;
import eternal.lemonadebot.database.GuildConfigManager;
import eternal.lemonadebot.database.RemainderManager;
import eternal.lemonadebot.messages.CommandManager;
import eternal.lemonadebot.messages.CommandMatcher;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class EventCommand extends UserCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    private final CommandManager commandParser;
    private final EventManager eventManager;
    private final RemainderManager remainderManager;
    private final ConfigManager configManager;

    /**
     * Constructor
     *
     * @param parser parser to use for parsing commands
     * @param db database to store events in
     */
    public EventCommand(CommandManager parser, DatabaseManager db) {
        this.commandParser = parser;
        this.eventManager = db.getEvents();
        this.remainderManager = db.getRemainders();
        this.configManager = db.getConfig();
    }

    @Override
    public String getCommand() {
        return "event";
    }

    @Override
    public String getHelp() {
        return "Syntax: event <action> <name> [description]\n"
                + "<action> can be one of the following:\n"
                + "  create - create new event, you will join the event automatically\n"
                + "  delete - deletes an event\n"
                + "  join - join an event\n"
                + "  leave - leave an event\n"
                + "  members - list members for event\n"
                + "  clear - clears event member list\n"
                + "  list - list active events\n"
                + "<name> is the name of the event\n"
                + "[description] description for the event";
    }

    @Override
    public void respond(CommandMatcher matcher) {
        final Optional<TextChannel> optChannel = matcher.getTextChannel();
        final Optional<Member> optMember = matcher.getMember();
        if (optChannel.isEmpty() || optMember.isEmpty()) {
            matcher.getMessageChannel().sendMessage("Events are specific to discord servers and must be edited on one.").queue();
            return;
        }
        final TextChannel textChannel = optChannel.get();
        final Member sender = optMember.get();

        final String[] opts = matcher.getArguments(2);
        if (opts.length == 0) {
            textChannel.sendMessage("Provide action to perform, check help for possible actions.").queue();
            return;
        }

        switch (opts[0]) {
            case "create": {
                createEvent(opts, textChannel, sender);
                break;
            }
            case "delete": {
                deleteEvent(opts, textChannel, sender);
                break;
            }
            case "join": {
                joinEvent(opts, textChannel, sender);
                break;
            }
            case "leave": {
                leaveEvent(opts, textChannel, sender);
                break;
            }
            case "members": {
                showEventMembers(opts, textChannel);
                break;
            }
            case "clear": {
                clearEventMembers(opts, textChannel, sender);
                break;
            }
            case "list": {
                listEvents(textChannel);
                break;
            }
            default: {
                textChannel.sendMessage("Unkown operation: " + opts[0]).queue();
            }
        }
    }

    private void createEvent(String[] opts, TextChannel textChannel, Member sender) {
        final GuildConfigManager guildConf = this.configManager.getGuildConfig(textChannel.getGuild());
        if (!this.commandParser.hasPermission(sender, guildConf.getEditPermission())) {
            textChannel.sendMessage("You do not have permission to create events").queue();
            return;
        }
        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to create.").queue();
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
            final Event newEvent = new Event(eventName, description, sender, textChannel.getGuild());
            if (!eventManager.addEvent(newEvent)) {
                textChannel.sendMessage("Event with that name alredy exists.").queue();
                return;
            }
            if (!eventManager.joinEvent(newEvent, sender)) {
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

    private void deleteEvent(String[] opts, TextChannel textChannel, Member sender) {
        final GuildConfigManager guildConf = this.configManager.getGuildConfig(textChannel.getGuild());
        if (!this.commandParser.hasPermission(sender, guildConf.getEditPermission())) {
            textChannel.sendMessage("You do not have permission to delete events").queue();
            return;
        }
        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to delete").queue();
            return;
        }
        final String eventName = opts[1];

        final Optional<Event> oldEvent = eventManager.getEvent(eventName, textChannel.getGuild());
        if (oldEvent.isEmpty()) {
            textChannel.sendMessage("Could not find event with name: " + eventName).queue();
            return;
        }
        final Event event = oldEvent.get();

        //Check if user has permission to remove the event
        final Member eventOwner = textChannel.getGuild().getMemberById(event.getOwner());
        final boolean hasPermission = commandParser.hasPermission(sender, eventOwner);
        if (!hasPermission) {
            textChannel.sendMessage("You do not have permission to remove that event, " + "only the event owner and admins can remove events").queue();
            return;
        }

        try {
            //Delete remainders for the event
            for (Remainder r : this.remainderManager.getRemainders(textChannel.getGuild())) {
                if (event.equals(r.getEvent())) {
                    try {
                        this.remainderManager.deleteRemainder(r);
                    } catch (SQLException e) {
                        LOGGER.warn("Error removing remainder for event about to be removed");
                        LOGGER.warn(e.getMessage());
                        LOGGER.trace("Stack Trace", e);
                    }
                }
            }
            //Delete event
            if (eventManager.removeEvent(event)) {
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

    private void joinEvent(String[] opts, TextChannel textChannel, Member sender) {
        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to join").queue();
            return;
        }
        final String eventName = opts[1];

        final Optional<Event> oldEvent = eventManager.getEvent(eventName, textChannel.getGuild());
        if (oldEvent.isEmpty()) {
            textChannel.sendMessage("Could not find event with name: " + eventName).queue();
            return;
        }
        try {
            if (eventManager.joinEvent(oldEvent.get(), sender)) {
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

    private void leaveEvent(String[] opts, TextChannel textChannel, Member sender) {
        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to leave").queue();
            return;
        }
        final String eventName = opts[1];

        final Optional<Event> oldEvent = eventManager.getEvent(eventName, textChannel.getGuild());
        if (oldEvent.isEmpty()) {
            textChannel.sendMessage("Could not find event with name: " + eventName).queue();
            return;
        }
        try {
            if (eventManager.leaveEvent(oldEvent.get(), sender.getIdLong())) {
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

    private void showEventMembers(String[] opts, TextChannel textChannel) {
        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to show members for").queue();
            return;
        }
        final String eventName = opts[1];

        final Optional<Event> opt = this.eventManager.getEvent(eventName, textChannel.getGuild());
        if (opt.isEmpty()) {
            textChannel.sendMessage("Could not find event with name: " + eventName).queue();
            return;
        }
        final Event event = opt.get();
        final List<Long> memberIds = event.getMembers();
        final StringBuilder sb = new StringBuilder("Members for the event: " + eventName + "\n");
        for (Long id : memberIds) {
            final Member m = textChannel.getGuild().getMemberById(id);
            if (m == null) {
                LOGGER.warn("Found user in event members who could not be found, removing from event\n");
                try {
                    eventManager.leaveEvent(event, id);
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

    private void clearEventMembers(String[] opts, TextChannel textChannel, Member sender) {
        if (opts.length == 1) {
            textChannel.sendMessage("Provide name of the event to clear").queue();
            return;
        }
        final String eventName = opts[1];

        final Optional<Event> opt = this.eventManager.getEvent(eventName, textChannel.getGuild());
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
            eventManager.clearEvent(event);
            textChannel.sendMessage("Succesfully cleared the event").queue();
        } catch (SQLException ex) {
            textChannel.sendMessage("Database error clearing event, " + "clear again once the issue is ").queue();

            LOGGER.error("Failure to clear event");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private void listEvents(TextChannel textChannel) {
        final List<Event> ev = this.eventManager.getEvents(textChannel.getGuild());
        final StringBuilder sb = new StringBuilder("Events:\n");
        for (Event e : ev) {
            if (e.getGuild() == textChannel.getGuild().getIdLong()) {
                sb.append(' ').append(e.getName()).append(" - ").append(e.getDescription()).append('\n');
            }
        }
        if (ev.isEmpty()) {
            sb.append("No events found.");
        }
        textChannel.sendMessage(sb.toString()).queue();
    }
}
