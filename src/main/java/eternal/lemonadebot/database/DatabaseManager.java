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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class DatabaseManager implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final HikariDataSource dataSource;
    private final StorageConfig storageConfig;

    /**
     * Constructor
     *
     * @param config Properties that sot
     * @throws SQLException if loading database fails
     */
    public DatabaseManager(final Properties config) throws SQLException, NumberFormatException {
        this.storageConfig = new StorageConfig(config);

        //Connect to database
        final String databaseLocation = config.getProperty("database-location", "database.db");
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseLocation);
        hikariConfig.setConnectionInitSql("PRAGMA foreign_keys = ON;");
        this.dataSource = new HikariDataSource(hikariConfig);

        //Initialize database
        initialize();
    }

    @Override
    public void close() {
        this.dataSource.close();
    }

    /**
     * Get the database conntection pool
     *
     * @return DataSource
     */
    public DataSource getDataSource() {
        return this.dataSource;
    }

    /**
     * Get bot configuration
     *
     * @return cacheConfig
     */
    public StorageConfig getConfig() {
        return this.storageConfig;
    }

    /**
     * Creates the database for the bot
     *
     * @throws SQLException if database connection fails
     */
    private void initialize() throws SQLException {
        LOGGER.debug("Initializing database");
        final String GUILDCONF = "CREATE TABLE IF NOT EXISTS Guilds("
                + "id INTEGER PRIMARY KEY NOT NULL,"
                + "commandPrefix TEXT NOT NULL,"
                + "locale TEXT NOT NULL,"
                + "timeZone TEXT NOT NULL,"
                + "logChannel INTEGER NOT NULL,"
                + "greetingTemplate TEXT);";
        final String PERMISSIONS = "CREATE TABLE IF NOT EXISTS Permissions("
                + "guild INTEGER NOT NULL,"
                + "action TEXT NOT NULL,"
                + "requiredRank TEXT NOT NULL,"
                + "requiredRole INTEGER NOT NULL,"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,action));";
        final String MESSAGES = "CREATE TABLE IF NOT EXISTS Messages("
                + "id INTEGER PRIMARY KEY NOT NULL,"
                + "guild INTEGER NOT NULL,"
                + "author INTEGER NOT NULL,"
                + "content TEXT NOT NULL,"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE);";
        final String DROP_TRIGGER_CLEANUP = "DROP TRIGGER IF EXISTS MessageCleanup;";
        //Trigger called to remove old messages, yes there is string concatenation but messageLogLimit is numeric
        final String MESSAGE_CLEANUP = "CREATE TRIGGER MessageCleanup AFTER INSERT ON Messages BEGIN "
                + "DELETE FROM Messages WHERE id IN "
                + "(SELECT id FROM Messages WHERE guild = NEW.guild ORDER BY id DESC LIMIT -1 OFFSET "
                + this.storageConfig.getMessageLogLimit() + "); END;";
        final String COMMANDS = "CREATE TABLE IF NOT EXISTS Commands("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "template TEXT NOT NULL,"
                + "owner INTEGER NOT NULL,"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,name));";
        final String COOLDOWNS = "Create TABLE IF NOT EXISTS Cooldowns("
                + "guild INTEGER NOT NULL,"
                + "command TEXT NOT NULL,"
                + "duration INTEGER NOT NULL,"
                + "activationTime INTEGER NOT NULL,"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,command));";
        final String EVENTS = "CREATE TABLE IF NOT EXISTS Events("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "description TEXT,"
                + "owner INTEGER NOT NULL,"
                + "locked INTEGER NOT NULL,"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,name));";
        final String ROLES = "CREATE TABLE IF NOT EXISTS Roles("
                + "guild INTEGER NOT NULL,"
                + "role INTEGER NOT NULL,"
                + "description TEXT,"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,role));";
        final String EVENT_MEMBERS = "CREATE TABLE IF NOT EXISTS EventMembers("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "member INTEGER NOT NULL,"
                + "FOREIGN KEY (guild, name) REFERENCES Events(guild, name) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,name,member));";
        final String REMINDERS = "CREATE TABLE IF NOT EXISTS Reminders("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "message TEXT NOT NULL,"
                + "author INTEGER NOT NULL,"
                + "channel INTEGER NOT NULL,"
                + "time INTEGER NOT NULL,"
                + "dayOfWeek INTEGER NOT NULL,"
                + "dayOfMonth INTEGER NOT NULL,"
                + "monthOfYear INTEGER NOT NULL,"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,name));";
        final String NOTIFCATIONS = "CREATE TABLE IF NOT EXISTS Notifications("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "message TEXT NOT NULL,"
                + "author INTEGER NOT NULL,"
                + "channel INTEGER NOT NULL,"
                + "time INTEGER NOT NULL,"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,name));";
        final String KEYWORDS = "Create TABLE IF NOT EXISTS Keywords("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "pattern TEXT NOT NULL,"
                + "template TEXT NOT NULL,"
                + "owner INTEGER NOT NULL,"
                + "runasowner INTEGER NOT NULL,"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,name));";
        final String INVENTORY = "CREATE TABLE IF NOT EXISTS Inventory("
                + "guild INTEGER NOT NULL,"
                + "owner INTEGER NOT NULL,"
                + "item TEXT NOT NULL,"
                + "count INTEGER NOT NULL CHECK(count >= 0),"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,owner,item));";
        final String INVENTORY_CLEANUP = "CREATE TRIGGER IF NOT EXISTS InventoryCleanup "
                + "AFTER UPDATE ON Inventory BEGIN "
                + "DELETE FROM Inventory WHERE count = 0; END;";
        final String REACTIONS = "CREATE TABLE IF NOT EXISTS Reactions("
                + "messageId INTEGER NOT NULL,"
                + "guild INTEGER NOT NULL,"
                + "channel INTEGER NOT NULL,"
                + "reaction INTEGER NOT NULL,"
                + "commandAdd TEXT NOT NULL,"
                + "commandRemove TEXT NOT NULL,"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,messageId,reaction));";
        try (final Connection connection = this.dataSource.getConnection();
                final Statement st = connection.createStatement()) {
            st.addBatch(GUILDCONF);
            st.addBatch(PERMISSIONS);
            st.addBatch(MESSAGES);
            st.addBatch(DROP_TRIGGER_CLEANUP);
            st.addBatch(MESSAGE_CLEANUP);
            st.addBatch(COMMANDS);
            st.addBatch(COOLDOWNS);
            st.addBatch(EVENTS);
            st.addBatch(ROLES);
            st.addBatch(EVENT_MEMBERS);
            st.addBatch(REMINDERS);
            st.addBatch(NOTIFCATIONS);
            st.addBatch(KEYWORDS);
            st.addBatch(INVENTORY);
            st.addBatch(INVENTORY_CLEANUP);
            st.addBatch(REACTIONS);
            st.executeBatch();
        }
        LOGGER.debug("Database initialized");
    }
}
