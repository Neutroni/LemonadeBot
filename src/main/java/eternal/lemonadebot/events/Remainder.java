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
package eternal.lemonadebot.events;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimerTask;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class Remainder extends TimerTask {

    private static final Logger LOGGER = LogManager.getLogger();

    private final JDA jda;
    private final DayOfWeek day;
    private final LocalTime time;
    private final long channelID;
    private final Event event;
    private final MentionEnum mentions;

    /**
     * Constructor
     *
     * @param jda JDA to use for getting channel
     * @param textChannel channel the message should be sent in
     * @param event event this remainder is for
     * @param mentions Who to mention in remainder messsage
     * @param day Weekday the reminder happens
     * @param time Time for remainder in UTC
     */
    public Remainder(JDA jda, long textChannel, Event event, MentionEnum mentions, DayOfWeek day, LocalTime time) {
        this.jda = jda;
        this.channelID = textChannel;
        this.event = event;
        this.mentions = mentions;
        this.day = day;
        this.time = time;
    }

    /**
     * Run this TimerTask
     */
    @Override
    public void run() {
        LOGGER.debug("Event remainder started for event" + this.event.getName());
        final TextChannel channel = this.jda.getTextChannelById(channelID);
        if (channel == null) {
            LOGGER.warn("Remainder for textchannel that does not exists, channel id:" + this.channelID);
            return;
        }
        MessageBuilder mb = new MessageBuilder(this.event.getDescription());
        mb.append(" time");
        switch (this.mentions) {
            case HERE: {
                final List<Role> roles = channel.getGuild().getRolesByName("here", true);
                for (Role r : roles) {
                    mb.append(' ');
                    mb.append(r);
                }
                break;
            }
            case EVENT: {
                for (long id : this.event.getMembers()) {
                    final Member m = channel.getGuild().getMemberById(id);
                    if (m != null) {
                        mb.append(' ');
                        mb.append(m);
                    }
                }
                break;
            }
        }

        mb.sendTo(channel).queue();
        LOGGER.debug("Message sent to channel " + channel.getName());
    }

    /**
     * Get the event this remainder is for
     *
     * @return Event
     */
    public Event getEvent() {
        return this.event;
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
     * @return day of the week as string
     */
    public DayOfWeek getDay() {
        return this.day;
    }

    /**
     * Get the Date this remainder activates at
     *
     * @return Date for this remainder
     */
    public Date getActivationDate() {
        OffsetDateTime activationTime = OffsetDateTime.now(Clock.systemUTC()).with(TemporalAdjusters.next(this.day)).with(time);
        return Date.from(activationTime.toInstant());
    }

    /**
     * Get mention mode for this remainder
     *
     * @return MentionEnum
     */
    public MentionEnum getMentionMode() {
        return this.mentions;
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
     * Check if remainder is valid such as the channel for the remainder exists
     *
     * @return true if channel for remainder exists
     */
    public boolean isValid() {
        return (this.jda.getTextChannelById(this.channelID) != null);
    }

    /**
     * Get the string presentation of this remainder
     *
     * @return String of format name day time on channel channel
     */
    @Override
    public String toString() {
        final TextChannel channel = this.jda.getTextChannelById(channelID);
        final StringBuilder sb = new StringBuilder();
        if (channel == null) {
            sb.append(this.event.getName()).append(' ')
                    .append(this.day.toString().toLowerCase()).append(' ')
                    .append(this.time.toString())
                    .append("channel could not be found and remainder is inactive");
            return sb.toString();
        }
        sb.append(this.event.getName()).append(' ')
                .append(this.day.toString().toLowerCase()).append(' ')
                .append(this.time.toString())
                .append(" on channel ").append(channel.getAsMention());
        //gdds sunday 17:00 on channel #general
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.day);
        hash = 41 * hash + Objects.hashCode(this.time);
        hash = 41 * hash + Objects.hashCode(this.event);
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Remainder) {
            Remainder otherRemainder = (Remainder) other;
            final boolean sameEvent = this.event.getName().equals(otherRemainder.getEvent().getName());
            final boolean sameDay = this.getDay().equals(otherRemainder.getDay());
            final boolean sameTime = this.getTime().equals(otherRemainder.getTime());
            return sameEvent && sameDay && sameTime;
        }
        return false;
    }

}
