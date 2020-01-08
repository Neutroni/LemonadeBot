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

import eternal.lemonadebot.messages.CommandPermission;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class DatabaseManager implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DATABASE_VERSION = "1.0";

    /**
     * Creates the database for the bot
     *
     * @param dbLocation location for database
     * @param ownerID ID of the bot owner
     * @throws SQLException if database connection fails
     */
    public static void initialize(String dbLocation, String ownerID) throws SQLException {
        final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbLocation);
        final String CONFIG = "CREATE TABLE Options("
                + "name TEXT PRIMARY KEY NOT NULL,"
                + "value TEXT NOT NULL);";
        final String CHANNELS = "CREATE TABLE Channels("
                + "id INTEGER PRIMARY KEY NOT NULL,"
                + "guild INTEGER NOT NULL);";
        final String GUILDCONF = "CREATE TABLE Guilds("
                + "id INTEGER PRIMARY KEY NOT NULL,"
                + "commandPrefix TEXT NOT NULL"
                + "commandEditPermission TEXT NOT NULL,"
                + "commandRunPermission TEXT NOT NULL,"
                + "eventEditPermission TEXT NOT NULL,"
                + "musicPlayPermission TEXT NOT NULL"
                + "greetingTemplate TEXT);";
        final String COMMANDS = "CREATE TABLE Commands("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "template TEXT NOT NULL,"
                + "owner INTEGER NOT NULL"
                + "PRIMARY KEY (guild,name));";
        final String EVENTS = "CREATE TABLE Events("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "description TEXT NOT NULL,"
                + "owner INTEGER NOT NULL,"
                + "PRIMARY KEY (guild,name));";
        final String EVENT_MEMBERS = "CREATE TABLE EventMembers("
                + "FOREIGN KEY (guild, name) REFERENCES Events(guild, name) ON DELETE CASCADE,"
                + "member INTEGER NOT NULL,"
                + "PRIMARY KEY (guild,name,member));";
        final String REMAINDERS = "CREATE TABLE Remainders("
                + "FOREIGN KEY (guild,name) REFERENCES Events(guild, name) ON DELETE CASCADE,"
                + "day TEXT NOT NULL,"
                + "time TEXT NOT NULL,"
                + "mention TEXT NOT NULL,"
                + "channel INTEGER NOT NULL,"
                + "PRIMARY KEY (guild,name,day,time));";
        final String INSERT = "INSERT INTO Options(name,value) VALUES(?,?);";
        try (Statement st = connection.createStatement()) {
            st.addBatch(CONFIG);
            st.addBatch(CHANNELS);
            st.addBatch(GUILDCONF);
            st.addBatch(COMMANDS);
            st.addBatch(EVENTS);
            st.addBatch(EVENT_MEMBERS);
            st.addBatch(REMAINDERS);
            st.executeBatch();
        }
        try (PreparedStatement ps = connection.prepareStatement(INSERT)) {
            ps.setString(1, ConfigKey.DATABASE_VERSION.name());
            ps.setString(2, DATABASE_VERSION);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(INSERT)) {
            ps.setString(1, ConfigKey.OWNER_ID.name());
            ps.setString(2, ownerID);
            ps.executeUpdate();
        }
    }

    private final Connection conn;
    private final JDA jda;

    private final String ownerID;
    private final Map<Long, GuildDataStore> guildDataStores = new HashMap<>();

    /**
     * Constructor
     *
     * @param filename location of database
     * @param jda JDA to pass to managers that need it
     * @throws SQLException if loading database fails
     * @throws InterruptedException If jda loading was interrupted
     */
    public DatabaseManager(String filename, JDA jda) throws SQLException, InterruptedException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
        this.jda = jda;

        //Load ownerID
        final Optional<String> optOwner = loadSetting(ConfigKey.OWNER_ID.name());
        if (optOwner.isEmpty()) {
            throw new SQLException("Missing owner id");
        }
        this.ownerID = optOwner.get();

        //Load database version
        final Optional<String> optVersion = loadSetting(ConfigKey.DATABASE_VERSION.name());
        if (optVersion.isEmpty()) {
            throw new SQLException("Missing database version");
        }
        final String databaseVersion = optVersion.get();

        //Check if database version is correct
        if (!DATABASE_VERSION.equals(databaseVersion)) {
            throw new SQLException("Database version mismatch");
        }

        //Load list of known guilds
        loadGuildList();
    }

    @Override
    public void close() throws SQLException {
        this.conn.close();
    }

    /**
     * Shortcut to get configmanager for given guild
     *
     * @param guild guild to get configmanager for
     * @return ConfigManager
     */
    public ConfigManager getConfig(Guild guild) {
        return getGuildData(guild).getConfigManager();
    }

    /**
     * Shortcut to get customcommandmanager for given guild
     *
     * @param guild guild to get customcommandmanager for
     * @return CustomCommandManager
     */
    public CustomCommandManager getCommands(Guild guild) {
        return getGuildData(guild).getCustomCommands();
    }

    /**
     * Shortcut to get channelmanager for guild
     *
     * @param guild guild to get channelmanager for
     * @return ChannelManager
     */
    public ChannelManager getChannels(Guild guild) {
        return getGuildData(guild).getChannelManager();
    }

    /**
     * Shortcut to get eventmanager for guild
     *
     * @param guild guild to get evens for
     * @return EventManager
     */
    public EventManager getEvents(Guild guild) {
        return getGuildData(guild).getEventManager();
    }

    /**
     * Shortcut to get remaindermanager for guild
     *
     * @param guild guild to get remainders for
     * @return RemainderManager
     */
    public RemainderManager getRemainders(Guild guild) {
        return getGuildData(guild).getRemainderManager();
    }

    /**
     * Check if user is the owner of this bot
     *
     * @param user User to check
     * @return true if the owner
     */
    public boolean isOwner(Member user) {
        return this.ownerID.equals(user.getId());
    }

    /**
     * Get GuildDataStore for guild
     *
     * @param guild guild to get manager for
     * @return GuildDataStore
     */
    public GuildDataStore getGuildData(Guild guild) {
        return this.guildDataStores.computeIfAbsent(guild.getIdLong(), (Long newGuildID) -> {
            try {
                if (addGuild(guild)) {
                    LOGGER.info("New guild added succesfully");
                } else {
                    LOGGER.warn("Tried to add guild to database but was already stored");
                }
            } catch (SQLException e) {
                LOGGER.error("Error adding missing guild", e);
            }
            //Return guildConfigManager to the guild
            return new GuildDataStore(this.conn, this.jda, newGuildID);
        });
    }

    /**
     * Add guild to database
     *
     * @param guild Guild to add
     * @return true if add was succesfull
     * @throws SQLException if database connection failed
     */
    private boolean addGuild(Guild guild) throws SQLException {
        final String query = "INSERT OR IGNORE INTO Guilds(id,commandPrefix,commandEditPermission,commandRunPermission,eventEditPermission,musicPlayPermission,greetingTemplate) VALUES (?,?,?,?,?,?);";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, guild.getIdLong());
            ps.setString(2, "lemonbot#");
            ps.setString(3, CommandPermission.ADMIN.name());
            ps.setString(4, CommandPermission.MEMBER.name());
            ps.setString(5, CommandPermission.ADMIN.name());
            ps.setString(6, CommandPermission.MEMBER.name());
            ps.setString(7, "Welcome to our discord {displayName}");
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Loads a setting from database
     *
     * @param key Key to retrieve setting for
     * @return Value for setting
     * @throws SQLException If database connection failed
     */
    private Optional<String> loadSetting(String key) throws SQLException {
        final String query = "SELECT value FROM Options WHERE name = ?;";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("value"));
                }
            }
        }
        return Optional.empty();
    }

    private void loadGuildList() throws SQLException {
        final String query = "SELECT id FROM Guilds;";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                final long guildID = rs.getLong("id");
                this.guildDataStores.put(guildID, new GuildDataStore(this.conn, this.jda, guildID));
            }
        }
    }

}
