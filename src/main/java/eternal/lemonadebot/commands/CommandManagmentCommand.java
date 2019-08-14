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

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.DatabaseException;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandParser;
import eternal.lemonadebot.messages.CommandPermission;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author joonas
 */
class CommandManagmentCommand implements ChatCommand {

    private final CommandParser commandParser;
    private final DatabaseManager DATABASE;

    /**
     * Constructor
     *
     * @param parser parser to parse arguments with
     * @param db database to store custom commands in
     */
    CommandManagmentCommand(CommandParser parser, DatabaseManager db) {
        this.commandParser = parser;
        this.DATABASE = db;
    }

    @Override
    public String getCommand() {
        return "custom";
    }

    @Override
    public String getHelp() {
        return "add - adds a new custom command\n" + "remove - removes custom command\n" + "keys - shows list of keys command can contain" + "Syntax for custom commands:\n" + "{key} substitute part of command with action use parameter keys to see all keys\n" + "| Include other outcomes to command, all options have equal chance";
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
            textChannel.sendMessage("Provide operation to perform," + " check help for possible operations").queue();
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
                            textChannel.sendMessage("Command with that name alredy exists, " + "if you want to edit command remove old one first, " + "otherwise provide different name for the command").queue();
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
                textChannel.sendMessage("You do not have permission to remove that command, " + "only owner of the command and people with admin rights can delete commands").queue();
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
