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
import java.util.HashMap;
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
    /**
     * List of all the built in commands
     */
    public static final List<ChatCommand> COMMANDS = List.of(
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
    private static final Map<Locale, Map<String, ChatCommand>> commandMap = new ConcurrentHashMap<>();
    private volatile Map<String,ChatCommand> currentMap;
    private final TemplateManager templateManager;

    /**
     * Constructor
     *
     * @param resource Locale in which to load command names in
     * @param templates TemplateManager to get templates from
     */
    public CommandProvider(final ResourceBundle resource, final TemplateManager templates) {
        this.templateManager = templates;
        //Load translated built in commands
        this.currentMap = commandMap.computeIfAbsent(resource.getLocale(), (Locale t) -> {
            final Map<String, ChatCommand> localeMap = new HashMap<>();
            COMMANDS.forEach(
                    command -> localeMap.put(command.getCommand(resource), command)
            );
            return localeMap;
        });
    }

    /**
     * Find built-in command by name
     *
     * @param commandName name of the command to find
     * @return Optional containing the command if found
     */
    public Optional<ChatCommand> getBuiltInCommand(final String commandName) {
        return Optional.ofNullable(this.currentMap.get(commandName));
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
        this.currentMap = commandMap.computeIfAbsent(rb.getLocale(), (Locale t) -> {
            final Map<String, ChatCommand> localeMap = new HashMap<>();
            COMMANDS.forEach(
                    command -> localeMap.put(command.getCommand(rb), command)
            );
            return localeMap;
        });
    }

}
