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

import java.text.Collator;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class used to convert translated strings into various enums
 *
 * @author Neutroni
 */
public class TranslationCache implements LocaleUpdateListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private volatile ResourceBundle resources;
    private final ReadWriteLock rwLock;
    private volatile Map<String, ChronoUnit> chronoMap;
    private final Map<String, ActionKey> actionMap;
    private volatile Collator collator;
    private volatile DateTimeFormatter timeFormat;

    /**
     * Constructor
     *
     * @param locale Locale to cache
     */
    public TranslationCache(final Locale locale) {
        this.rwLock = new ReentrantReadWriteLock();
        this.actionMap = new HashMap<>();
        localeUpdate(locale);
    }

    /**
     * Get the resource bundle for current locale
     *
     * @return ResourceBundle
     */
    public ResourceBundle getResourceBundle() {
        return this.resources;
    }

    /**
     * Get the case insesitive Collator for text comparisons
     *
     * @return Collator
     */
    public Collator getCollator() {
        return this.collator;
    }

    /**
     * Get formatter used to format time
     *
     * @return DateTimeFormatter
     */
    public DateTimeFormatter getTimeFormatter() {
        return this.timeFormat;
    }

    @Override
    public void updateLocale(final Locale newLocale) {
        localeUpdate(newLocale);
    }

    /**
     * Get ChronoUnit by translated name
     *
     * @param name name of the ChronoUnit in current locale
     * @return Optional containging ChronoUnit if found
     */
    public Optional<ChronoUnit> getChronoUnit(final String name) {
        this.rwLock.readLock().lock();
        try {
            final ChronoUnit unit = this.chronoMap.get(name);
            return Optional.ofNullable(unit);
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    /**
     * Get ActionKey by translated name
     *
     * @param name Translated action name to get a key for
     * @return Key for given action if found ActionKey.UNKNOWN if not found
     */
    public ActionKey getActionKey(final String name) {
        this.rwLock.readLock().lock();
        try {
            final ActionKey key = this.actionMap.get(name);
            if (key == null) {
                return ActionKey.UNKNOWN;
            }
            return key;
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    private void localeUpdate(final Locale newLocale) {
        this.rwLock.writeLock().lock();
        try {
            //Update resource bundle
            this.resources = ResourceBundle.getBundle("Translation", newLocale);
            //Update collator
            this.collator = Collator.getInstance(newLocale);
            this.collator.setStrength(Collator.SECONDARY);
            //Update time formatter
            final String timePattern = this.resources.getString("REMINDER_TIME_FORMAT");
            try {
                this.timeFormat = DateTimeFormatter.ofPattern(timePattern);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Error updating DateTimeFormatter for time parsing: {}", e.getMessage());
                LOGGER.error("Malformed time pattern for formatter: {}", timePattern);
                this.timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).localizedBy(Locale.ROOT);
            }
            //Update chronoMap
            this.chronoMap = new TreeMap<>(this.collator);
            this.chronoMap.put(this.resources.getString("TIME_SECOND"), ChronoUnit.SECONDS);
            this.chronoMap.put(this.resources.getString("TIME_MINUTE"), ChronoUnit.MINUTES);
            this.chronoMap.put(this.resources.getString("TIME_HOUR"), ChronoUnit.HOURS);
            this.chronoMap.put(this.resources.getString("TIME_DAY"), ChronoUnit.DAYS);
            this.chronoMap.put(this.resources.getString("TIME_WEEK"), ChronoUnit.WEEKS);
            this.chronoMap.put(this.resources.getString("TIME_MONTH"), ChronoUnit.MONTHS);
            this.chronoMap.put(this.resources.getString("TIME_YEAR"), ChronoUnit.YEARS);
            this.chronoMap.put(this.resources.getString("TIME_SECONDS"), ChronoUnit.SECONDS);
            this.chronoMap.put(this.resources.getString("TIME_MINUTES"), ChronoUnit.MINUTES);
            this.chronoMap.put(this.resources.getString("TIME_HOURS"), ChronoUnit.HOURS);
            this.chronoMap.put(this.resources.getString("TIME_DAYS"), ChronoUnit.DAYS);
            this.chronoMap.put(this.resources.getString("TIME_WEEKS"), ChronoUnit.WEEKS);
            this.chronoMap.put(this.resources.getString("TIME_MONTHS"), ChronoUnit.MONTHS);
            this.chronoMap.put(this.resources.getString("TIME_YEARS"), ChronoUnit.YEARS);
            //Update actionMap
            this.actionMap.clear();
            for (final ActionKey key : ActionKey.values()) {
                final String translationKey = key.getTranslationKey();
                if (translationKey != null) {
                    final String translation = this.resources.getString(translationKey);
                    this.actionMap.put(translation, key);
                }
            }
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

}
