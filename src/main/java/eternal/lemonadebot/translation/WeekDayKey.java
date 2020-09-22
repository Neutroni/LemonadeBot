/*
 * The MIT License
 *
 * Copyright 2020 joonas.
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
import java.time.DayOfWeek;
import java.util.Locale;

/**
 * Class user to convert translated day names to enum
 *
 * @author Neutroni
 */
public enum WeekDayKey {
    MONDAY(TranslationKey.DAY_MONDAY, DayOfWeek.MONDAY),
    TUESDAY(TranslationKey.DAY_TUESDAY, DayOfWeek.TUESDAY),
    WEDNESDAY(TranslationKey.DAY_WEDNESDAY, DayOfWeek.WEDNESDAY),
    THURSDAY(TranslationKey.DAY_THURSDAY, DayOfWeek.THURSDAY),
    FRIDAY(TranslationKey.DAY_FRIDAY, DayOfWeek.FRIDAY),
    SATURDAY(TranslationKey.DAY_SATURDAY, DayOfWeek.SATURDAY),
    SUNDAY(TranslationKey.DAY_SUNDAY, DayOfWeek.SUNDAY);

    public static WeekDayKey getDayFromTranslatedName(String dayName, ConfigManager guildConf) throws IllegalArgumentException {
        final Locale locale = guildConf.getLocale();
        final Collator collator = guildConf.getCollator();
        for (final WeekDayKey key : WeekDayKey.values()) {
            if (collator.equals(dayName, key.name.getTranslation(locale))) {
                return key;
            }
        }
        throw new IllegalArgumentException("Unknown day name: " + dayName);
    }

    public static WeekDayKey getFromDayOfWeek(DayOfWeek day) {
        for (WeekDayKey key : WeekDayKey.values()) {
            if (key.day.equals(day)) {
                return key;
            }
        }
        throw new IllegalArgumentException("Unknown DayOfWeek: " + day.name());
    }

    private final TranslationKey name;
    private final DayOfWeek day;

    private WeekDayKey(TranslationKey name, DayOfWeek day) {
        this.name = name;
        this.day = day;
    }

    /**
     * Get the DayOfWeek that maps to this name
     *
     * @return DayOfWeek
     */
    public DayOfWeek getDay() {
        return this.day;
    }

    /*
     * Get the translated day name
     */
    public String getDayString(Locale locale) {
        return this.name.getTranslation(locale);
    }

}
