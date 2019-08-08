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

import eternal.lemonadebot.commandmanagers.AdvancedCommandManager;
import eternal.lemonadebot.commandmanagers.CustomCommandManager;
import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.OwnerCommand;
import eternal.lemonadebot.commands.UserCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.DatabaseException;
import eternal.lemonadebot.database.DatabaseManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class CommandParser {

    private static final Logger LOGGER = LogManager.getLogger();
    private final DatabaseManager DATABASE;

    //Command managers
    private final AdvancedCommandManager advancedCommands;
    private final CustomCommandManager customCommands;

    //Lists of commands
    private final List<ChatCommand> commands = new ArrayList<>();

    /**
     *
     * @param db
     */
    public CommandParser(DatabaseManager db) {
        this.DATABASE = db;
        CommandMatcher.updatePattern(DATABASE.getCommandPrefix());
        this.advancedCommands = new AdvancedCommandManager(db, this);
        this.customCommands = new CustomCommandManager(db, this);

        //Add help commands
        this.commands.add(new Help());
        this.commands.add(new Commands());

        //Load commands
        this.commands.addAll(advancedCommands.getCommands());

        //Add management commands
        this.commands.add(new Prefix());
        this.commands.add(customCommands.getManagmentCommand());
    }

    /**
     * Get the action for command
     *
     * @param message message to find command for
     * @return CommandAction or Option.empty if command was not found
     */
    public Optional<ChatCommand> getAction(Message message) {
        final CommandMatcher m = getCommandMatcher(message);
        final String command = m.getCommand();
        if (command == null) {
            return Optional.empty();
        }
        for (ChatCommand c : commands) {
            if (command.equals(c.getCommand())) {
                return Optional.of(c);
            }
        }
        for (ChatCommand c : DATABASE.getCommands()) {
            if (command.equals(c.getCommand())) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /**
     * Get custom command
     *
     * @param command command to search action for
     * @return Optional containing the action if found, empty if not found
     */
    public Optional<CustomCommand> getCustomCommand(String command) {
        for (CustomCommand c : DATABASE.getCommands()) {
            if (command.equals(c.getCommand())) {
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
        return new CommandMatcher(text);
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
        final List<CommandPermission> perms = getPermissions(member);
        CommandPermission cp = t.getPermission();
        if (cp == CommandPermission.USER) {
            return true;
        }
        return perms.contains(cp);
    }

    private class Prefix extends OwnerCommand {

        @Override
        public String getCommand() {
            return "prefix";
        }

        @Override
        public String getHelp() {
            return "Set the command prefix used to call this bot";
        }

        @Override
        public void respond(Member member, Message message, TextChannel textChannel) {
            final CommandMatcher m = getCommandMatcher(message);
            final String[] opt = m.getParameters(1);

            if (opt.length == 0) {
                textChannel.sendMessage("Current command prefix: " + DATABASE.getCommandPrefix()).queue();
                return;
            }
            final String newPrefix = opt[0];
            CommandMatcher.updatePattern(newPrefix);
            try {
                DATABASE.setCommandPrefix(newPrefix);
                textChannel.sendMessage("Updated prefix succesfully").queue();
            } catch (DatabaseException ex) {
                LOGGER.warn(ex);
                textChannel.sendMessage("Storing prefix in DB failed, will still use new prefix until reboot, re-issue command once DB issue is fixed").queue();
            }
        }
    }

    private class Help extends UserCommand {

        @Override
        public String getCommand() {
            return "help";
        }

        @Override
        public void respond(Member member, Message message, TextChannel textChannel) {
            final CommandMatcher m = getCommandMatcher(message);
            String[] opt = m.getParameters(1);

            if (opt.length == 0) {
                textChannel.sendMessage("Provide command to search help for, use commands for list of commands.").queue();
                return;
            }
            final String command = opt[0];

            for (ChatCommand c : commands) {
                if (command.equals(c.getCommand())) {
                    textChannel.sendMessage(c.getCommand() + " - " + c.getHelp()).queue();
                    return;
                }
            }
            for (ChatCommand c : DATABASE.getCommands()) {
                if (command.equals(c.getCommand())) {
                    textChannel.sendMessage("User defined custom command, see command \"custom\" for details.").queue();
                    return;
                }
            }
            textChannel.sendMessage("No such command: " + command).queue();
        }

        @Override
        public String getHelp() {
            return "Prints help message for command.";
        }
    }

    private class Commands extends UserCommand {

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
