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
import eternal.lemonadebot.database.TemplateManager;
import eternal.lemonadebot.music.MusicCommand;
import eternal.lemonadebot.translation.LocaleUpdateListener;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class used to find command by name
 *
 * @author Neutroni
 */
public class CommandProvider implements LocaleUpdateListener {

    /**
     * List of all the built in commands
     */
    public static final List<ChatCommand> COMMANDS = List.of(
            new HelpCommand(),
            new MusicCommand(),
            new EventCommand(),
            new TemplateCommand(),
            new RoleCommand(),
            //Admin commands
            new ConfigCommand(),
            new CooldownCommand(),
            new ReminderCommand(),
            new PermissionCommand(),
            new KeywordCommand()
    );

    private final Map<String, ChatCommand> commandMap = new ConcurrentHashMap<>();
    private final TemplateManager templateManager;

    /**
     * Constructor
     *
     * @param locale Locale in which to load command names in
     * @param templates TemplateManager to get templates from
     */
    public CommandProvider(Locale locale, TemplateManager templates) {
        this.templateManager = templates;
        //Load translated built in commands
        COMMANDS.forEach(command -> {
            this.commandMap.put(command.getCommand(locale), command);
        });
    }

    /**
     * Find built-in command by name
     *
     * @param commandName name of the command to find
     * @return Optional containing the command if found
     */
    public Optional<ChatCommand> getBuiltInCommand(String commandName) {
        return Optional.ofNullable(this.commandMap.get(commandName));
    }

    /**
     * Get the action for command
     *
     * @param cmdMatcher Matcher to find command for
     * @return CommandAction or Option.empty if command was not found
     */
    public Optional<ChatCommand> getAction(CommandMatcher cmdMatcher) {
        return cmdMatcher.getCommand().flatMap((String commandName) -> {
            return getCommand(commandName);
        });
    }

    /**
     * Get command by name
     *
     * @param name command name to search action for
     * @return Optional containing the action if found, empty if not found
     */
    public Optional<ChatCommand> getCommand(String name) {
        //Checks if we find built in command by that name
        return getBuiltInCommand(name).or(() -> {
            //Did not find built in command, return optional from templateManager
            return this.templateManager.getCommand(name);
        });
    }

    /**
     * Update the locale of the commands
     *
     * @param newLocale Locale to switch to
     */
    @Override
    public void updateLocale(Locale newLocale) {
        this.commandMap.clear();
        COMMANDS.forEach(command -> {
            this.commandMap.put(command.getCommand(newLocale), command);
        });
    }

}
