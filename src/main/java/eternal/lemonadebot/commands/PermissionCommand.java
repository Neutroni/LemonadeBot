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
import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.PermissionManager;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.permissions.RequiredPermission;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Role;
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
        return "Syntax: permission <action> [rank] [role] <name>\n"
                + "<action> can be one of the following:"
                + " get to get current permission"
                + " set to update permission"
                + "<name> is the name of permission to update\n"
                + "[rank] can be one of following\n"
                + MemberRank.getLevelDescriptions()
                + "[role] is a name of role needed for permission, use 'anyone' to disable role check";
    }

    @Override
    public void respond(CommandMatcher message, GuildDataStore guildData) {
        final TextChannel channel = message.getTextChannel();
        final String[] arguments = message.getArguments(1);
        if (arguments.length == 0) {
            channel.sendMessage("Provide the name of the permission to modify, check help for possible permissions.").queue();
            return;
        }

        final PermissionManager permissions = guildData.getPermissionManager();
        final String actionString = arguments[0];
        switch (actionString) {
            case "get": {
                if (arguments.length < 2) {
                    channel.sendMessage("Provide name of permission to get curret value for.").queue();
                    return;
                }
                final String permissionName = arguments[1];
                final Optional<RequiredPermission> optPerm = permissions.getPermission(permissionName);
                optPerm.ifPresentOrElse((RequiredPermission perm) -> {
                    final long roleID = perm.getRequiredRoleID();
                    final Role r = channel.getGuild().getRoleById(roleID);
                    if (r == null) {
                        channel.sendMessage("Required rank: " + perm.getRequiredRank().name().toLowerCase()
                                + "required role could not be found, defaulting to anyone").queue();
                        return;
                    }
                    channel.sendMessage("Required rank: " + perm.getRequiredRank().name().toLowerCase()
                            + " required role: " + r.getName()).queue();

                }, () -> {
                    channel.sendMessage("No permission with that name currently set.").queue();
                });
                break;
            }
            case "set": {
                final String[] args = message.getArguments(3);
                if (args.length < 2) {
                    channel.sendMessage("Provide the rank to set for permission.").queue();
                    return;
                }
                if (args.length < 3) {
                    channel.sendMessage("Provide role to set for permission").queue();
                    return;
                }
                if (args.length < 4) {
                    channel.sendMessage("Provide name of permission to update").queue();
                    return;
                }
                final String rankName = args[1];
                final MemberRank rank;
                try {
                    final String enumString = rankName.toUpperCase();
                    rank = MemberRank.valueOf(enumString);
                } catch (IllegalArgumentException e) {
                    final MessageBuilder mb = new MessageBuilder();
                    mb.append("Unknown rank: ").append(rankName).append("\n");
                    mb.append("Valid values are:\n").append(MemberRank.getLevelDescriptions());
                    channel.sendMessage(mb.build()).queue();
                    return;
                }
                final String roleName = args[2];
                final Role role;
                if ("anyone".equals(roleName)) {
                    role = channel.getGuild().getPublicRole();
                } else {
                    final List<Role> roles = channel.getGuild().getRolesByName(roleName, true);

                    if (roles.isEmpty()) {
                        channel.sendMessage("Could not find a role with the name: " + roleName).queue();
                        return;
                    }
                    role = roles.get(0);
                }
                final String permissionName = args[3];
                final RequiredPermission perm = new RequiredPermission(rank, role.getIdLong());
                try {
                    permissions.setPermission(permissionName, perm);
                } catch (SQLException e) {
                    channel.sendMessage("Database connection failed, new permission in use until reboot").queue();
                    LOGGER.error("Failure to update permission in database");
                    LOGGER.error(e.getMessage());
                    LOGGER.trace("Stack trace:", e);
                }
                break;
            }
            default: {
                channel.sendMessage("Unkown action: " + actionString).queue();
            }
        }
    }
}
