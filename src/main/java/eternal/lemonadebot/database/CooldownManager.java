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
     */
    public boolean updateRuntime(ChatCommand command) {
        final Cooldown cooldown = this.cooldowns.get(command.getCommand());
        //Command does not have a cooldown
        if (cooldown == null) {
            return true;
        }
        final Instant now = Instant.now();
        final Duration timeDelta = Duration.between(cooldown.activationTime, now);
        //Command still on cooldown
        if (timeDelta.compareTo(cooldown.cooldownTime) < 0) {
            return false;
        }
        //Update activationTime
        cooldown.activationTime = now;
        return true;
    }

    /**
     * Get the string presentation for remaining cooldown of command
     *
     * @param command command to check cooldown for
     * @return remaining cooldown as string
     */
    public String getCooldownFormatted(ChatCommand command) {
        final Cooldown cd = this.cooldowns.get(command.getCommand());
        if (cd == null) {
            return "No cooldown set for command";
        }
        final Duration expiredCooldown = Duration.between(cd.activationTime, Instant.now());
        final Duration remainingCooldown = cd.cooldownTime.minus(expiredCooldown);
        if (remainingCooldown.isNegative() || remainingCooldown.isZero()) {
            return "00:00:00";
        }
        final long remainingDays = remainingCooldown.toDays();
        final long remainingHours = remainingCooldown.toDaysPart();
        final long remainingMinutes = remainingCooldown.toMinutesPart();
        final long remainingSeconds = remainingCooldown.toSecondsPart();

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
        final String query = "SELECT commandname,cooldownDuration,activationTime FROM Cooldowns WHERE guild = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String commandName = rs.getString("commandName");
                    final Cooldown cd = new Cooldown(rs.getLong("cooldownDuration"), rs.getLong("activationTime"));
                    cooldowns.put(commandName, cd);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading cooldowns from database failed");
            LOGGER.warn(e.getMessage());
            LOGGER.trace(e);
        }
    }

    void removeCooldown(ChatCommand command) {
        this.cooldowns.remove(command.getCommand());
    }

    private static class Cooldown {

        private Duration cooldownTime;
        private Instant activationTime;

        public Cooldown(long cooldownDurationSeconds, long activationTimeSeconds) {
            this.cooldownTime = Duration.ofSeconds(cooldownDurationSeconds);
            this.activationTime = Instant.ofEpochSecond(activationTimeSeconds);
        }
    }

}
