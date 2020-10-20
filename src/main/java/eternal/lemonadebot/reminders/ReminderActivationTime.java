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
package eternal.lemonadebot.reminders;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Class used to store reminder activation condition
 *
 * @author Neutroni
 */
class ReminderActivationTime {

    private final LocalTime time;
    private final DayOfWeek dayOfWeek;
    private final int dayOfMonth;
    private final Month monthOfYear;

    /**
     * Constructor
     *
     * @param activationTime Time reminder check for activation
     * @param dayOfWeek DayOfWeek reminder should activate on, null to ignore
     * @param dayOfMonth Day of Month reminder should activate on, 0 to ignore
     * @param monthOfYear Moth reminder should activate on, null to ignore
     */
    ReminderActivationTime(final LocalTime activationTime, final DayOfWeek dayOfWeek, final int dayOfMonth, final Month monthOfYear) {
        this.time = activationTime;
        this.dayOfWeek = dayOfWeek;
        this.dayOfMonth = dayOfMonth;
        this.monthOfYear = monthOfYear;
    }

    /**
     * Get the time until the next daily check for activation
     *
     * @param timeZone TimeZone the activation check happens in
     * @return Duration to next check
     */
    Duration getTimeToActivation(final ZoneId timeZone) {
        final ZonedDateTime now = ZonedDateTime.now(timeZone);
        ZonedDateTime activationTime = now.with(this.time);
        if (activationTime.isBefore(now)) {
            activationTime = activationTime.plus(1, ChronoUnit.DAYS);
        }
        return Duration.between(now, activationTime);
    }

    /**
     * Check if reminder date portion matches current date
     *
     * @param timeZone ZoneId the activation happens at
     * @return true if reminder should activate today
     */
    boolean shouldActivate(final ZoneId timeZone) {
        final ZonedDateTime now = ZonedDateTime.now(timeZone);
        if (this.dayOfWeek != null) {
            if (now.getDayOfWeek() != this.dayOfWeek) {
                return false;
            }
        }
        if (this.dayOfMonth != 0) {
            if (now.getDayOfMonth() != this.dayOfMonth) {
                return false;
            }
        }
        if (this.monthOfYear != null) {
            return now.getMonth() == this.monthOfYear;
        }
        return true;
    }

    @Nullable
    DayOfWeek getDayOfWeek() {
        return this.dayOfWeek;
    }

    int getDayOfMonth() {
        return this.dayOfMonth;
    }

    LocalTime getTime() {
        return this.time;
    }

    @Nullable
    Month getMonthOfYear() {
        return this.monthOfYear;
    }

    /**
     * Get cron like representation of the activation time
     *
     * @param locale Locale use for day of week and time format
     * @param formatter Formatter to use for formatting activation time
     * @return String representation of activation time
     */
    String getCronString(final Locale locale, final DateTimeFormatter formatter) {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.time.format(formatter));
        sb.append(' ');
        if (this.dayOfMonth == 0) {
            sb.append('*');
        } else {
            sb.append(this.dayOfMonth);
        }
        sb.append(' ');
        if (this.monthOfYear == null) {
            sb.append('*');
        } else {
            sb.append(this.monthOfYear.getValue());
        }
        sb.append(' ');
        if (this.dayOfWeek == null) {
            sb.append('*');
        } else {
            sb.append(this.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale));
        }
        return sb.toString();
    }

}
