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
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.commandtypes.MemberCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.customcommands.TemplateProvider;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.TemplateManager;
import eternal.lemonadebot.permissions.PermissionUtilities;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command to edit custom commands with
 *
 * @author Neutroni
 */
public class TemplateCommand extends MemberCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand() {
        return "template";
    }

    @Override
    public String getDescription() {
        return "Manage custom commands";
    }

    @Override
    public String getHelpText() {
        return " Syntax: template <option> [name] [template]\n"
                + "<option> can be one of the following:\n"
                + " create - create new custom command\n"
                + " delete - delete custom command\n"
                + " keys - shows list of keys custom command can contain\n"
                + " list - show list of custom commands\n"
                + "[name] name for action\n"
                + "[template] template for custom command, see below for syntax\n"
                + "Syntax for custom commands:\n"
                + " Text in the template will mostly be shown as is,\n"
                + " but you can use {key} to modify parts of the message.\n"
                + " See \"command keys\" to see all keys\n";
    }

    @Override
    public void respond(CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final String[] arguments = matcher.getArguments(2);
        if (arguments.length == 0) {
            textChannel.sendMessage("Provide operation to perform, check help for possible operations").queue();
            return;
        }
        switch (arguments[0]) {
            case "create": {
                createCustomCommand(arguments, matcher, guildData);
                break;
            }
            case "delete": {
                deleteCustomCommand(arguments, matcher, guildData);
                break;
            }
            case "list": {
                listCustomCommands(matcher, guildData);
                break;
            }
            case "keys": {
                final EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Templates:");
                eb.setDescription(TemplateProvider.getHelp());
                textChannel.sendMessage(eb.build()).queue();
                break;
            }
            default:
                textChannel.sendMessage("Unknown operation: " + arguments[0]).queue();
                break;
        }
    }

    private void createCustomCommand(String[] arguments, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        if (arguments.length < 2) {
            textChannel.sendMessage("Creating custom command requires a name for the command").queue();
            return;
        }
        if (arguments.length < 3) {
            textChannel.sendMessage("Custom command must contain a template string for the response").queue();
            return;
        }

        //Check that there is no such built in command
        final String commandName = arguments[1];
        final Optional<ChatCommand> optCommand = CommandProvider.getBuiltInCommand(commandName);
        if (optCommand.isPresent()) {
            textChannel.sendMessage("Custom command name reserved by command.").queue();
            return;
        }

        final String commandTemplate = arguments[2];
        final Member sender = matcher.getMember();
        final TemplateManager commands = guildData.getCustomCommands();
        final CustomCommand newAction = new CustomCommand(commandName, commandTemplate, sender.getIdLong());
        try {
            if (commands.addCommand(newAction)) {
                textChannel.sendMessage("Custom command added succesfully").queue();
                return;
            }
            textChannel.sendMessage("Custom command with that name alredy exists.").queue();
        } catch (SQLException ex) {
            textChannel.sendMessage("Adding custom command to database failed, added to temporary memory that will be lost on reboot").queue();

            LOGGER.error("Failure to add custom command: " + ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }

    }

    private void deleteCustomCommand(String[] arguments, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        if (arguments.length < 2) {
            textChannel.sendMessage("Deleting custom command requires a name of the the command to remove").queue();
            return;
        }
        final String commandName = arguments[1];
        final TemplateManager commands = guildData.getCustomCommands();
        final Optional<CustomCommand> optCommand = commands.getCommand(commandName);
        if (optCommand.isEmpty()) {
            textChannel.sendMessage("No such custom command as " + commandName).queue();
            return;
        }
        final CustomCommand command = optCommand.get();
        final Member commandOwner = textChannel.getGuild().getMemberById(command.getOwner());

        //Check if user has permission to remove the command
        final Member sender = matcher.getMember();
        final boolean hasPermission = PermissionUtilities.hasPermission(sender, commandOwner);
        if (!hasPermission) {
            textChannel.sendMessage("You do not have permission to delete that custom command, "
                    + "only the custom command owner and admins can delete custom commands").queue();
            return;
        }

        //Delete the command
        try {
            if (commands.removeCommand(command)) {
                textChannel.sendMessage("Custom command deleted succesfully").queue();
                return;
            }
            textChannel.sendMessage("Custom command was alredy deleted, propably a database error").queue();
        } catch (SQLException ex) {
            textChannel.sendMessage("Deleting custom command from database failed, deleted from temporary memory, command will be back after reboot").queue();

            LOGGER.error("Failure to delete custom command");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private void listCustomCommands(CommandMatcher matcher, GuildDataStore guildData) {
        final List<CustomCommand> coms = guildData.getCustomCommands().getCommands();
        final Guild guild = matcher.getGuild();
        final TextChannel textChannel = matcher.getTextChannel();
        final StringBuilder sb = new StringBuilder("Commands:\n");
        for (CustomCommand c : coms) {
            final Member creator = guild.retrieveMemberById(c.getOwner()).complete();
            final String creatorName;
            if (creator == null) {
                creatorName = "Unknown";
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
