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

import eternal.lemonadebot.commandtypes.UserCommand;
import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.database.GuildDataStore;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
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
    public String getDescription() {
        return "Add role to yourself";
    }

    @Override
    public String getHelpText() {
        return "Syntax: role [role]\n"
                + "If no role is provided try to automatically assign role.\n"
                + "Otherwise assign <role> to yourself";
    }

    @Override
    public void respond(CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel channel = matcher.getTextChannel();

        //Check that we can assign roles here
        final Guild guild = matcher.getGuild();
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            channel.sendMessage("It appears I can't assign roles here, if you think this to be a mistake contact server adminstrators").queue();
            return;
        }

        //Check persons doesn't already have a role
        final Member sender = matcher.getMember();
        final List<Role> currentRoles = sender.getRoles();
        if (currentRoles.size() > 0) {
            channel.sendMessage("You cannot assign role to yourself using this command if you alredy have a role").queue();
            return;
        }

        //Get the name of the guild user wants role for
        final String[] parameters = matcher.getArguments(1);
        if (parameters.length == 0) {
            autoAssignRole(channel, sender, guild);
            return;
        }

        //Ignore current server
        final String guildName = parameters[0];
        if (guild.getName().equalsIgnoreCase(guildName)) {
            channel.sendMessage("Can't assign role for current server, check rules if you want member status.").queue();
            return;
        }

        //Try to assign the role user wants
        assignRole(channel, sender, guild, guildName);
    }

    /**
     * Try to assign role to user
     *
     * @param channel Channel to use to respond
     * @param sender Command user
     * @param guild Current guild
     * @param roleName Name of the role user wants
     */
    private void assignRole(MessageChannel channel, Member sender, Guild guild, String roleName) {
        //Guilds we share with the user
        final List<Guild> mutualGuilds = sender.getUser().getMutualGuilds();

        //Get names of the mutual guilds
        final StringBuilder possibleRoleNames = new StringBuilder("Possible guilds:\n");

        //Find and assign role
        for (Guild g : mutualGuilds) {
            if (!g.getName().equals(roleName)) {
                possibleRoleNames.append(g.getName()).append('\n');
                continue;
            }

            //Make sure they are a member on the other server
            final Member otherMember = g.getMember(sender.getUser());
            if (otherMember == null) {
                channel.sendMessage("Did you leave the server while I wasn't looking? Could not find you on that server").queue();
                return;
            }
            if (otherMember.getRoles().isEmpty()) {
                channel.sendMessage("You do not have any roles on that server, as such no role was given").queue();
                return;
            }

            //Find the role for given guild
            final List<Role> roles = guild.getRolesByName(roleName, true);
            if (roles.isEmpty()) {
                channel.sendMessage("It appears we do not have a role for that server yet, please contact admin to fix").queue();
                return;
            }

            //Assign found role to the sender, this could assign multiple roles if there is multiple roles with same name
            guild.modifyMemberRoles(sender, roles, null).queue((t) -> {
                //Success
                channel.sendMessage("Role assigned succesfully").queue();
            }, (t) -> {
                //Failure
                LOGGER.warn(t);
                channel.sendMessage("Role assignment failed, either I can't assign roles on this server or other error occured").queue();
            });
            return;
        }

        //Did not find the guild, show list of found guilds
        channel.sendMessage("Could not find a guild named: " + roleName + possibleRoleNames).queue();
    }

    /**
     * Try to automatically assign role to sender
     *
     * @param channel Channel to use to respond
     * @param sender Command user
     * @param guild Current guild
     */
    private void autoAssignRole(MessageChannel channel, Member member, Guild guild) {
        final List<Guild> mutualGuilds = member.getUser().getMutualGuilds();
        if (mutualGuilds.size() < 2) {
            //Only this guild
            channel.sendMessage("You do not appear to be on any guilds other than this one that I can see,"
                    + " ask guild leaders if you belive this to be error.").queue();
            return;
        }
        if (mutualGuilds.size() == 2) {
            //This and another guild, try to get role for them based on other guild
            final List<Guild> mutableGuilds = new ArrayList<>(mutualGuilds);
            mutableGuilds.remove(guild);

            if (mutableGuilds.size() != 1) {
                channel.sendMessage("").queue();
                return;
            }

            final Guild otherGuild = mutableGuilds.get(0);
            final Member otherGuildmember = otherGuild.getMember(member.getUser());
            if (otherGuildmember == null) {
                //Has left other server somehow
                channel.sendMessage("Could not find a role to assign for you, are you on any other guilds I can see?").queue();
                return;
            }
            final List<Role> otherGuildRoles = otherGuildmember.getRoles();
            if (otherGuildRoles.isEmpty()) {
                //Not a member on other server
                channel.sendMessage("Could not find any guild where you are member, "
                        + "found one where you are but do not have any roles, as such no role was assigned").queue();
                return;
            }

            final String roleName = otherGuild.getName();
            final List<Role> roles = guild.getRolesByName(roleName, true);
            if (roles.isEmpty()) {
                channel.sendMessage("Can not find role for the other server, "
                        + "if you belive this to be error contact guild lead.").queue();
                return;
            }

            guild.modifyMemberRoles(member, roles, null).queue((t) -> {
                //Success
                channel.sendMessage(
                        "Succesfully assigned role: " + roles.get(0).getName()
                        + "if you believe this is wrong role for you please contact guild lead.").queue();
            }, (t) -> {
                //Failure
                LOGGER.warn(t);
                channel.sendMessage("Role assignment failed, either I can't assign roles on this server or other error occured").queue();
            });
            return;
        }

        //More guilds, ask them to use role command
        channel.sendMessage("You apper to be on multiple guilds and as such I can't find a role for you, "
                + "please provide the the guild name you want role for in the role command.").queue();

    }

}
