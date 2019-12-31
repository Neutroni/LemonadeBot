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
    private volatile CommandPermission commandRunPermission;
    private volatile CommandPermission commandEditPermission;
    private volatile CommandPermission eventEditPermission;
    private volatile CommandPermission musicPlayPermission;

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
        return this.commandEditPermission;
    }

    /**
     * Get permission to use custom commands and join events
     *
     * @return CommandPermissions
     */
    public CommandPermission getCommandRunPermission() {
        return this.commandRunPermission;
    }

    /**
     * Get permissiond to create events
     *
     * @return CommandPermission
     */
    public CommandPermission getEventPermission() {
        return this.eventEditPermission;
    }

    /**
     * Get the permission needed to play songs
     *
     * @return CommandPermission
     */
    public CommandPermission getPlayPermission() {
        return this.musicPlayPermission;
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
                        this.commandEditPermission = CommandPermission.valueOf(rs.getString("commandEditPermission"));
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Failed to load permissiond for editing commands, malformed enum value", e);
                        this.commandEditPermission = CommandPermission.ADMIN;
                    }
                    try {
                        this.commandRunPermission = CommandPermission.valueOf(rs.getString("commandRunPermission"));
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Failed to load permissiond for running commands, malformed enum value", e);
                        this.commandRunPermission = CommandPermission.MEMBER;
                    }
                    try {
                        this.eventEditPermission = CommandPermission.valueOf(rs.getString("eventEditPermission"));
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Failed to load permissiond for editing events, malformed enum value", e);
                        this.eventEditPermission = CommandPermission.ADMIN;
                    }
                    try {
                        this.musicPlayPermission = CommandPermission.valueOf(rs.getString("musicPlayPermission"));
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Failed to load permissiond for playing music, malformed enum value", e);
                        this.musicPlayPermission = CommandPermission.MEMBER;
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Failed to load permissions from database", ex);
        }
    }

    /**
     * Set the permission required to edit commands
     *
     * @param newEditPermission permission needed
     * @return did update succeed
     * @throws SQLException if database connection failed
     */
    public boolean setEditPermission(CommandPermission newEditPermission) throws SQLException {
        this.commandEditPermission = newEditPermission;

        final String query = "UPDATE Guilds SET commandEditPermission = ? WHERE guild = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            ps.setString(1, newEditPermission.name());
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Set the permissiond required to use commands
     *
     * @param newRunPermission permission needed
     * @return did update succeed
     * @throws SQLException if database connection failed
     */
    public boolean setCommandRunPermission(CommandPermission newRunPermission) throws SQLException {
        this.commandRunPermission = newRunPermission;

        final String query = "UPDATE Guilds SET commandRunPermission = ? WHERE guild = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            ps.setString(1, newRunPermission.name());
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Set the permission required to manage events
     *
     * @param newEventPermission permission needed
     * @return did update succeed
     * @throws SQLException if database connection failed
     */
    public boolean setEventPermission(CommandPermission newEventPermission) throws SQLException {
        this.eventEditPermission = newEventPermission;

        final String query = "UPDATE Guilds SET eventEditPermission = ? WHERE guild = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            ps.setString(1, newEventPermission.name());
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Set the permission required to play music
     *
     * @param newMusicPermission permission needed
     * @return did update succeed
     * @throws SQLException if database connection failed
     */
    public boolean setPlayPermission(CommandPermission newMusicPermission) throws SQLException {
        this.musicPlayPermission = newMusicPermission;

        final String query = "UPDATE Guilds SET musicPlayPermission = ? WHERE guild = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            ps.setString(1, newMusicPermission.name());
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
        }
    }

}
