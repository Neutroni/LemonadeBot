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

import eternal.lemonadebot.commandtypes.AdminCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.PermissionManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command used to set required permissions for commands
 *
 * @author Neutroni
 */
class PermissionCommand extends AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(Locale locale) {
        return TranslationKey.COMMAND_PERMISSION.getTranslation(locale);
    }

    @Override
    public String getDescription(Locale locale) {
        return TranslationKey.DESCRIPTION_PERMISSION.getTranslation(locale);
    }

    @Override
    public String getHelpText(Locale locale) {
        final String template = TranslationKey.SYNTAX_PERMISSION.getTranslation(locale);
        return String.format(template, MemberRank.getLevelDescriptions(locale));
    }

    @Override
    public void respond(CommandMatcher message, GuildDataStore guildData) {
        final TextChannel channel = message.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildConf.getLocale();
        final String[] arguments = message.getArguments(1);
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
                setPermission(message, channel, guildData);
                break;
            }
            default: {
                channel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + actionString).queue();
            }
        }
    }

    private void getPermission(String[] arguments, TextChannel channel, GuildDataStore guildData) {
        final PermissionManager permissions = guildData.getPermissionManager();
        final Locale locale = guildData.getConfigManager().getLocale();

        if (arguments.length < 2) {
            channel.sendMessage(TranslationKey.PERMISSION_GET_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String permissionName = arguments[1];
        final Optional<CommandPermission> optPerm = permissions.getPermission(permissionName);
        optPerm.ifPresentOrElse((CommandPermission perm) -> {
            final long roleID = perm.getRequiredRoleID();
            final Role r = channel.getGuild().getRoleById(roleID);
            if (r == null) {
                final String template = TranslationKey.PERMISSION_RANK_MISSING_ROLE.getTranslation(locale);
                final String rankName = perm.getRequiredRank().getNameKey().getTranslation(locale);
                final String actionName = perm.getAction();
                channel.sendMessageFormat(template, actionName, rankName).queue();
                return;
            }
            final String template = TranslationKey.PERMISSION_REQUIRED_RANK_ROLE.getTranslation(locale);
            final String rankName = perm.getRequiredRank().getNameKey().getTranslation(locale);
            final String actionName = perm.getAction();
            channel.sendMessageFormat(template, actionName, rankName, r.getName()).queue();
        }, () -> {
            channel.sendMessage(TranslationKey.PERMISSION_NOT_FOUND.getTranslation(locale)).queue();
        });
    }

    private void setPermission(CommandMatcher message, TextChannel channel, GuildDataStore guildData) {
        final PermissionManager permissions = guildData.getPermissionManager();
        final ConfigManager guildConf = guildData.getConfigManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildConf.getLocale();

        final String[] args = message.getArguments(3);
        if (args.length < 2) {
            channel.sendMessage(TranslationKey.PERMISSION_SET_MISSING_RANK.getTranslation(locale)).queue();
            return;
        }
        if (args.length < 3) {
            channel.sendMessage(TranslationKey.PERMISSION_SET_MISSING_ROLE.getTranslation(locale)).queue();
            return;
        }
        if (args.length < 4) {
            channel.sendMessage(TranslationKey.PERMISSION_SET_MISSING_ACTION.getTranslation(locale)).queue();
            return;
        }
        final String rankName = args[1];
        final MemberRank rank;
        try {
            rank = MemberRank.getByLocalizedName(rankName, guildConf.getLocale(), translationCache.getCollator());
        } catch (IllegalArgumentException e) {
            final MessageBuilder mb = new MessageBuilder();
            final String template = TranslationKey.PERMISSION_UNKNOWN_RANK.getTranslation(locale);
            mb.appendFormat(template, rankName, MemberRank.getLevelDescriptions(locale));
            channel.sendMessage(mb.build()).queue();
            return;
        }
        final String roleName = args[2];
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
        final String actionString = args[3];
        try {
            permissions.setPermission(new CommandPermission(actionString, rank, role.getIdLong()));
        } catch (SQLException e) {
            channel.sendMessage(TranslationKey.PERMISSION_SQL_ERROR_ON_SET.getTranslation(locale)).queue();
            LOGGER.error("Failure to update permission in database: {}", e.getMessage());
            LOGGER.trace("Stack trace:", e);
        }
    }
}
