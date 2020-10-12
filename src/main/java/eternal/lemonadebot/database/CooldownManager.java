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
package eternal.lemonadebot.database;

import eternal.lemonadebot.dataobjects.ActionCooldown;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class that manages cooldown for actions
 *
 * @author Neutroni
 */
public class CooldownManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DataSource dataSource;
    private final long guildID;
    private final Map<String, ActionCooldown> cooldowns = new ConcurrentHashMap<>();

    CooldownManager(DataSource ds, long guildID) {
        this.dataSource = ds;
        this.guildID = guildID;
        loadCooldowns();
    }

    /**
     * Get the list of set cooldowns
     *
     * @return Unmodifiable collection of cooldowns
     */
    public Collection<ActionCooldown> getCooldowns() {
        return Collections.unmodifiableCollection(this.cooldowns.values());
    }

    /**
     * Set action last seen time to current time if action is not on cooldown
     * and has set cooldown time
     *
     * @param action Action string to chechk cooldown for
     * @return Optional of remaining cooldown if command still on cooldown
     */
    public Optional<Duration> updateActivationTime(String action) {
        final Optional<ActionCooldown> cd = getActionCooldown(action);
        //Action does not have a cooldown
        if (cd.isEmpty()) {
            return Optional.empty();
        }

        final ActionCooldown cooldown = cd.get();
        final Instant now = Instant.now();
        final Instant lastActivation = cooldown.getLastActivationTime();
        final Duration timeDelta = Duration.between(lastActivation, now);

        //Command still on cooldown
        final Duration cooldownDuration = cooldown.getDuration();
        if (timeDelta.compareTo(cooldownDuration) < 0) {
            final Duration remainingCooldown = cooldownDuration.minus(timeDelta);
            return Optional.of(remainingCooldown);
        }

        //Update activationTime
        cooldown.updateActivationTime(now);

        //Store in database
        final String query = "UPDATE Cooldowns SET activationTime = ? WHERE guild = ? AND command = ?) VALUES(?,?,?)";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, now.getEpochSecond());
            ps.setLong(2, this.guildID);
            ps.setString(3, cooldown.getAction());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Updating command activation time failed!");
            LOGGER.error(e.getMessage());
            LOGGER.trace(e);
        }
        return Optional.empty();
    }

    /**
     * Remove cooldown from command
     *
     * @param action Action to remove cooldown from
     * @return true if cooldown was removed, false otherwise
     * @throws SQLException if database connection failed
     */
    public boolean removeCooldown(String action) throws SQLException {
        this.cooldowns.remove(action);

        //Remove from database
        final String query = "DELETE FROM Cooldowns Where guild = ? AND command = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, action);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Set cooldown for command
     *
     * @param action Action to set cooldown for
     * @param duration Lenght of the cooldown
     * @return true if cooldown was created or updated
     * @throws SQLException If database connection failed
     */
    public boolean setCooldown(String action, Duration duration) throws SQLException {
        final ActionCooldown cooldown = this.cooldowns.computeIfAbsent(action, (String cooldownAction) -> {
            return new ActionCooldown(cooldownAction, duration, Instant.EPOCH);
        });
        cooldown.updateCooldownDuration(duration);

        final String query = "INSERT OR REPLACE INTO Cooldowns(guild,command,duration,activationTime) VALUES(?,?,?,?)";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, action);
            ps.setLong(3, duration.getSeconds());
            ps.setLong(4, cooldown.getLastActivationTime().getEpochSecond());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get ActionCooldown by exact name
     *
     * @param name Name of cooldown to get
     * @return Optional for cooldown if found
     */
    public Optional<ActionCooldown> getActionCooldownByName(String name) {
        return Optional.ofNullable(this.cooldowns.get(name));
    }

    /**
     * Get cooldown by fuzzy action string
     *
     * @param action Action to get cooldown for, cooldown only needs to match
     * start of action
     * @return ActionCooldown if found
     */
    public Optional<ActionCooldown> getActionCooldown(String action) {
        //If the action does not contain whitespace get the cooldown directly from map
        if (action.indexOf(' ') == -1) {
            return Optional.ofNullable(this.cooldowns.get(action));
        }

        //Action has whitespace, find longest prefix for action string
        long keyLength = 0;
        ActionCooldown matchingCooldown = null;

        for (Map.Entry<String, ActionCooldown> cd : this.cooldowns.entrySet()) {
            final String key = cd.getKey();
            if (!action.startsWith(key)) {
                continue;
            }
            final long newKeyLength = key.length();
            if (newKeyLength > keyLength) {
                matchingCooldown = cd.getValue();
                keyLength = newKeyLength;
            }
        }
        return Optional.ofNullable(matchingCooldown);
    }

    /**
     * Load cooldowns from database for current guild
     */
    private void loadCooldowns() {
        final String query = "SELECT command,duration,activationTime FROM Cooldowns WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String action = rs.getString("command");
                    final long cooldownDurationSeconds = rs.getLong("duration");
                    final long lastActivationTime = rs.getLong("activationTime");
                    final ActionCooldown cd = new ActionCooldown(action, cooldownDurationSeconds, lastActivationTime);
                    cooldowns.put(action, cd);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading cooldowns from database failed: {}", e.getMessage());
            LOGGER.trace(e);
        }
    }

    /**
     * Format duratio to a string in locale
     *
     * @param duration Duration to format
     * @param locale Locale to return the duration string in
     * @return Duration as string
     */
    public static String formatDuration(Duration duration, Locale locale) {
        if (duration.isNegative() || duration.isZero()) {
            return "00:00:00";
        }
        final long remainingDays = duration.toDays();
        final long remainingHours = duration.toHoursPart();
        final long remainingMinutes = duration.toMinutesPart();
        final long remainingSeconds = duration.toSecondsPart();

        final String days = TranslationKey.TIME_DAYS.getTranslation(locale);
        final String hours = TranslationKey.TIME_HOURS.getTranslation(locale);
        final String minutes = TranslationKey.TIME_MINUTES.getTranslation(locale);
        final String seconds = TranslationKey.TIME_SECONDS.getTranslation(locale);
        final String day = TranslationKey.TIME_DAY.getTranslation(locale);
        final String hour = TranslationKey.TIME_HOUR.getTranslation(locale);
        final String minute = TranslationKey.TIME_MINUTE.getTranslation(locale);
        final String second = TranslationKey.TIME_SECOND.getTranslation(locale);

        final StringBuilder sb = new StringBuilder();
        boolean printRemaining = false;
        if (remainingDays != 0) {
            printRemaining = true;
            sb.append(remainingDays);
            if (remainingDays == 1) {
                sb.append(' ').append(day).append(' ');
            } else {
                sb.append(' ').append(days).append(' ');
            }
        }
        if (printRemaining || (remainingHours != 0)) {
            printRemaining = true;
            sb.append(remainingHours);
            if (remainingHours == 1) {
                sb.append(' ').append(hour).append(' ');
            } else {
                sb.append(' ').append(hours).append(' ');
            }
        }
        if (printRemaining || (remainingMinutes != 0)) {
            sb.append(remainingMinutes);
            if (remainingMinutes == 1) {
                sb.append(' ').append(minute).append(' ');
            } else {
                sb.append(' ').append(minutes).append(' ');
            }
        }
        sb.append(remainingSeconds);
        if (remainingSeconds == 1) {
            sb.append(' ').append(second);
        } else {
            sb.append(' ').append(seconds);
        }

        return sb.toString();
    }

}
