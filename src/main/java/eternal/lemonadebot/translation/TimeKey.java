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
package eternal.lemonadebot.translation;

import eternal.lemonadebot.database.ConfigManager;
import java.text.Collator;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 *
 * @author Neutroni
 */
public enum TimeKey {
    SECOND(TranslationKey.TIME_SECOND, ChronoUnit.SECONDS),
    MINUTE(TranslationKey.TIME_MINUTE, ChronoUnit.MINUTES),
    HOUR(TranslationKey.TIME_HOUR, ChronoUnit.HOURS),
    DAY(TranslationKey.TIME_DAY, ChronoUnit.DAYS),
    WEEK(TranslationKey.TIME_WEEK, ChronoUnit.WEEKS),
    MONTH(TranslationKey.TIME_MONTH, ChronoUnit.MONTHS),
    YEAR(TranslationKey.TIME_YEAR, ChronoUnit.YEARS),
    SECONDS(TranslationKey.TIME_SECONDS, ChronoUnit.SECONDS),
    MINUTES(TranslationKey.TIME_MINUTES, ChronoUnit.MINUTES),
    HOURS(TranslationKey.TIME_HOURS, ChronoUnit.HOURS),
    DAYS(TranslationKey.TIME_DAYS, ChronoUnit.DAYS),
    WEEKS(TranslationKey.TIME_WEEKS, ChronoUnit.WEEKS),
    MONTHS(TranslationKey.TIME_MONTHS, ChronoUnit.MONTHS),
    YEARS(TranslationKey.TIME_YEARS, ChronoUnit.YEARS);

    private final TranslationKey localName;
    private final ChronoUnit timeUnit;

    private TimeKey(TranslationKey name, ChronoUnit unit) {
        this.localName = name;
        this.timeUnit = unit;
    }

    /**
     * Get the ChronoUnit that this TimeKey maps to
     *
     * @return ChronoUnit
     */
    public ChronoUnit getChronoUnit() {
        return this.timeUnit;
    }

    /**
     * Get TimeKey by translated name
     *
     * @param name nane of the key in current locale
     * @param guildConf current locale
     * @return TimeKey for the name
     * @throws IllegalArgumentException if no TimeKey with the name exists
     */
    public static TimeKey getTimeKey(String name, ConfigManager guildConf) throws IllegalArgumentException {
        final Locale locale = guildConf.getLocale();
        final Collator collator = guildConf.getCollator();
        for (final TimeKey key : TimeKey.values()) {
            if (collator.equals(name, key.localName.getTranslation(locale))) {
                return key;
            }
        }
        throw new IllegalArgumentException("Unknown time unit: " + name);
    }
}
