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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class that manages cooldown for actions
 *
 * @author Neutroni
 */
public class CooldownManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Connection conn;
    private final long guildID;
    private final Map<String, ActionCooldown> cooldowns = new ConcurrentHashMap<>();

    CooldownManager(Connection connection, long guildID) {
        this.conn = connection;
        this.guildID = guildID;
        loadCooldowns();
    }

    /**
     * Set action last seen time to current time if action is not on cooldown
     * and has set cooldown time
     *
     * @param action Action string to chechk cooldown for
     * @return Optional containing template string for remaining cooldown if on
     * cooldown, empty otherwise
     */
    public Optional<String> updateActivationTime(String action) {
        final Optional<Map.Entry<String, ActionCooldown>> cd = getCooldownEntry(action);

        //Action does not have a cooldown
        if (cd.isEmpty()) {
            return Optional.empty();
        }

        final Map.Entry<String, ActionCooldown> entry = cd.get();
        final ActionCooldown cooldown = entry.getValue();

        final Instant now = Instant.now();
        final Duration timeDelta = Duration.between(cooldown.activationTime, now);

        //Command still on cooldown
        if (timeDelta.compareTo(cooldown.cooldownTime) < 0) {
            return Optional.of(cooldown.getRemainingTime(now));
        }

        //Update activationTime
        cooldown.activationTime = now;

        //Store in database
        final String query = "INSERT OR REPLACE INTO Cooldowns(guild,command,duration,activationTime) VALUES(?,?,?,?)";
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, entry.getKey());
            ps.setLong(3, cooldown.cooldownTime.getSeconds());
            ps.setLong(4, now.getEpochSecond());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Updating command activation time failed!");
            LOGGER.error(e.getMessage());
            LOGGER.trace(e);
        }
        return Optional.empty();
    }

    /**
     * Get the string presentation of cooldown that is set for action
     *
     * @param action Action to get cooldown for
     * @return String of cooldown
     */
    public Optional<String> getCooldownFormatted(String action) {
        final Optional<ActionCooldown> cd = getCooldown(action);

        //Action does not have a cooldown
        if (cd.isEmpty()) {
            return Optional.empty();
        }

        final ActionCooldown cooldown = cd.get();
        return Optional.of(cooldown.toString());
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
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, action);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Set cooldown for command
     *
     * @param action Action to set cooldown for
     * @param cooldownDuration Lenght of the cooldown
     * @return true if cooldown was created
     * @throws SQLException If database connection failed
     */
    public boolean setCooldown(String action, Duration cooldownDuration) throws SQLException {
        final ActionCooldown cooldown = this.cooldowns.computeIfAbsent(action, (String t) -> {
            return new ActionCooldown(cooldownDuration, Instant.EPOCH);
        });
        cooldown.cooldownTime = cooldownDuration;

        final String query = "INSERT OR REPLACE INTO Cooldowns(guild,command,duration,activationTime) VALUES(?,?,?,?)";
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, action);
            ps.setLong(3, cooldownDuration.getSeconds());
            ps.setLong(4, Instant.EPOCH.getEpochSecond());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get cooldown by action string
     *
     * @param action Action to get cooldown for
     * @return ActionCooldown if found
     */
    private Optional<ActionCooldown> getCooldown(String action) {
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
     * Get matching cooldown entry for given action
     *
     * @param action Action to find entry for
     * @return Optional containing the entry if found
     */
    private Optional<Map.Entry<String, ActionCooldown>> getCooldownEntry(String action) {
        long keyLength = 0;
        Map.Entry<String, ActionCooldown> matchingCooldown = null;

        for (Map.Entry<String, ActionCooldown> cd : this.cooldowns.entrySet()) {
            final String key = cd.getKey();
            if (!action.startsWith(key)) {
                continue;
            }
            final long newKeyLength = key.length();
            if (newKeyLength > keyLength) {
                matchingCooldown = cd;
                keyLength = newKeyLength;
            }
        }

        return Optional.ofNullable(matchingCooldown);
    }

    private void loadCooldowns() {
        final String query = "SELECT command,duration,activationTime FROM Cooldowns WHERE guild = ?;";
        try ( PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String commandName = rs.getString("command");
                    final ActionCooldown cd = new ActionCooldown(rs.getLong("duration"), rs.getLong("activationTime"));
                    cooldowns.put(commandName, cd);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading cooldowns from database failed: {}", e.getMessage());
            LOGGER.trace(e);
        }
    }

    private class ActionCooldown {

        Duration cooldownTime;
        Instant activationTime;

        ActionCooldown(Duration cooldownDuration, Instant lastActivation) {
            this.cooldownTime = cooldownDuration;
            this.activationTime = lastActivation;
        }

        ActionCooldown(long cooldownDurationSeconds, long activationTimeSeconds) {
            this.cooldownTime = Duration.ofSeconds(cooldownDurationSeconds);
            this.activationTime = Instant.ofEpochSecond(activationTimeSeconds);
        }

        @Override
        public String toString() {
            if (this.cooldownTime.isNegative() || this.cooldownTime.isZero()) {
                return "00:00:00";
            }
            final long remainingDays = this.cooldownTime.toDays();
            final long remainingHours = this.cooldownTime.toDaysPart();
            final long remainingMinutes = this.cooldownTime.toMinutesPart();
            final long remainingSeconds = this.cooldownTime.toSecondsPart();

            final StringBuilder sb = new StringBuilder();
            boolean printRemaining = false;
            if (remainingDays != 0) {
                printRemaining = true;
                sb.append(remainingDays);
                if (remainingDays == 1) {
                    sb.append(" %1s ");
                } else {
                    sb.append(" %5s ");
                }
            }
            if (printRemaining || (remainingHours != 0)) {
                printRemaining = true;
                sb.append(remainingHours);
                if (remainingHours == 1) {
                    sb.append(" %2s ");
                } else {
                    sb.append(" %6s ");
                }
            }
            if (printRemaining || (remainingMinutes != 0)) {
                sb.append(remainingMinutes).append(" %3s ");
            }
            sb.append(remainingSeconds).append(" %4s");

            return sb.toString();
        }

        String getRemainingTime(Instant currentTime) {
            final Duration expiredCooldown = Duration.between(this.activationTime, currentTime);
            final Duration cooldownToFormat = this.cooldownTime.minus(expiredCooldown);

            if (cooldownToFormat.isNegative() || cooldownToFormat.isZero()) {
                return "00:00:00";
            }
            final long remainingDays = cooldownToFormat.toDays();
            final long remainingHours = cooldownToFormat.toDaysPart();
            final long remainingMinutes = cooldownToFormat.toMinutesPart();
            final long remainingSeconds = cooldownToFormat.toSecondsPart();

            final StringBuilder sb = new StringBuilder();
            boolean printRemaining = false;
            if (remainingDays != 0) {
                printRemaining = true;
                sb.append(remainingDays).append(" %1s ");
            }
            if (printRemaining || (remainingHours != 0)) {
                printRemaining = true;
                sb.append(remainingHours).append(" %2s ");
            }
            if (printRemaining || (remainingMinutes != 0)) {
                sb.append(remainingMinutes).append(" %3s ");
            }
            sb.append(remainingSeconds).append(" %4s");

            return sb.toString();
        }
    }
}
