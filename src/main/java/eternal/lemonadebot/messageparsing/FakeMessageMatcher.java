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
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * CommandMatcher for custom commands that call other commands
 *
 * @author Neutroni
 */
public class FakeMessageMatcher implements CommandMatcher {

    private static final Pattern SPLIT_PATTERN = Pattern.compile(" ");

    private final CommandMatcher commandMatcher;
    private final Optional<String> command;
    private final String arguments;
    private final String action;

    /**
     * Constructor
     *
     * @param originalMatcher commandmatcher of the original custom command call
     * @param fakeContent content of the custom command call
     */
    public FakeMessageMatcher(CommandMatcher originalMatcher, String fakeContent) {
        this.commandMatcher = originalMatcher;
        final String[] args = originalMatcher.getArguments(0);
        final String messageText;
        if (args.length == 0) {
            messageText = fakeContent;
        } else {
            messageText = fakeContent + ' ' + args[0];
        }

        final String commandPrefix = "!";
        if (messageText.startsWith(commandPrefix)) {
            //Command name starts at the character after prefix
            final int actionStart = commandPrefix.length();
            this.action = messageText.substring(actionStart);
            //Command name ends at either before space or at the end of message
            final int i = this.action.indexOf(' ');
            if (i == -1) {
                //Message does not contain arguments
                this.command = Optional.of(this.action);
                this.arguments = "";
            } else {
                //Message has at least a space after command name
                this.command = Optional.of(this.action.substring(0, i));
                this.arguments = this.action.substring(i + 1);
            }
        } else {
            //Not a command
            this.command = Optional.empty();
            this.action = null;
            this.arguments = null;
        }
    }

    @Override
    public Optional<String> getCommand() {
        return this.command;
    }

    @Override
    public String[] getArguments(int count) {
        return SPLIT_PATTERN.split(this.arguments, count + 1);
    }

    @Override
    public String getAction() {
        return this.action;
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