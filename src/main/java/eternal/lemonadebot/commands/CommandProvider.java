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
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.TemplateManager;
import eternal.lemonadebot.music.MusicCommand;
import java.util.List;
import java.util.Optional;

/**
 * Class used to find command by name
 *
 * @author Neutroni
 */
public class CommandProvider {

    public static final List<ChatCommand> COMMANDS = List.of(
            new HelpCommand(),
            new MusicCommand(),
            new EventCommand(),
            new RemainderCommand(),
            new TemplateCommand(),
            new RoleCommand(),
            //Admin commands
            new ConfigCommand(),
            new CooldownCommand(),
            new PermissionCommand()
    );

    /**
     * Get the action for command
     *
     * @param cmdMatcher Matcher to find command for
     * @param guildData Stored data for the guild the message is from
     * @return CommandAction or Option.empty if command was not found
     */
    public static Optional<? extends ChatCommand> getAction(CommandMatcher cmdMatcher, GuildDataStore guildData) {
        final Optional<String> name = cmdMatcher.getCommand();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        final String commandName = name.get();
        return getCommand(commandName, guildData);
    }

    /**
     * Get command by name
     *
     * @param name command name to search action for
     * @param guild guild to search command for
     * @return Optional containing the action if found, empty if not found
     */
    public static Optional<? extends ChatCommand> getCommand(String name, GuildDataStore guild) {
        //Check if we find command by that name
        final Optional<ChatCommand> command = getBuiltInCommand(name);
        if (command.isPresent()) {
            return command;
        }

        //Check if we find custom command by that name
        final TemplateManager customManager = guild.getCustomCommands();
        final Optional<CustomCommand> custom = customManager.getCommand(name);
        if (custom.isPresent()) {
            return custom;
        }

        //Couldn't find a command with that name
        return Optional.empty();
    }

    /**
     * Find built-in command by name
     *
     * @param name name of the command to find
     * @return Optional containing the command if found
     */
    public static Optional<ChatCommand> getBuiltInCommand(String name) {
        for (ChatCommand c : COMMANDS) {
            if (name.equals(c.getCommand())) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /**
     * Constructor to hide the default constructor
     */
    private CommandProvider() {
        //No op
    }

}
