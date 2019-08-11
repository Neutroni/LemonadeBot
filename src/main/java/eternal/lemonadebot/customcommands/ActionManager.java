/*
 * The MIT License
 *
 * Copyright 2019 Neutroni.
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
    private final String[] COIN_SIDES = new String[]{"heads", "tails"};

    private final List<SimpleAction> actions = List.of(
            new SimpleAction("{coin}", "Flips a coin", (Message message, String input) -> {
                return COIN_SIDES[rng.nextInt(2)];
            }),
            new SimpleAction("{d6}", "Rolls a dice", (Message message, String input) -> {
                final int roll = rng.nextInt(6) + 1;
                return "" + roll;
            }),
            new SimpleAction("{d20}", "Rolls a d20", (Message message, String input) -> {
                final int roll = rng.nextInt(20) + 1;
                return "" + roll;
            }),
            new SimpleAction("{mentions}", "Lists the mentioned users", (Message message, String input) -> {
                if (!message.isFromType(ChannelType.TEXT)) {
                    return "";
                }
                final List<Member> mentionedMembers = message.getMentionedMembers();
                final Member self = message.getGuild().getSelfMember();
                final List<Member> mentions = new ArrayList<>(mentionedMembers);
                mentions.remove(self);

                final StringBuilder mentionMessage = new StringBuilder();
                for (int i = 0; i < mentions.size(); i++) {
                    final String nickName = mentions.get(i).getNickname();
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
    public String processActions(Message message, String action) {
        final String[] parts = action.split("\\|");
        String response = parts[rng.nextInt(parts.length)];

        //Process simple actions
        for (SimpleAction s : actions) {
            final Matcher m = s.getMatcher(response);
            final StringBuilder sb = new StringBuilder();

            while (m.find()) {
                final String replacement = s.getValue(message, m.group());
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
            sb.append(action.getKey()).append(" - ").append(action.getHelp()).append('\n');
        }
        return sb.toString();
    }

}
