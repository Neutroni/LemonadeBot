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

/**
 *
 * @author Neutroni
 */
class CacheConfig {

    private final boolean cacheInventory;
    private final boolean cacheEvents;
    private final boolean cacheTemplates;
    private final boolean cacheAllowedRoles;
    private final boolean cacheCooldowns;
    private final boolean cachePermissions;

    CacheConfig(final Properties config) {
        this.cacheInventory = Boolean.parseBoolean(config.getProperty("cache-inventory"));
        this.cacheEvents = Boolean.parseBoolean(config.getProperty("cache-events"));
        this.cacheTemplates = Boolean.parseBoolean(config.getProperty("cache-templates"));
        this.cacheAllowedRoles = Boolean.parseBoolean(config.getProperty("cache-allowed-roles"));
        this.cacheCooldowns = Boolean.parseBoolean(config.getProperty("cache-cooldowns"));
        this.cachePermissions = Boolean.parseBoolean(config.getProperty("cache-permissions"));
    }

    /**
     * Check if caching users inventories is enabled
     *
     * @return true if inventory cache is enabled
     */
    boolean inventoryCacheEnabled() {
        return this.cacheInventory;
    }

    /**
     * Check if caching events is enabled
     *
     * @return true if event cache is enabled
     */
    boolean eventCacheEnabled() {
        return this.cacheEvents;
    }

    /**
     * Check if caching the list of roles bot can assign is enabled
     *
     * @return true if allowed roles cache is enabled
     */
    boolean allowedRolesCacheEnabled() {
        return this.cacheAllowedRoles;
    }

    /**
     * Check if caching command cooldowns is enabled
     *
     * @return true if cooldown cache is enabled
     */
    boolean cooldownCacheEnabled() {
        return this.cacheCooldowns;
    }

    /**
     * Check if caching command permissions is enabled
     *
     * @return true if permission cache is enabled
     */
    boolean permissionsCacheEnabled() {
        return this.cachePermissions;
    }

    /**
     * Check if caching templates is enabled
     *
     * @return true if template cache is enabled
     */
    boolean templateCacheEnabled() {
        return this.cacheTemplates;
    }

}
