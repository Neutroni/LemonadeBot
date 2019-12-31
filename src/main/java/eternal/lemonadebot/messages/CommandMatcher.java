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
package eternal.lemonadebot.messages;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Matches given message for command pattern
 *
 * @author Neutroni
 */
public class CommandMatcher {

    private final Message message;
    private final String messageText;
    private final Matcher matcher;
    private final boolean matches;

    /**
     * Constructor
     *
     * @param pattern pattern to use to match commands
     * @param msg command message
     */
    CommandMatcher(Pattern pattern, Message msg) {
        this.message = msg;
        this.messageText = msg.getContentRaw();
        this.matcher = pattern.matcher(messageText);
        this.matches = this.matcher.find();

    }

    /**
     * Get the command from the match
     *
     * @return optional containing command string if found
     */
    public Optional<String> getCommand() {
        if (this.matches) {
            return Optional.of(matcher.group(2));
        }
        return Optional.empty();
    }

    /**
     * Get parameters limited by whitespace and the rest of the message as last
     * entry in returned array
     *
     * @param count number of parameters to return
     * @return array of parameters
     */
    public String[] getArguments(int count) {
        int parameterStart = matcher.end();

        //Check if message ends at match end
        if (parameterStart == this.messageText.length()) {
            return new String[0];
        }

        final String parameterString = this.messageText.substring(parameterStart);
        return parameterString.split(" ", count + 1);
    }

    /**
     * Get the message this commandmatcher matches against
     *
     * @return Message
     */
    public Message getMessage() {
        return this.message;
    }

    /**
     * Get member who sent the message
     *
     * @return Optional containing the member if found
     */
    public Optional<Member> getMember() {
        if (this.message.isFromGuild()) {
            return Optional.of(this.message.getMember());
        }
        return Optional.empty();
    }

    /**
     * Return the channel which the message was sent in
     *
     * @return Optional containing TextChannel of the message if found
     */
    public Optional<TextChannel> getTextChannel() {
        if (this.message.isFromGuild()) {
            return Optional.of(this.message.getTextChannel());
        }
        return Optional.empty();
    }

    /**
     * Get the channel the message was sent in
     *
     * @return MessageChannel of the message
     */
    public MessageChannel getMessageChannel() {
        return this.message.getChannel();
    }

    /**
     * Get the guild the message was sent in
     *
     * @return Guild
     */
    public Optional<Guild> getGuild() {
        if (this.message.isFromGuild()) {
            return Optional.of(this.message.getGuild());
        }
        return Optional.empty();
    }
}
