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

import eternal.lemonadebot.permissions.CommandPermission;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
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

    private final Connection conn;
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

        //Initialize database
        initialize();

        //Load list of known guilds
        loadGuildList(jda);
    }

    @Override
    public void close() throws SQLException {
        this.conn.close();
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
                + "commandEditPermission TEXT NOT NULL,"
                + "commandRunPermission TEXT NOT NULL,"
                + "eventEditPermission TEXT NOT NULL,"
                + "remainderPermission TEXT NOT NULL,"
                + "musicPlayPermission TEXT NOT NULL,"
                + "greetingTemplate TEXT);";
        final String COMMANDS = "CREATE TABLE IF NOT EXISTS Commands("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "template TEXT NOT NULL,"
                + "owner INTEGER NOT NULL,"
                + "PRIMARY KEY (guild,name));";
        final String EVENTS = "CREATE TABLE IF NOT EXISTS Events("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "description TEXT NOT NULL,"
                + "owner INTEGER NOT NULL,"
                + "PRIMARY KEY (guild,name));";
        final String EVENT_MEMBERS = "CREATE TABLE IF NOT EXISTS EventMembers("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "member INTEGER NOT NULL,"
                + "FOREIGN KEY (guild, name) REFERENCES Events(guild, name) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,name,member));";
        final String REMAINDERS = "CREATE TABLE IF NOT EXISTS Remainders("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "day TEXT NOT NULL,"
                + "time TEXT NOT NULL,"
                + "mention TEXT NOT NULL,"
                + "channel INTEGER NOT NULL,"
                + "FOREIGN KEY (guild,name) REFERENCES Events(guild, name) ON DELETE CASCADE,"
                + "PRIMARY KEY (guild,name,day,time));";
        try (final Statement st = this.conn.createStatement()) {
            st.addBatch(GUILDCONF);
            st.addBatch(COMMANDS);
            st.addBatch(EVENTS);
            st.addBatch(EVENT_MEMBERS);
            st.addBatch(REMAINDERS);
            st.executeBatch();
        }
        LOGGER.debug("Database initialized");
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
            return new GuildDataStore(this.conn, guild.getJDA(), newGuildID);
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
        LOGGER.debug("Adding guild to database: " + guild.getId());
        final String query = "INSERT OR IGNORE INTO Guilds("
                + "id,commandPrefix,commandEditPermission,commandRunPermission,eventEditPermission,"
                + "remainderPermission,musicPlayPermission,greetingTemplate) VALUES (?,?,?,?,?,?);";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, guild.getIdLong());
            ps.setString(2, "lemonbot#");
            ps.setString(3, CommandPermission.ADMIN.name());
            ps.setString(4, CommandPermission.MEMBER.name());
            ps.setString(5, CommandPermission.ADMIN.name());
            ps.setString(6, CommandPermission.ADMIN.name());
            ps.setString(7, CommandPermission.MEMBER.name());
            ps.setString(8, "Welcome to our discord {displayName}");
            return ps.executeUpdate() > 0;
        }
    }

    private void loadGuildList(JDA jda) throws SQLException {
        LOGGER.debug("Loading guilds from database");
        final String query = "SELECT id FROM Guilds;";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                final long guildID = rs.getLong("id");
                this.guildDataStores.put(guildID, new GuildDataStore(this.conn, jda, guildID));
            }
        }
        LOGGER.debug("Loaded guilds succesfully");
    }

}
