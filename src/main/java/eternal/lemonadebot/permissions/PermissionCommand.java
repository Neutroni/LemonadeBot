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
import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
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
    public String getCommand(final ResourceBundle locale) {
        return locale.getString("COMMAND_PERMISSION");
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_PERMISSION");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        final String template = locale.getString("SYNTAX_PERMISSION");
        return String.format(template, MemberRank.getLevelDescriptions(locale));
    }

    @Override
    public void respond(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final TranslationCache translationCache = context.getTranslation();
        final ResourceBundle locale = translationCache.getResourceBundle();
        final String[] arguments = matcher.getArguments(1);
        if (arguments.length == 0) {
            channel.sendMessage(locale.getString("ERROR_MISSING_OPERATION")).queue();
            return;
        }

        final String actionString = arguments[0];
        final ActionKey key = translationCache.getActionKey(actionString);
        switch (key) {
            case GET: {
                getPermission(arguments, context);
                break;
            }
            case SET: {
                setPermission(context);
                break;
            }
            case LIST: {
                listPermissions(context);
                break;
            }
            default: {
                channel.sendMessage(locale.getString("ERROR_UNKNOWN_OPERATION") + actionString).queue();
            }
        }
    }

    private static void getPermission(final String[] arguments, final CommandContext context) {
        final TextChannel channel = context.getChannel();
        final PermissionManager permissions = context.getPermissionManager();
        final ConfigManager config = context.getConfigManager();
        final ResourceBundle locale = context.getResource();

        if (arguments.length < 2) {
            channel.sendMessage(locale.getString("PERMISSION_GET_MISSING_NAME")).queue();
            return;
        }
        final String permissionName = arguments[1];

        //Check if there is a command for the given action
        final ChatCommand command;
        final CommandProvider commands = context.getCommandProvider();
        final int i = permissionName.indexOf(' ');
        if (i == -1) {
            final Optional<ChatCommand> optCommand = commands.getCommand(permissionName, config);
            if (optCommand.isEmpty()) {
                //No command for the action
                channel.sendMessage(locale.getString("PERMISSION_NO_COMMAND")).queue();
                return;
            }
            command = optCommand.get();
        } else {
            final String commandName = permissionName.substring(0, i);
            final Optional<ChatCommand> optCommand = commands.getCommand(commandName, config);
            if (optCommand.isEmpty()) {
                //No command for the action
                channel.sendMessage(locale.getString("PERMISSION_NO_COMMAND")).queue();
                return;
            }
            command = optCommand.get();
        }

        //Get the permission for the action
        final CommandPermission perm;
        try {
            perm = permissions.getPermission(command, permissionName, context.getGuild().getIdLong());
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("PERMISSION_SQL_ERROR_ON_FIND")).queue();
            LOGGER.error("Failure to get permission from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            return;
        }
        final long roleID = perm.getRequiredRoleID();
        final Role r = channel.getGuild().getRoleById(roleID);
        if (r == null) {
            final String template = locale.getString("PERMISSION_RANK_MISSING_ROLE");
            final String rankName = locale.getString(perm.getRequiredRank().getNameKey());
            final String actionName = perm.getAction();
            channel.sendMessageFormat(template, actionName, rankName).queue();
            return;
        }
        //Construct embed for response
        final EmbedBuilder eb = new EmbedBuilder();
        if (perm.getAction().isEmpty()) {
            eb.setTitle(locale.getString("PERMISSION_NO_PERMISSION"));
            eb.setDescription(locale.getString("PERMISSION_DEFAULTING_TO"));
        } else {
            eb.setTitle(locale.getString("HEADER_ACTION"));
            eb.setDescription(perm.getAction());
        }
        //Get required rank and role
        final String fieldName = locale.getString("HEADER_REQUIRED_PERMISSION");
        final String template = locale.getString("PERMISSION_REQUIRED_RANK_ROLE");
        final String rankName = locale.getString(perm.getRequiredRank().getNameKey());
        final String fieldValue = String.format(template, rankName, r.getAsMention());
        eb.addField(fieldName, fieldValue, false);
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    private static void setPermission(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final TranslationCache translationCache = context.getTranslation();
        final ResourceBundle locale = translationCache.getResourceBundle();

        final List<String> args = matcher.parseArguments(4);
        if (args.size() < 2) {
            channel.sendMessage(locale.getString("PERMISSION_SET_MISSING_RANK")).queue();
            return;
        }
        if (args.size() < 3) {
            channel.sendMessage(locale.getString("PERMISSION_SET_MISSING_ROLE")).queue();
            return;
        }
        if (args.size() < 4) {
            channel.sendMessage(locale.getString("PERMISSION_SET_MISSING_ACTION")).queue();
            return;
        }
        final String rankName = args.get(1);
        final MemberRank rank;
        try {
            rank = MemberRank.getByLocalizedName(rankName, translationCache);
        } catch (IllegalArgumentException e) {
            final MessageBuilder mb = new MessageBuilder();
            final String template = locale.getString("PERMISSION_UNKNOWN_RANK");
            mb.appendFormat(template, rankName, MemberRank.getLevelDescriptions(locale));
            channel.sendMessage(mb.build()).queue();
            return;
        }
        final String roleName = args.get(2);
        final Role role;
        final String localAnyRole = locale.getString("PERMISSION_ROLE_ANYONE");
        if (localAnyRole.equals(roleName)) {
            role = channel.getGuild().getPublicRole();
        } else {
            final List<Role> roles = channel.getGuild().getRolesByName(roleName, true);

            if (roles.isEmpty()) {
                channel.sendMessage(locale.getString("PERMISSION_ROLE_NOT_FOUND_NAME") + roleName).queue();
                return;
            }
            role = roles.get(0);
        }
        final String actionString = args.get(3);
        try {
            final PermissionManager permissions = context.getPermissionManager();
            final long guildID = context.getGuild().getIdLong();
            permissions.setPermission(new CommandPermission(actionString, rank, role.getIdLong(), guildID));
            channel.sendMessage(locale.getString("PERMISSION_UPDATE_SUCCESS")).queue();
        } catch (SQLException e) {
            channel.sendMessage(locale.getString("PERMISSION_SQL_ERROR_ON_SET")).queue();
            LOGGER.error("Failure to update permission in database: {}", e.getMessage());
            LOGGER.trace("Stack trace:", e);
        }
    }

    private static void listPermissions(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final Guild guild = matcher.getGuild();

        final ResourceBundle locale = context.getResource();

        final Collection<CommandPermission> permissions;
        try {
            final PermissionManager permissionManager = context.getPermissionManager();
            permissions = permissionManager.getPermissions(guild.getIdLong());
        } catch (SQLException ex) {
            channel.sendMessage(locale.getString("PERMISSION_SQL_ERROR_ON_LOAD")).queue();
            LOGGER.error("Failure to load permissions from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            return;
        }
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(locale.getString("HEADER_PERMISSIONS"));
        final StringBuilder description = new StringBuilder();
        final String template = locale.getString("PERMISSION_LIST_ELEMENT");
        for (final CommandPermission perm : permissions) {
            final String rankName = locale.getString(perm.getRequiredRank().getNameKey());
            //Get the name of role
            final String roleName;
            final long roleID = perm.getRequiredRoleID();
            final Role r = guild.getRoleById(roleID);
            if (r == null) {
                roleName = locale.getString("ROLE_MISSING");
            } else {
                roleName = r.getName();
            }
            //Format response
            final String line = String.format(template, perm.getAction(), rankName, roleName);
            description.append(line);
        }
        if (permissions.isEmpty()) {
            description.append(locale.getString("PERMISSION_NO_PERMISSIONS"));
        }
        eb.setDescription(description);
        channel.sendMessageEmbeds(eb.build()).queue();
    }
}
