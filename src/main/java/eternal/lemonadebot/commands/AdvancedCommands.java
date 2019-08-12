/* The MIT License
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

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.commandtypes.OwnerCommand;
import eternal.lemonadebot.commandtypes.UserCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.DatabaseException;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandParser;
import eternal.lemonadebot.messages.CommandPermission;
import eternal.lemonadebot.stores.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

/**
 * CommandProvider providing commands that use both database and commandparsing
 *
 * @author Neutroni
 */
public class AdvancedCommands implements CommandProvider {

    private final List<ChatCommand> COMMANDS = List.of(
            new EventCommand(),
            new AdminManagmentCommand(),
            new ChannelManagmentCommand(),
            new CommandManagmentCommand()
    );

    private final DatabaseManager DATABASE;
    private final CommandParser commandParser;

    /**
     * Constructor
     *
     * @param db Database for commands to use
     * @param cp commands parser for getting command arguments
     */
    public AdvancedCommands(DatabaseManager db, CommandParser cp) {
        this.DATABASE = db;
        this.commandParser = cp;
    }

    @Override
    public List<ChatCommand> getCommands() {
        return COMMANDS;
    }

    private class EventCommand extends UserCommand {

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
                        final Event newEvent = new Event(eventName, sender.getId());
                        if (DATABASE.addEvent(newEvent)) {
                            textChannel.sendMessage("Event created succesfully").queue();
                            return;
                        }
                        textChannel.sendMessage("Event with that name alredy exists").queue();
                    } catch (DatabaseException ex) {
                        textChannel.sendMessage("Database error adding event, "
                                + "add again once database issue is fixed to make add persist after reboot").queue();
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
                        textChannel.sendMessage("Database error joining event, "
                                + "join again once database issue is fixed to make joining persist after reboot").queue();
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
                        textChannel.sendMessage("Database error leaving event, "
                                + "leave again once database issue is fixed to make leave persist after reboot").queue();
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
                        textChannel.sendMessage("You do not have permission to remove that event, "
                                + "only the event owner and admins can remove events").queue();
                        return;
                    }
                    try {
                        if (DATABASE.removeEvent(oldEvent.get())) {
                            textChannel.sendMessage("Event succesfully removed").queue();
                        } else {
                            textChannel.sendMessage("Could not find event with name: " + eventName).queue();
                        }
                    } catch (DatabaseException ex) {
                        textChannel.sendMessage("Database error removing event, "
                                + "remove again once issue is fixed to make remove persistent").queue();
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
                        textChannel.sendMessage("Database error clearing event, "
                                + "clear again once the issue is ").queue();
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
                        sb.append(' ').append(e.getName()).append('\n');
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

    private class AdminManagmentCommand extends OwnerCommand {

        @Override
        public String getCommand() {
            return "admin";
        }

        @Override
        public String getHelp() {
            return "add - adds admin by mention\n"
                    + "remove - removes admin by mention\n"
                    + "list - lists admins";
        }

        @Override
        public synchronized void respond(Member sender, Message message, TextChannel textChannel) {
            final CommandMatcher matcher = commandParser.getCommandMatcher(message);
            final String[] opts = matcher.getArguments(1);
            if (opts.length == 0) {
                textChannel.sendMessage("Provide operation to perform, check help for possible operations").queue();
                return;
            }
            final String action = opts[0];

            switch (action) {
                case "add": {
                    final List<Member> mentions = new ArrayList<>(message.getMentionedMembers());
                    final Member self = textChannel.getGuild().getSelfMember();
                    mentions.remove(self);

                    //Check if any member was mentioned
                    if (mentions.isEmpty()) {
                        textChannel.sendMessage("Mention users you want to add as admin").queue();
                        return;
                    }

                    final StringBuilder sb = new StringBuilder();
                    for (Member m : mentions) {
                        try {
                            if (DATABASE.addAdmin(m.getUser())) {
                                sb.append("Succesfully added admin ").append(m.getNickname()).append('\n');
                            } else {
                                sb.append("Admin was alredy added ").append(m.getNickname()).append('\n');
                            }
                        } catch (DatabaseException ex) {
                            sb.append("Database error adding admin ").append(m.getNickname());
                            sb.append(" add again once database issue is fixed to make add persist after reboot\n");
                        }
                    }
                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                case "remove": {
                    final List<Member> mentions = new ArrayList<>(message.getMentionedMembers());
                    final Member self = textChannel.getGuild().getSelfMember();
                    mentions.remove(self);

                    //Check if any members was mentioned
                    if (mentions.isEmpty()) {
                        textChannel.sendMessage("Mention the users you want to remove admin status from").queue();
                        return;
                    }

                    final StringBuilder sb = new StringBuilder();
                    for (Member m : mentions) {
                        try {
                            if (DATABASE.removeAdmin(m.getUser().getId())) {
                                sb.append("Succesfully removed admin ").append(m.getNickname()).append('\n');
                            } else {
                                sb.append("Admin was alredy removed ").append(m.getNickname()).append('\n');
                            }
                        } catch (DatabaseException ex) {
                            sb.append("Database error removing admin ").append(m.getNickname());
                            sb.append(" remove again once database issue is fixed to make remove persist after reboot\n");
                        }
                    }
                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                case "list": {
                    final List<String> adminIds = DATABASE.getAdminIds();
                    final StringBuilder sb = new StringBuilder();
                    for (String adminId : adminIds) {
                        final User admin = textChannel.getJDA().getUserById(adminId);
                        if (admin == null) {
                            sb.append("Found admin in database who could not be found, removing admin status\n");
                            try {
                                if (DATABASE.removeAdmin(adminId)) {
                                    sb.append("Admin succesfully removed\n");
                                } else {
                                    sb.append("Admin alredy removed by someone else\n");
                                }
                            } catch (DatabaseException ex) {
                                sb.append("Database failure in removing the admin\n");
                            }
                            continue;
                        }
                        sb.append(admin.getName()).append('\n');
                    }

                    if (adminIds.isEmpty()) {
                        sb.append("No admins added.");
                    }
                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                default: {
                    textChannel.sendMessage("Unkown action: " + action).queue();
                }
            }
        }
    }

    private class ChannelManagmentCommand extends OwnerCommand {

        @Override
        public String getCommand() {
            return "channel";
        }

        @Override
        public String getHelp() {
            return "add - adds channel by mention\n"
                    + "remove - removes channel by mention\n"
                    + "list - lists channels";
        }

        @Override
        public synchronized void respond(Member sender, Message message, TextChannel textChannel) {
            final CommandMatcher matcher = commandParser.getCommandMatcher(message);
            final String[] opts = matcher.getArguments(1);
            if (opts.length == 0) {
                textChannel.sendMessage("Provide operation to perform, check help for possible operations").queue();
                return;
            }
            final String action = opts[0];

            switch (action) {
                case "add": {
                    final List<TextChannel> mentioned = message.getMentionedChannels();

                    //Check if any channels were mentioned
                    if (mentioned.isEmpty()) {
                        textChannel.sendMessage("Mention channels you want to add").queue();
                        return;
                    }

                    final StringBuilder sb = new StringBuilder();
                    for (TextChannel channel : mentioned) {
                        try {
                            if (DATABASE.addChannel(channel)) {
                                sb.append("Succesfully started listening on channel ").append(channel.getName()).append('\n');
                            } else {
                                sb.append("Was alredy listening on channel ").append(channel.getName()).append('\n');
                            }
                        } catch (DatabaseException ex) {
                            sb.append("Database error adding channel ").append(channel.getName());
                            sb.append(" will listen on channel until next reboot unless added succesfully to database\n");
                        }
                    }

                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                case "remove": {
                    final List<TextChannel> mentions = message.getMentionedChannels();

                    if (mentions.isEmpty()) {
                        textChannel.sendMessage("Mention channels you want to stop listening on").queue();
                        return;
                    }

                    final StringBuilder sb = new StringBuilder();
                    for (TextChannel channel : mentions) {
                        try {
                            if (DATABASE.removeChannel(channel.getId())) {
                                sb.append("Succesfully stopped listening on channel ").append(channel.getName()).append('\n');
                            } else {
                                sb.append("Was not listening on channel ").append(channel.getName()).append('\n');
                            }
                        } catch (DatabaseException ex) {
                            sb.append("Database error removing channel ").append(channel.getName());
                            sb.append(" Will not listen on channel until next reboot unless removed succesfully from database\n");
                        }
                    }

                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                case "list": {
                    final List<String> channelIds = DATABASE.getChannels();
                    final StringBuilder sb = new StringBuilder();
                    for (String id : channelIds) {
                        final TextChannel channel = textChannel.getGuild().getTextChannelById(id);
                        if (channel == null) {
                            sb.append("Channel in database which could not be found, removing from listened channels\n");
                            try {
                                if (DATABASE.removeChannel(id)) {
                                    sb.append("Stopped listening on channel succesfully\n");
                                } else {
                                    sb.append("Channel alredy removed by someone else\n");
                                }
                            } catch (DatabaseException ex) {
                                sb.append("Database failure in removing channel from database\n");
                            }
                            continue;
                        }
                        sb.append(channel.getName()).append('\n');
                    }
                    if (channelIds.isEmpty()) {
                        sb.append("Not listening on any channels");
                    }
                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                default: {
                    textChannel.sendMessage("Unkown action: " + action).queue();
                }
            }
        }
    }

    private class CommandManagmentCommand implements ChatCommand {

        @Override
        public String getCommand() {
            return "custom";
        }

        @Override
        public String getHelp() {
            return "add - adds a new custom command\n"
                    + "remove - removes custom command\n"
                    + "keys - shows list of keys command can contain"
                    + "Syntax for custom commands:\n"
                    + "{key} substitute part of command with action use parameter keys to see all keys\n"
                    + "| Include other outcomes to command, all options have equal chance";
        }

        @Override
        public CommandPermission getPermission() {
            return DATABASE.getCommandManagePermission();
        }

        @Override
        public synchronized void respond(Member sender, Message message, TextChannel textChannel) {
            final CommandMatcher m = commandParser.getCommandMatcher(message);
            final String[] opt = m.getArguments(2);
            if (opt.length == 0) {
                textChannel.sendMessage("Provide operation to perform,"
                        + " check help for possible operations").queue();
                return;
            }
            switch (opt[0]) {
                case "add": {
                    if (opt.length < 2) {
                        textChannel.sendMessage("Adding custom command requires a name for the command").queue();
                        return;
                    }
                    if (opt.length < 3) {
                        textChannel.sendMessage("Command must contain a template string for the response").queue();
                        return;
                    }
                    final String name = opt[1];
                    final Optional<CustomCommand> oldCommand = commandParser.getCustomCommand(name);
                    if (oldCommand.isPresent()) {
                        try {
                            if (DATABASE.addCommand(oldCommand.get())) {
                                textChannel.sendMessage("Found old command by that name in memory, succesfully added it to database").queue();
                            } else {
                                textChannel.sendMessage("Command with that name alredy exists, "
                                        + "if you want to edit command remove old one first, "
                                        + "otherwise provide different name for the command").queue();
                            }
                        } catch (DatabaseException ex) {
                            textChannel.sendMessage("Found old command by that name in memory but adding it to database failed.").queue();
                        }
                        return;
                    }
                    final String newValue = opt[2];
                    final CustomCommand newAction = DATABASE.getCommandBuilder().build(name, newValue, message.getAuthor().getId());
                    {
                        try {
                            if (DATABASE.addCommand(newAction)) {
                                textChannel.sendMessage("Command added succesfully").queue();
                            } else {
                                textChannel.sendMessage("Command alredy exists, propably a database error").queue();
                            }
                        } catch (DatabaseException ex) {
                            textChannel.sendMessage("Adding command to database failed, added to temporary memory that will be lost on reboot").queue();
                        }
                    }
                    break;
                }
                case "remove": {
                    if (opt.length < 2) {
                        textChannel.sendMessage("Removing custom command requires a name of the the command to remove").queue();
                        return;
                    }
                    final String name = opt[1];
                    final Optional<CustomCommand> optCommand = commandParser.getCustomCommand(name);
                    if (optCommand.isEmpty()) {
                        textChannel.sendMessage("No such command as " + name).queue();
                        return;
                    }
                    final CustomCommand command = optCommand.get();
                    final User commandOwner = textChannel.getJDA().getUserById(command.getOwner());

                    //Check if user has permission to remove the command
                    final boolean hasPermission;
                    if (commandOwner == null) {
                        hasPermission = DATABASE.isAdmin(sender.getUser()) || DATABASE.isOwner(sender.getUser());
                    } else {
                        hasPermission = commandParser.hasPermission(sender.getUser(), commandOwner);
                    }
                    if (hasPermission) {
                        try {
                            if (DATABASE.removeCommand(command)) {
                                textChannel.sendMessage("Command removed succesfully").queue();
                            } else {
                                textChannel.sendMessage("Command was alredy removed, propably a database error").queue();
                            }
                        } catch (DatabaseException ex) {
                            textChannel.sendMessage("Removing command from database failed, removed from temporary memory command will be back after reboot").queue();
                        }
                        return;
                    }
                    textChannel.sendMessage("You do not have permission to remove that command, "
                            + "only owner of the command and people with admin rights can delete commands").queue();
                    break;
                }
                case "keys": {
                    textChannel.sendMessage(DATABASE.getCommandBuilder().getActionManager().getHelp()).queue();
                    break;
                }
                default:
                    textChannel.sendMessage("Unkown operation: " + opt[0]).queue();
                    break;
            }
        }
    }

}
