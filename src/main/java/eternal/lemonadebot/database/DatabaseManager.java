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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class DatabaseManager implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final HikariDataSource dataSource;
    private final JDA jda;
    private final Map<Long, GuildDataStore> guildDataStores = new ConcurrentHashMap<>();
    private final int maxMessages;

    /**
     * Constructor
     *
     * @param config Properties that sot
     * @param jda JDA to use for Managers that need it
     * @throws SQLException if loading database fails
     */
    public DatabaseManager(final Properties config, final JDA jda) throws SQLException, NumberFormatException {
        final String numberString = config.getProperty("max-messages");
        if (numberString == null) {
            this.maxMessages = 4096;
            LOGGER.info("Max messages to store not set, defaulting to: {}", maxMessages);
        } else {
            this.maxMessages = Integer.parseInt(numberString);
            LOGGER.info("Set max messages to: {}", maxMessages);
        }
        this.jda = jda;

        //Connect to database
        final String databaseLocation = config.getProperty("database-location", "database.db");
        final HikariConfig hikariConfig = new HikariConfig(config);
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseLocation);
        this.dataSource = new HikariDataSource(hikariConfig);

        //Initialize database
        initialize();
        //load guilds from database
        loadGuilds();
    }

    @Override
    public void close() throws SQLException {
        this.dataSource.close();
        this.guildDataStores.forEach((Long t, GuildDataStore u) -> {
            u.close();
        });
    }

    /**
     * Get GuildDataStore for guild
     *
     * @param guild guild to get manager for
     * @return GuildDataStore
     */
    public GuildDataStore getGuildData(final Guild guild) {
        return this.guildDataStores.computeIfAbsent(guild.getIdLong(), (Long newGuildID) -> {
            return new GuildDataStore(this.dataSource, newGuildID, this.jda);
        });
    }

    /**
     * Creates the database for the bot
     *
     * @param dbLocation location for database
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
        final String MESSAGE_CLEANUP = "CREATE TRIGGER IF NOT EXISTS MessageCleanup AFTER INSERT ON Messages BEGIN "
                + "DELETE FROM Messages WHERE id IN "
                + "(SELECT id FROM Messages WHERE guild = NEW.guild ORDER BY id DESC LIMIT -1 OFFSET ?); END;";
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
        final String KEYWORDS = "Create TABLE IF NOT EXISTS Keywords("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "template TEXT NOT NULL,"
                + "owner INTEGER NOT NULL,"
                + "FOREIGN KEY (guild) REFERENCES Guilds(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,name));";
        try (final Connection connection = this.dataSource.getConnection();
                final Statement st = connection.createStatement()) {
            st.addBatch(GUILDCONF);
            st.addBatch(PERMISSIONS);
            st.addBatch(MESSAGES);
            st.addBatch(COMMANDS);
            st.addBatch(COOLDOWNS);
            st.addBatch(EVENTS);
            st.addBatch(ROLES);
            st.addBatch(EVENT_MEMBERS);
            st.addBatch(REMINDERS);
            st.addBatch(KEYWORDS);
            st.addBatch(DROP_TRIGGER_CLEANUP);
            st.executeBatch();
        }
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(MESSAGE_CLEANUP)) {
            ps.setInt(1, maxMessages);
            ps.executeUpdate();
        }
        LOGGER.debug("Database initialized");
    }

    private void loadGuilds() throws SQLException {
        LOGGER.debug("Loading guilds from database");
        final String query = "SELECT id FROM Guilds;";
        try (final Connection connection = this.dataSource.getConnection();
                final Statement st = connection.createStatement();
                final ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                final long guildID = rs.getLong("id");
                this.guildDataStores.put(guildID, new GuildDataStore(this.dataSource, guildID, this.jda));
            }
        }
        LOGGER.debug("Loaded guilds succesfully");
    }
}
