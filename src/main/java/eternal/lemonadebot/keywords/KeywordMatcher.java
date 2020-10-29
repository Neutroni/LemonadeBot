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
package eternal.lemonadebot.keywords;

import eternal.lemonadebot.messageparsing.CommandMatcher;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author Neutroni
 */
class KeywordMatcher implements CommandMatcher {

    private final CommandMatcher superMatcher;
    private final Member memberToRunAs;

    KeywordMatcher(CommandMatcher superMatcher, Member memberToRunAs) {
        this.superMatcher = superMatcher;
        this.memberToRunAs = memberToRunAs;
    }

    @Override
    public Optional<String> getCommand() {
        return this.superMatcher.getCommand();
    }

    @Override
    public String[] getArguments(int count) {
        return this.superMatcher.getArguments(count);
    }

    @Override
    public List<String> parseArguments(int count) {
        return this.superMatcher.parseArguments(count);
    }

    @Override
    public String getAction() {
        return this.superMatcher.getAction();
    }

    @Override
    public Guild getGuild() {
        return this.superMatcher.getGuild();
    }

    @Override
    public Member getMember() {
        return this.memberToRunAs;
    }

    @Override
    public TextChannel getTextChannel() {
        return this.superMatcher.getTextChannel();
    }

    @Override
    public List<Member> getMentionedMembers() {
        return this.superMatcher.getMentionedMembers();
    }

    @Override
    public List<TextChannel> getMentionedChannels() {
        return this.superMatcher.getMentionedChannels();
    }

}
