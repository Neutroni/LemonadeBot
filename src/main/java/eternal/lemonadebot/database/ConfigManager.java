/*
 * The MIT License
 *
 * Copyright 2019 Neutroni.
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages bot configuration
 *
 * @author Neutroni
 */
public class ConfigManager {

    private static final Logger LOGGER = LogManager.getLogger();

    //Database connection
    private final Connection conn;

    //Data
    private final String ownerID;
    private final String databaseVersion;
    private final Map<Long, GuildConfigManager> guilds = new HashMap<>();

    /**
     * Constructor
     *
     * @param connection Database to store config in
     * @throws SQLException If database connection failed or no bot owner found
     * in db
     */
    ConfigManager(Connection connection) throws SQLException {
        this.conn = connection;

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
        this.databaseVersion = optVersion.get();

        //Check if database version is correct
        if (!DatabaseManager.getVersion().equals(this.databaseVersion)) {
            throw new SQLException("Database version mismatch");
        }

        //Load list of known guilds
        loadGuildList();
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
     * Loads a setting from database
     *
     * @param key Key to retrieve setting for
     * @return Value for setting
     * @throws java.sql.SQLException
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
                this.guilds.put(guildID, new GuildConfigManager(conn, guildID));
            }
        }
    }

    /**
     * Add guild to database
     *
     * @param guild Guild to add
     * @return true if add was succesfull
     * @throws SQLException if database connection failed
     */
    public boolean addGuild(Guild guild) throws SQLException {
        final String query = "INSERT INTO Guilds(id,commandPrefix,commandEditPermission,commandRunPermission,eventEditPermission,musicPlayPermission,greetingTemplate) VALUES (?,?,?,?,?,?);";
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
     * Get configmanager for guild
     *
     * @param guild guild to get manager for
     * @return GuildConfigManager
     */
    public GuildConfigManager getGuildConfig(Guild guild) {
        return this.guilds.computeIfAbsent(guild.getIdLong(), (newGuildID) -> {
            try {
                if (!hasGuild(guild.getIdLong())) {
                    addGuild(guild);
                }
            } catch (SQLException e) {
                LOGGER.error("Error adding missing guild", e);
            }
            //Return guildConfigManager to the guild
            return new GuildConfigManager(conn, newGuildID);
        });
    }

    private boolean hasGuild(long guildID) throws SQLException{
        final String query = "SELECT id FROM Guilds WHERE id = ?;";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

}
