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

import eternal.lemonadebot.config.ConfigCommand;
import eternal.lemonadebot.cooldowns.CooldownCommand;
import eternal.lemonadebot.customcommands.TemplateCommand;
import eternal.lemonadebot.customcommands.TemplateManager;
import eternal.lemonadebot.events.EventCommand;
import eternal.lemonadebot.inventory.InventoryCommand;
import eternal.lemonadebot.keywords.KeywordCommand;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.music.MusicCommand;
import eternal.lemonadebot.notifications.NotificationCommand;
import eternal.lemonadebot.permissions.PermissionCommand;
import eternal.lemonadebot.reminders.ReminderCommand;
import eternal.lemonadebot.rolemanagement.RoleCommand;
import eternal.lemonadebot.translation.LocaleUpdateListener;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class used to find command by name
 *
 * @author Neutroni
 */
public class CommandProvider implements LocaleUpdateListener {

    private static final Logger LOGGER = LogManager.getLogger();

    private final List<ChatCommand> builtInCommands;
    private final Map<String, ChatCommand> commandMap = new ConcurrentHashMap<>();
    private final TemplateManager templateManager;

    /**
     * Constructor
     *
     * @param locale Locale in which to load command names in
     * @param templates TemplateManager to get templates from
     */
    public CommandProvider(final ResourceBundle locale, final TemplateManager templates) {
        this.builtInCommands = List.of(
                new HelpCommand(),
                new MusicCommand(),
                new EventCommand(),
                new TemplateCommand(),
                new RoleCommand(),
                new InventoryCommand(),
                //Admin commands
                new ConfigCommand(),
                new CooldownCommand(),
                new ReminderCommand(),
                new NotificationCommand(),
                new PermissionCommand(),
                new KeywordCommand()
        );
        this.templateManager = templates;
        //Load translated built in commands
        builtInCommands.forEach(command -> this.commandMap.put(command.getCommand(locale), command));
    }

    /**
     * Check if name is reserved for a command
     *
     * @param name Name to check
     * @return true if name is reserved
     */
    public boolean isReservedName(String name) {
        return this.commandMap.containsKey(name);
    }

    /**
     * Get the list of built in commands
     *
     * @return List of built in commands
     */
    public List<ChatCommand> getCommands() {
        return this.builtInCommands;
    }

    /**
     * Find built-in command by name
     *
     * @param commandName name of the command to find
     * @return Optional containing the command if found
     */
    private Optional<ChatCommand> getBuiltInCommand(final String commandName) {
        return Optional.ofNullable(this.commandMap.get(commandName));
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
        //Checks if we find built in command by that name
        return getBuiltInCommand(name).or(() -> {
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
     * Update the locale of the commands
     *
     * @param newLocale Locale to switch to
     */
    @Override
    public void updateLocale(final Locale newLocale) {
        final ResourceBundle rb = ResourceBundle.getBundle("Translation", newLocale);
        this.commandMap.clear();
        builtInCommands.forEach(command -> {
            this.commandMap.put(command.getCommand(rb), command);
        });
    }

}
