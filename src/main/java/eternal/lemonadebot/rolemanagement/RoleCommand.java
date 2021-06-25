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
package eternal.lemonadebot.rolemanagement;

import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.database.CacheConfig;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import net.dv8tion.jda.api.EmbedBuilder;
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
public class RoleCommand extends ChatCommand {

    private static final Logger LOGGER = LogManager.getLogger();
    private final DataSource dataSource;
    private final CacheConfig cacheConfig;
    private final Random RNG = new Random();
    private final Map<Long, RoleManager> managers;

    /**
     * Constructor
     *
     * @param db Database connection
     */
    public RoleCommand(final DatabaseManager db) {
        this.dataSource = db.getDataSource();
        this.cacheConfig = db.getCacheConfig();
        this.managers = new ConcurrentHashMap<>();
    }

    /**
     * Get RoleManager for a guild
     *
     * @param guild Guild to get rolemanager for
     * @return RoleManager
     */
    private RoleManager getRoleManager(final Guild guild) {
        return this.managers.computeIfAbsent(guild.getIdLong(), (Long t) -> {
            if (this.cacheConfig.allowedRolesCacheEnabled()) {
                return new RoleManagerCache(dataSource, t);
            } else {
                return new RoleManager(dataSource, t);
            }
        });
    }

    @Override
    public String getCommand(final ResourceBundle locale) {
        return locale.getString("COMMAND_ROLE");
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_ROLE");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        return locale.getString("SYNTAX_ROLE");
    }

    @Override
    public Collection<CommandPermission> getDefaultRanks(final ResourceBundle locale, final long guildID, final PermissionManager permissions) {
        final String commandName = getCommand(locale);
        final String actionGet = locale.getString("ACTION_GET");
        final String actionRemove = locale.getString("ACTION_REMOVE");
        final String actionAllow = locale.getString("ACTION_ALLOW");
        final String actionDisallow = locale.getString("ACTION_DISALLOW");
        return List.of(new CommandPermission(commandName, MemberRank.USER, guildID),
                new CommandPermission(commandName + ' ' + actionGet, MemberRank.MEMBER, guildID),
                new CommandPermission(commandName + ' ' + actionRemove, MemberRank.MEMBER, guildID),
                new CommandPermission(commandName + ' ' + actionAllow, MemberRank.ADMIN, guildID),
                new CommandPermission(commandName + ' ' + actionDisallow, MemberRank.ADMIN, guildID));
    }

    @Override
    protected void respond(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final TranslationCache translationCache = context.getTranslation();
        final ResourceBundle locale = translationCache.getResourceBundle();

        //Check that we can assign roles here
        final Guild guild = matcher.getGuild();
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            channel.sendMessage(locale.getString("ROLE_BOT_NO_PERMISSION")).queue();
            return;
        }

        final String[] opts = matcher.getArguments(2);
        if (opts.length == 0) {
            channel.sendMessage(locale.getString("ERROR_MISSING_OPERATION")).queue();
            return;
        }

        final String action = opts[0];
        final ActionKey key = translationCache.getActionKey(action);
        switch (key) {
            case ALLOW: {
                allowRole(context);
                break;
            }
            case DISALLOW: {
                disallowRole(opts, context);
                break;
            }
            case GET: {
                getRole(opts, context);
                break;
            }
            case REMOVE: {
                removeRole(opts, context);
                break;
            }
            case LIST: {
                listAllowedRoles(context);
                break;
            }
            case RANDOM: {
                getRandomMemberWithRole(opts, context);
                break;
            }
            case GUILD: {
                getRoleFromGuild(opts, context);
                break;
            }
            default: {
                channel.sendMessage(locale.getString("ERROR_UNKNOWN_OPERATION") + action).queue();
            }
        }

    }

    private static void getRoleFromGuild(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final Guild guild = matcher.getGuild();
        final TranslationCache translation = context.getTranslation();
        final ResourceBundle locale = translation.getResourceBundle();

        //Check persons doesn't already have a role
        final Member sender = matcher.getMember();
        final List<Role> currentRoles = sender.getRoles();
        if (currentRoles.size() > 0) {
            channel.sendMessage(locale.getString("ROLE_ALREADY_HAS_ROLE")).queue();
            return;
        }
        //Get the name of the guild user wants role for
        if (opts.length < 2) {
            autoAssignRole(channel, sender, locale);
            return;
        }
        //Ignore current server
        final String requestedRoleName = opts[1];
        final Collator collator = translation.getCollator();
        if (collator.equals(guild.getName(), requestedRoleName)) {
            channel.sendMessage(locale.getString("ROLE_CURRENT_GUILD_NOT_ALLOWED")).queue();
            return;
        }
        //Try to assign the role user wants
        assignRole(channel, sender, requestedRoleName, translation);
    }

    /**
     * Try to assign role to user
     *
     * @param channel Channel to use to respond
     * @param sender Command user
     * @param requestedRoleName Name of the role user wants
     * @param guildData GuildData to get locale from
     */
    private static void assignRole(final TextChannel channel, final Member sender, final String requestedRoleName, final TranslationCache translation) {
        final Guild currentGuild = channel.getGuild();
        final ResourceBundle locale = translation.getResourceBundle();
        final Collator collator = translation.getCollator();
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
                channel.sendMessage(locale.getString("ROLE_OTHER_SERVER_MEMBER_NOT_FOUND")).queue();
                return;
            }
            if (otherMember.getRoles().isEmpty()) {
                channel.sendMessage(locale.getString("ROLE_OTHER_SEVER_NO_ROLES")).queue();
                return;
            }

            //Find the matching role for given guild
            final List<Role> roles = currentGuild.getRolesByName(requestedRoleName, true);
            if (roles.isEmpty()) {
                channel.sendMessage(locale.getString("ROLE_NO_ROLE_FOR_SERVER")).queue();
                return;
            }

            //Assign found role to the sender, this could assign multiple roles if there is multiple roles with same name
            currentGuild.modifyMemberRoles(sender, roles, null).queue((Void t) -> {
                //Success
                channel.sendMessage(locale.getString("ROLE_ASSING_SUCCESS")).queue();
            }, (Throwable t) -> {
                //Failure
                LOGGER.warn("Assigning role failed: {}", t.getMessage());
                channel.sendMessage(locale.getString("ROLE_ASSIGN_FAILED")).queue();
            });
            return;
        }

        //Did not find guild with the requested name, show list of found guilds
        final MessageBuilder mb = new MessageBuilder();
        final String template = locale.getString("ROLE_DID_NOT_FIND_GUILD");
        mb.appendFormat(template, requestedRoleName);
        if (possibleRoleNames.isEmpty()) {
            mb.append(locale.getString("ROLE_NO_AVAILABLE_ROLES"));
        } else {
            mb.appendFormat(locale.getString("ROLE_VALID_ROLE_NAMES"), String.join(",", possibleRoleNames));
        }
        if (!missingRoleNames.isEmpty()) {
            mb.appendFormat(locale.getString("ROLE_GUILD_MISSING_ROLES"), String.join(",", missingRoleNames));
        }
        channel.sendMessage(mb.build()).queue();
    }

    /**
     * Try to automatically assign role to sender
     *
     * @param channel Channel to use to respond
     * @param member Command user
     * @param locale Locale for current guild
     */
    private static void autoAssignRole(final TextChannel channel, final Member member, final ResourceBundle locale) {
        final Guild currentGuild = channel.getGuild();
        final List<Guild> mutualGuilds = member.getUser().getMutualGuilds();
        //Construct the list of valid guilds
        final List<Guild> validGuilds = mutualGuilds.stream().filter((Guild otherGuild) -> {
            //Ignore current guild
            if (otherGuild.equals(currentGuild)) {
                return false;
            }
            //Get the person on the other server
            final Member otherGuildMember = otherGuild.getMember(member.getUser());
            if (otherGuildMember == null) {
                return false;
            }
            //Check if the person has any roles on the server.
            final List<Role> otherGuildRoles = otherGuildMember.getRoles();
            return !otherGuildRoles.isEmpty();
        }).collect(Collectors.toList());

        //Check if we found any valid guilds
        if (validGuilds.isEmpty()) {
            //Get the other guilds user is also on
            final List<Guild> mutableGuilds = new ArrayList<>(mutualGuilds);
            mutableGuilds.removeIf(currentGuild::equals);
            if (mutableGuilds.isEmpty()) {
                //Only this guild
                channel.sendMessage(locale.getString("ROLE_NO_MUTUAL_GUILDS")).queue();
            } else {
                //Not a member on other server
                channel.sendMessage(locale.getString("ROLE_NO_ROLES_ON_MUTUAL_SERVER")).queue();
            }
            return;
        }

        //Found exactly one valid guild, assign role
        if (validGuilds.size() == 1) {
            final Guild otherGuild = validGuilds.get(0);

            final String roleName = otherGuild.getName();
            final List<Role> roles = currentGuild.getRolesByName(roleName, true);
            if (roles.isEmpty()) {
                channel.sendMessage(locale.getString("ROLE_NO_ROLE_FOUND")).queue();
                return;
            }

            currentGuild.modifyMemberRoles(member, roles, null).queue((t) -> {
                //Success
                final String template = locale.getString("ROLE_AUTOMATIC_ASSIGN_SUCCESS");
                channel.sendMessageFormat(template, roles.get(0).getName()).queue();
            }, (t) -> {
                //Failure
                LOGGER.warn(t);
                channel.sendMessage(locale.getString("ROLE_ASSIGN_FAILED")).queue();
            });
            return;
        }

        //More guilds, ask them to use role command
        channel.sendMessage(locale.getString("ROLE_AUTOMATIC_MULTIPLE_GUILDS")).queue();

    }

    private void allowRole(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();
        final List<String> opts = matcher.parseArguments(3);
        final Member requester = matcher.getMember();
        if (!requester.hasPermission(Permission.MANAGE_ROLES)) {
            channel.sendMessage(locale.getString("ROLE_USER_NO_PERMISSION")).queue();
            return;
        }
        if (opts.size() < 2) {
            channel.sendMessage(locale.getString("ROLE_ALLOW_MISSING_ROLE_NAME")).queue();
            return;
        }
        final String roleName = opts.get(1);
        final Guild guild = matcher.getGuild();
        final List<Role> roles = guild.getRolesByName(roleName, false);
        if (roles.isEmpty()) {
            final String template = locale.getString("ROLE_NO_ROLE_WITH_NAME");
            channel.sendMessageFormat(template, roleName).queue();
            return;
        }
        if (roles.size() > 1) {
            final String template = locale.getString("ROLE_MULTIPLE_ROLES_WITH_NAME");
            channel.sendMessageFormat(template, roleName).queue();
            return;
        }
        final Role role = roles.get(0);

        //Get the description for role
        final String roleDescription;
        if (opts.size() < 3) {
            roleDescription = null;
        } else {
            roleDescription = opts.get(2);
        }

        //Add role to roleManagers list
        final RoleManager roleManager = getRoleManager(guild);
        final AllowedRole allowedRole = new AllowedRole(role, roleDescription);
        try {
            if (roleManager.allowRole(allowedRole)) {
                channel.sendMessage(locale.getString("ROLE_ALLOW_SUCCESS")).queue();
                return;
            }
            channel.sendMessage(locale.getString("ROLE_ALLOW_ALREADY_ALLOWED")).queue();
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("ROLE_SQL_ERROR_ON_ALLOW")).queue();
            LOGGER.error("Failure to allow role: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }

    }

    private void disallowRole(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();
        final Member requester = matcher.getMember();
        if (!requester.hasPermission(Permission.MANAGE_ROLES)) {
            channel.sendMessage(locale.getString("ROLE_USER_NO_PERMISSION")).queue();
            return;
        }
        if (opts.length < 2) {
            channel.sendMessage(locale.getString("ROLE_DISALLOW_MISSING_ROLE_NAME")).queue();
            return;
        }
        final String roleName = opts[1];
        final Guild guild = matcher.getGuild();
        final List<Role> roles = guild.getRolesByName(roleName, false);
        if (roles.isEmpty()) {
            final String template = locale.getString("ROLE_NO_ROLE_WITH_NAME");
            channel.sendMessageFormat(template, roleName).queue();
            return;
        }
        if (roles.size() > 1) {
            final String template = locale.getString("ROLE_MULTIPLE_ROLES_WITH_NAME");
            channel.sendMessageFormat(template, roleName).queue();
            return;
        }
        final Role role = roles.get(0);
        final RoleManager roleManager = getRoleManager(guild);
        try {
            if (roleManager.disallowRole(role)) {
                channel.sendMessage(locale.getString("ROLE_DISALLOW_SUCCESS")).queue();
                return;
            }
            channel.sendMessage(locale.getString("ROLE_DISALLOW_ALREADY_DISALLOWED")).queue();
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("ROLE_SQL_ERROR_ON_DISALLOW")).queue();
            LOGGER.error("Failure to disallow role: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private void getRole(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();
        if (opts.length < 2) {
            channel.sendMessage(locale.getString("ROLE_GET_MISSING_ROLE_NAME")).queue();
            return;
        }
        final String roleName = opts[1];

        //Get the role
        final Guild guild = matcher.getGuild();
        final List<Role> roles = guild.getRolesByName(roleName, false);
        if (roles.isEmpty()) {
            final String template = locale.getString("ROLE_NO_ROLE_WITH_NAME");
            channel.sendMessageFormat(template, roleName).queue();
            return;
        }
        if (roles.size() > 1) {
            final String template = locale.getString("ROLE_MULTIPLE_ROLES_WITH_NAME");
            channel.sendMessageFormat(template, roleName).queue();
            return;
        }
        final Role role = roles.get(0);
        final RoleManager roleManager = getRoleManager(guild);

        //Check if we can remove the role from requester
        final boolean roleAllowed;
        try {
            roleAllowed = roleManager.isAllowed(role);
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("ROLE_SQL_ERROR_ON_CHECK")).queue();
            LOGGER.error("Failure to check if we can remove role from user: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
            return;
        }

        //Can assign the role
        if (roleAllowed) {
            final Member requester = matcher.getMember();
            guild.addRoleToMember(requester, role).queue((Void t) -> {
                //Assigned role successfully
                final String template = locale.getString("ROLE_ASSIGNED_SUCCESFULLY");
                channel.sendMessageFormat(template, roleName).queue();
            }, (Throwable e) -> {
                //Failed to assign role
                channel.sendMessage(locale.getString("ROLE_BOT_NO_PERMISSION")).queue();
            });
            return;
        }

        //Role not allowed
        final String template = locale.getString("ROLE_ROLE_NOT_ALLOWED");
        channel.sendMessageFormat(template, roleName).queue();
    }

    private void removeRole(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();
        if (opts.length < 2) {
            channel.sendMessage(locale.getString("ROLE_REMOVE_MISSING_ROLE_NAME")).queue();
            return;
        }
        final String roleName = opts[1];
        final Guild guild = matcher.getGuild();
        final List<Role> roles = guild.getRolesByName(roleName, false);
        if (roles.isEmpty()) {
            final String template = locale.getString("ROLE_NO_ROLE_WITH_NAME");
            channel.sendMessageFormat(template, roleName).queue();
            return;
        }
        if (roles.size() > 1) {
            final String template = locale.getString("ROLE_MULTIPLE_ROLES_WITH_NAME");
            channel.sendMessageFormat(template, roleName).queue();
            return;
        }
        final Role role = roles.get(0);
        final RoleManager roleManager = getRoleManager(guild);

        //Check if we can remove the role from requester
        final boolean roleAllowed;
        try {
            roleAllowed = roleManager.isAllowed(role);
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("ROLE_SQL_ERROR_ON_CHECK")).queue();
            LOGGER.error("Failure to check if we can remove role from user: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
            return;
        }

        //Can remove role
        if (roleAllowed) {
            final Member requester = matcher.getMember();
            guild.removeRoleFromMember(requester, role).queue((Void t) -> {
                //Assigned role successfully
                channel.sendMessage(locale.getString("ROLE_REMOVED_SUCCESFULLY")).queue();
            }, (Throwable e) -> {
                //Failed to assign role
                channel.sendMessage(locale.getString("ROLE_BOT_NO_PERMISSION")).queue();
            });
            return;
        }

        //Role not allowed
        final String template = locale.getString("ROLE_ROLE_NOT_ALLOWED");
        channel.sendMessageFormat(template, roleName).queue();

    }

    private void listAllowedRoles(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();
        final Guild guild = matcher.getGuild();

        //Get the list of roles we are allowed to assign
        final RoleManager roleManager = getRoleManager(guild);
        final Collection<AllowedRole> roles;
        try {
            roles = roleManager.getRoles();
        } catch (SQLException ex) {
            LOGGER.error("Failure to get list of roles from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
            channel.sendMessage(locale.getString("ROLE_SQL_ERROR_ON_LIST")).queue();
            return;
        }

        //Construct embed for the list of roles
        final StringBuilder sb = new StringBuilder();
        roles.forEach(role -> {
            sb.append(role.toListElement(locale, guild));
        });
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(locale.getString("HEADER_ALLOWED_ROLES"));
        eb.setDescription(sb);

        channel.sendMessageEmbeds(eb.build()).queue();
    }

    private void getRandomMemberWithRole(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();
        if (opts.length < 2) {
            channel.sendMessage(locale.getString("ROLE_RANDOM_MISSING_ROLE_NAME")).queue();
            return;
        }
        final String roleName = opts[1];
        final Guild guild = matcher.getGuild();
        final List<Role> roles = guild.getRolesByName(roleName, false);
        if (roles.isEmpty()) {
            final String template = locale.getString("ROLE_NO_ROLE_WITH_NAME");
            channel.sendMessageFormat(template, roleName).queue();
            return;
        }
        if (roles.size() > 1) {
            final String template = locale.getString("ROLE_MULTIPLE_ROLES_WITH_NAME");
            channel.sendMessageFormat(template, roleName).queue();
            return;
        }
        final Role role = roles.get(0);
        guild.findMembers((Member guildMember) -> {
            return guildMember.getRoles().contains(role);
        }).onSuccess((List<Member> members) -> {
            if (members.isEmpty()) {
                final String template = locale.getString("ROLE_NO_MEMBERS");
                channel.sendMessageFormat(template, roleName).queue();
                return;
            }
            final int index = this.RNG.nextInt(members.size());
            final Member member = members.get(index);
            final String template = locale.getString("ROLE_SELECTED_USER");
            channel.sendMessageFormat(template, member.getEffectiveName()).queue();
        }).onError((t) -> {
            channel.sendMessage(locale.getString("ROLE_BOT_PRIVILIGE_MISSING")).queue();
            LOGGER.warn("Failed to get members with role: {}, error: {}", role.getId(), t.getMessage());
            LOGGER.trace("Stack trace", t);
        });
    }
}
