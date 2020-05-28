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

import eternal.lemonadebot.commandtypes.ChatCommand;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author joonas
 */
public class CooldownManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Connection conn;
    private final long guildID;
    private final Map<String, Cooldown> cooldowns;

    CooldownManager(Connection connection, long guildID) {
        this.conn = connection;
        this.guildID = guildID;
        this.cooldowns = Collections.synchronizedMap(new HashMap<>());
        loadCooldowns();
    }

    /**
     * Set command last seen time to current time if command is not on cooldown
     * and has set cooldown time
     *
     * @param command command to check
     * @return true if command last seen time was updated or does not have
     * cooldown, false otherwise
     * @throws SQLException If database connection failed
     */
    public Optional<String> updateActivationTime(ChatCommand command) throws SQLException {
        final Cooldown cooldown = this.cooldowns.get(command.getCommand());
        //Command does not have a cooldown
        if (cooldown == null) {
            return Optional.empty();
        }

        final Instant now = Instant.now();
        final Duration timeDelta = Duration.between(cooldown.activationTime, now);

        //Command still on cooldown
        if (timeDelta.compareTo(cooldown.cooldownTime) < 0) {
            return Optional.of(getCooldownFormatted(cooldown, true));
        }

        //Update activationTime
        cooldown.activationTime = now;

        //Store in database
        final String query = "INSERT OR REPLACE INTO Cooldowns(guild,command,duration,activationTime) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, command.getCommand());
            ps.setLong(3, cooldown.cooldownTime.getSeconds());
            ps.setLong(4, now.getEpochSecond());
            ps.executeUpdate();
        }
        return Optional.empty();
    }

    public Optional<String> getCooldownFormatted(ChatCommand command) {
        final Cooldown cd = this.cooldowns.get(command.getCommand());
        if (cd == null) {
            return Optional.empty();
        }

        final Duration cooldownToFormat = cd.cooldownTime;
        if (cooldownToFormat.isNegative() || cooldownToFormat.isZero()) {
            return Optional.of("00:00:00");
        }
        final long remainingDays = cooldownToFormat.toDays();
        final long remainingHours = cooldownToFormat.toDaysPart();
        final long remainingMinutes = cooldownToFormat.toMinutesPart();
        final long remainingSeconds = cooldownToFormat.toSecondsPart();

        final StringBuilder sb = new StringBuilder();
        boolean printRemaining = false;
        if (remainingDays != 0) {
            printRemaining = true;
            sb.append(remainingDays).append(" days ");
        }
        if (printRemaining || (remainingHours != 0)) {
            printRemaining = true;
            sb.append(remainingHours).append(" hours ");
        }
        if (printRemaining || (remainingMinutes != 0)) {
            sb.append(remainingMinutes).append(" minutes ");
        }
        sb.append(remainingSeconds).append(" seconds");

        return Optional.of(sb.toString());
    }

    /**
     * Remove cooldown from command
     *
     * @param command Command to remove cooldown from
     * @return true if cooldown was removed, false otherwise
     * @throws SQLException if database connection failed
     */
    public boolean removeCooldown(ChatCommand command) throws SQLException {
        this.cooldowns.remove(command.getCommand());

        //Remove from database
        final String query = "DELETE FROM Cooldowns Where guild = ? AND command = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, command.getCommand());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Set cooldown for command
     *
     * @param command Command to set cooldown for
     * @param cooldownDuration Lenght of the cooldown
     * @return true if cooldown was created
     * @throws SQLException If database connection failed
     */
    public boolean setCooldown(ChatCommand command, Duration cooldownDuration) throws SQLException {
        final Cooldown cooldown = this.cooldowns.computeIfAbsent(command.getCommand(), (String t) -> {
            return new Cooldown(cooldownDuration, Instant.EPOCH);
        });
        cooldown.cooldownTime = cooldownDuration;

        final String query = "INSERT OR REPLACE INTO Cooldowns(guild,command,duration,activationTime) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, command.getCommand());
            ps.setLong(3, cooldownDuration.getSeconds());
            ps.setLong(4, Instant.EPOCH.getEpochSecond());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get the string presentation for remaining cooldown of command
     *
     * @param command command to check cooldown for
     * @param remainingCooldownOnly only the cooldown remaining
     * @return remaining cooldown as string
     */
    private String getCooldownFormatted(Cooldown cooldown, boolean remainingCooldownOnly) {
        final Duration cooldownToFormat;
        if (remainingCooldownOnly) {
            final Duration expiredCooldown = Duration.between(cooldown.activationTime, Instant.now());
            cooldownToFormat = cooldown.cooldownTime.minus(expiredCooldown);
        } else {
            cooldownToFormat = cooldown.cooldownTime;
        }

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
            sb.append(remainingDays).append(" days ");
        }
        if (printRemaining || (remainingHours != 0)) {
            printRemaining = true;
            sb.append(remainingHours).append(" hours ");
        }
        if (printRemaining || (remainingMinutes != 0)) {
            sb.append(remainingMinutes).append(" minutes ");
        }
        sb.append(remainingSeconds).append(" seconds");

        return sb.toString();
    }

    private void loadCooldowns() {
        final String query = "SELECT command,duration,activationTime FROM Cooldowns WHERE guild = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String commandName = rs.getString("command");
                    final Cooldown cd = new Cooldown(rs.getLong("duration"), rs.getLong("activationTime"));
                    cooldowns.put(commandName, cd);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading cooldowns from database failed");
            LOGGER.warn(e.getMessage());
            LOGGER.trace(e);
        }
    }

    /**
     * Class to store cooldows in
     */
    private static class Cooldown {

        private Duration cooldownTime;
        private Instant activationTime;

        Cooldown(Duration cooldownDuration, Instant lastActivation) {
            this.cooldownTime = cooldownDuration;
            this.activationTime = lastActivation;
        }

        Cooldown(long cooldownDurationSeconds, long activationTimeSeconds) {
            this.cooldownTime = Duration.ofSeconds(cooldownDurationSeconds);
            this.activationTime = Instant.ofEpochSecond(activationTimeSeconds);
        }
    }

}
