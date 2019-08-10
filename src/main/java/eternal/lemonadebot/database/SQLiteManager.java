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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of database access for Databasemanager
 *
 * @author Neutroni
 */
class SQLiteManager implements AutoCloseable {

    private final Connection conn;

    /**
     * Constructor
     *
     * @param filename file to store the database in
     * @throws java.sql.SQLException
     */
    SQLiteManager(String filename) throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
    }

    @Override
    public void close() throws SQLException {
        this.conn.close();
    }

    /**
     * Sets the value of setting
     *
     * @param key Key for setting
     * @param value New value for setting
     * @throws java.sql.SQLException
     */
    void updateSetting(String key, String value) throws SQLException {
        final String query = "UPDATE Options set value = ? WHERE name = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    /**
     * Loads a setting from database
     *
     * @param key Key to retrieve setting for
     * @return Value for setting
     * @throws java.sql.SQLException
     */
    Optional<String> loadSetting(String key) throws SQLException {
        final String query = "SELECT value FROM Options WHERE name = ?;";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("value"));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Adds a command to database
     *
     * @param key Key of the command
     * @param value command value
     * @param owner command creator
     * @throws SQLException if database connection fails
     */
    void addCommand(String key, String value, String owner) throws SQLException {
        final String query = "INSERT INTO Commands(name,value,owner) VALUES(?,?,?);";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, owner);
            ps.executeUpdate();
        }
    }

    /**
     * Remove command from database
     *
     * @param key key of command
     * @return 1 on success, 0 if command was not found
     * @throws SQLException if database connection fails
     */
    int removeCommand(String key) throws SQLException {
        final String query = "DELETE FROM Commands WHERE name = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            return ps.executeUpdate();
        }
    }

    /**
     * Adds a channel to listen on
     *
     * @param id
     * @throws java.sql.SQLException
     */
    void addChannel(String id) throws SQLException {
        final String query = "INSERT INTO Channels(id) VALUES (?);";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Remove channel from channels we listen on
     *
     * @param id ID
     * @return number of removed channels
     * @throws SQLException
     */
    int removeChannel(String id) throws SQLException {
        final String query = "DELETE FROM Channels WHERE id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, id);
            return ps.executeUpdate();
        }
    }

    /**
     * Adds admin
     *
     * @param id id of admin
     * @throws SQLException if database connection failed
     */
    void addAdmin(String id) throws SQLException {
        final String query = "INSERT INTO Admins(id) VALUES(?);";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Removes admin
     *
     * @param id id of the admin
     * @return 1 on success, 0 if admin was not found
     * @throws SQLException if database connection fails
     */
    int removeAdmin(String id) throws SQLException {
        final String query = "DELETE FROM Admins Where id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, id);
            return ps.executeUpdate();
        }
    }

    /**
     * Returns a synchronized list of channels we listen on
     *
     * @return List of watched channel ids
     * @throws java.sql.SQLException
     */
    List<String> loadChannels() throws SQLException {
        final List<String> channels = Collections.synchronizedList(new ArrayList<>());
        final String query = "SELECT id FROM Channels;";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                channels.add(rs.getString("id"));
            }
        }
        return channels;
    }

    /**
     * Returns synchronized list of admins
     *
     * @return List of admin ids
     * @throws SQLException if database connection fails
     */
    List<String> loadAdmins() throws SQLException {
        final List<String> admins = Collections.synchronizedList(new ArrayList<>());
        final String query = "SELECT id FROM Admins;";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                admins.add(rs.getString("id"));
            }
        }
        return admins;
    }

    /**
     * Return list of command parts
     *
     * @return Custom commands as string components
     * @throws SQLException if Database command failed
     */
    List<String[]> loadCommands() throws SQLException {
        final List<String[]> admins = Collections.synchronizedList(new ArrayList<>());
        final String query = "SELECT name,value,owner FROM Commands;";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                admins.add(new String[]{rs.getString("name"), rs.getString("value"), rs.getString("owner")});
            }
        }
        return admins;
    }

    /**
     * Creates the database for the bot
     *
     * @param ownerID ID of the bot owner
     * @throws SQLException if database connection fails
     */
    void initialize(String ownerID) throws SQLException {
        final String CONFIG = "CREATE TABLE Options(name TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL);";
        final String ADMINS = "CREATE TABLE Admins(id TEXT PRIMARY KEY NOT NULL);";
        final String CHANNELS = "CREATE TABLE Channels(id TEXT PRIMRY KEY NOT NULL);";
        final String COMMANDS = "CREATE TABLE Commands(name TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL, owner TEXT NOT NULL);";
        final String INSERT = "INSERT INTO Options(name,value) VALUES(?,?);";
        try (Statement st = conn.createStatement()) {
            st.addBatch(CONFIG);
            st.addBatch(ADMINS);
            st.addBatch(CHANNELS);
            st.addBatch(COMMANDS);
            st.executeBatch();
        }
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, ConfigKey.OWNER_ID.name());
            ps.setString(2, ownerID);
            ps.executeUpdate();
        }

    }

    /**
     * Check if custom commands exists in database
     *
     * @param key command to search for
     * @return true if command was found, false otherwise
     * @throws java.sql.SQLException if database connection fails
     */
    boolean hasCommand(String key) throws SQLException {
        final String query = "SELECT name FROM Commands WHERE name = ?";
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
     * Check if channel exists in database
     *
     * @param id channel to search for
     * @return true if channel was found, false otherwise
     * @throws SQLException if database connection fails
     */
    boolean hasChannel(String id) throws SQLException {
        final String query = "SELECT id FROM Channels WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if admin exists in database
     *
     * @param id admin to search for
     * @return true if admin was found, false otherwise
     * @throws SQLException if database connection fails
     */
    boolean hasAdmin(String id) throws SQLException {
        final String query = "SELECT id FROM Admins WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

}
