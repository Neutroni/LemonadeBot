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
import java.util.TimerTask;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author Neutroni
 */
public class Remainder extends TimerTask {

    private final String name;
    private final DayOfWeek day;
    private final LocalTime time;
    private final TextChannel channel;
    private final Event event;
    private final MentionEnum mentions;

    /**
     * Constructor
     *
     * @param name Name of the event
     * @param textChannel channel the message should be sent in
     * @param event event this remainder is for
     * @param mentions Who to mention in remainder messsage
     * @param day Weekday the reminder happens
     * @param time Time for remainder in UTC
     */
    public Remainder(String name, TextChannel textChannel, Event event, MentionEnum mentions, DayOfWeek day, LocalTime time) {
        this.name = name;
        this.channel = textChannel;
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
        MessageBuilder mb = new MessageBuilder();
        mb.append("");

        mb.sendTo(channel).queue();
    }

    /**
     * Get the name of this remainder
     *
     * @return name id for the remainder
     */
    public String getName() {
        return this.name;
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
     * Get the text channel this remainder will appear in
     *
     * @return TextChannel
     */
    public TextChannel getChannel() {
        return this.channel;
    }

}
