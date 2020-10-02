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

import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Matcher given to ActionTemplates when reminder activates
 *
 * @author Neutroni
 */
public class SimpleMessageMatcher implements CommandMatcher {

    private final Member author;
    private final TextChannel channel;

    /**
     * Constructor
     *
     * @param author reminder author
     * @param channel Channel reminder should be sent in
     */
    public SimpleMessageMatcher(Member author, TextChannel channel) {
        this.author = author;
        this.channel = channel;
    }

    @Override
    public Optional<String> getCommand() {
        return Optional.empty();
    }

    @Override
    public String getAction() {
        return "";
    }

    @Override
    public String[] getArguments(int count) {
        return new String[0];
    }

    @Override
    public Member getMember() {
        return this.author;
    }

    @Override
    public TextChannel getTextChannel() {
        return this.channel;
    }

    @Override
    public Guild getGuild() {
        return this.channel.getGuild();
    }

    @Override
    public List<Member> getMentionedMembers() {
        return List.of();
    }

    @Override
    public List<Role> getMentionedRoles() {
        return List.of();
    }

    @Override
    public List<TextChannel> getMentionedChannels() {
        return List.of();
    }
}
