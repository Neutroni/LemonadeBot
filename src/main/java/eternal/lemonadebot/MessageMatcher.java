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

import eternal.lemonadebot.database.ConfigManager;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Matches given message for command pattern
 *
 * @author Neutroni
 */
class MessageMatcher implements CommandMatcher {

    private final Message message;
    private final String messageText;
    private final Matcher matcher;
    private final boolean matches;

    /**
     * Constructor
     *
     * @param guildDataStore datastore for guild
     * @param msg command message
     */
    MessageMatcher(final ConfigManager configManager, final Message msg) {
        if (!msg.isFromGuild()) {
            throw new IllegalArgumentException("Only messages from guilds supported");
        }

        this.message = msg;
        this.messageText = msg.getContentRaw();
        final Pattern pattern = configManager.getCommandPattern();
        this.matcher = pattern.matcher(this.messageText);
        this.matches = this.matcher.find();
    }

    /**
     * Get the command from the match
     *
     * @return optional containing command string if found
     */
    @Override
    public Optional<String> getCommand() {
        if (this.matches) {
            return Optional.of(matcher.group(1));
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
    @Override
    public String[] getArguments(int count) {
        int parameterStart = matcher.end();

        final String parameterString = this.messageText.substring(parameterStart);
        return parameterString.split(" ", count + 1);
    }

    /**
     * Get the action from this matcher including the command but excluding the
     * command prefix
     *
     * @return Action String
     */
    @Override
    public String getAction() {
        int argumentStart = matcher.start(1);
        return this.messageText.substring(argumentStart);
    }

    /**
     * Get member who sent the message
     *
     * @return Member who sent the message
     */
    @Override
    public Member getMember() {
        return this.message.getMember();
    }

    /**
     * Get text content of the message
     *
     * @return Same as message.getContentRaw()
     */
    @Override
    public String getMessageText() {
        return this.messageText;
    }

    /**
     * Return the channel which the message was sent in
     *
     * @return TextChannel of the message
     */
    @Override
    public TextChannel getTextChannel() {
        return this.message.getTextChannel();
    }

    /**
     * Get the guild the message was sent in
     *
     * @return Guild
     */
    @Override
    public Guild getGuild() {
        return this.message.getGuild();
    }

    @Override
    public List<Member> getMentionedMembers() {
        return this.message.getMentionedMembers();
    }

    @Override
    public List<Role> getMentionedRoles() {
        return this.message.getMentionedRoles();
    }

}
