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
import net.dv8tion.jda.api.JDA;

/**
 *
 * @author Neutroni
 */
public class DatabaseManager implements AutoCloseable {

    private static final String DATABASEVERSION = "1.0";

    private final Connection conn;
    private final JDA jda;

    private final ConfigManager config;
    private final ChannelManager channels;
    private final CustomCommandManager commands;
    private final EventManager events;
    private final RemainderManager remainders;

    /**
     * Constructor
     *
     * @param filename location of database
     * @param jda JDA to pass to managers that need it
     * @throws SQLException if loading database fails
     * @throws InterruptedException If jda loading was interrupted
     */
    public DatabaseManager(String filename, JDA jda) throws SQLException, InterruptedException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
        this.jda = jda;

        this.config = new ConfigManager(conn);
        this.channels = new ChannelManager(conn);
        this.commands = new CustomCommandManager(conn, config);
        this.events = new EventManager(conn);
        this.remainders = new RemainderManager(conn, this.events, this.jda);
    }

    @Override
    public void close() throws SQLException {
        this.conn.close();
    }

    /**
     * Get the version of database in use
     *
     * @return Version string
     */
    static String getVersion() {
        return DATABASEVERSION;
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
     * Get the JDA that is used with the DB
     *
     * @return JDA instance
     */
    public JDA getJDA() {
        return this.jda;
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
     * Get the remainder manager
     *
     * @return RemainderManager
     */
    public RemainderManager getRemainders() {
        return this.remainders;
    }

    /**
     * Creates the database for the bot
     *
     * @param dbLocation location for database
     * @param ownerID ID of the bot owner
     * @throws SQLException if database connection fails
     */
    public static void initialize(String dbLocation, String ownerID) throws SQLException {
        final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbLocation);
        final String CONFIG = "CREATE TABLE Options(name TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL);";
        final String CHANNELS = "CREATE TABLE Channels(id INTEGER PRIMARY KEY NOT NULL);";
        final String GUILDCONF = "CREATE TABLE Guilds("
                + "id INTEGER PRIMARY KEY NOT NULL,"
                + "commandPrefix TEXT NOT NULL"
                + "commandEditPermission TEXT NOT NULL,"
                + "commandRunPermission TEXT NOT NULL,"
                + "eventEditPermission TEXT NOT NULL,"
                + "musicPlayPermission TEXT NOT NULL"
                + "greetingTemplate TEXT NOT NULL"
                + ");";
        final String COMMANDS = "CREATE TABLE Commands("
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "template TEXT NOT NULL,"
                + "owner INTEGER NOT NULL"
                + "PRIMARY KEY (guild,name));";
        final String EVENTS = "CREATE TABLE Events("
                + "id INTEGER PRIMARY KEY,"
                + "guild INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "description TEXT NOT NULL,"
                + "owner INTEGER NOT NULL,"
                + "UNIQUE (guild,name));";
        final String EVENT_MEMBERS = "CREATE TABLE EventMembers("
                + "FOREIGN KEY (event) REFERENCES Events(id) ON DELETE CASCADE,"
                + "member INTEGER NOT NULL,"
                + "PRIMARY KEY (event,member));";
        final String REMAINDERS = "CREATE TABLE Remainders("
                + "FOREIGN KEY (event) REFERENCES Events(id) ON DELETE CASCADE,"
                + "day TEXT NOT NULL,"
                + "time TEXT NOT NULL,"
                + "mention TEXT NOT NULL,"
                + "channel INTEGER NOT NULL,"
                + "PRIMARY KEY (event,day,time));";
        final String INSERT = "INSERT INTO Options(name,value) VALUES(?,?);";
        try (Statement st = connection.createStatement()) {
            st.addBatch(CONFIG);
            st.addBatch(CHANNELS);
            st.addBatch(GUILDCONF);
            st.addBatch(COMMANDS);
            st.addBatch(EVENTS);
            st.addBatch(EVENT_MEMBERS);
            st.addBatch(REMAINDERS);
            st.executeBatch();
        }
        try (PreparedStatement ps = connection.prepareStatement(INSERT)) {
            ps.setString(1, ConfigKey.DATABASE_VERSION.name());
            ps.setString(2, DATABASEVERSION);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(INSERT)) {
            ps.setString(1, ConfigKey.OWNER_ID.name());
            ps.setString(2, ownerID);
            ps.executeUpdate();
        }
    }

}
