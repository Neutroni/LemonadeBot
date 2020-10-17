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

import eternal.lemonadebot.config.ConfigManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
public class MessageMatcher implements CommandMatcher {

    private static final Pattern SPLIT_PATTERN = Pattern.compile(" ");

    private final Message message;
    private final Locale locale;
    private final Optional<String> command;
    private final String arguments;
    private final String action;

    /**
     * Constructor
     *
     * @param configManager ConfigManager to get command prefix from
     * @param msg command message
     */
    public MessageMatcher(final ConfigManager configManager, final Message msg) {
        if (!msg.isFromGuild()) {
            throw new IllegalArgumentException("Only messages from guilds supported");
        }

        this.message = msg;
        this.locale = configManager.getLocale();
        final String messageText = msg.getContentRaw();
        final String commandPrefix = configManager.getCommandPrefix();
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
            this.action = "";
            this.arguments = "";
        }
    }

    @Override
    public String getAction() {
        return this.action;
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
        //For empty imput return empty collection
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
                        //Reset stringbuilder
                        current.setLength(0);
                        //Check if we have enough arguments
                        if (args.size() == limit) {
                            args.add(this.arguments.substring(i + 1, this.arguments.length()));
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
    public Member getMember() {
        return this.message.getMember();
    }

    @Override
    public TextChannel getTextChannel() {
        return this.message.getTextChannel();
    }

    @Override
    public Locale getLocale() {
        return this.locale;
    }

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

    @Override
    public List<TextChannel> getMentionedChannels() {
        return this.message.getMentionedChannels();
    }

}
