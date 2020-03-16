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
package eternal.lemonadebot.customcommands;

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.CustomCommandManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class CommandManagmentCommand implements ChatCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    private final PermissionManager commandManager;
    private final DatabaseManager db;

    /**
     * Constructor
     *
     * @param parser parser to parse arguments with
     * @param database database to store custom commands in
     */
    public CommandManagmentCommand(PermissionManager parser, DatabaseManager database) {
        this.commandManager = parser;
        this.db = database;
    }

    @Override
    public String getCommand() {
        return "custom";
    }

    @Override
    public String getHelp() {
        return " Syntax: custom <action> [name] [template]"
                + "<action> can be one of the following:\n"
                + "  create - create new custom command\n"
                + "  delete - deletes custom command\n"
                + "  keys - shows list of keys command can contain\n"
                + "  list - show list of custom commands\n"
                + "[name] name for custom command"
                + "[template] template for custom command, see below for syntax"
                + "Syntax for custom commands:\n"
                + "  Text in the template will mostly be shown as is,\n"
                + "  but you can use {key} to modify parts of the message.\n"
                + "  See \"custom keys\" to see all keys\n";
    }

    @Override
    public CommandPermission getPermission(Guild guild) {
        final ConfigManager guildConf = this.db.getGuildData(guild).getConfigManager();
        return guildConf.getEditPermission();
    }

    @Override
    public void respond(CommandMatcher matcher) {
        final TextChannel textChannel = matcher.getTextChannel();
        final String[] arguments = matcher.getArguments(2);
        if (arguments.length == 0) {
            textChannel.sendMessage("Provide operation to perform, check help for possible operations").queue();
            return;
        }
        switch (arguments[0]) {
            case "create": {
                createCustomCommand(arguments, matcher);
                break;
            }
            case "delete": {
                deleteCustomCommand(arguments, matcher);
                break;
            }
            case "list": {
                listCustomCommands(matcher);
                break;
            }
            case "keys": {
                textChannel.sendMessage(ActionManager.getHelp()).queue();
                break;
            }
            default:
                textChannel.sendMessage("Unkown operation: " + arguments[0]).queue();
                break;
        }
    }

    private void createCustomCommand(String[] arguments, CommandMatcher matcher) {
        final TextChannel textChannel = matcher.getTextChannel();
        if (arguments.length < 2) {
            textChannel.sendMessage("Creating custom command requires a name for the command").queue();
            return;
        }
        if (arguments.length < 3) {
            textChannel.sendMessage("Command must contain a template string for the response").queue();
            return;
        }
        final String commandName = arguments[1];
        final CustomCommandManager commands = matcher.getGuildData().getCustomCommands();
        final Optional<CustomCommand> oldCommand = commands.getCommand(commandName);
        if (oldCommand.isPresent()) {
            try {
                if (commands.addCommand(oldCommand.get())) {
                    textChannel.sendMessage("Found old command by that name in memory, succesfully added it to database").queue();
                } else {
                    textChannel.sendMessage("Command with that name alredy exists, "
                            + "if you want to edit command remove old one first, "
                            + "otherwise provide different name for the command").queue();
                }
            } catch (SQLException ex) {
                textChannel.sendMessage("Found old command by that name in memory but adding it to database failed.").queue();

                LOGGER.error("Failure to add custom command to database");
                LOGGER.warn(ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
            return;
        }
        final String commandTemplate = arguments[2];
        final Member sender = matcher.getMember();
        final CustomCommand newAction = commands.build(commandName, commandTemplate, sender.getIdLong());
        {
            try {
                if (commands.addCommand(newAction)) {
                    textChannel.sendMessage("Command added succesfully").queue();
                } else {
                    textChannel.sendMessage("Command alredy exists, propably a database error").queue();
                }
            } catch (SQLException ex) {
                textChannel.sendMessage("Adding command to database failed, added to temporary memory that will be lost on reboot").queue();

                LOGGER.error("Failure to add custom command");
                LOGGER.warn(ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        }
    }

    private void deleteCustomCommand(String[] arguments, CommandMatcher matcher) {
        final TextChannel textChannel = matcher.getTextChannel();
        if (arguments.length < 2) {
            textChannel.sendMessage("Deleting custom command requires a name of the the command to remove").queue();
            return;
        }
        final String commandName = arguments[1];
        final CustomCommandManager commands = matcher.getGuildData().getCustomCommands();
        final Optional<CustomCommand> optCommand = commands.getCommand(commandName);
        if (optCommand.isEmpty()) {
            textChannel.sendMessage("No such command as " + commandName).queue();
            return;
        }
        final CustomCommand command = optCommand.get();
        final Member commandOwner = textChannel.getGuild().getMemberById(command.getOwner());

        //Check if user has permission to remove the command
        final Member sender = matcher.getMember();
        final boolean hasPermission = commandManager.hasPermission(sender, commandOwner);
        if (!hasPermission) {
            textChannel.sendMessage("You do not have permission to delete that command, "
                    + "only the command owner and admins can delete commands").queue();
            return;
        }

        //Delete the command
        try {
            if (commands.removeCommand(command)) {
                textChannel.sendMessage("Command deleted succesfully").queue();
                return;
            }
            textChannel.sendMessage("Command was alredy deleted, propably a database error").queue();
        } catch (SQLException ex) {
            textChannel.sendMessage("Deleting command from database failed, deleted from temporary memory, command will be back after reboot").queue();

            LOGGER.error("Failure to delete custom command");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private void listCustomCommands(CommandMatcher matcher) {
        final List<CustomCommand> coms = matcher.getGuildData().getCustomCommands().getCommands();
        final Guild guild = matcher.getGuild();
        final TextChannel textChannel = matcher.getTextChannel();
        final StringBuilder sb = new StringBuilder("Commands:\n");
        for (CustomCommand c : coms) {
            final Member creator = guild.getMemberById(c.getOwner());
            final String creatorName;
            if (creator == null) {
                creatorName = "Unkown";
            } else {
                creatorName = creator.getEffectiveName();
            }
            sb.append(c.getCommand()).append(" by ").append(creatorName).append('\n');
        }
        if (coms.isEmpty()) {
            sb.append("No custom commands");
        }
        textChannel.sendMessage(sb.toString()).queue();
    }
}
