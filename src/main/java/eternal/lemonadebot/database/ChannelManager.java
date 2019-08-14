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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author Neutroni
 */
public class ChannelManager {

    private final Connection conn;
    private final Set<String> channels = new HashSet<>();

    /**
     * Constructor
     *
     * @param connetion database connection to use
     * @throws SQLException If loading channels from database failed
     */
    ChannelManager(Connection connetion) throws SQLException {
        this.conn = connetion;
        loadChannels();
    }

    /**
     * Get channels ids
     *
     * @return list of channel ids
     */
    public List<String> getChannels() {
        return List.copyOf(this.channels);
    }

    /**
     * Adds a channel to listen on
     *
     * @param channel Channel to add
     * @return true if channels was added
     * @throws SQLException if database connection failed
     */
    public boolean addChannel(TextChannel channel) throws SQLException {
        synchronized (this) {
            boolean added = this.channels.add(channel.getId());

            //Add to database
            final String query = "INSERT INTO Channels(id) VALUES (?);";
            if (!hasChannel(channel.getId())) {
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, channel.getId());
                    return ps.executeUpdate() > 0;
                }
            }
            return added;
        }
    }

    /**
     * Remove channel from channels we listen on
     *
     * @param id ID of the channel to remove
     * @return number of removed channels
     * @throws SQLException if database connection failed
     */
    public boolean removeChannel(String id) throws SQLException {
        synchronized (this) {
            boolean removed = this.channels.remove(id);

            //Remove from database
            final String query = "DELETE FROM Channels WHERE id = ?;";
            if (hasChannel(id)) {
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, id);
                    return ps.executeUpdate() > 0;
                }
            }

            return removed;
        }
    }

    /**
     * Check if we store given channel
     *
     * @param channel channel to check
     * @return true if channel was found
     */
    public boolean hasChannel(TextChannel channel) {
        synchronized (this) {
            return this.channels.contains(channel.getId());
        }
    }

    /**
     * Check if channel exists in database
     *
     * @param id channel to search for
     * @return true if channel was found, false otherwise
     * @throws SQLException if database connection fails
     */
    private boolean hasChannel(String id) throws SQLException {
        final String query = "SELECT id FROM Channels WHERE id = ?;";
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
     * Returns a list of channels we listen on
     *
     * @return List of channels
     * @throws SQLException if database connection fails
     */
    private void loadChannels() throws SQLException {
        final String query = "SELECT id FROM Channels;";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                this.channels.add(rs.getString("id"));
            }
        }
    }

    /**
     * Check if we store any channels
     *
     * @return true if no channels are currently stored
     */
    public boolean isEmpty() {
        synchronized (this) {
            return this.channels.isEmpty();
        }
    }

}
