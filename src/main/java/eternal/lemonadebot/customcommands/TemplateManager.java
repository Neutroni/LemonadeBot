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
package eternal.lemonadebot.customcommands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Storage of custom commands
 *
 * @author Neutroni
 */
public class TemplateManager {

    private final DataSource dataSource;
    private final long guildID;

    /**
     * Constructor
     *
     * @param ds Database connection to use
     * @param guildID guild to store templates for
     */
    public TemplateManager(final DataSource ds, final long guildID) {
        this.dataSource = ds;
        this.guildID = guildID;
    }

    /**
     * Get command by name
     *
     * @param name name of the command
     * @return optional containing the command
     * @throws SQLException if database connection failed
     */
    public Optional<CustomCommand> getCommand(final String name) throws SQLException {
        final String query = "SELECT template,owner FROM Commands WHERE guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, name);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String commandTemplate = rs.getString("template");
                    final long commandOwnerID = rs.getLong("owner");
                    return Optional.of(new CustomCommand(name, commandTemplate, commandOwnerID));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Adds a command to database
     *
     * @param command Command to add
     * @return true if added successfully
     * @throws SQLException if database connection fails
     */
    boolean addCommand(final CustomCommand command) throws SQLException {
        final String query = "INSERT OR IGNORE INTO Commands(guild,name,template,owner) VALUES(?,?,?,?);";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, command.getName());
            ps.setString(3, command.getTemplate());
            ps.setLong(4, command.getAuthor());
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
    boolean removeCommand(final CustomCommand command) throws SQLException {
        final String query = "DELETE FROM Commands WHERE name = ? AND guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, command.getName());
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get list of commands
     *
     * @return custom commands
     * @throws SQLException of database connection failed
     */
    Collection<CustomCommand> getCommands() throws SQLException {
        final List<CustomCommand> commands = new ArrayList<>();
        final String query = "SELECT name,template,owner FROM Commands WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String commandName = rs.getString("name");
                    final String commandTemplate = rs.getString("template");
                    final long commandOwnerID = rs.getLong("owner");
                    final CustomCommand newCommand = new CustomCommand(commandName, commandTemplate, commandOwnerID);
                    commands.add(newCommand);
                }
            }
        }
        return Collections.unmodifiableCollection(commands);
    }

}
