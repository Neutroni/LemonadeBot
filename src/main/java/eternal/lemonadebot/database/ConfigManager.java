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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class ConfigManager {

    private static final Logger LOGGER = LogManager.getLogger();

    //Database connection
    private final Connection conn;

    //Stored values
    private final long guildID;
    private volatile Pattern commandPattern;
    private volatile Optional<String> greetingTemplate;
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
    ConfigManager(Connection connection, long guild) {
        this.conn = connection;
        this.guildID = guild;
        loadValues();
    }

    /**
     * Get the id of the guild this confimanager stores settings for
     *
     * @return id of the guild
     */
    public long getGuildID() {
        return this.guildID;
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
     * Get the template that should be used for greeting new members of guild
     *
     * @return String
     */
    public Optional<String> getGreetingTemplate() {
        return this.greetingTemplate;
    }

    /**
     * Set command prefix
     *
     * @param prefix new command prefix
     * @return Was comamnd prefix set succesfully
     * @throws SQLException if updating prefix failed
     */
    public boolean setCommandPrefix(String prefix) throws SQLException {
        this.commandPattern = getCommandPattern(prefix);

        final String query = "UPDATE Guilds SET prefix = ? WHERE guild = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            ps.setString(1, prefix);
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
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

    /**
     * Set the template used to greet new members with
     *
     * @param newTemplate Template string to use, null to disable greeting
     * @return did update succeed
     * @throws SQLException id database connection failed
     */
    public boolean setGreetingTemplate(String newTemplate) throws SQLException {
        this.greetingTemplate = Optional.ofNullable(newTemplate);

        final String query = "UPDATE Guilds SET greetingTemplate = ? WHERE guild = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            ps.setString(1, newTemplate);
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
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

    /**
     * Load the values this guildconfig stores
     */
    private void loadValues() {
        final String query = "SELECT prefix,greetingTemplate,commandEditPermission,commandRunPermission,eventEditPermission,musicPlayPermission FROM Guilds WHERE id = ?;";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                //Check that database contains this guild
                if (!rs.next()) {
                    LOGGER.error("Tried to load guild that does not exist in database");
                    loadDefaultValues();
                    return;
                }
                final ConfigOptions opts = new ConfigOptions(rs);
                setValues(opts);
            }
        } catch (SQLException ex) {
            LOGGER.error("Failed to load guild config from database");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace(ex);
            loadDefaultValues();
        }
    }

    /**
     * Load the values stored in configoptions
     *
     * @param opts ConfigOptions to load
     */
    private void setValues(ConfigOptions opts) {
        //Load command prefix
        this.commandPattern = getCommandPattern(opts.getCommandPrefix());
        //Load greeting template
        this.greetingTemplate = Optional.ofNullable(opts.getGreetingTemplate());
        //Load permissionds
        try {
            this.commandEditPermission = CommandPermission.valueOf(opts.getCommandEditPermission());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to load permissiond for editing commands, malformed enum value", e);
            this.commandEditPermission = CommandPermission.ADMIN;
        }
        try {
            this.commandRunPermission = CommandPermission.valueOf(opts.getCommandRunPermission());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to load permissiond for running commands, malformed enum value", e);
            this.commandRunPermission = CommandPermission.MEMBER;
        }
        try {
            this.eventEditPermission = CommandPermission.valueOf(opts.getEventEditPermission());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to load permissiond for editing events, malformed enum value", e);
            this.eventEditPermission = CommandPermission.ADMIN;
        }
        try {
            this.musicPlayPermission = CommandPermission.valueOf(opts.getMusicPlayPermission());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to load permissiond for playing music, malformed enum value", e);
            this.musicPlayPermission = CommandPermission.MEMBER;
        }
    }

    /**
     * Called when guild can not be found in database to load default values
     */
    private void loadDefaultValues() {
        LOGGER.warn("Loading default values for guild: " + this.guildID);
        this.commandPattern = getCommandPattern("lemonbot#");
        this.greetingTemplate = Optional.empty();
        this.commandEditPermission = CommandPermission.ADMIN;
        this.commandRunPermission = CommandPermission.MEMBER;
        this.eventEditPermission = CommandPermission.ADMIN;
        this.musicPlayPermission = CommandPermission.MEMBER;
    }

    private class ConfigOptions {

        private final String commandPrefix;
        private final String greetingTemplate;
        private final String commandEditPermission;
        private final String commandRunPermission;
        private final String eventEditPermission;
        private final String musicPlayPermission;

        ConfigOptions(ResultSet rs) throws SQLException {
            this.commandPrefix = rs.getString("prefix");
            this.greetingTemplate = rs.getString("greetingTemplate");
            this.commandEditPermission = rs.getString("commandEditPermission");
            this.commandRunPermission = rs.getString("commandRunPermission");
            this.eventEditPermission = rs.getString("eventEditPermission");
            this.musicPlayPermission = rs.getString("musicPlayPermission");
        }

        public String getCommandPrefix() {
            return commandPrefix;
        }

        public String getGreetingTemplate() {
            return greetingTemplate;
        }

        public String getCommandEditPermission() {
            return commandEditPermission;
        }

        public String getCommandRunPermission() {
            return commandRunPermission;
        }

        public String getEventEditPermission() {
            return eventEditPermission;
        }

        public String getMusicPlayPermission() {
            return musicPlayPermission;
        }
    }

}
