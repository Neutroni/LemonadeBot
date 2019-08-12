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
package eternal.lemonadebot.messages;

import eternal.lemonadebot.commands.AdvancedCommands;
import eternal.lemonadebot.commands.SimpleCommands;
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.commandtypes.UserCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.DatabaseManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Various utility functions for parsing command from messages
 *
 * @author Neutroni
 */
public class CommandParser {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DatabaseManager DATABASE;
    private final CommandPattern PATTERN;

    //Command managers
    private final AdvancedCommands advancedCommands;
    private final SimpleCommands simpleCommands;

    //List of commands
    private final List<ChatCommand> commands = new ArrayList<>();

    /**
     * Constructor
     *
     * @param db database to use
     */
    public CommandParser(DatabaseManager db) {
        this.DATABASE = db;
        this.PATTERN = new CommandPattern(db);

        //Add help command
        this.commands.add(new ListCommands());

        //Command providers
        this.advancedCommands = new AdvancedCommands(db, this);
        this.simpleCommands = new SimpleCommands(this);

        //Load commands
        this.commands.addAll(advancedCommands.getCommands());
        this.commands.addAll(simpleCommands.getCommands());
    }

    /**
     * Get the action for command
     *
     * @param message message to find command for
     * @return CommandAction or Option.empty if command was not found
     */
    public Optional<ChatCommand> getAction(Message message) {
        final CommandMatcher m = getCommandMatcher(message);
        final Optional<String> name = m.getCommand();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        //get the command name
        final String commandName = name.get();
        //Check if we find command by that name
        final Optional<ChatCommand> command = getCommand(commandName);
        if (command.isPresent()) {
            return command;
        }

        //Log the message if debug is enabled
        LOGGER.debug(() -> {
            return "Found command: " + commandName + " in " + message.getContentRaw();
        });

        //Check if we find custom command by that name
        final Optional<CustomCommand> custom = getCustomCommand(commandName);
        if (custom.isPresent()) {
            return Optional.of(custom.get());
        }
        return Optional.empty();
    }

    /**
     * Get command by name
     *
     * @param name command name to search action for
     * @return Optional containing the action if found, empty if not found
     */
    public Optional<ChatCommand> getCommand(String name) {
        for (ChatCommand c : commands) {
            if (name.equals(c.getCommand())) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /**
     * Get custom command by name
     *
     * @param name command to search action for
     * @return Optional containing the action if found, empty if not found
     */
    public Optional<CustomCommand> getCustomCommand(String name) {
        for (CustomCommand c : DATABASE.getCommandBuilder().getItems()) {
            if (name.equals(c.getCommand())) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /**
     * Match of command elements
     *
     * @param message Message to parse
     * @return Mathcher for the message
     */
    public CommandMatcher getCommandMatcher(Message message) {
        final String text = message.getContentRaw();
        return PATTERN.getCommandMatcher(text);
    }

    /**
     * Extra permissions user has (OWNER,ADMIN,MEMBER)
     *
     * @param member user to check
     * @return List of permissions
     */
    private List<CommandPermission> getPermissions(Member member) {
        final User user = member.getUser();
        final List<CommandPermission> cp = new ArrayList<>();
        if (DATABASE.isOwner(user)) {
            cp.add(CommandPermission.OWNER);
            cp.add(CommandPermission.ADMIN);
        }
        if (DATABASE.isAdmin(user)) {
            cp.add(CommandPermission.ADMIN);
        }
        if (member.getRoles().size() > 0) {
            cp.add(CommandPermission.MEMBER);
        }
        cp.add(CommandPermission.USER);
        return cp;
    }

    /**
     * Checks wheter given user has permission to run given command
     *
     * @param member Person to check permission for
     * @param command Command to check
     * @return Does the person have permission
     */
    public boolean hasPermission(Member member, ChatCommand command) {
        return getPermissions(member).contains(command.getPermission());
    }

    /**
     * Check if user has permission to manage content owned by other member
     *
     * @param user User trying to do an action that modifies content
     * @param owner owner of the content
     * @return true if user has permission
     */
    public boolean hasPermission(User user, User owner) {
        if (user.equals(owner)) {
            return true;
        }
        if (DATABASE.isAdmin(user) && !DATABASE.isAdmin(owner)) {
            return true;
        }
        return DATABASE.isOwner(user);
    }

    /**
     * Sets the commands prefix
     *
     * @param newPrefix prefix to use
     * @return was storing prefix in database succesfull
     */
    public boolean setPrefix(String newPrefix) {
        return PATTERN.setPrefix(newPrefix);
    }

    private class ListCommands extends UserCommand {

        @Override
        public String getCommand() {
            return "commands";
        }

        @Override
        public String getHelp() {
            return "Shows list of commands";
        }

        @Override
        public void respond(Member member, Message message, TextChannel textChannel) {
            //Check what commands we should print
            final CommandMatcher m = getCommandMatcher(message);
            final String[] opt = m.getArguments(1);
            final boolean printDefault;
            final boolean printCustom;
            if (opt.length == 0) {
                printDefault = true;
                printCustom = false;
            } else {
                switch (opt[0]) {
                    case "all":
                        printDefault = true;
                        printCustom = true;
                        break;
                    case "custom":
                        printDefault = false;
                        printCustom = true;
                        break;
                    default:
                        printDefault = true;
                        printCustom = false;
                        break;
                }
            }

            //Construct the list of commands
            final StringBuilder sb = new StringBuilder();
            if (printDefault) {
                sb.append("Commands:\n");
                for (ChatCommand c : commands) {
                    if (hasPermission(member, c)) {
                        sb.append(' ').append(c.getCommand()).append('\n');
                    }
                }
            }
            if (printCustom) {
                sb.append("Custom commands:\n");
                for (CustomCommand c : DATABASE.getCommandBuilder().getItems()) {
                    if (hasPermission(member, c)) {
                        sb.append(' ').append(c.getCommand()).append('\n');
                    }
                }
            }
            textChannel.sendMessage(sb.toString()).queue();
        }
    }
}
