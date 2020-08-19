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
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.CooldownManager;
import eternal.lemonadebot.database.EventManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.PermissionManager;
import eternal.lemonadebot.events.Event;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
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
            new ActionTemplate("choice (.*(\\|.*)+)", "{choice a|b} - Selects value separated by | randomly",
                    (CommandMatcher message, GuildDataStore guildData, Matcher input) -> {
                        final String[] parts = input.group(1).split("\\|");
                        final String response = parts[RNG.nextInt(parts.length)];
                        return response;
                    }),
            new ActionTemplate("rng (\\d+),(\\d+)", "{rng x,y} - Generate random number between the two inputs.",
                    (CommandMatcher message, GuildDataStore guildData, Matcher input) -> {
                        final int start = Integer.parseInt(input.group(1));
                        final int end = Integer.parseInt(input.group(2));
                        return "" + (RNG.nextInt(end) + start);
                    }),
            new ActionTemplate("message", "{message} Use the input as part of the reply",
                    (CommandMatcher message, GuildDataStore guildData, Matcher input) -> {
                        final String[] messageText = message.getArguments(0);
                        if (messageText.length == 0) {
                            return "";
                        }
                        return messageText[0];
                    }),
            new ActionTemplate("messageText", "{messageText} - Use the input as part of the reply but remove mentions",
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
            new ActionTemplate("mentions", "{mentions} - Lists the mentioned users",
                    (CommandMatcher matcher, GuildDataStore guildData, Matcher input) -> {

                        final List<Member> mentionedMembers = matcher.getMentionedMembers();
                        return mentionedMembers.stream().map((Member member) -> {
                            return member.getEffectiveName();
                        }).collect(Collectors.joining(","));
                    }),
            new ActionTemplate("sender", "{sender} - The name of the command sender",
                    (CommandMatcher matcher, GuildDataStore guildData, Matcher input) -> {
                        return matcher.getMember().getEffectiveName();
                    }),
            new ActionTemplate("randomEventMember (\\S+)", "{randomEventMember <eventname>} - Pick random member from event",
                    (CommandMatcher matcher, GuildDataStore guildData, Matcher input) -> {
                        final Guild guild = matcher.getGuild();

                        final String eventName = input.group(1);
                        final EventManager eventManager = guildData.getEventManager();
                        final Optional<Event> optEvent = eventManager.getEvent(eventName);
                        if (optEvent.isEmpty()) {
                            return "No such event: " + eventName;
                        }

                        final Event event = optEvent.get();
                        final List<Long> eventMemberIDs = event.getMembers();
                        if (eventMemberIDs.isEmpty()) {
                            return "No members in that event";
                        }
                        final List<Long> memberIDsMutable = new ArrayList<>(eventMemberIDs);
                        Collections.shuffle(memberIDsMutable);
                        for (final Long l : memberIDsMutable) {
                            final Member m = guild.retrieveMemberById(l).complete();
                            if (m != null) {
                                return m.getEffectiveName();
                            }
                        }
                        return "Could not find members for that event";
                    }),
            new ActionTemplate("\\{command (\\.+)\\}", "{command <command>} - Run a command before the custom command",
                    (commandMatcher, guildData, templateMatcher) -> {
                        final String inputString = templateMatcher.group(1);
                        final CommandMatcher fakeMatcher = new FakeMessageMatcher(commandMatcher, inputString);
                        final Optional<? extends ChatCommand> optCommand = CommandProvider.getAction(fakeMatcher, guildData);
                        if (optCommand.isEmpty()) {
                            return "Could not find command with input: " + inputString;
                        }
                        final ChatCommand command = optCommand.get();
                        if (command instanceof CustomCommand) {
                            final CustomCommand custom = (CustomCommand) command;
                            if (custom.getTemplate().contains("{command ")) {
                                return "Custom command cannot run a command that calls another command";
                            }
                        }
                        final Member member = commandMatcher.getMember();
                        final PermissionManager permissions = guildData.getPermissionManager();
                        if (permissions.hasPermission(member, inputString)) {
                            return "Insufficient permississions to run that command";
                        }

                        final CooldownManager cdm = guildData.getCooldownManager();
                        final Optional<String> optCooldown = cdm.updateActivationTime(inputString);
                        if (optCooldown.isPresent()) {
                            final String currentCooldown = optCooldown.get();
                            return "Command on cooldown, time remaining: " + currentCooldown + '.';
                        }

                        command.respond(fakeMatcher, guildData);
                        return "";
                    }),
            new ActionTemplate("\\{daysSince (\\d+-\\d+\\d+)\\}", "{daysSince <date>} - Days since date specified according to ISO-8601",
                    (commandMatcher, guildData, templateMatcher) -> {
                        final String dateString = templateMatcher.group(1);
                        try {
                            final LocalDate date = LocalDate.parse(dateString);
                            final Period period = Period.between(date, LocalDate.now());
                            return Math.abs(period.getDays()) + " days";
                        } catch (DateTimeParseException e) {
                            return "Unknown date: " + dateString;
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
        if (action.length() == 0) {
            return "";
        }

        //Construct stack to hold the parsed responses
        final Deque<StringBuilder> stack = new ArrayDeque<>();

        //Initialize the stack if needed
        if (action.charAt(0) != '{') {
            stack.addFirst(new StringBuilder(action.length()));
        }

        //Parse the action
        for (int i = 0; i < action.length(); i++) {
            final char c = action.charAt(i);
            switch (c) {
                case '{':
                    stack.addFirst(new StringBuilder());
                    break;
                case '}':
                    final StringBuilder sb = stack.removeFirst();
                    boolean foundMatch = false;
                    for (final ActionTemplate s : actions) {
                        final Matcher m = s.getMatcher(sb);
                        if (m.matches()) {
                            foundMatch = true;
                            final String response = s.getValue(message, guildData, m);
                            stack.peekFirst().append(response);
                            break;
                        }
                    }
                    if (!foundMatch) {
                        stack.peekFirst().append(sb);
                    }
                    break;
                default:
                    stack.peekFirst().append(c);
                    break;
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
     * Gets the help text for simpleactions
     *
     * @return help text
     */
    public static String getHelp() {
        final StringBuilder sb = new StringBuilder();
        for (final ActionTemplate action : actions) {
            sb.append(action.getHelp()).append('\n');
        }
        return sb.toString();
    }

}