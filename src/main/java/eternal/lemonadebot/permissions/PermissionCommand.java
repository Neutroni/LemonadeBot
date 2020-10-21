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
package eternal.lemonadebot.permissions;

import eternal.lemonadebot.commands.AdminCommand;
import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command used to set required permissions for commands
 *
 * @author Neutroni
 */
public class PermissionCommand extends AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(final Locale locale) {
        return TranslationKey.COMMAND_PERMISSION.getTranslation(locale);
    }

    @Override
    public String getDescription(final Locale locale) {
        return TranslationKey.DESCRIPTION_PERMISSION.getTranslation(locale);
    }

    @Override
    public String getHelpText(final Locale locale) {
        final String template = TranslationKey.SYNTAX_PERMISSION.getTranslation(locale);
        return String.format(template, MemberRank.getLevelDescriptions(locale));
    }

    @Override
    public void respond(final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel channel = matcher.getTextChannel();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildData.getConfigManager().getLocale();
        final String[] arguments = matcher.getArguments(1);
        if (arguments.length == 0) {
            channel.sendMessage(TranslationKey.ERROR_MISSING_OPERATION.getTranslation(locale)).queue();
            return;
        }

        final String actionString = arguments[0];
        final ActionKey key = translationCache.getActionKey(actionString);
        switch (key) {
            case GET: {
                getPermission(arguments, channel, guildData);
                break;
            }
            case SET: {
                setPermission(matcher, guildData);
                break;
            }
            case LIST: {
                listPermissions(matcher, guildData);
                break;
            }
            default: {
                channel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + actionString).queue();
            }
        }
    }

    private static void getPermission(final String[] arguments, final TextChannel channel, final GuildDataStore guildData) {
        final PermissionManager permissions = guildData.getPermissionManager();
        final Locale locale = guildData.getConfigManager().getLocale();

        if (arguments.length < 2) {
            channel.sendMessage(TranslationKey.PERMISSION_GET_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String permissionName = arguments[1];

        //Check if there is a command for the given action
        final ChatCommand command;
        final CommandProvider commands = guildData.getCommandProvider();
        final int i = permissionName.indexOf(' ');
        if (i == -1) {
            final Optional<ChatCommand> optCommand = commands.getCommand(permissionName);
            if (optCommand.isEmpty()) {
                //No command for the action
                channel.sendMessage(TranslationKey.PERMISSION_NO_COMMAND.getTranslation(locale)).queue();
                return;
            }
            command = optCommand.get();
        } else {
            final String commandName = permissionName.substring(0, i);
            final Optional<ChatCommand> optCommand = commands.getCommand(commandName);
            if (optCommand.isEmpty()) {
                //No command for the action
                channel.sendMessage(TranslationKey.PERMISSION_NO_COMMAND.getTranslation(locale)).queue();
                return;
            }
            command = optCommand.get();
        }

        //Get the permission for the action
        final CommandPermission perm;
        try {
            perm = permissions.getPermission(command, permissionName);
        } catch (SQLException ex) {
            channel.sendMessage(TranslationKey.PERMISSION_SQL_ERROR_ON_FIND.getTranslation(locale)).queue();
            LOGGER.error("Failure to get permission from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            return;
        }
        final long roleID = perm.getRequiredRoleID();
        final Role r = channel.getGuild().getRoleById(roleID);
        if (r == null) {
            final String template = TranslationKey.PERMISSION_RANK_MISSING_ROLE.getTranslation(locale);
            final String rankName = perm.getRequiredRank().getNameKey().getTranslation(locale);
            final String actionName = perm.getAction();
            channel.sendMessageFormat(template, actionName, rankName).queue();
            return;
        }
        //Construct embed for response
        final EmbedBuilder eb = new EmbedBuilder();
        if (perm.getAction().isEmpty()) {
            eb.setTitle(TranslationKey.PERMISSION_NO_PERMISSION.getTranslation(locale));
            eb.setDescription(TranslationKey.PERMISSION_DEFAULTING_TO.getTranslation(locale));
        } else {
            eb.setTitle(TranslationKey.HEADER_ACTION.getTranslation(locale));
            eb.setDescription(perm.getAction());
        }
        //Get required rank and role
        final String fieldName = TranslationKey.HEADER_REQUIRED_PERMISSION.getTranslation(locale);
        final String template = TranslationKey.PERMISSION_REQUIRED_RANK_ROLE.getTranslation(locale);
        final String rankName = perm.getRequiredRank().getNameKey().getTranslation(locale);
        final String fieldValue = String.format(template, rankName, r.getAsMention());
        eb.addField(fieldName, fieldValue, false);
        channel.sendMessage(eb.build()).queue();
    }

    private static void setPermission(final CommandMatcher message, final GuildDataStore guildData) {
        final TextChannel channel = message.getTextChannel();
        final PermissionManager permissions = guildData.getPermissionManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildData.getConfigManager().getLocale();

        final List<String> args = message.parseArguments(4);
        if (args.size() < 2) {
            channel.sendMessage(TranslationKey.PERMISSION_SET_MISSING_RANK.getTranslation(locale)).queue();
            return;
        }
        if (args.size() < 3) {
            channel.sendMessage(TranslationKey.PERMISSION_SET_MISSING_ROLE.getTranslation(locale)).queue();
            return;
        }
        if (args.size() < 4) {
            channel.sendMessage(TranslationKey.PERMISSION_SET_MISSING_ACTION.getTranslation(locale)).queue();
            return;
        }
        final String rankName = args.get(1);
        final MemberRank rank;
        try {
            rank = MemberRank.getByLocalizedName(rankName, locale, translationCache.getCollator());
        } catch (IllegalArgumentException e) {
            final MessageBuilder mb = new MessageBuilder();
            final String template = TranslationKey.PERMISSION_UNKNOWN_RANK.getTranslation(locale);
            mb.appendFormat(template, rankName, MemberRank.getLevelDescriptions(locale));
            channel.sendMessage(mb.build()).queue();
            return;
        }
        final String roleName = args.get(2);
        final Role role;
        final String localAnyRole = TranslationKey.PERMISSION_ROLE_ANYONE.getTranslation(locale);
        if (localAnyRole.equals(roleName)) {
            role = channel.getGuild().getPublicRole();
        } else {
            final List<Role> roles = channel.getGuild().getRolesByName(roleName, true);

            if (roles.isEmpty()) {
                channel.sendMessage(TranslationKey.PERMISSION_ROLE_NOT_FOUND_NAME.getTranslation(locale) + roleName).queue();
                return;
            }
            role = roles.get(0);
        }
        final String actionString = args.get(3);
        try {
            permissions.setPermission(new CommandPermission(actionString, rank, role.getIdLong()));
            channel.sendMessage(TranslationKey.PERMISSION_UPDATE_SUCCESS.getTranslation(locale)).queue();
        } catch (SQLException e) {
            channel.sendMessage(TranslationKey.PERMISSION_SQL_ERROR_ON_SET.getTranslation(locale)).queue();
            LOGGER.error("Failure to update permission in database: {}", e.getMessage());
            LOGGER.trace("Stack trace:", e);
        }
    }

    private static void listPermissions(final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel channel = matcher.getTextChannel();
        final Guild guild = matcher.getGuild();
        final PermissionManager permissionManager = guildData.getPermissionManager();
        final Locale locale = guildData.getConfigManager().getLocale();

        final Collection<CommandPermission> permissions;
        try {
            permissions = permissionManager.getPermissions();
        } catch (SQLException ex) {
            channel.sendMessage(TranslationKey.PERMISSION_SQL_ERROR_ON_LOAD.getTranslation(locale)).queue();
            LOGGER.error("Failure to load permissions from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            return;
        }
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(TranslationKey.HEADER_PERMISSIONS.getTranslation(locale));
        final StringBuilder description = new StringBuilder();
        final String template = TranslationKey.PERMISSION_LIST_ELEMENT.getTranslation(locale);
        for (final CommandPermission perm : permissions) {
            final String rankName = perm.getRequiredRank().getNameKey().getTranslation(locale);
            //Get the name of role
            final String roleName;
            final long roleID = perm.getRequiredRoleID();
            final Role r = guild.getRoleById(roleID);
            if (r == null) {
                roleName = TranslationKey.ROLE_MISSING.getTranslation(locale);
            } else {
                roleName = r.getName();
            }
            //Format response
            final String line = String.format(template, perm.getAction(), rankName, roleName);
            description.append(line);
        }
        if (permissions.isEmpty()) {
            description.append(TranslationKey.PERMISSION_NO_PERMISSIONS.getTranslation(locale));
        }
        eb.setDescription(description);
        channel.sendMessage(eb.build()).queue();
    }
}
