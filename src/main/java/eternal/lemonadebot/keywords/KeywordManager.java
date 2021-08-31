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

import eternal.lemonadebot.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Guild;

/**
 *
 * @author Neutroni
 */
public class KeywordManager {

    private final DataSource dataSource;

    /**
     * Constructor
     *
     * @param db Database connection to use
     */
    public KeywordManager(final DatabaseManager db) {
        this.dataSource = db.getDataSource();
    }

    /**
     * Adds a command to database
     *
     * @param command Command to add
     * @return true if added successfully
     * @throws SQLException if database connection fails
     */
    boolean addKeyword(final KeywordAction command) throws SQLException {
        final String query = "INSERT OR IGNORE INTO Keywords(guild,name,pattern,template,owner,runasowner) VALUES(?,?,?,?,?);";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, command.getGuildID());
            ps.setString(2, command.getName());
            ps.setString(3, command.getPatternString());
            ps.setString(4, command.getTemplate());
            ps.setLong(5, command.getAuthor());
            ps.setBoolean(6, command.shouldRunAsOwner());
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
    boolean removeKeyword(final KeywordAction command) throws SQLException {
        final String query = "DELETE FROM Keywords WHERE name = ? AND guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, command.getName());
            ps.setLong(2, command.getGuildID());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get command by name
     *
     * @param name name of the command
     * @return optional containing the command
     */
    Optional<KeywordAction> getCommand(final String name, final Guild guild) throws SQLException{
        final long guildID = guild.getIdLong();
        final String query = "SELECT pattern,template,owner,runasowner FROM Keywords WHERE guild = ? AND name = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            ps.setString(2,name);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String commandPattern = rs.getString("pattern");
                    final String commandTemplate = rs.getString("template");
                    final long commandOwnerID = rs.getLong("owner");
                    final boolean runAsOwner = rs.getBoolean("runasowner");
                    return Optional.of(new KeywordAction(name, commandPattern, commandTemplate, commandOwnerID, runAsOwner, guildID));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Get list of commands
     *
     * @return custom commands
     */
    Collection<KeywordAction> getCommands(final Guild guild) throws SQLException{
        final String query = "SELECT name,pattern,template,owner,runasowner FROM Keywords WHERE guild = ?;";
        final long guildID = guild.getIdLong();
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                final ArrayList<KeywordAction> commands = new ArrayList<>();
                while (rs.next()) {
                    final String commandName = rs.getString("name");
                    final String commandPattern = rs.getString("pattern");
                    final String commandTemplate = rs.getString("template");
                    final long commandOwnerID = rs.getLong("owner");
                    final boolean runAsOwner = rs.getBoolean("runasowner");
                    commands.add(new KeywordAction(commandName, commandPattern, commandTemplate, commandOwnerID, runAsOwner, guildID));
                }
                return commands;
            }
        }
    }

}
