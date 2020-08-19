/*
 * The MIT License
 *
 * Copyright 2020 joonas.
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
package eternal.lemonadebot.customcommands;

import eternal.lemonadebot.CommandMatcher;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author joonas
 */
public class FakeMessageMatcher implements CommandMatcher {

    private static final Pattern PATTERN = Pattern.compile("^(\\S+) ?");
    private final CommandMatcher commandMatcher;
    private final String messageText;
    private final Matcher matcher;
    private final boolean matches;

    public FakeMessageMatcher(CommandMatcher originalMatcher, String fakeContent) {
        this.commandMatcher = originalMatcher;
        final String[] args = originalMatcher.getArguments(0);
        if (args.length == 0) {
            this.messageText = fakeContent;
        } else {
            this.messageText = fakeContent + ' ' + args[0];
        }
        this.matcher = PATTERN.matcher(this.messageText);
        this.matches = this.matcher.find();
    }

    @Override
    public Optional<String> getCommand() {
        if (this.matches) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    @Override
    public String[] getArguments(int count) {
        int parameterStart = matcher.end();

        final String parameterString = this.messageText.substring(parameterStart);
        return parameterString.split(" ", count + 1);
    }

    @Override
    public String getAction() {
        int argumentStart = matcher.start(1);
        return this.messageText.substring(argumentStart);
    }

    @Override
    public Guild getGuild() {
        return this.commandMatcher.getGuild();
    }

    @Override
    public Member getMember() {
        return this.commandMatcher.getMember();
    }

    @Override
    public String getMessageText() {
        return this.messageText;
    }

    @Override
    public TextChannel getTextChannel() {
        return this.commandMatcher.getTextChannel();
    }

    @Override
    public List<Member> getMentionedMembers() {
        return this.commandMatcher.getMentionedMembers();
    }

    @Override
    public List<Role> getMentionedRoles() {
        return this.commandMatcher.getMentionedRoles();
    }

    @Override
    public List<TextChannel> getMentionedChannels() {
        return this.commandMatcher.getMentionedChannels();
    }

}
