/*
 * The MIT License
 *
 * Copyright 2021 Neutroni.
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
import eternal.lemonadebot.database.CacheConfig;
import eternal.lemonadebot.events.EventCommand;
import eternal.lemonadebot.inventory.InventoryCommand;
import eternal.lemonadebot.keywords.KeywordCommand;
import eternal.lemonadebot.music.MusicCommand;
import eternal.lemonadebot.notifications.NotificationCommand;
import eternal.lemonadebot.permissions.PermissionCommand;
import eternal.lemonadebot.reminders.ReminderCommand;
import eternal.lemonadebot.rolemanagement.RoleCommand;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

/**
 *
 * @author Neutroni
 */
public class CommandList implements Iterable<ChatCommand> {

    /**
     * List of all the built in commands
     */
    private final List<ChatCommand> commands;
    private final Map<Locale, Map<String, ChatCommand>> commandMap = new ConcurrentHashMap<>();

    /**
     * Constructor
     * @param ds DataSource
     * @param cacheConf Configuration for caching behaviour
     */
    public CommandList(final DataSource ds, CacheConfig cacheConf) {
        this.commands = List.of(
                new HelpCommand(),
                new MusicCommand(),
                new EventCommand(),
                new TemplateCommand(),
                new RoleCommand(ds, cacheConf),
                new InventoryCommand(ds, cacheConf),
                //Admin commands
                new ConfigCommand(),
                new CooldownCommand(),
                new ReminderCommand(),
                new NotificationCommand(),
                new PermissionCommand(),
                new KeywordCommand()
        );
    }

    @Override
    public Iterator<ChatCommand> iterator() {
        return this.commands.iterator();
    }

    /**
     * Find built-in command by name
     *
     * @param commandName name of the command to find
     * @param locale Locale to get the command name in
     * @return Optional containing the command if found
     */
    public Optional<ChatCommand> getBuiltInCommand(final String commandName, Locale locale) {
        return Optional.ofNullable(getActiveMap(locale).get(commandName));
    }

    private Map<String, ChatCommand> getActiveMap(Locale locale) {
        //Load translated built in commands
        return commandMap.computeIfAbsent(locale, (Locale t) -> {
            final ResourceBundle resource = ResourceBundle.getBundle("Translation", locale);
            final Map<String, ChatCommand> localeMap = new HashMap<>();
            this.commands.forEach(
                    command -> localeMap.put(command.getCommand(resource), command)
            );
            return localeMap;
        });
    }
}
