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
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.StorageManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
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
    private final TemplateManager templates;

    /**
     * Constructor
     *
     * @param db DataSource
     */
    public CommandProvider(final DatabaseManager db) {
        this.commands = new CommandList(db);
        this.templates = new TemplateManager(db);
    }

    /**
     * Get the action for command
     *
     * @param matcher Matcher to find command for
     * @param config Configuration to get command prefix from
     * @return CommandAction or Option.empty if command was not found
     */
    public Optional<ChatCommand> getAction(final CommandMatcher matcher, final ConfigManager config) {
        final Optional<String> optName = matcher.getCommand();
        if (optName.isEmpty()) {
            return Optional.empty();
        }
        final String commandName = optName.get();
        return getCommand(commandName, config);
    }

    /**
     * Get command by name
     *
     * @param name command name to search action for
     * @param config Configuration to get guild and locale from
     * @return Optional containing the action if found, empty if not found
     */
    public Optional<ChatCommand> getCommand(final String name, final ConfigManager config) {
        final Locale locale = config.getLocale();
        final long guildID = config.getGuildID();
        //Checks if we find built in command by that name
        return this.commands.getBuiltInCommand(name, locale).or(() -> {
            //Did not find built in command, return optional from templateManager
            try {
                return this.templates.getCommand(name, guildID);
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
    public CommandList getBuiltInCommands() {
        return this.commands;
    }

    /**
     * Get the template manager
     *
     * @return TemplateManager
     */
    public TemplateManager getTemplateManager() {
        return this.templates;
    }

    /**
     * Initialize storage for each command
     *
     * @param guilds Guilds to initialize commands for
     * @param storage SotrageManager to use for initialization
     */
    public void initialize(final List<Guild> guilds, final StorageManager storage) {
        this.commands.forEach((ChatCommand t) -> {
            t.initialize(guilds, storage);
        });
    }

}
