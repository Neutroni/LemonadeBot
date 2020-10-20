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
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.cooldowns.CooldownCache;
import eternal.lemonadebot.cooldowns.CooldownManager;
import eternal.lemonadebot.customcommands.TemplateCache;
import eternal.lemonadebot.customcommands.TemplateManager;
import eternal.lemonadebot.events.EventCache;
import eternal.lemonadebot.events.EventManager;
import eternal.lemonadebot.inventory.InventoryCache;
import eternal.lemonadebot.inventory.InventoryManager;
import eternal.lemonadebot.keywords.KeywordManager;
import eternal.lemonadebot.messagelogs.MessageManager;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.permissions.PermissionManagerCache;
import eternal.lemonadebot.reminders.ReminderManager;
import eternal.lemonadebot.rolemanagement.RoleManager;
import eternal.lemonadebot.rolemanagement.RoleManagerCache;
import eternal.lemonadebot.translation.TranslationCache;
import java.io.Closeable;
import java.util.Locale;
import javax.sql.DataSource;
import net.dv8tion.jda.api.JDA;

/**
 * Class storing info for guilds
 *
 * @author Neutroni
 */
public class GuildDataStore implements Closeable {

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
    private final InventoryManager inventoryManager;

    /**
     * Constructor
     *
     * @param dataSource database connection to use
     * @param guildID Guild this config is for
     * @param jda JDA to use for reminders
     * @param cacheConf Configuration for what data to cache
     */
    GuildDataStore(final DataSource dataSource, final long guildID, final JDA jda, final CacheConfig cacheConf) {
        this.guildID = guildID;
        this.config = new ConfigManager(dataSource, guildID);
        final Locale locale = this.config.getLocale();
        if (cacheConf.permissionsCacheEnabled()) {
            this.permissions = new PermissionManagerCache(dataSource, guildID, locale);
        } else {
            this.permissions = new PermissionManager(dataSource, guildID, locale);
        }
        this.reminders = new ReminderManager(dataSource, jda, this);
        this.messages = new MessageManager(dataSource);
        this.translationCache = new TranslationCache(locale);
        this.keywordManager = new KeywordManager(dataSource, guildID);
        if (cacheConf.cooldownCacheEnabled()) {
            this.cooldowns = new CooldownCache(dataSource, guildID);
        } else {
            this.cooldowns = new CooldownManager(dataSource, guildID);
        }
        if (cacheConf.templateCacheEnabled()) {
            this.commands = new TemplateCache(dataSource, guildID);
        } else {
            this.commands = new TemplateManager(dataSource, guildID);
        }
        this.commandProvider = new CommandProvider(locale, this.commands);
        if (cacheConf.eventCacheEnabled()) {
            this.events = new EventCache(dataSource, guildID);
        } else {
            this.events = new EventManager(dataSource, guildID);
        }
        if (cacheConf.allowedRolesCacheEnabled()) {
            this.roleManager = new RoleManagerCache(dataSource, guildID);
        } else {
            this.roleManager = new RoleManager(dataSource, guildID);
        }
        if (cacheConf.inventoryCacheEnabled()) {
            this.inventoryManager = new InventoryCache(dataSource, guildID);
        } else {
            this.inventoryManager = new InventoryManager(dataSource, guildID);
        }

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
     * Get the configManager for this datastore
     *
     * @return ConfigManager
     */
    public ConfigManager getConfigManager() {
        return this.config;
    }

    /**
     * Get the permissionsManager for this datastore
     *
     * @return PermissionsManager
     */
    public PermissionManager getPermissionManager() {
        return this.permissions;
    }

    /**
     * Get te customCommandManager for this datatore
     *
     * @return CustomCommandManager
     */
    public TemplateManager getCustomCommands() {
        return this.commands;
    }

    /**
     * Get the eventManager for this datastore
     *
     * @return EventManager
     */
    public EventManager getEventManager() {
        return this.events;
    }

    /**
     * Get the roleManager for this guild
     *
     * @return RoleManager
     */
    public RoleManager getRoleManager() {
        return this.roleManager;
    }

    /**
     * Get the reminderManager for this datastore
     *
     * @return ReminderManager
     */
    public ReminderManager getReminderManager() {
        return this.reminders;
    }

    /**
     * Get the cooldownManager for this datastore
     *
     * @return CooldownManager
     */
    public CooldownManager getCooldownManager() {
        return this.cooldowns;
    }

    /**
     * Get the messageManager for this guild
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
     * Get the translationCache
     *
     * @return TranslationCache
     */
    public TranslationCache getTranslationCache() {
        return this.translationCache;
    }

    /**
     * Get the keywordManager for guild
     *
     * @return KeywordManager
     */
    public KeywordManager getKeywordManager() {
        return this.keywordManager;
    }

    /**
     * Get the inventoryManager for guild
     *
     * @return inventoryManager
     */
    public InventoryManager getInventoryManager() {
        return this.inventoryManager;
    }

    @Override
    public void close() {
        this.reminders.close();
    }

}
