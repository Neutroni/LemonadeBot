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
package eternal.lemonadebot.database;

import eternal.lemonadebot.commands.CommandList;
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.config.ConfigCache;
import eternal.lemonadebot.cooldowns.CooldownManagerCache;
import eternal.lemonadebot.cooldowns.CooldownManager;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.permissions.PermissionManagerCache;
import java.io.Closeable;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class StorageManager implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DatabaseManager database;
    private final CommandProvider commandProvider;
    private final ConfigCache configCache;
    private final PermissionManager permissionManager;
    private final CooldownManager cooldownManager;

    /**
     * Constructor
     *
     * @param properties Properties to get configuration from
     * @throws SQLException If database initialization failes
     */
    public StorageManager(final Properties properties) throws SQLException {
        this.database = new DatabaseManager(properties);
        this.commandProvider = new CommandProvider(this.database);
        this.configCache = new ConfigCache(this.database);
        final CommandList commandList = this.commandProvider.getBuiltInCommands();
        final StorageConfig storageConfig = this.database.getConfig();
        if(storageConfig.permissionsCacheEnabled()){
            this.permissionManager = new PermissionManagerCache(this.database, this.configCache, commandList);
        } else {
            this.permissionManager = new PermissionManager(this.database, this.configCache, commandList);
        }
        if(storageConfig.cooldownCacheEnabled()) {
            this.cooldownManager = new CooldownManagerCache(this.database);
        } else {
            this.cooldownManager = new CooldownManager(this.database);
        }
    }

    /**
     * Get CommandProvider
     *
     * @return CommandProvider
     */
    public CommandProvider getCommandProvider() {
        return this.commandProvider;
    }

    /**
     * Get configCache
     *
     * @return ConfigCache
     */
    public ConfigCache getConfigCache() {
        return this.configCache;
    }

    /**
     * Get permissionManager
     *
     * @return PermissionManager
     */
    public PermissionManager getPermissionManager() {
        return this.permissionManager;
    }

    /**
     * Shortcut to get datasource from database
     *
     * @return DataSource
     */
    public DataSource getDataSource() {
        return this.database.getDataSource();
    }

    /**
     * get cooldownManager
     *
     * @return CooldownManager
     */
    public CooldownManager getCooldownManager() {
        return this.cooldownManager;
    }

    /**
     * Initialize data and commands
     *
     * @param guilds Guilds to initialize data for
     */
    public void initialize(final List<Guild> guilds) {
        LOGGER.debug("Initializing RuntimeStorage");
        commandProvider.initialize(guilds, this);
        LOGGER.debug("RuntimeStorage intialized succesfully");
    }

    @Override
    public void close() {
        this.database.close();
    }

}
