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
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Neutroni
 */
public class DatabaseManager implements AutoCloseable {

    private final Connection conn;

    private final ConfigManager config;
    private final ChannelManager channels;
    private final CustomCommandManager commands;
    private final EventManager events;

    /**
     * Constructor
     *
     * @param filename location of database
     * @throws SQLException if loading database fails
     */
    public DatabaseManager(String filename) throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
        this.config = new ConfigManager(conn);
        this.channels = new ChannelManager(conn);
        this.commands = new CustomCommandManager(conn, config);
        this.events = new EventManager(conn);
    }

    @Override
    public void close() throws SQLException {
        this.conn.close();
    }

    /**
     * Get the configuration manager
     *
     * @return configuration manager
     */
    public ConfigManager getConfig() {
        return this.config;
    }

    /**
     * Get the channel manager
     *
     * @return channel manager
     */
    public ChannelManager getChannels() {
        return this.channels;
    }

    /**
     * Get the custom command manager
     *
     * @return custom command manager
     */
    public CustomCommandManager getCustomCommands() {
        return this.commands;
    }

    /**
     * Get the event manager
     *
     * @return event manager
     */
    public EventManager getEvents() {
        return this.events;
    }

    /**
     * Creates the database for the bot
     *
     * @param ownerID ID of the bot owner
     * @throws SQLException if database connection fails
     */
    public void initialize(String ownerID) throws SQLException {
        final String CONFIG = "CREATE TABLE Options(name TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL);";
        final String CHANNELS = "CREATE TABLE Channels(id TEXT PRIMRY KEY NOT NULL);";
        final String COMMANDS = "CREATE TABLE Commands(name TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL, owner TEXT NOT NULL);";
        final String EVENTS = "CREATE TABLE Events(name TEXT PRIMARY KEY NOT NULL, description TEXT NOT NULL, owner TEXT NOT NULL)";
        final String EVENT_MEMBERS = "CREATE TABLE EventMembers(event REFERENCES Events(name) ON DELETE CASCADE, member TEXT NOT NULL, PRIMARY KEY (event,member));";
        final String INSERT = "INSERT INTO Options(name,value) VALUES(?,?);";
        try (Statement st = conn.createStatement()) {
            st.addBatch(CONFIG);
            st.addBatch(CHANNELS);
            st.addBatch(COMMANDS);
            st.addBatch(EVENTS);
            st.addBatch(EVENT_MEMBERS);
            st.executeBatch();
        }
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, ConfigKey.OWNER_ID.name());
            ps.setString(2, ownerID);
            ps.executeUpdate();
        }
    }

}
