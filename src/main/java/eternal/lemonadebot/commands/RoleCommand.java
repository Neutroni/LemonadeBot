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
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.translation.TranslationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command used to assign roles to user based on other servers they are also on
 *
 * @author Neutroni
 */
class RoleCommand implements ChatCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(Locale locale) {
        return TranslationKey.COMMAND_ROLE.getTranslation(locale);
    }

    @Override
    public String getDescription(Locale locale) {
        return TranslationKey.DESCRIPTION_ROLE.getTranslation(locale);
    }

    @Override
    public String getHelpText(Locale locale) {
        return TranslationKey.SYNTAX_ROLE.getTranslation(locale);
    }

    @Override
    public Collection<CommandPermission> getDefaultRanks(Locale locale, long guildID) {
        return List.of(new CommandPermission(getCommand(locale), MemberRank.USER, guildID));
    }

    @Override
    public void respond(CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel channel = matcher.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final Locale locale = guildConf.getLocale();

        //Check that we can assign roles here
        final Guild guild = matcher.getGuild();
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            channel.sendMessage(TranslationKey.ROLE_BOT_NO_PERMISSION.getTranslation(locale)).queue();
            return;
        }

        //Check persons doesn't already have a role
        final Member sender = matcher.getMember();
        final List<Role> currentRoles = sender.getRoles();
        if (currentRoles.size() > 0) {
            channel.sendMessage(TranslationKey.ROLE_ALREADY_HAS_ROLE.getTranslation(locale)).queue();
            return;
        }

        //Get the name of the guild user wants role for
        final String[] parameters = matcher.getArguments(0);
        if (parameters.length == 0) {
            autoAssignRole(channel, sender, locale);
            return;
        }

        //Ignore current server
        final String requestedRoleName = parameters[0];
        final Collator collator = guildData.getTranslationCache().getCollator();
        if (collator.equals(guild.getName(), requestedRoleName)) {
            channel.sendMessage(TranslationKey.ROLE_CURRENT_GUILD_NOT_ALLOWED.getTranslation(locale)).queue();
            return;
        }

        //Try to assign the role user wants
        assignRole(channel, sender, requestedRoleName, guildData);
    }

    /**
     * Try to assign role to user
     *
     * @param channel Channel to use to respond
     * @param sender Command user
     * @param currentGuild Current guild
     * @param requestedRoleName Name of the role user wants
     */
    private void assignRole(TextChannel channel, Member sender, String requestedRoleName, GuildDataStore guildData) {
        final Guild currentGuild = channel.getGuild();
        final ConfigManager guildConf = guildData.getConfigManager();
        final Locale locale = guildConf.getLocale();
        final Collator collator = guildData.getTranslationCache().getCollator();
        //Guilds we share with the user
        final List<Guild> mutualGuilds = sender.getUser().getMutualGuilds();

        //Get names of the mutual guilds
        final List<String> possibleRoleNames = new ArrayList<>(mutualGuilds.size());
        final List<String> missingRoleNames = new ArrayList<>(mutualGuilds.size());

        //Find and assign role
        for (final Guild otherGuild : mutualGuilds) {
            //Ignore current guild
            if (otherGuild.equals(currentGuild)) {
                continue;
            }

            //Check if guild name is equal to one user requested
            final String otherGuildName = otherGuild.getName();
            if (!collator.equals(otherGuildName, requestedRoleName)) {
                if (currentGuild.getRolesByName(otherGuildName, true).isEmpty()) {
                    //Found guild that current guild does not have a role for
                    missingRoleNames.add(otherGuildName);
                } else {
                    possibleRoleNames.add(otherGuildName);
                }
                continue;
            }

            //Make sure they are a member on the other server
            final Member otherMember = otherGuild.getMember(sender.getUser());
            if (otherMember == null) {
                channel.sendMessage(TranslationKey.ROLE_OTHER_SERVER_MEMBER_NOT_FOUND.getTranslation(locale)).queue();
                return;
            }
            if (otherMember.getRoles().isEmpty()) {
                channel.sendMessage(TranslationKey.ROLE_OTHER_SEVER_NO_ROLES.getTranslation(locale)).queue();
                return;
            }

            //Find the matching role for given guild
            final List<Role> roles = currentGuild.getRolesByName(requestedRoleName, true);
            if (roles.isEmpty()) {
                channel.sendMessage(TranslationKey.ROLE_NO_ROLE_FOR_SERVER.getTranslation(locale)).queue();
                return;
            }

            //Assign found role to the sender, this could assign multiple roles if there is multiple roles with same name
            currentGuild.modifyMemberRoles(sender, roles, null).queue((Void t) -> {
                //Success
                channel.sendMessage(TranslationKey.ROLE_ASSING_SUCCESS.getTranslation(locale)).queue();
            }, (Throwable t) -> {
                //Failure
                LOGGER.warn("Assigning role failed: {}", t.getMessage());
                channel.sendMessage(TranslationKey.ROLE_ASSIGN_FAILED.getTranslation(locale)).queue();
            });
            return;
        }

        //Did not find guild with the requested name, show list of found guilds
        final MessageBuilder mb = new MessageBuilder();
        final String template = TranslationKey.ROLE_DID_NOT_FIND_GUILD.getTranslation(locale);
        mb.appendFormat(template, requestedRoleName);
        if (possibleRoleNames.isEmpty()) {
            mb.append(TranslationKey.ROLE_NO_AVAILABLE_ROLES.getTranslation(locale));
        } else {
            mb.appendFormat(TranslationKey.ROLE_VALID_ROLE_NAMES.getTranslation(locale), String.join(",", possibleRoleNames));
        }
        if (!missingRoleNames.isEmpty()) {
            mb.appendFormat(TranslationKey.ROLE_GUILD_MISSING_ROLES.getTranslation(locale), String.join(",", missingRoleNames));
        }
        channel.sendMessage(mb.build()).queue();
    }

    /**
     * Try to automatically assign role to sender
     *
     * @param channel Channel to use to respond
     * @param sender Command user
     * @param guild Current guild
     */
    private void autoAssignRole(TextChannel channel, Member member, Locale locale) {
        final Guild currentGuild = channel.getGuild();
        final List<Guild> mutualGuilds = member.getUser().getMutualGuilds();
        //Construct the list of valid guilds
        final List<Guild> validGuilds = mutualGuilds.stream().filter((Guild otherGuild) -> {
            //Ignore current guild
            if (otherGuild.equals(currentGuild)) {
                return false;
            }
            //Get the person on the other server
            final Member otherGuildmember = otherGuild.getMember(member.getUser());
            if (otherGuildmember == null) {
                return false;
            }
            //Check if the person has any roles on the server.
            final List<Role> otherGuildRoles = otherGuildmember.getRoles();
            return !otherGuildRoles.isEmpty();
        }).collect(Collectors.toList());

        //Check if we found any valid guilds
        if (validGuilds.isEmpty()) {
            //Get the other guilds user is also on
            final List<Guild> mutableGuilds = new ArrayList<>(mutualGuilds);
            mutableGuilds.removeIf(currentGuild::equals);
            if (mutableGuilds.isEmpty()) {
                //Only this guild
                channel.sendMessage(TranslationKey.ROLE_NO_MUTUAL_GUILDS.getTranslation(locale)).queue();
            } else {
                //Not a member on other server
                channel.sendMessage(TranslationKey.ROLE_NO_ROLES_ON_MUTUAL_SERVER.getTranslation(locale)).queue();
            }
            return;
        }

        //Found exactly one valid guild, assign role
        if (validGuilds.size() == 1) {
            final Guild otherGuild = validGuilds.get(0);

            final String roleName = otherGuild.getName();
            final List<Role> roles = currentGuild.getRolesByName(roleName, true);
            if (roles.isEmpty()) {
                channel.sendMessage(TranslationKey.ROLE_NO_ROLE_FOUND.getTranslation(locale)).queue();
                return;
            }

            currentGuild.modifyMemberRoles(member, roles, null).queue((t) -> {
                //Success
                final String template = TranslationKey.ROLE_AUTOMATIC_ASSIGN_SUCCESS.getTranslation(locale);
                channel.sendMessageFormat(template, roles.get(0).getName()).queue();
            }, (t) -> {
                //Failure
                LOGGER.warn(t);
                channel.sendMessage(TranslationKey.ROLE_ASSIGN_FAILED.getTranslation(locale)).queue();
            });
            return;
        }

        //More guilds, ask them to use role command
        channel.sendMessage(TranslationKey.ROLE_AUTOMATIC_MULTIPLE_GUILDS.getTranslation(locale)).queue();

    }

}
