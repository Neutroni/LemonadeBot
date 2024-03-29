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
package eternal.lemonadebot.cooldowns;

import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.permissions.MemberRank;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Member;
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

    /**
     * Constructor
     *
     * @param db DataSource for database

     */
    public CooldownManager(final DatabaseManager db) {
        this.dataSource = db.getDataSource();
    }

    /**
     * Check if command is on cooldown
     *
     * @param member Member to check, if rank admin or greater cooldown not used
     * @param action Action user is trying to perform
     * @return optional containing the cooldown remaining for the action
     */
    public Optional<Duration> checkCooldown(final Member member, final String action) {
        if (MemberRank.getRank(member).ordinal() > MemberRank.ADMIN.ordinal()) {
            //Command is never on cooldown for admins
            return Optional.empty();
        }

        final long guildID = member.getGuild().getIdLong();
        try {
            final Optional<ActionCooldown> optCooldown = getActionCooldown(action, guildID);
            if (optCooldown.isEmpty()) {
                //No cooldown set for action
                return Optional.empty();
            }

            final ActionCooldown cd = optCooldown.get();
            final Duration duration = cd.getRemainingDuration();
            if (duration.compareTo(Duration.ZERO) >= 0) {
                //Active cooldown
                return Optional.of(duration);
            }
            //Inactive cooldown, update most recent activation
            updateActivationTime(cd.getAction(), guildID);
        } catch (SQLException ex) {
            LOGGER.error("Failure to get cooldown for action from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
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
    boolean removeCooldown(final String action, final long guildID) throws SQLException {
        final String query = "DELETE FROM Cooldowns Where guild = ? AND command = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            ps.setString(2, action);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Set cooldown for command
     *
     * @param action Action to set cooldown for
     * @param duration Length of the cooldown
     * @return true if cooldown was created or updated
     * @throws SQLException If database connection failed
     */
    boolean setCooldown(final String action, final Duration duration, final long guildID) throws SQLException {
        final String query = "INSERT INTO Cooldowns(guild,command,duration,activationTime) VALUES(?,?,?,?) ON CONFLICT DO UPDATE SET duration = ?";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            ps.setString(2, action);
            ps.setLong(3, duration.getSeconds());
            ps.setLong(4, Instant.EPOCH.getEpochSecond());
            ps.setLong(5, duration.getSeconds());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get cooldown by fuzzy action string
     *
     * @param action Action to get cooldown for, cooldown only needs to match
     * start of action
     * @return ActionCooldown if found
     * @throws SQLException if database connection failed
     */
    Optional<ActionCooldown> getActionCooldown(final String action, final long guildID) throws SQLException {
        final String query = "SELECT command,duration,activationTime FROM Cooldowns "
                + "WHERE guild = ? AND (command = ? OR ? LIKE command || ' %') "
                + "ORDER BY length(command) DESC LIMIT 1;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String command = rs.getString("command");
                    final long cooldownDurationSeconds = rs.getLong("duration");
                    final long lastActivationTime = rs.getLong("activationTime");
                    return Optional.of(new ActionCooldown(command, cooldownDurationSeconds, lastActivationTime));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Get the list of set cooldowns
     *
     * @return Unmodifiable collection of cooldowns
     * @throws SQLException if database connection failed
     */
    Collection<ActionCooldown> getCooldowns(final long guildID) throws SQLException {
        final List<ActionCooldown> cooldowns = new ArrayList<>();
        final String query = "SELECT command,duration,activationTime FROM Cooldowns WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String action = rs.getString("command");
                    final long cooldownDurationSeconds = rs.getLong("duration");
                    final long lastActivationTime = rs.getLong("activationTime");
                    final ActionCooldown cd = new ActionCooldown(action, cooldownDurationSeconds, lastActivationTime);
                    cooldowns.add(cd);
                }
            }
        }
        return Collections.unmodifiableCollection(cooldowns);
    }

    /**
     * Set action last seen time to current time.
     *
     * @param action Action string to set last seen time
     * @param guildID ID of the guild to update action in
     * @return true if activate time updated successfully
     * @throws SQLException If database connection failed
     */
    protected boolean updateActivationTime(final String action, final long guildID) throws SQLException {
        final String query = "UPDATE Cooldowns SET activationTime = ? WHERE guild = ? AND command = ? VALUES(?,?,?)";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, Instant.now().getEpochSecond());
            ps.setLong(2, guildID);
            ps.setString(3, action);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Format duration to a string in locale
     *
     * @param duration Duration to format
     * @param locale ResourceBundle to get translated strings from
     * @return Duration as string
     */
    public static String formatDuration(final Duration duration, final ResourceBundle locale) {
        if (duration.isNegative() || duration.isZero()) {
            return "00:00:00";
        }
        final long remainingDays = duration.toDays();
        final long remainingHours = duration.toHoursPart();
        final long remainingMinutes = duration.toMinutesPart();
        final long remainingSeconds = duration.toSecondsPart();

        final String days = locale.getString("TIME_DAYS");
        final String hours = locale.getString("TIME_HOURS");
        final String minutes = locale.getString("TIME_MINUTES");
        final String seconds = locale.getString("TIME_SECONDS");
        final String day = locale.getString("TIME_DAY");
        final String hour = locale.getString("TIME_HOUR");
        final String minute = locale.getString("TIME_MINUTE");
        final String second = locale.getString("TIME_SECOND");

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
