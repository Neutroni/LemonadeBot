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

import eternal.lemonadebot.customcommands.ActionManager;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.messages.CommandManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Builder for custom commands
 *
 * @author Neutroni
 */
public class CustomCommandManager {

    private final Connection conn;
    private final Set<CustomCommand> commands = new HashSet<>();

    private final ActionManager actionManager = new ActionManager();
    private final ConfigManager configManager;

    /**
     * Constructor
     *
     * @param connection Database connection to use
     * @param config configuration manager to use
     * @throws SQLException if loading commands from datbase failed
     */
    CustomCommandManager(Connection connection, ConfigManager config) throws SQLException {
        this.configManager = config;
        this.conn = connection;
        loadCommands();
    }

    /**
     * Builds a custom command
     *
     * @param key key for command
     * @param pattern pattern for command
     * @param owner owner fo the command
     * @return the new custom command
     */
    public CustomCommand build(String key, String pattern, long owner) {
        return new CustomCommand(this.configManager, this.actionManager, key, pattern, owner);
    }

    /**
     * Gets the action manager used to build commands
     *
     * @return ActionManager
     */
    public ActionManager getActionManager() {
        return this.actionManager;
    }

    /**
     * Adds a command to database
     *
     * @param command Command to add
     * @return true if added succesfully
     * @throws SQLException if database connection fails
     */
    public boolean addCommand(CustomCommand command) throws SQLException {
        synchronized (this) {
            final boolean added = this.commands.add(command);

            //Add to database
            final String query = "INSERT INTO Commands(name,value,owner) VALUES(?,?,?);";
            if (!hasCommand(command.getCommand())) {
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, command.getCommand());
                    ps.setString(2, command.getAction());
                    ps.setLong(3, command.getOwner());
                    return ps.executeUpdate() > 0;
                }
            }
            return added;
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
        synchronized (this) {
            boolean removed = this.commands.remove(command);

            //Remove from database
            final String query = "DELETE FROM Commands WHERE name = ?;";
            if (hasCommand(command.getCommand())) {
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, command.getCommand());
                    return ps.executeUpdate() > 0;
                }
            }

            return removed;
        }
    }

    /**
     * Get command by name
     *
     * @param name name of the command
     * @return optional containing the command
     */
    public Optional<CustomCommand> getCommand(String name) {
        synchronized (this) {
            for (CustomCommand c : this.commands) {
                if (c.getCommand().equals(name)) {
                    return Optional.of(c);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Check if custom commands exists in database
     *
     * @param key command to search for
     * @return true if command was found, false otherwise
     * @throws java.sql.SQLException if database connection fails
     */
    private boolean hasCommand(String key) throws SQLException {
        final String query = "SELECT name FROM Commands WHERE name = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Loads custom commands from database
     *
     * @throws SQLException if Database connection failed
     */
    private void loadCommands() throws SQLException {
        final String query = "SELECT name,value,owner FROM Commands;";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                final CustomCommand newCommand = build(rs.getString("name"), rs.getString("value"), rs.getLong("owner"));
                this.commands.add(newCommand);
            }
        }
    }

    /**
     * Get list of commands
     *
     * @return custom commands
     */
    public List<CustomCommand> getCommands() {
        synchronized (this) {
            return List.copyOf(this.commands);
        }
    }

}
