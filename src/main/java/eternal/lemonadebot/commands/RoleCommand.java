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

import eternal.lemonadebot.commandtypes.UserCommand;
import eternal.lemonadebot.messages.CommandManager;
import eternal.lemonadebot.messages.CommandMatcher;
import java.util.List;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
class RoleCommand extends UserCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    private final CommandManager commandParser;

    /**
     * Constructor
     *
     * @param parser command parser to parse commands with
     */
    RoleCommand(CommandManager parser) {
        this.commandParser = parser;
    }

    @Override
    public String getCommand() {
        return "role";
    }

    @Override
    public String getHelp() {
        return "Syntax: role <role>\n"
                + "Assign <role> to yourself based on other guild you also are in";
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
            final Member otherMember = g.getMember(sender.getUser());
            if(otherMember == null){
                textChannel.sendMessage("You do not have any roles on that server, as such no role was given").queue();
                return;
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
