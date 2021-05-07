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

import eternal.lemonadebot.commands.CommandList;
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.cooldowns.CooldownCache;
import eternal.lemonadebot.cooldowns.CooldownManager;
import eternal.lemonadebot.customcommands.TemplateCache;
import eternal.lemonadebot.customcommands.TemplateManager;
import eternal.lemonadebot.events.EventCache;
import eternal.lemonadebot.events.EventManager;
import eternal.lemonadebot.keywords.KeywordManager;
import eternal.lemonadebot.notifications.NotificationManager;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.permissions.PermissionManagerCache;
import java.io.Closeable;
import javax.sql.DataSource;
import net.dv8tion.jda.api.JDA;

/**
 * Class storing info for guilds
 *
 * @author Neutroni
 */
public class GuildDataStore implements Closeable {

    private final long guildID;
    private final DatabaseManager database;
    private final RuntimeStorage storage;
    private final ConfigManager config;
    private final PermissionManager permissions;
    private final TemplateManager commands;
    private final EventManager events;
    private final NotificationManager notifications;
    private final CooldownManager cooldowns;
    private final CommandProvider commandProvider;
    private final KeywordManager keywordManager;

    /**
     * Constructor
     *
     * @param dataSource database connection to use
     * @param guildID Guild this config is for
     * @param jda JDA to use for reminders
     * @param cacheConf Configuration for what data to cache
     */
    GuildDataStore(final DatabaseManager db, final long guildID, final JDA jda, final RuntimeStorage storage) {
        this.guildID = guildID;
        this.database = db;
        this.storage = storage;
        final DataSource dataSource = db.getDataSource();
        this.config = new ConfigManager(dataSource, guildID);
        final CommandList commandList = storage.getCommands();
        final CacheConfig cacheConf = db.getCacheConfig();
        if (cacheConf.permissionsCacheEnabled()) {
            this.permissions = new PermissionManagerCache(dataSource, guildID, this.config, commandList);
        } else {
            this.permissions = new PermissionManager(dataSource, guildID, this.config, commandList);
        }
        this.notifications = new NotificationManager(dataSource, jda, this);
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
        this.commandProvider = new CommandProvider(commandList, this.config, this.commands);
        if (cacheConf.eventCacheEnabled()) {
            this.events = new EventCache(dataSource, guildID);
        } else {
            this.events = new EventManager(dataSource, guildID);
        }
        
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
     * Get the database manager
     *
     * @return DatabaseManager
     */
    public DatabaseManager getDataBaseManager() {
        return this.database;
    }
    
    public RuntimeStorage getRuntimeStorage() {
        return this.storage;
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
     * Get the notificationManager for this datastore
     *
     * @return NotificationManager
     */
    public NotificationManager getNotificationManager() {
        return this.notifications;
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
     * Get the localized CommandProvider for this guild
     *
     * @return CommandProvider
     */
    public CommandProvider getCommandProvider() {
        return this.commandProvider;
    }

    /**
     * Get the keywordManager for guild
     *
     * @return KeywordManager
     */
    public KeywordManager getKeywordManager() {
        return this.keywordManager;
    }

    @Override
    public void close() {
        this.notifications.close();
    }

}
