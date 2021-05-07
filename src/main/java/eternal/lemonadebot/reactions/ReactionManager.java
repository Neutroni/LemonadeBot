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
package eternal.lemonadebot.reactions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class used to store messages for which to activate to reactions
 *
 * @author Neutroni
 */
public class ReactionManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DataSource dataSource;

    /**
     * Constructor
     *
     * @param ds DataSource to get connection from
     */
    public ReactionManager(final DataSource ds) {
        this.dataSource = ds;
    }

    /**
     * Logs a message in database and removes records when more than maxMessages
     * stored
     *
     * @param message Message to store
     * @param emote Emote to react to
     * @param commandAdd Command to call when message receives a reaction
     * @param commandRemove Command to call when message loses a reaction
     */
    public void addMessageToFollow(final Message message, final MessageReaction.ReactionEmote emote, final String commandAdd, final String commandRemove) {
        final String query = "INSERT INTO Reactions(messageId,guild,channel,reaction,commandAdd,commandRemove) VALUES(?,?,?,?,?,?)";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, message.getIdLong());
            ps.setLong(2, message.getGuild().getIdLong());
            ps.setLong(3, message.getChannel().getIdLong());
            ps.setString(4, commandAdd);
            ps.setString(5, commandRemove);
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.error("Failed to add message to follow to database: {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
        }
    }

    /**
     * Get stored message from database if available
     *
     * @param messageID ID of the message which to retrieve
     * @param emote Emote to retrieve command for
     * @return Optional containing the message content if stored
     */
    public Optional<String> onReactionAdd(final long messageID, final MessageReaction.ReactionEmote emote) {
        final String query = "SELECT commandAdd FROM Reaction WHERE messageID = ? AND reaction = ?";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, messageID);
            ps.setLong(2, emote.getIdLong());
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String command = rs.getString("commandAdd");
                    return Optional.of(command);
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Failed to get command for reaction for message from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
        }
        return Optional.empty();
    }

    /**
     * Get stored message from database if available
     *
     * @param messageID ID of the message which to retrieve
     * @param emote Emote to retrieve command for
     * @return Optional containing the message content if stored
     */
    public Optional<String> onReactionRemove(final long messageID, final MessageReaction.ReactionEmote emote) {
        final String query = "SELECT commandRemove FROM Reaction WHERE messageID = ? AND reaction = ?";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, messageID);
            ps.setLong(2, emote.getIdLong());
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String command = rs.getString("commandRemove");
                    return Optional.of(command);
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Failed to get command for removal of reaction for message from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
        }
        return Optional.empty();
    }

}
