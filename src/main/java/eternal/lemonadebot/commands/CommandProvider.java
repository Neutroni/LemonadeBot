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

import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.customcommands.TemplateManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class used to find command by name
 *
 * @author Neutroni
 */
public class CommandProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private final CommandList commands;
    private final ConfigManager configManager;
    private final TemplateManager templateManager;

    /**
     * Constructor
     *
     * @param commands List of built in commands
     * @param config Locale in which to load command names in
     * @param templates TemplateManager to get templates from
     */
    public CommandProvider(final CommandList commands, final ConfigManager config, final TemplateManager templates) {
        this.commands = commands;
        this.configManager = config;
        this.templateManager = templates;
    }

    /**
     * Get the action for command
     *
     * @param cmdMatcher Matcher to find command for
     * @return CommandAction or Option.empty if command was not found
     */
    public Optional<ChatCommand> getAction(final CommandMatcher cmdMatcher) {
        return cmdMatcher.getCommand().flatMap(this::getCommand);
    }

    /**
     * Get command by name
     *
     * @param name command name to search action for
     * @return Optional containing the action if found, empty if not found
     */
    public Optional<ChatCommand> getCommand(final String name) {
        final Locale locale = this.configManager.getLocale();
        //Checks if we find built in command by that name
        return this.commands.getBuiltInCommand(name, locale).or(() -> {
            //Did not find built in command, return optional from templateManager
            try {
                return this.templateManager.getCommand(name);
            } catch (SQLException e) {
                LOGGER.error("Loading templates from database failed: {}", e.getMessage());
                LOGGER.trace("Stack trace: ", e);
                return Optional.empty();
            }
        });
    }

    /**
     * Get the list of base commands
     *
     * @return CommandList
     */
    public Iterable<ChatCommand> getBuiltInCommands() {
        return this.commands;
    }

    /**
     * Get built in command by name
     *
     * @param commandName name of the command to find
     * @return ChatCommand if found
     */
    public Optional<ChatCommand> getBuiltInCommand(String commandName) {
        final Locale locale = this.configManager.getLocale();
        return this.commands.getBuiltInCommand(commandName, locale);
    }

}
