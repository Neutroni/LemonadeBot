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

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.commandtypes.OwnerCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.DatabaseException;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandParser;
import eternal.lemonadebot.messages.CommandPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

/**
 * CommandProvider providing commands that use both database and commandparsing
 *
 * @author Neutroni
 */
public class AdvancedCommands implements CommandProvider {

    private final List<ChatCommand> COMMANDS = List.of(
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
        public void respond(Member sender, Message message, TextChannel textChannel) {
            final CommandMatcher matcher = commandParser.getCommandMatcher(message);
            final String[] opts = matcher.getArguments(1);
            if (opts.length == 0) {
                textChannel.sendMessage("Provide operation to perform, check help custom for possible operations").queue();
                return;
            }
            final String action = opts[0];

            switch (action) {
                case "add": {
                    final StringBuilder sb = new StringBuilder();
                    final List<Member> mentioned = message.getMentionedMembers();
                    final Member self = textChannel.getGuild().getSelfMember();
                    final List<Member> mentions = new ArrayList<>(mentioned);
                    mentions.remove(self);
                    for (Member m : mentions) {
                        try {
                            final boolean added = DATABASE.addAdmin(m.getUser());
                            if (added) {
                                sb.append("Succesfully added admin ").append(m.getNickname()).append('\n');
                            } else {
                                sb.append("Admin was alredy added ").append(m.getNickname()).append('\n');
                            }
                        } catch (DatabaseException ex) {
                            sb.append("Database error adding admin ").append(m.getNickname());
                            sb.append(" has admin right until next reboot unlessa added succesfully to database\n");
                        }
                    }
                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                case "remove": {
                    final StringBuilder sb = new StringBuilder();
                    final List<Member> mentioned = message.getMentionedMembers();
                    final Member self = textChannel.getGuild().getSelfMember();
                    final List<Member> mentions = new ArrayList<>(mentioned);
                    mentions.remove(self);
                    for (Member m : mentions) {
                        try {
                            final boolean removed = DATABASE.removeAdmin(m.getUser().getId());
                            if (removed) {
                                sb.append("Succesfully removed admin ").append(m.getNickname()).append('\n');
                            } else {
                                sb.append("Admin was alredy removed ").append(m.getNickname()).append('\n');
                            }
                        } catch (DatabaseException ex) {
                            sb.append("Database error removing admin ").append(m.getNickname());
                            sb.append(" does not have admin rights until next reboot unless removed succesfully from database\n");
                        }
                    }
                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                case "list": {
                    final List<String> adminIds = DATABASE.getAdminIds();
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < adminIds.size(); i++) {
                        final User admin = textChannel.getJDA().getUserById(adminIds.get(i));
                        if (admin == null) {
                            sb.append("Found admin in database who could not be found, removing admin status\n");
                            try {
                                final boolean removed = DATABASE.removeAdmin(adminIds.get(i));
                                if (removed) {
                                    sb.append("Admin succesfully removed\n");
                                } else {
                                    sb.append("Admin alredy removed by someone else\n");
                                }
                            } catch (DatabaseException ex) {
                                sb.append("Database failure in removing the admin\n");
                            }
                            continue;
                        }
                        sb.append(admin.getName());
                        if (i < adminIds.size() - 1) {
                            sb.append('\n');
                        }
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
        public void respond(Member sender, Message message, TextChannel textChannel) {
            final CommandMatcher matcher = commandParser.getCommandMatcher(message);
            final String[] opts = matcher.getArguments(1);
            if (opts.length == 0) {
                textChannel.sendMessage("Provide operation to perform, check help custom for possible operations").queue();
                return;
            }
            final String action = opts[0];

            switch (action) {
                case "add": {
                    final StringBuilder sb = new StringBuilder();
                    final List<TextChannel> mentioned = message.getMentionedChannels();
                    for (TextChannel channel : mentioned) {
                        try {
                            final boolean added = DATABASE.addChannel(channel);
                            if (added) {
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
                    final StringBuilder sb = new StringBuilder();
                    final List<TextChannel> mentions = message.getMentionedChannels();
                    for (TextChannel channel : mentions) {
                        try {
                            final boolean removed = DATABASE.removeChannel(channel.getId());
                            if (removed) {
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
                    final List<String> channelIds = DATABASE.getChannelIds();
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < channelIds.size(); i++) {
                        final TextChannel channel = textChannel.getGuild().getTextChannelById(channelIds.get(i));
                        if (channel == null) {
                            sb.append("Channel in database which could not be found, removing from listened channels\n");
                            try {
                                final boolean removed = DATABASE.removeChannel(channelIds.get(i));
                                if (removed) {
                                    sb.append("Stopped listening on channel succesfully\n");
                                } else {
                                    sb.append("Channel alredy removed by someone else\n");
                                }
                            } catch (DatabaseException ex) {
                                sb.append("Database failure in removing channel from database\n");
                            }
                            continue;
                        }
                        sb.append(channel.getName());
                        if (i < channelIds.size() - 1) {
                            sb.append('\n');
                        }
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
        public void respond(Member member, Message message, TextChannel textChannel) {
            final CommandMatcher m = commandParser.getCommandMatcher(message);
            final String[] opt = m.getArguments(2);
            if (opt.length == 0) {
                textChannel.sendMessage("Provide operation to perform,"
                        + " check help custom for possible operations").queue();
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
                    Optional<CustomCommand> oldCommand = commandParser.getCustomCommand(name);
                    if (oldCommand.isPresent()) {
                        try {
                            final boolean added = DATABASE.addCommand(oldCommand.get());
                            if (added) {
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
                            final boolean added = DATABASE.addCommand(newAction);
                            if (added) {
                                textChannel.sendMessage("Command added succesfully").queue();
                                return;
                            }
                            textChannel.sendMessage("Command alredy exists, propably a database error").queue();
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
                    final List<CommandPermission> userPerms = commandParser.getPermissions(member);

                    final Member commandOwner = textChannel.getGuild().getMemberById(command.getOwner());
                    final List<CommandPermission> ownerPerms;
                    if (commandOwner == null) {
                        ownerPerms = new ArrayList<>(0);
                    } else {
                        ownerPerms = commandParser.getPermissions(commandOwner);
                    }

                    boolean hasPermission = false;
                    if (command.getOwner().equals(member.getUser().getId())) {
                        hasPermission = true;
                    } else if ((userPerms.size() > ownerPerms.size()) && userPerms.contains(CommandPermission.ADMIN)) {
                        hasPermission = true;
                    } else {
                        hasPermission = userPerms.contains(CommandPermission.OWNER);
                    }
                    if (hasPermission) {
                        try {
                            final boolean removed = DATABASE.removeCommand(command);
                            if (removed) {
                                textChannel.sendMessage("Command removed succesfully").queue();
                                return;
                            }
                            textChannel.sendMessage("Command was alredy removed, propably a database error").queue();
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
