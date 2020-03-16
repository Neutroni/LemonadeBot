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

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.customcommands.CommandManagmentCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.events.EventCommand;
import eternal.lemonadebot.events.RemainderCommand;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.database.CustomCommandManager;
import eternal.lemonadebot.music.MusicCommand;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;

/**
 *
 * @author Neutroni
 */
public class CommandProvider {

    private final List<ChatCommand> commands;
    private final DatabaseManager dataBase;

    /**
     * Constructor
     *
     * @param permissionManager permission helper for commands to use
     * @param db database for commands to use
     */
    public CommandProvider(PermissionManager permissionManager, DatabaseManager db) {
        this.commands = List.of(
                new HelpCommand(permissionManager, this),
                new EventCommand(permissionManager),
                new ChannelManagmentCommand(),
                new CommandManagmentCommand(permissionManager, db),
                new PrefixCommand(),
                new RoleCommand(),
                new ShutdownCommand(),
                new MusicCommand(db),
                new RemainderCommand(db),
                new PermissionCommand(),
                new VersionCommand(db)
        );
        this.dataBase = db;
    }

    /**
     * Get the action for command
     *
     * @param cmdMatcher Matcher to find command for
     * @return CommandAction or Option.empty if command was not found
     */
    public Optional<? extends ChatCommand> getAction(CommandMatcher cmdMatcher) {
        final Optional<String> name = cmdMatcher.getCommand();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        final String commandName = name.get();
        final Guild guild = cmdMatcher.getGuild();
        return getCommand(commandName, guild);
    }

    /**
     * Get command by name
     *
     * @param name command name to search action for
     * @param guild guild to search command for
     * @return Optional containing the action if found, empty if not found
     */
    public Optional<? extends ChatCommand> getCommand(String name, Guild guild) {
        //Check if we find command by that name
        for (ChatCommand c : this.commands) {
            if (name.equals(c.getCommand())) {
                return Optional.of(c);
            }
        }

        //Check if we find custom command by that name
        final CustomCommandManager customManager = this.dataBase.getGuildData(guild).getCustomCommands();
        final Optional<CustomCommand> custom = customManager.getCommand(name);
        if (custom.isPresent()) {
            return custom;
        }

        //Couldn't find a command with that name
        return Optional.empty();
    }

    /**
     * Get the commands
     *
     * @return List of commands
     */
    public List<ChatCommand> getCommands() {
        return this.commands;
    }

}
