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
package eternal.lemonadebot.database;

import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.translation.TranslationCache;
import java.sql.Connection;
import java.util.Locale;
import net.dv8tion.jda.api.JDA;

/**
 * Class storing info for guilds
 *
 * @author Neutroni
 */
public class GuildDataStore implements AutoCloseable {

    private final long guildID;
    private final ConfigManager config;
    private final PermissionManager permissions;
    private final TemplateManager commands;
    private final EventManager events;
    private final RoleManager roleManager;
    private final ReminderManager reminders;
    private final CooldownManager cooldowns;
    private final MessageManager messages;
    private final CommandProvider commandProvider;
    private final TranslationCache translationCache;
    private final KeywordManager keywordManager;

    /**
     * Constructor
     *
     * @param connection database connection to use
     * @param guild Guild this config is for
     * @param jda JDA to use for reminders
     */
    GuildDataStore(final Connection connection, final Long guildID, JDA jda) {
        this.guildID = guildID;
        this.config = new ConfigManager(connection, guildID);
        final Locale locale = this.config.getLocale();
        this.permissions = new PermissionManager(connection, guildID, locale);
        this.events = new EventManager(connection, guildID);
        this.roleManager = new RoleManager(connection, guildID);
        this.cooldowns = new CooldownManager(connection, guildID);
        this.commands = new TemplateManager(connection, this.cooldowns, guildID);
        this.reminders = new ReminderManager(connection, jda, this, guildID);
        this.messages = new MessageManager(connection);
        this.commandProvider = new CommandProvider(locale, this.commands);
        this.translationCache = new TranslationCache(locale);
        this.keywordManager = new KeywordManager(connection, guildID, this.cooldowns);

        //Add locale update listeners
        this.config.registerLocaleUpdateListener(this.permissions);
        this.config.registerLocaleUpdateListener(this.commandProvider);
        this.config.registerLocaleUpdateListener(this.translationCache);
    }

    /**
     * Get the ID of the guild this datastore is for
     *
     * @return guildID
     */
    public long getGuildID() {
        return this.guildID;
    }

    /**
     * Get the configmanager for this datastore
     *
     * @return ConfigManager
     */
    public ConfigManager getConfigManager() {
        return this.config;
    }

    /**
     * Get the permissionsmanager for this datastore
     *
     * @return PermissionsManager
     */
    public PermissionManager getPermissionManager() {
        return this.permissions;
    }

    /**
     * Get tge customcommandmanager for this datastore
     *
     * @return CustomCommandManager
     */
    public TemplateManager getCustomCommands() {
        return this.commands;
    }

    /**
     * Get the eventmanager for this datastore
     *
     * @return EventManager
     */
    public EventManager getEventManager() {
        return this.events;
    }

    /**
     * Get the rolemanager for this guild
     *
     * @return RoleManager
     */
    public RoleManager getRoleManager() {
        return this.roleManager;
    }

    /**
     * Get the remindermanager for this datastore
     *
     * @return ReminderManager
     */
    public ReminderManager getReminderManager() {
        return this.reminders;
    }

    /**
     * Get the cooldownmanager for this datastore
     *
     * @return CooldownManager
     */
    public CooldownManager getCooldownManager() {
        return this.cooldowns;
    }

    /**
     * Get the messagemanager for this guild
     *
     * @return MessageManager
     */
    public MessageManager getMessageManager() {
        return this.messages;
    }

    /**
     * Get the localized CommandProvider for this guild
     *
     * @return CommandProvider
     */
    public CommandProvider getCommandProvider() {
        return this.commandProvider;
    }

    /**
     * Get the translationcache
     *
     * @return TranslationCache
     */
    public TranslationCache getTranslationCache() {
        return this.translationCache;
    }

    /**
     * Get the keywordmanager for guild
     *
     * @return KeywordManager
     */
    public KeywordManager getKeywordManager() {
        return this.keywordManager;
    }

    @Override
    public void close() {
        this.reminders.close();
    }

}
