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

import eternal.lemonadebot.StoredMessage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class used to store received messages to log when a message is edited or
 * removed
 *
 * @author Neutroni
 */
public class MessageManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Connection conn;
    private final int maxMessages;

    MessageManager(Connection connection, int maxMessages) {
        this.conn = connection;
        this.maxMessages = maxMessages;
    }

    /**
     * Logs a message in database and removes records when more than maxMessages
     * stored
     *
     * @param message Message to log
     */
    public void logMessage(Message message) {
        final long guildID = message.getGuild().getIdLong();
        final String query = "INSERT INTO Messages(id,guild,author,content) VALUES(?,?,?)";
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, message.getIdLong());
            ps.setLong(2, guildID);
            ps.setLong(3, message.getAuthor().getIdLong());
            ps.setString(4, message.getContentRaw());
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.error("Failed to log message in database" + ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
        }

        final String cleanup = "DELETE FROM Messages WHERE id IN"
                + " (SELECT id FROM Messages WHERE guild = ? ORDER BY id DESC LIMIT -1 OFFSET ?);";
        try ( PreparedStatement ps = conn.prepareStatement(cleanup)) {
            ps.setLong(1, guildID);
            ps.setInt(2, this.maxMessages);
            int count = ps.executeUpdate();
            LOGGER.debug("Deleted " + count + " rows from database on cleanup");
        } catch (SQLException ex) {
            LOGGER.error("Failed to clean up message log in database" + ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
        }
    }

    /**
     * Get stored message from database if available
     *
     * @param messageID ID of the message which to retrieve
     * @return Optional containing the message content if stored
     */
    public Optional<StoredMessage> getMessageContent(long messageID) {
        final String query = "SELECT author,content from Messages WHERE id = ?";
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, messageID);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final long authorID = rs.getLong("author");
                    final String content = rs.getString("content");
                    final StoredMessage message = new StoredMessage(authorID, content);
                    return Optional.of(message);
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Failed to get message content from database" + ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
        }
        return Optional.empty();
    }
}
