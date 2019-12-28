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
import eternal.lemonadebot.messages.CommandMatcher;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
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
    public void respond(CommandMatcher matcher) {
        final Optional<TextChannel> optChannel = matcher.getTextChannel();
        final Optional<Member> optMember = matcher.getMember();
        if (optChannel.isEmpty() || optMember.isEmpty()) {
            matcher.getMessageChannel().sendMessage("Commands are specific to discord servers and must be edited on one").queue();
            return;
        }
        final TextChannel textChannel = optChannel.get();
        final Member sender = optMember.get();

        final List<Role> currentRoles = sender.getRoles();
        if (currentRoles.size() > 0) {
            textChannel.sendMessage("You cannot assign role to yourself using this command if you alredy have a role").queue();
            return;
        }
        final String[] parameters = matcher.getArguments(1);
        if (parameters.length == 0) {
            textChannel.sendMessage("Please provide guild name you want the role for").queue();
            return;
        }

        //Name of the guild user wants role for
        final String guildName = parameters[0];

        //Ignore current server
        final Guild currentGuild = textChannel.getGuild();
        if (currentGuild.getName().equals(guildName)) {
            textChannel.sendMessage("Can't assign role for current server, check rules if you want member status.").queue();
            return;
        }

        //Guilds we share with the user
        final List<Guild> mutualGuilds = sender.getUser().getMutualGuilds();

        //Get names of the mutual guilds
        final StringBuilder possibleRoleNames = new StringBuilder("Possible guilds:\n");

        //Find and assign role
        for (Guild g : mutualGuilds) {
            if (!g.getName().equals(guildName)) {
                possibleRoleNames.append(g.getName()).append('\n');
                continue;
            }

            //Make sure they are a member on the other server
            final Member otherMember = g.getMember(sender.getUser());
            if (otherMember == null) {
                textChannel.sendMessage("Did you leave the server while I wasn't looking? Could not find you on that server").queue();
                return;
            }
            if (otherMember.getRoles().isEmpty()) {
                textChannel.sendMessage("You do not have any roles on that server, as such no role was given").queue();
                return;
            }

            //Find the role for given guild
            final List<Role> roles = textChannel.getGuild().getRolesByName(guildName, false);
            if (roles.isEmpty()) {
                textChannel.sendMessage("It appears we do not have a role for that server yet, please contact admin to fix").queue();
                return;
            }

            //Assign found role to the sender, this could assign multiple roles if there is multiple roles with same name
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

        //Did not find the guild, show list of found guilds
        textChannel.sendMessage("Could not find a guild named: " + guildName + possibleRoleNames).queue();
    }

}
