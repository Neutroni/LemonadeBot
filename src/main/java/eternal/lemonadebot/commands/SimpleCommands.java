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
import eternal.lemonadebot.commandtypes.UserCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandParser;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * CommandProvider for commands that only require command parser and do not do
 * database operations directly
 *
 * @author Neutroni
 */
public class SimpleCommands implements CommandProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private final List<ChatCommand> COMMANDS = List.of(
            new ShutdownCommand(),
            new HelpCommand(),
            new PrefixCommand(),
            new RoleCommand()
    );

    private final CommandParser commandParser;

    /**
     * Constructor
     *
     * @param parser Parser used to parse commands
     */
    public SimpleCommands(CommandParser parser) {
        this.commandParser = parser;
    }

    @Override
    public List<ChatCommand> getCommands() {
        return COMMANDS;
    }

    private class ShutdownCommand extends OwnerCommand {

        @Override
        public String getCommand() {
            return "shutdown";
        }

        @Override
        public String getHelp() {
            return "Shuts the bot down";
        }

        @Override
        public void respond(Member sender, Message message, TextChannel textChannel) {
            textChannel.sendMessage("Shutting down").queue();
            message.getJDA().shutdown();
        }
    }

    private class HelpCommand extends UserCommand {

        @Override
        public String getCommand() {
            return "help";
        }

        @Override
        public void respond(Member member, Message message, TextChannel textChannel) {
            final CommandMatcher m = commandParser.getCommandMatcher(message);
            final String[] options = m.getArguments(1);

            if (options.length == 0) {
                textChannel.sendMessage("Provide command to search help for, use commands for list of commands.").queue();
                return;
            }
            final String name = options[0];

            final Optional<ChatCommand> opt = commandParser.getCommand(name);
            if (opt.isPresent()) {
                final ChatCommand com = opt.get();
                if (commandParser.hasPermission(member, com)) {
                    textChannel.sendMessage(com.getCommand() + " - " + com.getHelp()).queue();
                } else {
                    textChannel.sendMessage("You do not have permission to run that command, as such no help was provided").queue();
                }
                return;
            }

            final Optional<CustomCommand> custom = commandParser.getCustomCommand(name);
            if (custom.isPresent()) {
                final CustomCommand com = custom.get();
                textChannel.sendMessage("User defined custom command, see command \"custom\" for details.").queue();
                return;

            }
            textChannel.sendMessage("No such command: " + name).queue();
        }

        @Override
        public String getHelp() {
            return "Prints help message for command.";
        }
    }

    private class PrefixCommand extends OwnerCommand {

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
            final CommandMatcher m = commandParser.getCommandMatcher(message);
            final String[] options = m.getArguments(1);

            //Check if user provide prefix
            if (options.length == 0) {
                textChannel.sendMessage("Provide a prefix to set commandprefix to.").queue();
                return;
            }

            //Update the prefix
            final String newPrefix = options[0];
            final boolean updateSuccess = commandParser.setPrefix(newPrefix);
            if (updateSuccess) {
                textChannel.sendMessage("Updated prefix succesfully to: " + newPrefix).queue();
            } else {
                textChannel.sendMessage("Storing prefix in DB failed, will still use new prefix until reboot, re-issue command once DB issue is fixed.").queue();
            }
        }
    }

    private class RoleCommand extends UserCommand {

        @Override
        public String getCommand() {
            return "role";
        }

        @Override
        public String getHelp() {
            return "Assign role to yourself based on other guild you also are in";
        }

        @Override
        public void respond(Member sender, Message message, TextChannel textChannel) {
            final List<Role> currentRoles = sender.getRoles();
            if (currentRoles.size() > 0) {
                textChannel.sendMessage("You cannot assign role to yourself using this command if you alredy have a role").queue();
                return;
            }
            final CommandMatcher matcher = commandParser.getCommandMatcher(message);
            final String[] parameters = matcher.getArguments(1);
            if (parameters.length == 0) {
                textChannel.sendMessage("Please provide guild name you want the role for").queue();
                return;
            }
            final String guildName = parameters[0];
            final List<Guild> mutualGuilds = sender.getUser().getMutualGuilds();
            for (Guild g : mutualGuilds) {
                if (!g.getName().equals(guildName)) {
                    continue;
                }
                final List<Role> roles = textChannel.getGuild().getRolesByName(guildName, false);
                textChannel.getGuild().modifyMemberRoles(sender, roles, null).queue((t) -> {
                    //Success
                    textChannel.sendMessage("Role assigned succesfully").queue();
                }, (t) -> {
                    //Failure
                    LOGGER.warn(t);
                    textChannel.sendMessage("Role assignment failed, either I can't assign roles on this server or other error occured").queue();
                });
                return;

            }
            textChannel.sendMessage("Could not find a guild named: " + guildName).queue();
        }

    }

}
