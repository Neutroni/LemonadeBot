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
import eternal.lemonadebot.customcommands.TemplateManager;
import eternal.lemonadebot.database.GuildDataStore;
import java.sql.SQLException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Optional;
import java.util.TimerTask;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class that stores a remainder, used to send a message at specified time
 *
 * @author Neutroni
 */
public class Remainder extends TimerTask {

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
        LOGGER.debug("Remainder started: " + this.name);
        //Check remainder channel can be found
        final TextChannel channel = this.jda.getTextChannelById(channelID);
        if (channel == null) {
            LOGGER.debug("Remainder for textchannel that does not exists, channel id:" + this.channelID + " Deleting remainder");
            try {
                this.guildData.getRemainderManager().deleteRemainder(this);
                LOGGER.info("Remainder with missing channel deleted: " + this.channelID);
            } catch (SQLException ex) {
                LOGGER.error("Error removing remainder with missing channel");
                LOGGER.error(ex.getMessage());
                LOGGER.trace(ex);
            }
            return;
        }
        //Check remainder author can be found
        final Member member = channel.getGuild().getMemberById(this.author);
        if (member == null) {
            LOGGER.debug("Remainder with missing author, member id:" + this.author + " Deleting remainder");
            try {
                this.guildData.getRemainderManager().deleteRemainder(this);
                LOGGER.info("Remainder with missing author deleted");
            } catch (SQLException ex) {
                LOGGER.error("Error removing remainder with missing author");
                LOGGER.error(ex.getMessage());
                LOGGER.trace(ex);
            }
            return;
        }

        final CommandMatcher matcher = new RemainderMessageMatcher(member, channel);
        final CharSequence reponse = TemplateManager.parseAction(matcher, guildData, this.remainderText);
        channel.sendMessage(reponse).queue();
        LOGGER.debug("Remainder succesfully activated on channel" + channel.getName());
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
     * Get the ID of the guild this remainder is for
     *
     * @return GuildID
     */
    public long getGuildID() {
        return this.guildData.getGuildID();
    }

    /**
     * Get weekday this event activates at
     *
     * @return day of the week or null if no day specified
     */
    public Optional<DayOfWeek> getDay() {
        return this.day;
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
     * Check if remainder is valid such as the channel for the remainder exists
     *
     * @return true if channel for remainder exists
     */
    public boolean isValid() {
        final TextChannel channel = this.jda.getTextChannelById(this.channelID);
        if (channel == null) {
            return false;
        }
        final Member member = channel.getGuild().getMemberById(this.author);
        return member != null;
    }

    /**
     * Get the string presentation of this remainder
     *
     * @return String of format name day time on channel channel
     */
    @Override
    public String toString() {
        final TextChannel channel = this.jda.getTextChannelById(channelID);
        if (channel == null) {
            return "Invalid remainder";
        }
        final Member member = channel.getGuild().getMemberById(this.author);
        if (member == null) {
            return "Invalid remainder";
        }
        final StringBuilder sb = new StringBuilder();
        final String dayString;
        if (this.day == null) {
            dayString = "every day";
        } else {
            dayString = this.day.toString().toLowerCase();
        }
        sb.append(this.getName()).append(" - ")
                .append(this.time.toString()).append(' ')
                .append(dayString).append(' ')
                .append(" on channel ").append(channel.getAsMention())
                .append(" by ").append(member.getEffectiveName());
        //gdds - sunday 17:00 on channel #general by author
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Remainder) {
            Remainder otherRemainder = (Remainder) other;
            return this.getName().equals(otherRemainder.getName());
        }
        return false;
    }

}
