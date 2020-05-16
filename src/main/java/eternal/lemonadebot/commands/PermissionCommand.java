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
import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.PermissionKey;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
class PermissionCommand extends AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand() {
        return "permission";
    }

    @Override
    public String getDescription() {
        return "Manage permissions for commands";
    }

    @Override
    public String getHelpText() {
        final String keys = Arrays.stream(PermissionKey.values()).map(
                (PermissionKey key) -> {
                    return key.toString();
                }).collect(Collectors.joining(","));
        return "Syntax: permission <name> [level]\n"
                + "<name> is one of: " + keys + "\n"
                + "[level] can be one of following\n"
                + CommandPermission.getLevelDescriptions();
    }

    @Override
    public void respond(CommandMatcher message, GuildDataStore guildData) {
        final TextChannel channel = message.getTextChannel();
        final String[] arguments = message.getArguments(2);
        if (arguments.length == 0) {
            channel.sendMessage("Provide the name of the permission to modify, check help for possible permissions.").queue();
            return;
        }

        //Parse arguments
        final ConfigManager guildConf = guildData.getConfigManager();
        final String permissionName = arguments[0];
        try {
            final PermissionKey key = PermissionKey.valueOf(permissionName);
            if (arguments.length == 1) {
                final CommandPermission perm = guildConf.getRequiredPermission(key);
                final String enumName = perm.name().toLowerCase();
                channel.sendMessage("Current permission: " + enumName).queue();
                return;
            }
            try {
                final String enumString = arguments[1].toUpperCase();
                final CommandPermission newPermission = CommandPermission.valueOf(enumString);
                if (guildConf.setPermission(key, newPermission)) {
                    channel.sendMessage("Permission updated succesfully").queue();
                } else {
                    channel.sendMessage("Updating permission failed, database failed to modify any recods").queue();
                }
            } catch (IllegalArgumentException e) {
                channel.sendMessage("Not a valid value for the permission: " + arguments[1]).queue();
            } catch (SQLException e) {
                channel.sendMessage("Database connection failed, new permission in use until reboot").queue();
                LOGGER.error("Failure to update permission in database");
                LOGGER.error(e.getMessage());
                LOGGER.trace("Stack trace:", e);
            }
        } catch (IllegalArgumentException e) {
            channel.sendMessage("Unknown permission: " + permissionName).queue();
        }
    }
}
