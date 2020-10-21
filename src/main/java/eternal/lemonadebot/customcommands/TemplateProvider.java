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

import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.events.EventManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
            new ActionTemplate("choice (.*(\\|.*)+)", TranslationKey.HELP_TEMPLATE_CHOICE,
                    (CommandMatcher message, GuildDataStore guildData, Matcher input) -> {
                        final String[] parts = input.group(1).split("\\|");
                        return parts[RNG.nextInt(parts.length)];
                    }),
            new ActionTemplate("rng (\\d+),(\\d+)", TranslationKey.HELP_TEMPLATE_RNG,
                    (CommandMatcher message, GuildDataStore guildData, Matcher input) -> {
                        final int start = Integer.parseInt(input.group(1));
                        final int end = Integer.parseInt(input.group(2));
                        return String.valueOf(RNG.nextInt(end + 1) + start);
                    }),
            new ActionTemplate("message", TranslationKey.HELP_TEMPLATE_MESSAGE,
                    (CommandMatcher message, GuildDataStore guildData, Matcher input) -> {
                        final String[] messageText = message.getArguments(0);
                        if (messageText.length == 0) {
                            return "";
                        }
                        return messageText[0];
                    }),
            new ActionTemplate("argument (\\d+),(\\d+)", TranslationKey.HELP_TEMPLATE_ARGUMENT,
                    (commandMatcher, guildData, templateMatcher) -> {
                        final int groups = Integer.parseUnsignedInt(templateMatcher.group(1));
                        final int n = Integer.parseUnsignedInt(templateMatcher.group(2));
                        final String[] args = commandMatcher.getArguments(groups - 1);
                        if (args.length > n) {
                            return args[n];
                        }
                        return "";
                    }),
            new ActionTemplate("messageText", TranslationKey.HELP_TEMPLATE_MESSAGE_TEXT,
                    (CommandMatcher message, GuildDataStore guildData, Matcher input) -> {
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
            new ActionTemplate("mentions", TranslationKey.HELP_TEMPLATE_MENTIONS,
                    (CommandMatcher matcher, GuildDataStore guildData, Matcher input) -> {

                        final List<Member> mentionedMembers = matcher.getMentionedMembers();
                        return mentionedMembers.stream().map(Member::getEffectiveName).collect(Collectors.joining(","));
                    }),
            new ActionTemplate("sender", TranslationKey.HELP_TEMPLATE_SENDER,
                    (CommandMatcher matcher, GuildDataStore guildData, Matcher input) -> {
                        return matcher.getMember().getEffectiveName();
                    }),
            new ActionTemplate("randomEventMember (\\S+)", TranslationKey.HELP_TEMPLATE_RANDOM_EVENT_MEMBER,
                    (CommandMatcher matcher, GuildDataStore guildData, Matcher input) -> {
                        final String eventName = input.group(1);
                        final EventManager eventManager = guildData.getEventManager();
                        final Guild guild = matcher.getGuild();
                        final Locale locale = guildData.getConfigManager().getLocale();

                        try {
                            final Optional<Member> optMember = eventManager.getRandomMember(eventName, guild);
                            if (optMember.isEmpty()) {
                                return TranslationKey.EVENT_NO_MEMBERS.getTranslation(locale);
                            }
                            final Member member = optMember.get();
                            return member.getEffectiveName();
                        } catch (NoSuchElementException e) {
                            //Could not find event with provided name
                            return String.format(TranslationKey.EVENT_NOT_FOUND_WITH_NAME.getTranslation(locale), eventName);
                        } catch (SQLException e) {
                            //Database failed to retrieve event or members for event
                            return TranslationKey.EVENT_SQL_ERROR_ON_FINDING_EVENT.getTranslation(locale);
                        }
                    }),
            new ActionTemplate("daysSince (\\d+-\\d+\\d+)", TranslationKey.HELP_TEMPLATE_DAYS_SINCE,
                    (commandMatcher, guildData, templateMatcher) -> {
                        final Locale locale = guildData.getConfigManager().getLocale();
                        final String dateString = templateMatcher.group(1);
                        try {
                            final LocalDate date = LocalDate.parse(dateString);
                            final Period period = Period.between(date, LocalDate.now());
                            final int dayAmount = Math.abs(period.getDays());
                            if (dayAmount == 1) {
                                return dayAmount + ' ' + TranslationKey.TIME_DAY.getTranslation(locale);
                            }
                            return dayAmount + ' ' + TranslationKey.TIME_DAYS.getTranslation(locale);
                        } catch (DateTimeParseException e) {
                            return TranslationKey.ERROR_UNKNOWN_DATE.getTranslation(locale) + dateString;
                        }
                    })
    );

    /**
     * Parse the template string
     *
     * @param message Message this action is a reply to
     * @param guildData GuildDataStore for current guild
     * @param action Template string for action
     * @return String response
     */
    public static CharSequence parseAction(final CommandMatcher message, final GuildDataStore guildData, final String action) {
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
                                final String response = s.getValue(message, guildData, m);
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
    public static String getHelp(final Locale locale) {
        final StringBuilder sb = new StringBuilder();
        for (final ActionTemplate action : actions) {
            sb.append(action.getHelp(locale)).append('\n');
        }
        return sb.toString();
    }

}
