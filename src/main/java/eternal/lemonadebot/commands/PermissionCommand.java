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

import eternal.lemonadebot.commandtypes.OwnerCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import java.sql.SQLException;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class PermissionCommand extends OwnerCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand() {
        return "permission";
    }

    @Override
    public String getHelp() {
        return "Syntax: permission <name> [level]\n"
                + "<name> is one of commandEdit,commandRun,eventEdit,playMusic\n"
                + "[level] is one of user,member,admin,owner";
    }

    @Override
    public void respond(CommandMatcher message) {
        final TextChannel channel = message.getTextChannel();
        final String[] arguments = message.getArguments(2);
        if (arguments.length == 0) {
            channel.sendMessage("Provide the name of the permission to modify, check help for possible permissions.").queue();
            return;
        }

        //Parse arguments
        final ConfigManager guildConf = message.getGuildData().getConfigManager();
        switch (arguments[0]) {
            case "commandEdit": {
                if (arguments.length == 1) {
                    final String enumName = guildConf.getEditPermission().name().toLowerCase();
                    channel.sendMessage("Current permission for editing custom commands: " + enumName).queue();
                    return;
                }
                try {
                    final String enumString = arguments[1].toUpperCase();
                    final CommandPermission newEditPermission = CommandPermission.valueOf(enumString);
                    if (guildConf.setEditPermission(newEditPermission)) {
                        channel.sendMessage("Permission updated succesfully").queue();
                    } else {
                        channel.sendMessage("Updating permission failed, database failed to modify any recods").queue();
                    }
                } catch (IllegalArgumentException e) {
                    channel.sendMessage("Not a valid value for the permission: " + arguments[1]).queue();
                } catch (SQLException e) {
                    channel.sendMessage("Database connection failed, new permission in use untill reboot").queue();
                    LOGGER.debug(e.getMessage());
                    LOGGER.trace("Stack trace:", e);
                }
                break;
            }
            case "commandRun": {
                if (arguments.length == 1) {
                    final String enumName = guildConf.getCommandRunPermission().name().toLowerCase();
                    channel.sendMessage("Current permission for using custom commands: " + enumName).queue();
                    return;
                }
                try {
                    final String enumString = arguments[1].toUpperCase();
                    final CommandPermission newRunPermission = CommandPermission.valueOf(enumString);
                    if (guildConf.setCommandRunPermission(newRunPermission)) {
                        channel.sendMessage("Permission updated succesfully").queue();
                    } else {
                        channel.sendMessage("Updating permission failed, database failed to modify any recods").queue();
                    }
                } catch (IllegalArgumentException e) {
                    channel.sendMessage("Not a valid value for the permission: " + arguments[1]).queue();
                } catch (SQLException e) {
                    channel.sendMessage("Database connection failed, new permission in use untill reboot").queue();
                    LOGGER.debug(e.getMessage());
                    LOGGER.trace("Stack trace:", e);
                }
                break;
            }
            case "eventEdit": {
                if (arguments.length == 1) {
                    final String enumName = guildConf.getEventPermission().name().toLowerCase();
                    channel.sendMessage("Current permission for editing events: " + enumName).queue();
                    return;
                }
                try {
                    final String enumString = arguments[1].toUpperCase();
                    final CommandPermission newEventPermission = CommandPermission.valueOf(enumString);
                    if (guildConf.setEventPermission(newEventPermission)) {
                        channel.sendMessage("Permission updated succesfully").queue();
                    } else {
                        channel.sendMessage("Updating permission failed, database failed to modify any recods").queue();
                    }
                } catch (IllegalArgumentException e) {
                    channel.sendMessage("Not a valid value for the permission: " + arguments[1]).queue();
                } catch (SQLException e) {
                    channel.sendMessage("Database connection failed, new permission in use untill reboot").queue();
                    LOGGER.debug(e.getMessage());
                    LOGGER.trace("Stack trace:", e);
                }
                break;
            }
            case "remainderEdit": {
                if (arguments.length == 1) {
                    final String enumName = guildConf.getRemainderPermissions().name().toLowerCase();
                    channel.sendMessage("Current permission for editing remainders: " + enumName).queue();
                    return;
                }
                try {
                    final String enumString = arguments[1].toUpperCase();
                    final CommandPermission newRemainderPermission = CommandPermission.valueOf(enumString);
                    if (guildConf.setRemainderPermission(newRemainderPermission)) {
                        channel.sendMessage("Permission updated succesfully").queue();
                    } else {
                        channel.sendMessage("Updating permission failed, database failed to modify any recods").queue();
                    }
                } catch (IllegalArgumentException e) {
                    channel.sendMessage("Not a valid value for the permission: " + arguments[1]).queue();
                } catch (SQLException e) {
                    channel.sendMessage("Database connection failed, new permission in use untill reboot").queue();
                    LOGGER.debug(e.getMessage());
                    LOGGER.trace("Stack trace:", e);
                }
                break;
            }
            case "playMusic": {
                if (arguments.length == 1) {
                    final String enumName = guildConf.getPlayPermission().name().toLowerCase();
                    channel.sendMessage("Current permission for playing music: " + enumName).queue();
                    return;
                }
                try {
                    final String enumString = arguments[1].toUpperCase();
                    final CommandPermission newMusicPermission = CommandPermission.valueOf(enumString);
                    if (guildConf.setPlayPermission(newMusicPermission)) {
                        channel.sendMessage("Permission updated succesfully").queue();
                    } else {
                        channel.sendMessage("Updating permission failed, database failed to modify any recods").queue();
                    }
                } catch (IllegalArgumentException e) {
                    channel.sendMessage("Not a valid value for the permission: " + arguments[1]).queue();
                } catch (SQLException e) {
                    channel.sendMessage("Database connection failed, new permission in use untill reboot").queue();
                    LOGGER.debug(e.getMessage());
                    LOGGER.trace("Stack trace:", e);
                }
                break;
            }
            default: {
                channel.sendMessage("Unkown option: " + arguments[0]).queue();
            }
        }
    }

}
