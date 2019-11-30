/*
 * The MIT License
 *
 * Copyright 2019 Neutroni.
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
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandManager;
import eternal.lemonadebot.music.MusicCommand;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Neutroni
 */
public class CommandProvider {

    private final List<ChatCommand> commands;

    /**
     * Constructor
     *
     * @param parser parser for commands to use
     * @param db database for commands to use
     */
    public CommandProvider(CommandManager parser, DatabaseManager db) {
        this.commands = List.of(
                new HelpCommand(parser, this, db),
                new EventCommand(parser, db),
                new ChannelManagmentCommand(parser, db),
                new CommandManagmentCommand(parser, db),
                new PrefixCommand(parser),
                new RoleCommand(parser),
                new ShutdownCommand(),
                new MusicCommand(parser)
        );
    }

    /**
     * Get command by name
     *
     * @param name command name to search action for
     * @return Optional containing the action if found, empty if not found
     */
    public Optional<ChatCommand> getCommand(String name) {
        for (ChatCommand c : this.commands) {
            if (name.equals(c.getCommand())) {
                return Optional.of(c);
            }
        }
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
