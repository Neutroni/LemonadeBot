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
package eternal.lemonadebot.events;

import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.permissions.PermissionUtilities;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command used to manage events
 *
 * @author Neutroni
 */
public class EventCommand extends ChatCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(final ResourceBundle locale) {
        return locale.getString("COMMAND_EVENT");
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_EVENT");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        return locale.getString("SYNTAX_EVENT");
    }

    @Override
    public Collection<CommandPermission> getDefaultRanks(final ResourceBundle locale, final long guildID, final PermissionManager permissions) {
        return List.of(new CommandPermission(getCommand(locale), MemberRank.MEMBER, guildID));
    }

    @Override
    protected void respond(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel textChannel = matcher.getTextChannel();
        final TranslationCache translationCache = context.getTranslation();
        final ResourceBundle locale = translationCache.getResourceBundle();

        final String[] opts = matcher.getArguments(2);
        if (opts.length == 0) {
            textChannel.sendMessage(locale.getString("ERROR_MISSING_OPERATION")).queue();
            return;
        }

        final String action = opts[0];
        final ActionKey key = translationCache.getActionKey(action);
        switch (key) {
            case CREATE: {
                //event create(0) name(1) description(2)
                createEvent(opts, context);
                break;
            }
            case DELETE: {
                deleteEvent(opts, context);
                break;
            }
            case JOIN: {
                joinEvent(opts, context);
                break;
            }
            case LEAVE: {
                leaveEvent(opts, context);
                break;
            }
            case LIST_MEMBERS: {
                showEventMembers(opts, context);
                break;
            }
            case CLEAR: {
                clearEventMembers(opts, context);
                break;
            }
            case LIST: {
                listEvents(context);
                break;
            }
            case RANDOM: {
                pickRandomEventMember(opts, context);
                break;
            }
            case LOCK: {
                lockEvent(opts, context);
                break;
            }
            case UNLOCK: {
                unlockEvent(opts, context);
                return;
            }
            default: {
                textChannel.sendMessage(locale.getString("ERROR_UNKNOWN_OPERATION") + action).queue();
            }
        }
    }

    private static void createEvent(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final Member sender = matcher.getMember();
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();
        final GuildDataStore guildData = context.getGuildData();
        final EventManager events = guildData.getEventManager();
        if (opts.length < 2) {
            textChannel.sendMessage(locale.getString("EVENT_CREATE_MISSING_NAME")).queue();
            return;
        }
        final String eventName = opts[1];
        final String description;
        if (opts.length < 3) {
            description = null;
        } else {
            description = opts[2];
        }
        final Event newEvent = new Event(eventName, description, sender);

        try {
            if (!events.addEvent(newEvent)) {
                textChannel.sendMessage(locale.getString("EVENT_ALREADY_EXISTS")).queue();
                return;
            }
            if (!events.joinEvent(newEvent, sender)) {
                textChannel.sendMessage(locale.getString("EVENT_CREATE_JOIN_FAILED")).queue();
                return;
            }
            textChannel.sendMessage(locale.getString("EVENT_CREATE_SUCCESS")).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_CREATE")).queue();
            LOGGER.error("Failure to create event: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void deleteEvent(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final Member sender = matcher.getMember();
        final TextChannel textChannel = matcher.getTextChannel();
        final GuildDataStore guildData = context.getGuildData();
        final EventManager events = guildData.getEventManager();
        final ResourceBundle locale = context.getResource();
        if (opts.length < 2) {
            textChannel.sendMessage(locale.getString("EVENT_DELETE_MISSING_NAME")).queue();
            return;
        }
        final String eventName = opts[1];

        //Get the event to remove
        final Optional<Event> oldEvent;
        try {
            oldEvent = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_FINDING_EVENT")).queue();
            return;
        }
        if (oldEvent.isEmpty()) {
            textChannel.sendMessageFormat(locale.getString("EVENT_NOT_FOUND_WITH_NAME"), eventName).queue();
            return;
        }
        final Event event = oldEvent.get();
        textChannel.getGuild().retrieveMemberById(event.getOwner()).submit().whenComplete((Member eventOwner, Throwable error) -> {
            //Check if user has permission to remove the event
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, eventOwner);
            if (!hasPermission) {
                textChannel.sendMessage(locale.getString("EVENT_REMOVE_PERMISSION_DENIED")).queue();
                return;
            }
            try {
                events.removeEvent(event);
                textChannel.sendMessage(locale.getString("EVENT_REMOVED_SUCCESFULLY")).queue();
            } catch (SQLException ex) {
                textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_REMOVE")).queue();
                LOGGER.error("Failure to remove event: {}", ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });

    }

    private static void joinEvent(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();
        final Member sender = matcher.getMember();

        if (opts.length < 2) {
            textChannel.sendMessage(locale.getString("EVENT_JOIN_MISSING_NAME")).queue();
            return;
        }
        final String eventName = opts[1];
        final GuildDataStore guildData = context.getGuildData();
        final EventManager events = guildData.getEventManager();

        //Get the event to join
        final Optional<Event> oldEvent;
        try {
            oldEvent = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_FINDING_EVENT")).queue();
            return;
        }
        if (oldEvent.isEmpty()) {
            textChannel.sendMessageFormat(locale.getString("EVENT_NOT_FOUND_WITH_NAME"), eventName).queue();
            return;
        }

        //Check if event is locked
        final Event event = oldEvent.get();
        if (event.isLocked()) {
            textChannel.sendMessage(locale.getString("EVENT_JOIN_LOCKED")).queue();
            return;
        }

        try {
            if (events.joinEvent(event, sender)) {
                textChannel.sendMessage(locale.getString("EVENT_JOIN_SUCCESS")).queue();
                return;
            }
            textChannel.sendMessage(locale.getString("EVENT_JOIN_ALREADY_JOINED")).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_JOIN")).queue();
            LOGGER.error("Failure to join event: {}\n{}", eventName, ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void leaveEvent(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();
        final Member sender = matcher.getMember();

        if (opts.length < 2) {
            textChannel.sendMessage(locale.getString("EVENT_LEAVE_MISSING_NAME")).queue();
            return;
        }
        final String eventName = opts[1];
        final GuildDataStore guildData = context.getGuildData();
        final EventManager events = guildData.getEventManager();

        //Get the event to leave
        final Optional<Event> oldEvent;
        try {
            oldEvent = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_FINDING_EVENT")).queue();
            return;
        }
        if (oldEvent.isEmpty()) {
            textChannel.sendMessageFormat(locale.getString("EVENT_NOT_FOUND_WITH_NAME"), eventName).queue();
            return;
        }

        //Check if event is locked
        final Event event = oldEvent.get();
        if (event.isLocked()) {
            textChannel.sendMessage(locale.getString("EVENT_LEAVE_LOCKED")).queue();
            return;
        }

        //Leave the event
        try {
            if (events.leaveEvent(event, sender.getIdLong())) {
                textChannel.sendMessage(locale.getString("EVENT_LEAVE_SUCCESS")).queue();
                return;
            }
            textChannel.sendMessage(locale.getString("EVENT_LEAVE_ALREADY_LEFT")).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_LEAVE")).queue();
            LOGGER.error("Failure to leave event: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void showEventMembers(final String[] opts, final CommandContext context) {
        final TextChannel textChannel = context.getChannel();
        final ResourceBundle locale = context.getResource();
        final GuildDataStore guildData = context.getGuildData();

        if (opts.length < 2) {
            textChannel.sendMessage(locale.getString("EVENT_SHOW_MEMBERS_MISSING_NAME")).queue();
            return;
        }
        final String eventName = opts[1];
        final EventManager events = guildData.getEventManager();

        //Get the event
        final Optional<Event> opt;
        try {
            opt = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_FINDING_EVENT")).queue();
            return;
        }
        if (opt.isEmpty()) {
            textChannel.sendMessageFormat(locale.getString("EVENT_NOT_FOUND_WITH_NAME"), eventName).queue();
            return;
        }
        final Event event = opt.get();
        final List<Long> eventMemberIDs;
        try {
            eventMemberIDs = events.getMembers(event);
        } catch (SQLException e) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_LOADING_MEMBERS")).queue();
            return;
        }

        final String header = String.format(locale.getString("HEADER_EVENT_MEMBERS"), eventName);
        batchMemberList(eventMemberIDs).forEach((List<Long> idBatch) -> {
            textChannel.getGuild().retrieveMembersByIds(false, idBatch).onSuccess((List<Member> foundMembersList) -> {
                final EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle(header);

                //Add names of all event members who could be found
                final String mentions = foundMembersList.stream().map(IMentionable::getAsMention).collect(Collectors.joining(","));

                //Did not find any event members
                if (foundMembersList.isEmpty()) {
                    //If any batch returns empty this might appear without reason
                    eb.setDescription(locale.getString("EVENT_NO_MEMBERS"));
                } else {
                    eb.setDescription(mentions);
                }

                //Send the message
                textChannel.sendMessage(eb.build()).queue();

                //Remove missing members from event
                cleanEvent(event, idBatch, foundMembersList, events);
            });
        });

    }

    private static void clearEventMembers(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final GuildDataStore guildData = context.getGuildData();
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();
        final Member sender = matcher.getMember();

        if (opts.length < 2) {
            textChannel.sendMessage(locale.getString("EVENT_CLEAR_MISSING_NAME")).queue();
            return;
        }
        final String eventName = opts[1];
        final EventManager events = guildData.getEventManager();

        //Get the event
        final Optional<Event> opt;
        try {
            opt = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_FINDING_EVENT")).queue();
            return;
        }
        if (opt.isEmpty()) {
            textChannel.sendMessageFormat(locale.getString("EVENT_NOT_FOUND_WITH_NAME"), eventName).queue();
            return;
        }
        final Event event = opt.get();
        if (event.getOwner() != sender.getIdLong()) {
            textChannel.sendMessage(locale.getString("EVENT_CLEAR_PERMISSION_DENIED")).queue();
            return;
        }
        try {
            events.clearEvent(event);
            textChannel.sendMessage(locale.getString("EVENT_CLEAR_SUCCESS")).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_CLEAR")).queue();
            LOGGER.error("Failure to clear event: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void listEvents(final CommandContext context) {
        final TextChannel textChannel = context.getChannel();
        final ResourceBundle locale = context.getResource();
        final GuildDataStore guildData = context.getGuildData();
        final EventManager eventManager = guildData.getEventManager();

        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(locale.getString("HEADER_EVENTS"));

        //Get the list of events
        final Collection<Event> ev;
        try {
            ev = eventManager.getEvents();
        } catch (SQLException e) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_LOADING_EVENTS")).queue();
            return;
        }
        final List<CompletableFuture<String>> futures = new ArrayList<>(ev.size());
        ev.forEach((Event event) -> {
            futures.add(event.toListElement(locale, textChannel.getJDA()));
        });
        //After all the futures all initialized start waiting for results
        final StringBuilder contentBuilder = new StringBuilder();
        futures.forEach((CompletableFuture<String> desc) -> {
            contentBuilder.append(desc.join());
        });
        if (ev.isEmpty()) {
            contentBuilder.append(locale.getString("EVENT_NO_EVENTS"));
        }
        eb.setDescription(contentBuilder);
        textChannel.sendMessage(eb.build()).queue();
    }

    private static void pickRandomEventMember(final String[] opts, final CommandContext context) {
        final ResourceBundle locale = context.getResource();
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final GuildDataStore guildData = context.getGuildData();

        if (opts.length < 2) {
            channel.sendMessage(locale.getString("EVENT_PICK_RANDOM_MISSING_NAME")).queue();
            return;
        }
        final String eventName = opts[1];
        final Guild guild = matcher.getGuild();
        final EventManager events = guildData.getEventManager();

        try {
            final Optional<Member> optMember = events.getRandomMember(eventName, guild);
            optMember.ifPresentOrElse((Member member) -> {
                final String template = locale.getString("EVENT_SELECTED_MEMBER");
                channel.sendMessageFormat(template, member.getEffectiveName()).queue();
            }, () -> {
                channel.sendMessage(locale.getString("EVENT_NO_MEMBERS")).queue();
            });
        } catch (NoSuchElementException e) {
            //Could not find event with provided name
            final String template = locale.getString("EVENT_NOT_FOUND_WITH_NAME");
            channel.sendMessageFormat(template, eventName).queue();
        } catch (SQLException e) {
            //Database failed to retrieve event or members for event
            channel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_FINDING_EVENT")).queue();
        }
    }

    /**
     * Lock event
     *
     * @param opts Argument for event name to lock
     * @param matcher Matcher for request
     * @param guildData guildData for guild
     */
    private static void lockEvent(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();
        final GuildDataStore guildData = context.getGuildData();
        if (opts.length < 2) {
            channel.sendMessage(locale.getString("EVENT_LOCK_MISSING_NAME")).queue();
            return;
        }
        final String eventName = opts[1];
        final EventManager events = guildData.getEventManager();

        //Get the event to unlock
        final Optional<Event> oldEvent;
        try {
            oldEvent = events.getEvent(eventName);
        } catch (SQLException e) {
            channel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_FINDING_EVENT")).queue();
            return;
        }
        if (oldEvent.isEmpty()) {
            channel.sendMessageFormat(locale.getString("EVENT_NOT_FOUND_WITH_NAME"), eventName).queue();
            return;
        }
        final Event event = oldEvent.get();
        if (event.isLocked()) {
            channel.sendMessage(locale.getString("EVENT_ALREADY_LOCKED")).queue();
            return;
        }
        channel.getGuild().retrieveMemberById(event.getOwner()).submit().whenComplete((Member eventOwner, Throwable error) -> {
            //Check if user has permission to remove the event
            final Member sender = matcher.getMember();
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, eventOwner);
            if (!hasPermission) {
                channel.sendMessage(locale.getString("EVENT_LOCK_PERMISSION_DENIED")).queue();
                return;
            }
            try {
                events.lockEvent(event);
                channel.sendMessage(locale.getString("EVENT_LOCKED_SUCCESFULLY")).queue();
            } catch (SQLException ex) {
                channel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_LOCK")).queue();
                LOGGER.error("Failure to lock event: {}", ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });
    }

    /**
     * Unlock event
     *
     * @param opts event name as second element
     * @param matcher commandMatcher to get requester from
     * @param guildData guildData for guild to find event in
     */
    private static void unlockEvent(final String[] opts, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final Member sender = matcher.getMember();
        final TextChannel textChannel = matcher.getTextChannel();
        final GuildDataStore guildData = context.getGuildData();
        final EventManager events = guildData.getEventManager();
        final ResourceBundle locale = context.getResource();
        if (opts.length < 2) {
            textChannel.sendMessage(locale.getString("EVENT_UNLOCK_MISSING_NAME")).queue();
            return;
        }
        final String eventName = opts[1];

        //Get the event to unlock
        final Optional<Event> oldEvent;
        try {
            oldEvent = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_FINDING_EVENT")).queue();
            return;
        }
        if (oldEvent.isEmpty()) {
            textChannel.sendMessageFormat(locale.getString("EVENT_NOT_FOUND_WITH_NAME"), eventName).queue();
            return;
        }

        //Check if event is locked
        final Event event = oldEvent.get();
        if (!event.isLocked()) {
            textChannel.sendMessage(locale.getString("EVENT_ALREADY_UNLOCKED")).queue();
            return;
        }

        //Find the member of the event for permission check
        textChannel.getGuild().retrieveMemberById(event.getOwner()).submit().whenComplete((Member eventOwner, Throwable error) -> {
            //Check if user has permission to remove the event
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, eventOwner);
            if (!hasPermission) {
                textChannel.sendMessage(locale.getString("EVENT_UNLOCK_PERMISSION_DENIED")).queue();
                return;
            }
            try {
                events.unlockEvent(event);
                textChannel.sendMessage(locale.getString("EVENT_UNLOCKED_SUCCESFULLY")).queue();
            } catch (SQLException ex) {
                textChannel.sendMessage(locale.getString("EVENT_SQL_ERROR_ON_UNLOCK")).queue();
                LOGGER.error("Failure to unlock event: {}", ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });
    }

    /**
     * Used to split event members to groups of 100 to limit amount of members
     * retrieved per call
     *
     * @param source All the event member ids
     * @return stream of lists where each list has 100 or less ids
     */
    private static Stream<List<Long>> batchMemberList(final List<Long> source) {
        final int maxCount = 100;
        if (source.isEmpty()) {
            return Stream.empty();
        }
        final int size = source.size();
        final int fullChunks = (size - 1) / maxCount;
        return IntStream.range(0, fullChunks + 1).mapToObj(
                n -> source.subList(n * maxCount, n == fullChunks ? size : (n + 1) * maxCount));
    }

    /**
     * Used to remove members from event who are no longer in guild
     *
     * @param event Event to clean
     * @param memberIDList list of members who were attempted to retrieve
     * @param foundMembersList Members who could still be found
     * @param eventManager EventManager the event is from
     */
    private static void cleanEvent(final Event event, final List<Long> memberIDList, final List<Member> foundMembersList, final EventManager eventManager) {
        //Clear all the members from the event who could not be found
        memberIDList.stream().filter((Long eventMemberID) -> {
            //Get the IDs of event members who do not appear in the found members list
            return foundMembersList.stream().noneMatch((Member foundMember) -> {
                return foundMember.getIdLong() == eventMemberID;
            });
        }).forEach((Long missingMemberID) -> {
            LOGGER.info("Found user: {} in event: {} members who could not be found, removing from event", missingMemberID, event.getName());
            try {
                eventManager.leaveEvent(event, missingMemberID);
                LOGGER.info("Successfully removed missing member from event\n");
            } catch (SQLException ex) {
                LOGGER.error("Failure to remove member: {} from event: {}, Error: {}", missingMemberID, event.getName(), ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });
    }

}
