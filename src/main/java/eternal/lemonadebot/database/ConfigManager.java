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
    private final HashMap<Long, String> prefixMap = new HashMap<>();
    private volatile CommandPermission runPermission = CommandPermission.USER;
    private volatile CommandPermission editPermission = CommandPermission.MEMBER;

    /**
     * Constructor
     *
     * @param connection Database to store config in
     * @throws SQLException If database connection failed or no bot owner found
     * in db
     */
    ConfigManager(Connection connection) throws SQLException {
        this.conn = connection;
        final Optional<String> optOwner = loadSetting(ConfigKey.OWNER_ID.name());
        if (optOwner.isEmpty()) {
            throw new SQLException("Missing owner id");
        }
        this.ownerID = optOwner.get();

        //Load command prefix
        loadCommandPrefixes();

        //Load permissions
        loadUsePerm();
        loadManagePerm();
    }

    /**
     * Loads the permission to use custom commands from DB
     *
     * @throws SQLException if databaseconnection failed
     */
    private void loadManagePerm() throws SQLException {
        final Optional<String> opt = loadSetting(ConfigKey.CUSTOM_COMMAND_MANAGE.name());
        if (opt.isEmpty()) {
            LOGGER.info("No permission for managing custom commands defined in DB.");
            LOGGER.info("Saving default value: " + this.editPermission.getDescription());
            updateSetting(ConfigKey.CUSTOM_COMMAND_MANAGE.name(), this.editPermission.name());
            return;
        }
        final String managePerm = opt.get();
        try {
            this.editPermission = CommandPermission.valueOf(managePerm);
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Malformed command permission for custom commands: " + managePerm);
            LOGGER.info("Using default value: " + this.editPermission.name());
        }
    }

    /**
     * Loads the permission to use custom commands from DB
     *
     * @throws SQLException if database connection failed
     */
    private void loadUsePerm() throws SQLException {
        final Optional<String> opt = loadSetting(ConfigKey.CUSTOM_COMMAND_USE.name());
        if (opt.isEmpty()) {
            LOGGER.info("No permission for using custom commands defined in DB.");
            LOGGER.info("Saving default value: " + this.runPermission.getDescription());
            updateSetting(ConfigKey.CUSTOM_COMMAND_USE.name(), this.runPermission.name());
            return;
        }
        final String usePerm = opt.get();
        try {
            this.runPermission = CommandPermission.valueOf(opt.get());
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Malformed command permission for custom commands: " + usePerm);
            LOGGER.info("Using default value: " + this.runPermission.name());
        }
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
     * Get permission required to edit custom commands and events
     *
     * @return CommandPermissions
     */
    public CommandPermission getEditPermission() {
        return this.editPermission;
    }

    /**
     * Get permission to use custom commands and join events
     *
     * @return CommandPermissions
     */
    public CommandPermission getUsePermission() {
        return this.runPermission;
    }

    /**
     * get command prefix
     *
     * @param guildID Guild to get prefix for
     * @return command prefix for guild if found, '!' otherwise
     */
    public String getCommandPrefix(long guildID) {
        return this.prefixMap.getOrDefault(guildID, "!");
    }

    /**
     * Set command prefix
     *
     * @param guild Guild the prefix is for
     * @param prefix new command prefix
     * @throws SQLException if updating prefix failed
     */
    public void setCommandPrefix(Guild guild, String prefix) throws SQLException {
        updatePrefix(guild.getIdLong(), prefix);
    }

    /**
     * Sets the value of setting
     *
     * @param key Key for setting
     * @param value New value for setting
     * @throws java.sql.SQLException
     */
    private void updateSetting(String key, String value) throws SQLException {
        final String query = "UPDATE Options set value = ? WHERE name = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
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

    /**
     * Load prefixces stored in database
     *
     * @throws SQLException if database connection failed
     */
    private void loadCommandPrefixes() throws SQLException {
        final String query = "SELECT guild,prefix FROM Prefixes";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                this.prefixMap.put(rs.getLong("guild"), rs.getString("prefix"));
            }
        }
    }

    /**
     * Update prefix for guild
     *
     * @param guildID Guild to update prefix for
     * @param prefix prefix to set for guild
     * @throws SQLException if database connection failed
     */
    private void updatePrefix(long guildID, String prefix) throws SQLException {
        final String query = "INSERT OR REPLACE INTO Prefixes(guild,prefix) VALUES(?,?);";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, guildID);
            ps.setString(2, prefix);
            ps.executeUpdate();
        }
    }

}
