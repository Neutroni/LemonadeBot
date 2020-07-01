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
    private volatile String commandPrefix = "lemonbot#";
    private volatile Pattern commandPattern = getCommandPattern(commandPrefix);
    private volatile Optional<String> greetingTemplate = Optional.empty();

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
     * Prefix used by commands
     *
     * @return Command prefix for the server
     */
    public String getCommandPrefix() {
        return this.commandPrefix;
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
    public boolean setCommandPrefix(final String prefix) throws SQLException {
        this.commandPrefix = prefix;
        this.commandPattern = getCommandPattern(prefix);

        final String query = "UPDATE Guilds SET commandPrefix = ? WHERE id = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            ps.setString(1, prefix);
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
    public boolean setGreetingTemplate(final String newTemplate) throws SQLException {
        this.greetingTemplate = Optional.ofNullable(newTemplate);

        final String query = "UPDATE Guilds SET greetingTemplate = ? WHERE id = ?;";
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
    private Pattern getCommandPattern(final String prefix) {
        //Start of match, prefix, command, arguments
        return Pattern.compile("^" + Pattern.quote(prefix) + "(\\S+) ?");
    }

    /**
     * Load the values this guildconfig stores
     */
    private void loadValues() {
        final String query = "SELECT commandPrefix,greetingTemplate FROM Guilds WHERE id = ?;";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                //Check that database contains this guild
                if (rs.next()) {
                    //Load command prefix
                    try {
                        this.commandPrefix = rs.getString("commandPrefix");
                        this.commandPattern = getCommandPattern(commandPrefix);
                    } catch (SQLException ex) {
                        LOGGER.error("SQL error on fetching the command prefix");
                        LOGGER.warn(ex);
                    }

                    //Load greeting template
                    try {
                        this.greetingTemplate = Optional.ofNullable(rs.getString("greetingTemplate"));
                    } catch (SQLException ex) {
                        LOGGER.error("SQL error on fetching greeting template");
                        LOGGER.warn(ex);
                    }
                    return;
                }
                LOGGER.info("Tried to load guild that does not exist in database, adding to database");
                if (!addGuild()) {
                    LOGGER.error("Adding the guild to database failed");
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Failed to load guild config from database");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace(ex);
        }
    }

    /**
     * Add this guild to database
     *
     * @return true if add was succesfull
     * @throws SQLException if database connection failed
     */
    private boolean addGuild() throws SQLException {
        LOGGER.debug("Adding guild to database: " + this.guildID);
        final String query = "INSERT OR IGNORE INTO Guilds("
                + "id,commandPrefix,greetingTemplate) VALUES (?,?,?);";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, "lemonbot#");
            ps.setString(3, this.greetingTemplate.orElse(null));
            return ps.executeUpdate() > 0;
        }
    }

}
