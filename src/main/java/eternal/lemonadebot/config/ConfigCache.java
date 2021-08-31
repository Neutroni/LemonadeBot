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
package eternal.lemonadebot.config;

import eternal.lemonadebot.cache.ItemCache;
import eternal.lemonadebot.database.DatabaseManager;
import java.util.Collections;
import java.util.Map;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Guild;

/**
 *
 * @author Neutroni
 */
public class ConfigCache {

    private final DataSource ds;
    private final Map<Long, ConfigManager> configs;

    /**
     * Constructor
     *
     * @param database Database to store configurations in
     */
    public ConfigCache(final DatabaseManager database) {
        final int cacheLimit = database.getConfig().configCacheEnabled();
        this.ds = database.getDataSource();
        this.configs = Collections.synchronizedMap(new ItemCache<>(cacheLimit));
    }

    /**
     * Get configManager for a guild
     *
     * @param guildID ID of the guild to get config for
     * @return ConfigManager
     */
    public ConfigManager getConfigManager(final long guildID) {
        return this.configs.computeIfAbsent(guildID, (t) -> {
            return new ConfigManager(ds, t);
        });
    }

    /**
     * Get configManager for a guild
     *
     * @param guild Guild to get config for
     * @return ConfigManager
     */
    public ConfigManager getConfigManager(final Guild guild) {
        return this.configs.computeIfAbsent(guild.getIdLong(), (Long guildID) -> {
            return new ConfigManager(ds, guildID);
        });
    }

}
