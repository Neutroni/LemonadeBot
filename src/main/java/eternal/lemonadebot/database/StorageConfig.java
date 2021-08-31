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

import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class StorageConfig {

    private static final Logger LOGGER = LogManager.getLogger();

    //Cache settings
    private final int cacheConfigs;
    private final int cacheTemplates;
    private final boolean cacheCooldowns;
    private final boolean cachePermissions;

    //Message storage limit
    private final int messageLogLimit;

    //Limits
    private final int perGuildEventLimit;
    private final int perGuildTemplateLimit;
    private final int perGuildCooldownLimit;
    private final int perGuildPermissionLimit;
    private final int perGuildInventoryLimit;
    private final int perGuildKeywordLimit;
    private final int perGuildNotificationLimit;
    private final int perGuildReminderLimit;

    StorageConfig(final Properties config) {
        //Cache
        this.cacheConfigs = parseConfig(config, "cache-configs-limit", 10);
        this.cacheTemplates = parseConfig(config, "cache-templates-limit", 1024);
        this.cacheCooldowns = Boolean.parseBoolean(config.getProperty("cache-cooldowns"));
        this.cachePermissions = Boolean.parseBoolean(config.getProperty("cache-permissions"));

        //Amount of messages to keep in database
        this.messageLogLimit = parseConfig(config, "max-messages", 4096);

        //Per guild Limits
        this.perGuildEventLimit = parseConfig(config, "event-limit", 16386);
        this.perGuildTemplateLimit = parseConfig(config, "template-limit", 16386);
        this.perGuildCooldownLimit = parseConfig(config, "cooldowns-limit", 65536);
        this.perGuildPermissionLimit = parseConfig(config, "permissions-limit", 65536);
        this.perGuildInventoryLimit = parseConfig(config, "inventory-items-limit", 65536);
        this.perGuildKeywordLimit = parseConfig(config, "keywords-limit", 255);
        this.perGuildNotificationLimit = parseConfig(config, "notifications-limit", 16386);
        this.perGuildReminderLimit = parseConfig(config, "reminders-limit", 16386);
    }

    /**
     * Check if caching configs is enabled
     *
     * @return amount of configs to keep in cache
     */
    public int configCacheEnabled() {
        return this.cacheConfigs;
    }

    /**
     * Check if caching templates is enabled
     *
     * @return amount of templates to keep in cache
     */
    public int templateCacheEnabled() {
        return this.cacheTemplates;
    }

    /**
     * Check if caching command cooldowns is enabled
     *
     * @return true if cooldown cache is enabled
     */
    public boolean cooldownCacheEnabled() {
        return this.cacheCooldowns;
    }

    /**
     * Check if caching command permissions is enabled
     *
     * @return true if permission cache is enabled
     */
    public boolean permissionsCacheEnabled() {
        return this.cachePermissions;
    }

    /**
     * Get the max number of messages to keep in the database log
     *
     * @return number of messages to store
     */
    public int getMessageLogLimit() {
        return this.messageLogLimit;
    }

    /**
     * Get number of events each guild is allowed to have
     *
     * @return number of events allowed for each guild
     */
    public int getPerGuildEventLimit() {
        return perGuildEventLimit;
    }

    /**
     * Get the number of templates each guild is allowed to have
     *
     * @return number of templates allowed for each guild
     */
    public int getPerGuildTemplateLimit() {
        return perGuildTemplateLimit;
    }

    /**
     * Get the number of cooldowns each guild is allowed to set
     *
     * @return number of cooldowns allowed for each guild
     */
    public int getPerGuildCooldownLimit() {
        return perGuildCooldownLimit;
    }

    /**
     * Get the number of permissions each guilds is allowed to set
     *
     * @return number of permissions allowerd for each guild
     */
    public int getPerGuildPermissionLimit() {
        return perGuildPermissionLimit;
    }

    /**
     * Get the number of inventory items each guild is allowed to have
     *
     * @return number of inventory items allowed for each guild
     */
    public int getPerGuildInventoryLimit() {
        return perGuildInventoryLimit;
    }

    /**
     * Get the number of keywords each guild is allowed to define
     *
     * @return number of keywords each guild can define
     */
    public int getPerGuildKeywordLimit() {
        return perGuildKeywordLimit;
    }

    /**
     * Get the number of notificatons each guild is allowed to define
     *
     * @return number of notifications each guild can define
     */
    public int getPerGuildNotificationLimit() {
        return perGuildNotificationLimit;
    }

    /**
     * Get the number of reminders each guilds is allowed to define
     *
     * @return number of reminders each guild can define
     */
    public int getPerGuildReminderLimit() {
        return perGuildReminderLimit;
    }

    /**
     * Helper to parse ints from properties file and print errors to log
     *
     * @param properties Properties file to get values from
     * @param key Key to get value for
     * @param fallback Value to return if key does not map to value
     * @return Parsed value
     */
    private int parseConfig(final Properties properties, final String key, final int fallback) {
        final String propertyValue = properties.getProperty(key);
        if (propertyValue == null) {
            LOGGER.info("Configuration value for key '{}' not set in the config, defaulting to: {}", key, fallback);
            return fallback;
        }
        try {
            final int value = Integer.parseInt(propertyValue);
            LOGGER.info("Configuration value for key '{}' parsed from the config to: {}", key, value);
            return value;
        } catch (NumberFormatException ex) {
            LOGGER.warn("Configuration value for key '{}' contains malformed input: '{}', defaulting to: {}", key, propertyValue, fallback);
            return fallback;
        }
    }

}
