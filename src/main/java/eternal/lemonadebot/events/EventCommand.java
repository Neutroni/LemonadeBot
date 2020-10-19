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
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.permissions.PermissionUtilities;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
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
public class EventCommand implements ChatCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(final Locale locale) {
        return TranslationKey.COMMAND_EVENT.getTranslation(locale);
    }

    @Override
    public String getDescription(final Locale locale) {
        return TranslationKey.DESCRIPTION_EVENT.getTranslation(locale);
    }

    @Override
    public String getHelpText(final Locale locale) {
        return TranslationKey.SYNTAX_EVENT.getTranslation(locale);
    }

    @Override
    public Collection<CommandPermission> getDefaultRanks(final Locale locale, final long guildID, final PermissionManager permissions) {
        return List.of(new CommandPermission(getCommand(locale), MemberRank.MEMBER, guildID));
    }

    @Override
    public void respond(final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildConf.getLocale();

        final String[] opts = matcher.getArguments(2);
        if (opts.length == 0) {
            textChannel.sendMessage(TranslationKey.ERROR_MISSING_OPERATION.getTranslation(locale)).queue();
            return;
        }

        final String action = opts[0];
        final ActionKey key = translationCache.getActionKey(action);
        switch (key) {
            case CREATE: {
                //event create(0) name(1) description(2)
                createEvent(opts, matcher, guildData);
                break;
            }
            case DELETE: {
                deleteEvent(opts, matcher, guildData);
                break;
            }
            case JOIN: {
                joinEvent(opts, matcher, guildData);
                break;
            }
            case LEAVE: {
                leaveEvent(opts, matcher, guildData);
                break;
            }
            case LIST_MEMBERS: {
                showEventMembers(opts, matcher, guildData);
                break;
            }
            case CLEAR: {
                clearEventMembers(opts, matcher, guildData);
                break;
            }
            case LIST: {
                listEvents(matcher, guildData);
                break;
            }
            case RANDOM: {
                pickRandomEventMember(opts, matcher, guildData);
                break;
            }
            case LOCK: {
                lockEvent(opts, matcher, guildData);
                break;
            }
            case UNLOCK: {
                unlockEvent(opts, matcher, guildData);
                return;
            }
            default: {
                textChannel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + action).queue();
            }
        }
    }

    private static void createEvent(final String[] opts, final CommandMatcher matcher, final GuildDataStore guildData) {
        final Member sender = matcher.getMember();
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();
        final EventManager events = guildData.getEventManager();
        if (opts.length < 2) {
            textChannel.sendMessage(TranslationKey.EVENT_CREATE_MISSING_NAME.getTranslation(locale)).queue();
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
                textChannel.sendMessage(TranslationKey.EVENT_ALREADY_EXISTS.getTranslation(locale)).queue();
                return;
            }
            if (!events.joinEvent(newEvent, sender)) {
                textChannel.sendMessage(TranslationKey.EVENT_CREATE_JOIN_FAILED.getTranslation(locale)).queue();
                return;
            }
            textChannel.sendMessage(TranslationKey.EVENT_CREATE_SUCCESS.getTranslation(locale)).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_CREATE.getTranslation(locale)).queue();
            LOGGER.error("Failure to create event: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void deleteEvent(final String[] opts, final CommandMatcher matcher, final GuildDataStore guildData) {
        final Member sender = matcher.getMember();
        final TextChannel textChannel = matcher.getTextChannel();
        final EventManager events = guildData.getEventManager();
        final Locale locale = guildData.getConfigManager().getLocale();
        if (opts.length < 2) {
            textChannel.sendMessage(TranslationKey.EVENT_DELETE_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String eventName = opts[1];

        //Get the event to remove
        final Optional<Event> oldEvent;
        try {
            oldEvent = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_FINDING_EVENT.getTranslation(locale)).queue();
            return;
        }
        if (oldEvent.isEmpty()) {
            textChannel.sendMessageFormat(TranslationKey.EVENT_NOT_FOUND_WITH_NAME.getTranslation(locale), eventName).queue();
            return;
        }
        final Event event = oldEvent.get();
        textChannel.getGuild().retrieveMemberById(event.getOwner()).submit().whenComplete((Member eventOwner, Throwable error) -> {
            //Check if user has permission to remove the event
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, eventOwner);
            if (!hasPermission) {
                textChannel.sendMessage(TranslationKey.EVENT_REMOVE_PERMISSION_DENIED.getTranslation(locale)).queue();
                return;
            }
            try {
                events.removeEvent(event);
                textChannel.sendMessage(TranslationKey.EVENT_REMOVED_SUCCESFULLY.getTranslation(locale)).queue();
            } catch (SQLException ex) {
                textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_REMOVE.getTranslation(locale)).queue();
                LOGGER.error("Failure to remove event: {}", ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });

    }

    private static void joinEvent(final String[] opts, final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();
        final Member sender = matcher.getMember();

        if (opts.length < 2) {
            textChannel.sendMessage(TranslationKey.EVENT_JOIN_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String eventName = opts[1];
        final EventManager events = guildData.getEventManager();

        //Get the event to join
        final Optional<Event> oldEvent;
        try {
            oldEvent = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_FINDING_EVENT.getTranslation(locale)).queue();
            return;
        }
        if (oldEvent.isEmpty()) {
            textChannel.sendMessageFormat(TranslationKey.EVENT_NOT_FOUND_WITH_NAME.getTranslation(locale), eventName).queue();
            return;
        }

        //Check if event is locked
        final Event event = oldEvent.get();
        if (event.isLocked()) {
            textChannel.sendMessage(TranslationKey.EVENT_JOIN_LOCKED.getTranslation(locale)).queue();
            return;
        }

        try {
            if (events.joinEvent(event, sender)) {
                textChannel.sendMessage(TranslationKey.EVENT_JOIN_SUCCESS.getTranslation(locale)).queue();
                return;
            }
            textChannel.sendMessage(TranslationKey.EVENT_JOIN_ALREADY_JOINED.getTranslation(locale)).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_JOIN.getTranslation(locale)).queue();
            LOGGER.error("Failure to join event: {}\n{}", eventName, ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void leaveEvent(final String[] opts, final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();
        final Member sender = matcher.getMember();

        if (opts.length < 2) {
            textChannel.sendMessage(TranslationKey.EVENT_LEAVE_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String eventName = opts[1];
        final EventManager events = guildData.getEventManager();

        //Get the event to leave
        final Optional<Event> oldEvent;
        try {
            oldEvent = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_FINDING_EVENT.getTranslation(locale)).queue();
            return;
        }
        if (oldEvent.isEmpty()) {
            textChannel.sendMessageFormat(TranslationKey.EVENT_NOT_FOUND_WITH_NAME.getTranslation(locale), eventName).queue();
            return;
        }

        //Check if event is locked
        final Event event = oldEvent.get();
        if (event.isLocked()) {
            textChannel.sendMessage(TranslationKey.EVENT_LEAVE_LOCKED.getTranslation(locale)).queue();
            return;
        }

        //Leave the event
        try {
            if (events.leaveEvent(event, sender.getIdLong())) {
                textChannel.sendMessage(TranslationKey.EVENT_LEAVE_SUCCESS.getTranslation(locale)).queue();
                return;
            }
            textChannel.sendMessage(TranslationKey.EVENT_LEAVE_ALREADY_LEFT.getTranslation(locale)).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_LEAVE.getTranslation(locale)).queue();
            LOGGER.error("Failure to leave event: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void showEventMembers(final String[] opts, final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();

        if (opts.length < 2) {
            textChannel.sendMessage(TranslationKey.EVENT_SHOW_MEMBERS_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String eventName = opts[1];
        final EventManager events = guildData.getEventManager();

        //Get the event
        final Optional<Event> opt;
        try {
            opt = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_FINDING_EVENT.getTranslation(locale)).queue();
            return;
        }
        if (opt.isEmpty()) {
            textChannel.sendMessageFormat(TranslationKey.EVENT_NOT_FOUND_WITH_NAME.getTranslation(locale), eventName).queue();
            return;
        }
        final Event event = opt.get();
        final List<Long> eventMemberIDs;
        try {
            eventMemberIDs = events.getMembers(event);
        } catch (SQLException e) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_LOADING_MEMBERS.getTranslation(locale)).queue();
            return;
        }

        final String header = String.format(TranslationKey.HEADER_EVENT_MEMBERS.getTranslation(locale), eventName);
        batchMemberList(eventMemberIDs).forEach((List<Long> idBatch) -> {
            textChannel.getGuild().retrieveMembersByIds(false, idBatch).onSuccess((List<Member> foundMembersList) -> {
                final EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle(header);

                //Add names of all event members who could be found
                final String mentions = foundMembersList.stream().map(IMentionable::getAsMention).collect(Collectors.joining(","));

                //Did not find any event members
                if (foundMembersList.isEmpty()) {
                    //If any batch returns empty this might appear without reason
                    eb.setDescription(TranslationKey.EVENT_NO_MEMBERS.getTranslation(locale));
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

    private static void clearEventMembers(final String[] opts, final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();
        final Member sender = matcher.getMember();

        if (opts.length < 2) {
            textChannel.sendMessage(TranslationKey.EVENT_CLEAR_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String eventName = opts[1];
        final EventManager events = guildData.getEventManager();

        //Get the event
        final Optional<Event> opt;
        try {
            opt = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_FINDING_EVENT.getTranslation(locale)).queue();
            return;
        }
        if (opt.isEmpty()) {
            textChannel.sendMessageFormat(TranslationKey.EVENT_NOT_FOUND_WITH_NAME.getTranslation(locale), eventName).queue();
            return;
        }
        final Event event = opt.get();
        if (event.getOwner() != sender.getIdLong()) {
            textChannel.sendMessage(TranslationKey.EVENT_CLEAR_PERMISSION_DENIED.getTranslation(locale)).queue();
            return;
        }
        try {
            events.clearEvent(event);
            textChannel.sendMessage(TranslationKey.EVENT_CLEAR_SUCCESS.getTranslation(locale)).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_CLEAR.getTranslation(locale)).queue();
            LOGGER.error("Failure to clear event: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private static void listEvents(final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();
        final EventManager eventManager = guildData.getEventManager();

        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(TranslationKey.HEADER_EVENTS.getTranslation(locale));

        //Get the list of events
        final Collection<Event> ev;
        try {
            ev = eventManager.getEvents();
        } catch (SQLException e) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_LOADING_EVENTS.getTranslation(locale)).queue();
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
            contentBuilder.append(TranslationKey.EVENT_NO_EVENTS.getTranslation(locale));
        }
        eb.setDescription(contentBuilder);
        textChannel.sendMessage(eb.build()).queue();
    }

    private static void pickRandomEventMember(final String[] opts, final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel channel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();

        if (opts.length < 2) {
            channel.sendMessage(TranslationKey.EVENT_PICK_RANDOM_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String eventName = opts[1];
        final Guild guild = matcher.getGuild();
        final EventManager events = guildData.getEventManager();

        try {
            final Optional<Member> optMember = events.getRandomMember(eventName, guild);
            optMember.ifPresentOrElse((Member member) -> {
                final String template = TranslationKey.EVENT_SELECTED_MEMBER.getTranslation(locale);
                channel.sendMessageFormat(template, member.getEffectiveName()).queue();
            }, () -> {
                channel.sendMessage(TranslationKey.EVENT_NO_MEMBERS.getTranslation(locale)).queue();
            });
        } catch (NoSuchElementException e) {
            //Could not find event with provided name
            final String template = TranslationKey.EVENT_NOT_FOUND_WITH_NAME.getTranslation(locale);
            channel.sendMessageFormat(template, eventName).queue();
        } catch (SQLException e) {
            //Database failed to retrieve event or members for event
            channel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_FINDING_EVENT.getTranslation(locale)).queue();
        }
    }

    /**
     * Lock event
     *
     * @param opts Argument for event name to lock
     * @param matcher Matcher for request
     * @param guildData guildData for guild
     */
    private static void lockEvent(final String[] opts, final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel channel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();
        if (opts.length < 2) {
            channel.sendMessage(TranslationKey.EVENT_LOCK_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String eventName = opts[1];
        final EventManager events = guildData.getEventManager();

        //Get the event to unlock
        final Optional<Event> oldEvent;
        try {
            oldEvent = events.getEvent(eventName);
        } catch (SQLException e) {
            channel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_FINDING_EVENT.getTranslation(locale)).queue();
            return;
        }
        if (oldEvent.isEmpty()) {
            channel.sendMessageFormat(TranslationKey.EVENT_NOT_FOUND_WITH_NAME.getTranslation(locale), eventName).queue();
            return;
        }
        final Event event = oldEvent.get();
        if (event.isLocked()) {
            channel.sendMessage(TranslationKey.EVENT_ALREADY_LOCKED.getTranslation(locale)).queue();
            return;
        }
        channel.getGuild().retrieveMemberById(event.getOwner()).submit().whenComplete((Member eventOwner, Throwable error) -> {
            //Check if user has permission to remove the event
            final Member sender = matcher.getMember();
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, eventOwner);
            if (!hasPermission) {
                channel.sendMessage(TranslationKey.EVENT_LOCK_PERMISSION_DENIED.getTranslation(locale)).queue();
                return;
            }
            try {
                events.lockEvent(event);
                channel.sendMessage(TranslationKey.EVENT_LOCKED_SUCCESFULLY.getTranslation(locale)).queue();
            } catch (SQLException ex) {
                channel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_LOCK.getTranslation(locale)).queue();
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
    private static void unlockEvent(final String[] opts, final CommandMatcher matcher, final GuildDataStore guildData) {
        final Member sender = matcher.getMember();
        final TextChannel textChannel = matcher.getTextChannel();
        final EventManager events = guildData.getEventManager();
        final Locale locale = guildData.getConfigManager().getLocale();
        if (opts.length < 2) {
            textChannel.sendMessage(TranslationKey.EVENT_UNLOCK_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String eventName = opts[1];

        //Get the event to unlock
        final Optional<Event> oldEvent;
        try {
            oldEvent = events.getEvent(eventName);
        } catch (SQLException e) {
            textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_FINDING_EVENT.getTranslation(locale)).queue();
            return;
        }
        if (oldEvent.isEmpty()) {
            textChannel.sendMessageFormat(TranslationKey.EVENT_NOT_FOUND_WITH_NAME.getTranslation(locale), eventName).queue();
            return;
        }

        //Check if event is locked
        final Event event = oldEvent.get();
        if (!event.isLocked()) {
            textChannel.sendMessage(TranslationKey.EVENT_ALREADY_UNLOCKED.getTranslation(locale)).queue();
            return;
        }

        //Find the member of the event for permission check
        textChannel.getGuild().retrieveMemberById(event.getOwner()).submit().whenComplete((Member eventOwner, Throwable error) -> {
            //Check if user has permission to remove the event
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, eventOwner);
            if (!hasPermission) {
                textChannel.sendMessage(TranslationKey.EVENT_UNLOCK_PERMISSION_DENIED.getTranslation(locale)).queue();
                return;
            }
            try {
                events.unlockEvent(event);
                textChannel.sendMessage(TranslationKey.EVENT_UNLOCKED_SUCCESFULLY.getTranslation(locale)).queue();
            } catch (SQLException ex) {
                textChannel.sendMessage(TranslationKey.EVENT_SQL_ERROR_ON_UNLOCK.getTranslation(locale)).queue();
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
                LOGGER.info("Succesfully removed missing member from event\n");
            } catch (SQLException ex) {
                LOGGER.error("Failure to remove member: {} from event: {}, Error: {}", missingMemberID, event.getName(), ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });
    }

}
