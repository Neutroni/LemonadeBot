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
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class GuildConfigManager {

    private static final Logger LOGGER = LogManager.getLogger();

    //Database connection
    private final Connection conn;

    //Stored values
    private final long guildID;
    private volatile Pattern commandPattern;
    private volatile CommandPermission runPermission;
    private volatile CommandPermission editPermission;
    private volatile CommandPermission eventPermission;
    private volatile CommandPermission playPermission;

    /**
     * Constructor
     *
     * @param connection database connection to use
     * @param guild Guild this config is for
     */
    public GuildConfigManager(Connection connection, long guild) {
        this.conn = connection;
        this.guildID = guild;
        loadPermissions();
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
     * Get permissiond to create events
     *
     * @return CommandPermission
     */
    public CommandPermission getEventPermission() {
        return this.eventPermission;
    }

    /**
     * Get the permission needed to play songs
     *
     * @return CommandPermission
     */
    public CommandPermission getPlayPermission() {
        return this.playPermission;
    }

    /**
     * Get command pattern
     *
     * @return command pattern
     */
    public Pattern getCommandPattern() {
        return this.commandPattern;
    }

    /**
     * Set command prefix
     *
     * @param prefix new command prefix
     * @throws SQLException if updating prefix failed
     */
    public void setCommandPrefix(String prefix) throws SQLException {
        this.commandPattern = getCommandPattern(prefix);
        final String query = "UPDATE Guilds SET prefix = ? WHERE guild = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            ps.setString(1, prefix);
            ps.setLong(2, this.guildID);
            ps.executeUpdate();
        }
    }

    /**
     * Updates command pattern
     *
     * @param prefix prefix to use in pattern
     */
    private Pattern getCommandPattern(String prefix) {
        //Start of match, optionally @numericID, prefix, match group 2 is command
        return Pattern.compile("^(@\\d+ )?" + Pattern.quote(prefix) + "(\\w+) ?");
    }

    private void loadPermissions() {
        final String query = "SELECT  FROM Guilds WHERE id = ?;";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try {
                        this.editPermission = CommandPermission.valueOf(rs.getString("commandEditPermission"));
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Failed to load permissiond for editing commands, malformed enum value", e);
                        this.editPermission = CommandPermission.ADMIN;
                    }
                    try {
                        this.runPermission = CommandPermission.valueOf(rs.getString("commandRunPermission"));
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Failed to load permissiond for running commands, malformed enum value", e);
                        this.runPermission = CommandPermission.MEMBER;
                    }
                    try {
                        this.eventPermission = CommandPermission.valueOf(rs.getString("eventEditPermission"));
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Failed to load permissiond for editing events, malformed enum value", e);
                        this.eventPermission = CommandPermission.ADMIN;
                    }
                    try {
                        this.playPermission = CommandPermission.valueOf(rs.getString("musicPlayPermission"));
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Failed to load permissiond for playing music, malformed enum value", e);
                        this.playPermission = CommandPermission.MEMBER;
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Failed to load permissions from database", ex);
        }
    }

}