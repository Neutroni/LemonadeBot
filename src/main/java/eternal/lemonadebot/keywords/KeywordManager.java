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
package eternal.lemonadebot.keywords;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class KeywordManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DataSource dataSource;
    private final long guildID;
    private final Map<String, KeywordAction> commands = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param ds Database connection to use
     * @param guildID ID of the guild to store keywords for
     */
    public KeywordManager(DataSource ds, long guildID) {
        this.dataSource = ds;
        this.guildID = guildID;
        loadCommands();
    }

    /**
     * Adds a command to database
     *
     * @param command Command to add
     * @return true if added succesfully
     * @throws SQLException if database connection fails
     */
    boolean addKeyword(KeywordAction command) throws SQLException {
        this.commands.putIfAbsent(command.getName(), command);

        //Add to database
        final String query = "INSERT OR IGNORE INTO Keywords(guild,name,pattern,template,owner) VALUES(?,?,?,?);";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, command.getName());
            ps.setString(3, command.getPatternString());
            ps.setString(4, command.getTemplate());
            ps.setLong(5, command.getAuthor());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Remove command from database
     *
     * @param command command to remove
     * @return true if command was removed
     * @throws SQLException if database connection fails
     */
    boolean removeKeyword(KeywordAction command) throws SQLException {
        this.commands.remove(command.getName());

        //Remove from database
        final String query = "DELETE FROM Keywords WHERE name = ? AND guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, command.getName());
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get command by name
     *
     * @param name name of the command
     * @return optional containing the command
     */
    Optional<KeywordAction> getCommand(String name) {
        return Optional.ofNullable(this.commands.get(name));
    }

    /**
     * Get list of commands
     *
     * @return custom commands
     */
    Collection<KeywordAction> getCommands() {
        return Collections.unmodifiableCollection(this.commands.values());
    }

    /**
     * Loads custom commands from database
     *
     * @throws SQLException if Database connection failed
     */
    private void loadCommands() {
        final String query = "SELECT name,pattern,template,owner FROM Keywords WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String commandName = rs.getString("name");
                    final String commandPattern = rs.getString("pattern");
                    final String commandTemplate = rs.getString("template");
                    final long commandOwnerID = rs.getLong("owner");
                    final KeywordAction newCommand = new KeywordAction(commandName, commandPattern, commandTemplate, commandOwnerID);
                    this.commands.put(newCommand.getName(), newCommand);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading Keywords from database failed: {}", e.getMessage());
            LOGGER.trace(e);
        }
    }

}
