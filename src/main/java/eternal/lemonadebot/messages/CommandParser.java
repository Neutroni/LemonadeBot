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
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author Neutroni
 */
public class CommandParser {

    private final DatabaseManager DATABASE;
    private final CommandPattern PATTERN;

    //Command managers
    private final AdvancedCommands advancedCommands;
    private final SimpleCommands simpleCommands;

    //List of commands
    private final List<ChatCommand> commands = new ArrayList<>();

    /**
     *
     * @param db
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
        final String name = m.getCommand();
        if (name == null) {
            return Optional.empty();
        }
        //Check if we find command by that name
        final Optional<ChatCommand> command = getCommand(name);
        if (command.isPresent()) {
            return command;
        }
        //Check if we find custom command by that name
        final Optional<CustomCommand> custom = getCustomCommand(name);
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
        for (CustomCommand c : DATABASE.getCommands()) {
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
    public List<CommandPermission> getPermissions(Member member) {
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
        return cp;
    }

    /**
     * Checks wheter given user has permission to run given command
     *
     * @param member Person to check permission for
     * @param t Command to check
     * @return Does the person have permission
     */
    public boolean hasPermission(Member member, ChatCommand t) {
        //Check if command requires any permissions
        final CommandPermission cp = t.getPermission();
        if (cp == CommandPermission.USER) {
            return true;
        }
        //Check if user has required permissions
        final List<CommandPermission> perms = getPermissions(member);
        return perms.contains(cp);
    }

    /**
     *
     * @param newPrefix
     * @return
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
            final String[] opt = m.getParameters(1);
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
            final StringBuilder sb = new StringBuilder("Commands:\n");
            final List<CommandPermission> cp = getPermissions(member);
            if (printDefault) {
                for (ChatCommand c : commands) {
                    if (cp.contains(c.getPermission())) {
                        sb.append(' ').append(c.getCommand()).append('\n');
                    }
                }
            }
            if (printCustom) {
                sb.append("Custom commands:");
                for (ChatCommand c : DATABASE.getCommands()) {
                    if (cp.contains(c.getPermission())) {
                        sb.append(' ').append(c.getCommand()).append('\n');
                    }
                }
            }
            textChannel.sendMessage(sb.toString()).queue();
        }
    }
}
