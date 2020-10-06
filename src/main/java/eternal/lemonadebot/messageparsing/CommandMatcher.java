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
package eternal.lemonadebot.messageparsing;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author Neutroni
 */
public interface CommandMatcher {

    /**
     * Get the command from the match
     *
     * @return optional containing command string if found
     */
    Optional<String> getCommand();

    /**
     * Get parameters limited by whitespace and the rest of the message as last
     * entry in returned array.
     *
     * Examples: String "foo bar baz"
     * count 1 {"foo","bar baz"}
     * count 0 {"foo bar baz"}
     * count -1 {"foo","bar","baz"]
     *
     * @param count number of parameters to return
     * @return array of parameters
     */
    String[] getArguments(int count);

    /**
     * Parse arguments from command, Collection will contain at most count
     * elements and the last element will contain all the input beyond the
     * count - 1 arguments input beyond n - 1 arguments will not be validated.
     *
     * @param count Maximum size of collection
     * @return Collection of argument strings
     * @throws IllegalArgumentException If maxArguments is negative
     */
    List<String> parseArguments(int count);

    /**
     * Get the action for this command
     *
     * @return String of command name and arguments
     */
    String getAction();

    /**
     * Get the locale at the time for the guild message was sent in.
     *
     * @return Locale
     */
    Locale getLocale();

    /**
     * Get the guild the message was sent in
     *
     * @return Same as message.getGuild()
     */
    Guild getGuild();

    /**
     * Get member who sent the message
     *
     * @return Same as message.getMember()
     */
    Member getMember();

    /**
     * Return the channel which the message was sent in
     *
     * @return same as message.getTextChannel()
     */
    TextChannel getTextChannel();

    /**
     * Return the list of members mentioned in the message
     *
     * @return same as message.getMentinedMembers()
     */
    List<Member> getMentionedMembers();

    /**
     * Return list of roles mentioned in the message
     *
     * @return Same as message.getMentionedRoles()
     */
    List<Role> getMentionedRoles();

    /**
     * Return list of channels mentions in the message
     *
     * @return Same as message.getMentionedChannels()
     */
    List<TextChannel> getMentionedChannels();

}
