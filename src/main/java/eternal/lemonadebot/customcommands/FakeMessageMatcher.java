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
package eternal.lemonadebot.customcommands;

import eternal.lemonadebot.CommandMatcher;
import java.util.ArrayList;
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
class FakeMessageMatcher implements CommandMatcher {

    private static final Pattern COMMAND_PATTERN = Pattern.compile("^(?:<@!\\d+> *)*(\\w+) ?");
    private final Member author;
    private final TextChannel channel;
    private final String messageText;
    private final Matcher matcher;
    private final boolean matches;

    /**
     * Constructor for aliased command invocations
     *
     * @param orignalMatcher Matcher that originally handled the message
     * @param fakeContent content to override the matchers content with
     */
    FakeMessageMatcher(CommandMatcher orignalMatcher, String fakeContent) {
        this.author = orignalMatcher.getMember();
        this.channel = orignalMatcher.getTextChannel();
        this.messageText = fakeContent;
        this.matcher = COMMAND_PATTERN.matcher(fakeContent);
        this.matches = this.matcher.find();
    }

    /**
     * Constructor for remainder command invocations
     *
     * @param author message author
     * @param channel Channel message was sent int
     * @param fakeContent Message content
     */
    FakeMessageMatcher(Member author, TextChannel channel, String fakeContent) {
        this.author = author;
        this.channel = channel;
        this.messageText = fakeContent;
        this.matcher = COMMAND_PATTERN.matcher(fakeContent);
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
     * Get the arguments from this matcher including the command
     *
     * @return String of arguments
     */
    @Override
    public String getArgumentString() {
        int argumentStart = matcher.start(1);
        return this.messageText.substring(argumentStart);
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

        //Check if message ends at match end
        if (parameterStart == this.messageText.length()) {
            return new String[0];
        }

        final String parameterString = this.messageText.substring(parameterStart);
        return parameterString.split(" ", count + 1);
    }

    /**
     * Get member who sent the message
     *
     * @return Member who sent the message
     */
    @Override
    public Member getMember() {
        return this.author;
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
        return this.channel;
    }

    /**
     * Get the guild the message was sent in
     *
     * @return Guild
     */
    @Override
    public Guild getGuild() {
        return this.channel.getGuild();
    }

    @Override
    public List<Member> getMentionedMembers() {
        final Pattern memberPattern = Message.MentionType.USER.getPattern();
        final Matcher m = memberPattern.matcher(this.messageText);
        final List<Member> members = new ArrayList<>();
        while (m.find()) {
            final String memberID = m.group(1);
            final Member member = this.channel.getGuild().getMemberById(memberID);
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    @Override
    public List<Role> getMentionedRoles() {
        final Pattern memberPattern = Message.MentionType.ROLE.getPattern();
        final Matcher m = memberPattern.matcher(this.messageText);
        final List<Role> roles = new ArrayList<>();
        while (m.find()) {
            final String roleID = m.group(1);
            final Role r = this.channel.getGuild().getRoleById(roleID);
            if (r != null) {
                roles.add(r);
            }
        }
        return roles;
    }
    
    @Override
    public String getMessageStripMentions() {
        String response = this.messageText;
        for(final Message.MentionType mt: Message.MentionType.values()){
            final Pattern pattern = mt.getPattern();
            final Matcher mentionMatcher = pattern.matcher(response);
            final StringBuilder sb = new StringBuilder();

            while (mentionMatcher.find()) {
                mentionMatcher.appendReplacement(sb, "");
            }
            mentionMatcher.appendTail(sb);
            response = sb.toString();
        }
        
        return  response;
    }
}
