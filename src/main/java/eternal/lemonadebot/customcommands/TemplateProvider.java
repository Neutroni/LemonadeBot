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

import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.events.EventManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

/**
 * Class that holds templates for custom commands and how to substitute given
 * template with its actual content
 *
 * @author Neutroni
 */
public class TemplateProvider {

    private static final Random RNG = new Random();
    private static final List<ActionTemplate> actions = List.of(
            new ActionTemplate("choice (.*(\\|.*)+)", "HELP_TEMPLATE_CHOICE",
                    (CommandContext context, Matcher input) -> {
                        final String[] parts = input.group(1).split("\\|");
                        return parts[RNG.nextInt(parts.length)];
                    }),
            new ActionTemplate("rng (\\d+),(\\d+)", "HELP_TEMPLATE_RNG",
                    (CommandContext context, Matcher input) -> {
                        final int start = Integer.parseInt(input.group(1));
                        final int end = Integer.parseInt(input.group(2));
                        return String.valueOf(RNG.nextInt(end + 1) + start);
                    }),
            new ActionTemplate("message", "HELP_TEMPLATE_MESSAGE",
                    (CommandContext context, Matcher input) -> {
                        final CommandMatcher message = context.getMatcher();
                        final String[] messageText = message.getArguments(0);
                        if (messageText.length == 0) {
                            return "";
                        }
                        return messageText[0];
                    }),
            new ActionTemplate("argument (\\d+),(\\d+)", "HELP_TEMPLATE_ARGUMENT",
                    (CommandContext context, Matcher templateMatcher) -> {
                        final CommandMatcher commandMatcher = context.getMatcher();
                        final int groups = Integer.parseUnsignedInt(templateMatcher.group(1));
                        final int n = Integer.parseUnsignedInt(templateMatcher.group(2));
                        final String[] args = commandMatcher.getArguments(groups - 1);
                        if (args.length > n) {
                            return args[n];
                        }
                        return "";
                    }),
            new ActionTemplate("messageText", "HELP_TEMPLATE_MESSAGE_TEXT",
                    (CommandContext context, Matcher input) -> {
                        final CommandMatcher message = context.getMatcher();
                        final String[] messageText = message.getArguments(0);
                        if (messageText.length == 0) {
                            return "";
                        }
                        String response = messageText[0];
                        for (final Message.MentionType mt : Message.MentionType.values()) {
                            final Pattern pattern = mt.getPattern();
                            final Matcher matcher = pattern.matcher(response);
                            final StringBuilder sb = new StringBuilder();

                            while (matcher.find()) {
                                matcher.appendReplacement(sb, "");
                            }
                            matcher.appendTail(sb);
                            response = sb.toString();
                        }
                        return response.trim();
                    }),
            new ActionTemplate("mentions", "HELP_TEMPLATE_MENTIONS",
                    (CommandContext context, Matcher input) -> {
                        final CommandMatcher matcher = context.getMatcher();
                        final List<Member> mentionedMembers = matcher.getMentionedMembers();
                        return mentionedMembers.stream().map(Member::getEffectiveName).collect(Collectors.joining(","));
                    }),
            new ActionTemplate("sender", "HELP_TEMPLATE_SENDER",
                    (CommandContext context, Matcher input) -> {
                        final CommandMatcher matcher = context.getMatcher();
                        return matcher.getMember().getEffectiveName();
                    }),
            new ActionTemplate("randomEventMember (\\S+)", "HELP_TEMPLATE_RANDOM_EVENT_MEMBER",
                    (CommandContext context, Matcher input) -> {
                        final String eventName = input.group(1);
                        final CommandMatcher matcher = context.getMatcher();
                        final DataSource ds = context.getStorageManager().getDataSource();
                        final Guild guild = matcher.getGuild();
                        final ResourceBundle locale = context.getTranslation().getResourceBundle();

                        try {
                            final Optional<Member> optMember = EventManager.getRandomMember(ds, eventName, guild);
                            if (optMember.isEmpty()) {
                                return locale.getString("EVENT_NO_MEMBERS");
                            }
                            final Member member = optMember.get();
                            return member.getEffectiveName();
                        } catch (NoSuchElementException e) {
                            //Could not find event with provided name
                            return String.format(locale.getString("EVENT_NOT_FOUND_WITH_NAME"), eventName);
                        } catch (SQLException e) {
                            //Database failed to retrieve event or members for event
                            return locale.getString("EVENT_SQL_ERROR_ON_FINDING_EVENT");
                        }
                    }),
            new ActionTemplate("daysSince (\\d+-\\d+\\d+)", "HELP_TEMPLATE_DAYS_SINCE",
                    (CommandContext context, Matcher templateMatcher) -> {
                        final ResourceBundle locale = context.getTranslation().getResourceBundle();
                        final String dateString = templateMatcher.group(1);
                        try {
                            final LocalDate date = LocalDate.parse(dateString);
                            final Period period = Period.between(date, LocalDate.now());
                            final int dayAmount = Math.abs(period.getDays());
                            if (dayAmount == 1) {
                                return dayAmount + ' ' + locale.getString("TIME_DAY");
                            }
                            return dayAmount + ' ' + locale.getString("TIME_DAYS");
                        } catch (DateTimeParseException e) {
                            return locale.getString("ERROR_UNKNOWN_DATE") + dateString;
                        }
                    })
    );

    /**
     * Parse the template string
     *
     * @param context Message this action is a reply to
     * @param action Template string for action
     * @return String response
     */
    public static CharSequence parseAction(final CommandContext context, final String action) {
        //Check that action is not empty string
        if (action.isEmpty()) {
            return "";
        }

        //Construct stack to hold the parsed responses
        final Deque<StringBuilder> stack = new ArrayDeque<>();

        //Initialize the stack
        stack.addFirst(new StringBuilder(action.length()));

        //Parse the action
        boolean escaped = false;
        for (int i = 0; i < action.length(); i++) {
            final char c = action.charAt(i);

            //Check if character is escaped
            if (escaped) {
                final StringBuilder sb = stack.peekFirst();
                if (sb == null) {
                    final StringBuilder newBuilder = new StringBuilder();
                    newBuilder.append(c);
                    stack.addFirst(newBuilder);
                } else {
                    sb.append(c);
                }
                escaped = false;
                continue;
            }

            //Check what character was found
            switch (c) {
                case '\\': {
                    escaped = true;
                    break;
                }
                case '{': {
                    stack.addFirst(new StringBuilder());
                    break;
                }
                case '}': {
                    final StringBuilder sb = stack.pollFirst();
                    if (sb == null) {
                        //No StringBuilders on stack
                        final StringBuilder newBuilder = new StringBuilder();
                        newBuilder.append(c);
                        stack.addFirst(newBuilder);
                    } else {
                        //Try to match any action against the input
                        boolean foundMatch = false;
                        for (final ActionTemplate s : actions) {
                            final Matcher m = s.getMatcher(sb);
                            if (m.matches()) {
                                foundMatch = true;
                                final String response = s.getValue(context, m);
                                final StringBuilder newBuilder = stack.peekFirst();
                                if (newBuilder == null) {
                                    stack.addFirst(new StringBuilder(response));
                                } else {
                                    newBuilder.append(response);
                                }
                                break;
                            }
                        }
                        //Did not find a match, add input to the stack
                        if (!foundMatch) {
                            final StringBuilder newBuilder = stack.peekFirst();
                            if (newBuilder == null) {
                                stack.addFirst(sb);
                            } else {
                                newBuilder.append(sb);
                            }
                        }
                    }
                    break;
                }
                default: {
                    final StringBuilder sb = stack.peekFirst();
                    if (sb == null) {
                        final StringBuilder newBuilder = new StringBuilder();
                        newBuilder.append(c);
                        stack.addFirst(newBuilder);
                    } else {
                        sb.append(c);
                    }
                    break;
                }
            }
        }
        final StringBuilder parsed = stack.removeLast();
        final Iterator<StringBuilder> it = stack.descendingIterator();
        while (it.hasNext()) {
            parsed.append(it.next());
        }
        return parsed;
    }

    /**
     * Gets the help text for simpleActions
     *
     * @param locale Locale to get the help text in
     * @return help text
     */
    public static String getHelp(final ResourceBundle locale) {
        final StringBuilder sb = new StringBuilder();
        actions.forEach(action -> {
            sb.append(action.getHelp(locale)).append('\n');
        });
        return sb.toString();
    }

}
