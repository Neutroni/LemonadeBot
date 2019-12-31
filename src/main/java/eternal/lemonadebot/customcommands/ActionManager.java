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

import eternal.lemonadebot.messages.CommandMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

/**
 * Class that holds actions for custom commands and how to substitute given
 * templace with its actual content
 *
 * @author Neutroni
 */
public class ActionManager {

    private final Random rng = new Random();

    private final List<SimpleAction> actions = List.of(
            new SimpleAction("\\{choice (.*(\\|.*)+)\\}", "{choice a|b} - Selects value separated by | randomly", (CommandMatcher message, Matcher input) -> {
                final String[] parts = input.group(1).split("\\|");
                final String response = parts[rng.nextInt(parts.length)];
                return "" + response;
            }),
            new SimpleAction("\\{rng (\\d+),(\\d+)\\}", "{rng x,y} - Generate random number between the two inputs.", (CommandMatcher t, Matcher u) -> {
                final int start = Integer.parseInt(u.group(1));
                final int end = Integer.parseInt(u.group(2));
                return "" + (rng.nextInt(end) + start);
            }),
            new SimpleAction("\\{message\\}", "{message} Use the input as part of the reply", (CommandMatcher message, Matcher input) -> {
                final String[] messageText = message.getArguments(1);
                if (messageText.length == 0) {
                    return "";
                }
                return messageText[0];
            }),
            new SimpleAction("\\{mentions\\}", "{mentions} - Lists the mentioned users", (CommandMatcher matcher, Matcher input) -> {
                final Message message = matcher.getMessage();
                if (!message.isFromType(ChannelType.TEXT)) {
                    return "";
                }
                final List<Member> mentionedMembers = message.getMentionedMembers();
                final Member self = message.getGuild().getSelfMember();
                final List<Member> mentions = new ArrayList<>(mentionedMembers);
                mentions.remove(self);

                final StringBuilder mentionMessage = new StringBuilder();
                for (int i = 0; i < mentions.size(); i++) {
                    final String nickName = mentions.get(i).getEffectiveName();
                    mentionMessage.append(nickName);
                    if (i < mentionedMembers.size() - 1) {
                        mentionMessage.append(' ');
                    }
                }
                return mentionMessage.toString();
            }));

    /**
     * Gets a response to a message
     *
     * @param message Message that activated the actions
     * @param action Action template string
     * @return Response string
     */
    public CharSequence processActions(CommandMatcher message, String action) {
        String response = action;

        //Process simple actions
        for (SimpleAction s : actions) {
            //Check if 
            final Matcher m = s.getMatcher(response);
            final StringBuilder sb = new StringBuilder();

            while (m.find()) {

                final String replacement = s.getValue(message, m);
                m.appendReplacement(sb, replacement);
            }
            m.appendTail(sb);
            response = sb.toString();
        }
        return response;
    }

    /**
     * Gets the help text for simpleactions
     *
     * @return help text
     */
    public String getHelp() {
        final StringBuilder sb = new StringBuilder();
        for (SimpleAction action : this.actions) {
            sb.append(action.getHelp()).append('\n');
        }
        return sb.toString();
    }

}
