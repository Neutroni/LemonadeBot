/*
 * The MIT License
 *
 * Copyright 2021 Neutroni.
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

import eternal.lemonadebot.messageparsing.CommandMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

/**
 *
 * @author Neutroni
 */
public class ReactionMatcher implements CommandMatcher {

    private static final Pattern SPLIT_PATTERN = Pattern.compile(" ");

    private final Optional<String> command;
    private final String action;
    private final String arguments;
    private final GenericGuildMessageReactionEvent event;

    public ReactionMatcher(final String command, final GenericGuildMessageReactionEvent event) {
        this.event = event;
        this.action = command;
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

    }

    @Override
    public Optional<String> getCommand() {
        return this.command;
    }

    @Override
    public String[] getArguments(int count) {
        //If there is not arguments return empty array
        if (this.arguments.isEmpty()) {
            return new String[0];
        }
        //Otherwise split the argument string
        return SPLIT_PATTERN.split(this.arguments, count + 1);
    }

    @Override
    public List<String> parseArguments(int maxArguments) {
        //Check to make sure maxArguments is positive
        if (maxArguments < 0) {
            throw new IllegalArgumentException("Error parsing arguments, maxArguments can not be negative.");
        }
        //For empty input return empty collection
        final List<String> args = new ArrayList<>();
        if (this.arguments.isEmpty()) {
            return args;
        }
        //If user only asks for one element, just add the input and return
        if (maxArguments == 1) {
            args.add(this.arguments);
            return args;
        }
        //limit is one less than maxArguments so we can rest of input as last element
        final int limit = maxArguments - 1;
        final StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = 0; i < this.arguments.length(); i++) {
            final char c = this.arguments.charAt(i);
            switch (c) {
                case '"': {
                    if (escaped) {
                        current.append(c);
                        escaped = false;
                    } else {
                        inQuotes = !inQuotes;
                    }
                    break;
                }
                case ' ': {
                    escaped = false;
                    if (inQuotes) {
                        current.append(c);
                    } else {
                        args.add(current.toString());
                        //Reset StringBuilder
                        current.setLength(0);
                        //Check if we have enough arguments
                        if (args.size() == limit) {
                            args.add(this.arguments.substring(i + 1));
                            return args;
                        }
                    }
                    break;
                }
                case '\\': {
                    if (escaped) {
                        current.append(c);
                        escaped = false;
                    } else {
                        escaped = true;
                    }
                    break;
                }
                default: {
                    escaped = false;
                    current.append(c);
                }
            }
        }
        args.add(current.toString());
        return args;
    }

    @Override
    public String getAction() {
        return this.action;
    }

    @Override
    public Guild getGuild() {
        return this.event.getGuild();
    }

    @Override
    public Member getMember() {
        return this.event.retrieveMember().complete();
    }

    @Override
    public TextChannel getTextChannel() {
        return this.event.getChannel();
    }

    @Override
    public List<Member> getMentionedMembers() {
        return List.of();
    }

    @Override
    public List<TextChannel> getMentionedChannels() {
        return List.of();
    }

}
