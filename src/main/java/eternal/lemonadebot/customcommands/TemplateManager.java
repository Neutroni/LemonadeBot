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

import eternal.lemonadebot.cache.ItemCache;
import eternal.lemonadebot.cache.NamedGuildItem;
import eternal.lemonadebot.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Storage of custom commands
 *
 * @author Neutroni
 */
public class TemplateManager {

    private final DataSource dataSource;
    private final Map<NamedGuildItem, CustomCommand> templateCache;

    /**
     * Constructor
     *
     * @param db Database connection to use
     */
    public TemplateManager(final DatabaseManager db) {
        this.dataSource = db.getDataSource();
        final int cacheLimit = db.getConfig().templateCacheEnabled();
        this.templateCache = Collections.synchronizedMap(new ItemCache<>(cacheLimit));
    }

    /**
     * Get command by name
     *
     * @param name name of the command
     * @param guildID guild to find a command for
     * @return optional containing the command
     * @throws SQLException if database connection failed
     */
    public Optional<CustomCommand> getCommand(final String name, final long guildID) throws SQLException {
        final NamedGuildItem key = new NamedGuildItem(guildID, name);
        synchronized (this.templateCache) {
            //Check if command is cached
            final CustomCommand com = this.templateCache.get(key);
            if (com != null) {
                return Optional.of(com);
            }
            //Not in cache, check database
            final Optional<CustomCommand> optCommand = getCommandFromDatabase(key);
            optCommand.ifPresent((CustomCommand t) -> {
                this.templateCache.put(key, t);
            });
            return optCommand;
        }
    }

    /**
     * Function to fetch template from database
     *
     * @param key Key to find command for
     * @return Optional containing the template if found
     * @throws SQLException if database connection failed
     */
    private Optional<CustomCommand> getCommandFromDatabase(NamedGuildItem key) throws SQLException {
        final long guildID = key.getGuildID();
        final String name = key.getItemName();
        final String query = "SELECT template,owner FROM Commands WHERE guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            ps.setString(2, name);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String commandTemplate = rs.getString("template");
                    final long commandOwnerID = rs.getLong("owner");
                    return Optional.of(new CustomCommand(name, commandTemplate, commandOwnerID, guildID));
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
            ps.setLong(1, command.getGuildID());
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
            ps.setLong(2, command.getGuildID());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get list of commands
     *
     * @return custom commands
     * @throws SQLException of database connection failed
     */
    Collection<CustomCommand> getCommands(final long guildID) throws SQLException {
        final List<CustomCommand> commands = new ArrayList<>();
        final String query = "SELECT name,template,owner FROM Commands WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String commandName = rs.getString("name");
                    final String commandTemplate = rs.getString("template");
                    final long commandOwnerID = rs.getLong("owner");
                    final CustomCommand newCommand = new CustomCommand(commandName, commandTemplate, commandOwnerID, guildID);
                    commands.add(newCommand);
                }
            }
        }
        return Collections.unmodifiableCollection(commands);
    }

}
