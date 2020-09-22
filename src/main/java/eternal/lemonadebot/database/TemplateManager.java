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

import eternal.lemonadebot.customcommands.CustomCommand;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Builder for custom commands
 *
 * @author Neutroni
 */
public class TemplateManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Connection conn;
    private final long guildID;
    private final Set<CustomCommand> commands = ConcurrentHashMap.newKeySet();

    private final CooldownManager cooldownManager;

    /**
     * Constructor
     *
     * @param connection Database connection to use
     * @param config configuration manager to use
     */
    TemplateManager(Connection connection, CooldownManager cooldownManager, long guildID) {
        this.conn = connection;
        this.cooldownManager = cooldownManager;
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
    public boolean addCommand(CustomCommand command) throws SQLException {
        this.commands.add(command);

        //Add to database
        final String query = "INSERT OR IGNORE INTO Commands(guild,name,template,owner) VALUES(?,?,?,?);";
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, command.getCommandName());
            ps.setString(3, command.getTemplate());
            ps.setLong(4, command.getOwner());
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
    public boolean removeCommand(CustomCommand command) throws SQLException {
        this.commands.remove(command);
        this.cooldownManager.removeCooldown(command.getCommandName());

        //Remove from database
        final String query = "DELETE FROM Commands WHERE name = ? AND guild = ?;";
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, command.getCommandName());
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
    public Optional<CustomCommand> getCommand(String name) {
        for (CustomCommand c : this.commands) {
            if (!c.getCommandName().equals(name)) {
                continue;
            }
            return Optional.of(c);
        }
        return Optional.empty();
    }

    /**
     * Get list of commands
     *
     * @return custom commands
     */
    public Set<CustomCommand> getCommands() {
        return Collections.unmodifiableSet(this.commands);
    }

    /**
     * Loads custom commands from database
     *
     * @throws SQLException if Database connection failed
     */
    private void loadCommands() {
        final String query = "SELECT name,template,owner FROM Commands WHERE guild = ?;";
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final CustomCommand newCommand = new CustomCommand(rs.getString("name"), rs.getString("template"), rs.getLong("owner"));
                    this.commands.add(newCommand);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading custom commands from database failed: {}", e.getMessage());
            LOGGER.trace(e);
        }
    }

}
