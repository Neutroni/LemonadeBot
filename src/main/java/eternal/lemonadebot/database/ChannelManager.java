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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class ChannelManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Connection conn;
    private final long guildID;
    private final Set<Long> channels = Collections.synchronizedSet(new HashSet<>());

    /**
     * Constructor
     *
     * @param connetion database connection to use
     * @param guildID ID of the guild this channelmanager stores channels for
     */
    ChannelManager(Connection connetion, long guildID) {
        this.conn = connetion;
        this.guildID = guildID;
        loadChannels();
    }

    /**
     * Get channels ids
     *
     * @return list of channel ids
     */
    public List<Long> getChannels() {
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
        LOGGER.debug("Storing channel in database: " + channel.getId());
        this.channels.add(channel.getIdLong());

        //Add to database
        final String query = "INSERT OR IGNORE INTO Channels(id,guild) VALUES (?,?);";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, channel.getIdLong());
            ps.setLong(2, channel.getGuild().getIdLong());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Remove channel from channels we listen on
     *
     * @param id ID of the channel to remove
     * @return number of removed channels
     * @throws SQLException if database connection failed
     */
    public boolean removeChannel(Long id) throws SQLException {
        LOGGER.debug("Removing channel from database: " + id);
        this.channels.remove(id);

        //Remove from database
        final String query = "DELETE FROM Channels WHERE id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Check if we store given channel
     *
     * @param channel channel to check
     * @return true if channel was found
     */
    public boolean hasChannel(TextChannel channel) {
        return this.channels.contains(channel.getIdLong());
    }

    /**
     * Returns a list of channels we listen on
     *
     * @return List of channels
     * @throws SQLException if database connection fails
     */
    private void loadChannels() {
        final String query = "SELECT id FROM Channels WHERE guild = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (ResultSet rs = ps.executeQuery(query)) {
                while (rs.next()) {
                    this.channels.add(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading channels from database failed");
            LOGGER.warn(e.getMessage());
            LOGGER.trace(e);
        }
    }

}
