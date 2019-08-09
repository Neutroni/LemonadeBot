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
import eternal.lemonadebot.commandtypes.UserCommand;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandParser;
import java.util.List;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class SimpleCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    private final List<ChatCommand> COMMANDS = List.of(new RoleCommand());
    private final CommandParser commandParser;

    /**
     * Constructor
     *
     * @param parser Parser used to parse commands
     */
    public SimpleCommands(CommandParser parser) {
        this.commandParser = parser;
    }

    /**
     * Get list of commands this manager provides
     *
     * @return Immutable list of commands
     */
    public List<ChatCommand> getCommands() {
        return COMMANDS;
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
            final String[] parameters = matcher.getParameters(1);
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
                textChannel.getGuild().getController().addRolesToMember(sender, roles).queue((t) -> {
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
