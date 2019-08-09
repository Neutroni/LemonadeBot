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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command used to manage custom commands
 *
 * @author Neutroni
 */
public class CustomCommandManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DatabaseManager DATABASE;
    private final CommandParser COMMAND_PARSER;
    private final ChatCommand managmentCommand;

    /**
     * Constructor
     *
     * @param db Database to use
     * @param cp Parser to use
     */
    public CustomCommandManager(DatabaseManager db, CommandParser cp) {
        this.DATABASE = db;
        this.COMMAND_PARSER = cp;
        this.managmentCommand = new ManagmentCommand();
    }

    /**
     * Gets the command used to manage custom commands
     *
     * @return
     */
    public ChatCommand getManagmentCommand() {
        return this.managmentCommand;
    }

    private class ManagmentCommand implements ChatCommand {

        @Override
        public String getCommand() {
            return "custom";
        }

        @Override
        public String getHelp() {
            return "add - adds a new custom command\n"
                    + "remove - removes custom command\n"
                    + "Syntax:\n"
                    + "{key} substitute part of command with action use parameter keys to see all keys\n"
                    + "| Include other outcomes to command, all options have equal chance";
        }

        @Override
        public CommandPermission getPermission() {
            return DATABASE.getCommandManagePermission();
        }

        @Override
        public void respond(Member member, Message message, TextChannel textChannel) {
            final CommandMatcher m = COMMAND_PARSER.getCommandMatcher(message);
            final String[] opt = m.getParameters(2);
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
                    final String name = opt[1];
                    Optional<CustomCommand> oldCommand = COMMAND_PARSER.getCustomCommand(name);
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
                            LOGGER.warn(ex);
                        }
                        return;
                    }
                    final Optional<String> optValue = m.getData(2);
                    if (optValue.isEmpty()) {
                        textChannel.sendMessage("Command requires an action to perform, "
                                + "please provide one after the name of the command").queue();
                        return;
                    }
                    final String newValue = optValue.get();
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
                            LOGGER.warn(ex);
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
                    Optional<CustomCommand> optCommand = COMMAND_PARSER.getCustomCommand(name);
                    if (optCommand.isEmpty()) {
                        textChannel.sendMessage("No such command as " + name).queue();
                        return;
                    }
                    final CustomCommand command = optCommand.get();
                    final List<CommandPermission> userPerms = COMMAND_PARSER.getPermissions(member);

                    final Member commandOwner = textChannel.getGuild().getMemberById(command.getOwner());
                    final List<CommandPermission> ownerPerms;
                    if (commandOwner == null) {
                        ownerPerms = new ArrayList<>(0);
                    } else {
                        ownerPerms = COMMAND_PARSER.getPermissions(commandOwner);
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
                            LOGGER.warn(ex);
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
