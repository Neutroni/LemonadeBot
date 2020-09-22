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
package eternal.lemonadebot;

import java.util.Optional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Message stored in the database
 *
 * @author Neutroni
 */
public class StoredMessage {

    private static final Logger LOGGER = LogManager.getLogger();

    private final long author;
    private final String content;

    public StoredMessage(long author, String content) {
        this.author = author;
        this.content = content;
    }

    /**
     * Get the auhor of the message
     *
     * @param jda JDA to use to fetch the user
     * @return Optional containing the user if found
     */
    public Optional<User> getAuthor(JDA jda) {
        try {
            final User user = jda.retrieveUserById(author).complete();
            return Optional.ofNullable(user);
        } catch (ErrorResponseException e) {
            LOGGER.debug("User for a stored message could not be found", e);
            return Optional.empty();
        }
    }

    /**
     * Text content of the stored message
     *
     * @return Message.getContentRaw()
     */
    public String getContent() {
        return this.content;
    }

}
