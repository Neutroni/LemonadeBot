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

import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.customcommands.TemplateProvider;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.translation.TranslationKey;
import eternal.lemonadebot.translation.WeekDayKey;
import java.sql.SQLException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Formattable;
import java.util.Formatter;
import java.util.Locale;
import java.util.Optional;
import java.util.TimerTask;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class that stores a remainder, used to send a message at specified time
 *
 * @author Neutroni
 */
public class Remainder extends TimerTask implements Formattable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final JDA jda;
    private final String name;
    private final Optional<DayOfWeek> day;
    private final LocalTime time;
    private final long author;
    private final long channelID;
    private final String remainderText;
    private final GuildDataStore guildData;

    /**
     * Constructor
     *
     * @param jda JDA to use for getting channel
     * @param guildData Datastore for the guild
     * @param name Name of the remainder
     * @param channelID ID of the channel the message should be sent in
     * @param author Autoher of the remainder
     * @param input Input string to either send or execute if it is a command
     * @param day Weekday the reminder happens
     * @param time Time for remainder in UTC
     */
    public Remainder(JDA jda, GuildDataStore guildData, String name, long channelID,
            long author, String input, DayOfWeek day, LocalTime time) {
        this.jda = jda;
        this.guildData = guildData;
        this.name = name;
        this.channelID = channelID;
        this.author = author;
        this.remainderText = input;
        this.day = Optional.ofNullable(day);
        this.time = time;
    }

    /**
     * Run this TimerTask
     */
    @Override
    public void run() {
        LOGGER.debug("Remainder started: {}", this.name);
        //Make sure JDA is loaded
        try {
            this.jda.awaitReady();
        } catch (InterruptedException e) {
            LOGGER.error("JDA loading interrupted while trying to run a remainder: {}", e.getMessage());
            LOGGER.trace("Stack trace: ", e);
        }
        //Check remainder channel can be found
        final TextChannel channel = this.jda.getTextChannelById(channelID);
        if (channel == null) {
            LOGGER.debug("Deleting remainder for textchannel that does not exist, channel id: {}", this.channelID);
            try {
                this.guildData.getRemainderManager().deleteRemainder(this);
                LOGGER.info("Deleted remainder with missing channel: {}", this.channelID);
            } catch (SQLException ex) {
                LOGGER.error("Error removing remainder with missing channel: {}", ex.getMessage());
                LOGGER.trace("Stack trace: ", ex);
            }
            return;
        }
        //Check remainder author can be found
        final Member member = channel.getGuild().getMemberById(this.author);
        if (member == null) {
            LOGGER.debug("Deleting remainder with missing author, member id: {}", this.author);
            try {
                this.guildData.getRemainderManager().deleteRemainder(this);
                LOGGER.info("Remainder with missing author deleted");
            } catch (SQLException ex) {
                LOGGER.error("Error removing remainder with missing author: {}", ex.getMessage());
                LOGGER.trace("Stack trace: ", ex);
            }
            return;
        }

        final CommandMatcher matcher = new RemainderMessageMatcher(member, channel);
        final CharSequence reponse = TemplateProvider.parseAction(matcher, guildData, this.remainderText);
        if (reponse.toString().isBlank()) {
            LOGGER.debug("Ignored empty response for remainder: {} with template: {}", this.name, this.remainderText);
            return;
        }
        channel.sendMessage(reponse).queue();
        LOGGER.debug("Remainder succesfully activated on channel: {}", channel.getName());
    }

    /**
     * Get the name of the remainder
     *
     * @return name of remainderF
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the ID for text channel this remainder will appear in
     *
     * @return TextChannel
     */
    public long getChannel() {
        return this.channelID;
    }

    /**
     * Get the text for the remainder
     *
     * @return String remainder send on activation
     */
    public String getMessage() {
        return this.remainderText;
    }

    /**
     * Get the ID of the remainder author
     *
     * @return id of remainder author
     */
    public long getAuthor() {
        return this.author;
    }

    /**
     * Get time this event activates at
     *
     * @return LocalTime
     */
    public LocalTime getTime() {
        return this.time;
    }

    /**
     * Get weekday this event activates at
     *
     * @return day of the week or null if no day specified
     */
    public Optional<DayOfWeek> getDay() {
        return this.day;
    }

    private Optional<Member> getMember() {
        final long guildID = this.guildData.getGuildID();
        final Guild currentGuild = this.jda.getGuildById(guildID);
        if (currentGuild == null) {
            return Optional.empty();
        }
        try {
            final Member member = currentGuild.retrieveMemberById(this.author).complete();
            return Optional.ofNullable(member);
        } catch (ErrorResponseException e) {
            return Optional.empty();
        }
    }

    private Optional<TextChannel> getTextChannel() {
        final long guildID = this.guildData.getGuildID();
        final Guild currentGuild = this.jda.getGuildById(guildID);
        if (currentGuild == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(currentGuild.getTextChannelById(this.channelID));
    }

    /**
     * Get the Date this remainder activates at
     *
     * @return Date for this remainder
     */
    public Date getActivationDate() {
        OffsetDateTime activationTime = OffsetDateTime.now(Clock.systemUTC()).with(time);
        if (this.day.isPresent()) {
            final DayOfWeek weekday = this.day.get();
            activationTime = activationTime.with(TemporalAdjusters.nextOrSame(weekday));
            if (activationTime.isBefore(OffsetDateTime.now())) {
                activationTime = activationTime.with(TemporalAdjusters.next(weekday));
            }
        } else if (activationTime.isBefore(OffsetDateTime.now())) {
            activationTime = activationTime.plusDays(1);
        }
        return Date.from(activationTime.toInstant());
    }

    /**
     * Check if remainder is valid such that the creator and the channel for the
     * remainder exists
     *
     * @return true if channel and the creator of remainder exists
     */
    public boolean isValid() {
        //Check if we find the textchannel for this remainder
        final Optional<TextChannel> channel = getTextChannel();
        if (channel.isEmpty()) {
            return false;
        }
        //Check if we find the creator of this member
        final Optional<Member> member = getMember();
        return member.isPresent();
    }

    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
        final Locale locale = formatter.locale();
        if (locale == null) {
            formatter.format(toString());
            return;
        }

        //Get the day remainder activates on
        final String dayName;
        if (this.day.isPresent()) {
            dayName = WeekDayKey.getFromDayOfWeek(this.day.get()).getDayString(locale);
        } else {
            dayName = TranslationKey.REMAINDER_DAY_DAILY.getTranslation(locale);
        }

        //Get the channel for remainder
        final Optional<TextChannel> optChannel = getTextChannel();
        final String channelName;
        if (optChannel.isPresent()) {
            channelName = optChannel.get().getAsMention();
        } else {
            channelName = TranslationKey.REMAINDER_CHANNEL_MISSING.getTranslation(locale);
        }

        //Get the remainder owner
        final Optional<Member> optMember = getMember();
        final String ownerName;
        if (optMember.isPresent()) {
            ownerName = optMember.get().getEffectiveName();
        } else {
            ownerName = TranslationKey.UNKNOWN_USER.getTranslation(locale);
        }

        //gdds - sunday 17:00 on channel #general by author
        final String template = TranslationKey.REMAINDER_LIST_ELEMENT_TEMPLATE.getTranslation(locale);
        formatter.format(template, this.name, dayName, this.time.toString(), channelName, ownerName);
    }

    /**
     * Get the string presentation of this remainder
     *
     * @return String of format name day time on channel channel
     */
    @Override
    public String toString() {
        //Get the day remainder activates on
        final String dayName;
        if (this.day.isPresent()) {
            dayName = this.day.get().name().toLowerCase();
        } else {
            dayName = TranslationKey.REMAINDER_DAY_DAILY.getDefaultText();
        }

        //Get the channel for remainder
        final Optional<TextChannel> optChannel = getTextChannel();
        final String channelName;
        if (optChannel.isPresent()) {
            channelName = optChannel.get().getAsMention();
        } else {
            channelName = TranslationKey.REMAINDER_CHANNEL_MISSING.getDefaultText();
        }

        //Get the remainder owner
        final Optional<Member> optMember = getMember();
        final String ownerName;
        if (optMember.isPresent()) {
            ownerName = optMember.get().getEffectiveName();
        } else {
            ownerName = TranslationKey.UNKNOWN_USER.getDefaultText();
        }

        //gdds - sunday 17:00 on channel #general by author
        final String template = TranslationKey.REMAINDER_LIST_ELEMENT_TEMPLATE.getDefaultText();
        return String.format(template, this.name, dayName, this.time.toString(), channelName, ownerName);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Remainder) {
            final Remainder otherRemainder = (Remainder) other;
            return this.name.equals(otherRemainder.name);
        }
        return false;
    }

}
