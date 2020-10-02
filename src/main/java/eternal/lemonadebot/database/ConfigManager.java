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

import eternal.lemonadebot.translation.LocaleUpdateListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class ConfigManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    public static final Set<Locale> SUPPORTED_LOCALES = Set.of(DEFAULT_LOCALE, new Locale("fi"));

    //Database connection
    private final Connection conn;
    //Locale update listeners
    private final List<LocaleUpdateListener> localeListeners = new ArrayList<>(3);

    //Stored values
    private final long guildID;
    private volatile String commandPrefix = "lemonbot#";
    private volatile Optional<String> greetingTemplate = Optional.empty();
    private volatile Optional<Long> logChannelID = Optional.empty();
    private volatile Locale locale = DEFAULT_LOCALE;
    private volatile ZoneId timeZone = ZoneOffset.UTC;

    /**
     * Constructor
     *
     * @param connection database connection to use
     * @param guild Guild this config is for
     */
    ConfigManager(Connection connection, long guild) {
        this.conn = connection;
        this.guildID = guild;
        loadValues();
    }

    /**
     * Prefix used by commands
     *
     * @return Command prefix for the server
     */
    public String getCommandPrefix() {
        return this.commandPrefix;
    }

    /**
     * Get the template that should be used for greeting new members of guild
     *
     * @return String
     */
    public Optional<String> getGreetingTemplate() {
        return this.greetingTemplate;
    }

    /**
     * Get the ID of the channel used to log messages
     *
     * @return channel id
     */
    public Optional<Long> getLogChannelID() {
        return this.logChannelID;
    }

    /**
     * Get the locale set for guild
     *
     * @return Locale
     */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * Get the timezone for the guild
     *
     * @return ZoneId
     */
    public ZoneId getZoneId() {
        return this.timeZone;
    }

    /**
     * Register a listener that will get notfied when locale changes
     *
     * @param listener LocaleUpdateListener
     */
    public void registerLocaleUpdateListener(LocaleUpdateListener listener) {
        this.localeListeners.add(listener);
    }

    /**
     * Set command prefix
     *
     * @param prefix new command prefix
     * @return Was comamnd prefix set succesfully
     * @throws SQLException if updating prefix failed
     */
    public boolean setCommandPrefix(final String prefix) throws SQLException {
        this.commandPrefix = prefix;

        final String query = "UPDATE Guilds SET commandPrefix = ? WHERE id = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            ps.setString(1, prefix);
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Set the channel used to send message logs to
     *
     * @param channel Channel to send messages to, null to disable log
     * @return did update succeed
     * @throws SQLException if database connection failed
     */
    public boolean setLogChannel(final TextChannel channel) throws SQLException {
        final String query = "UPDATE Guilds SET logChannel = ? WHERE id = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            if (channel == null) {
                this.logChannelID = Optional.empty();
                ps.setLong(1, 0);
            } else {
                final long channelID = channel.getIdLong();
                this.logChannelID = Optional.of(channelID);
                ps.setLong(1, channelID);
            }
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Set locale used in guild
     *
     * @param newLocale locale to set
     * @return false if update failed due to unsupported locale
     * @throws SQLException if database error occured
     */
    public boolean setLocale(final Locale newLocale) throws SQLException {
        if (!SUPPORTED_LOCALES.contains(newLocale)) {
            return false;
        }
        this.locale = newLocale;

        //Notify listeners
        this.localeListeners.forEach((LocaleUpdateListener t) -> {
            t.updateLocale(newLocale);
        });

        final String query = "UPDATE Guilds SET locale = ? WHERE id = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            ps.setString(1, newLocale.toLanguageTag());
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Set the template used to greet new members with
     *
     * @param newTemplate Template string to use, null to disable greeting
     * @return did update succeed
     * @throws SQLException id database connection failed
     */
    public boolean setGreetingTemplate(final String newTemplate) throws SQLException {
        this.greetingTemplate = Optional.ofNullable(newTemplate);

        final String query = "UPDATE Guilds SET greetingTemplate = ? WHERE id = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            ps.setString(1, newTemplate);
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Update guild time zone
     *
     * @param zoneId ZoneId to set for guild
     * @return did update succeed
     * @throws SQLException SQLException if database connection failed
     */
    public boolean setZoneId(final ZoneId zoneId) throws SQLException {
        this.timeZone = zoneId;

        final String query = "UPDATE Guilds SET timeZone = ? WHERE id = ?;";
        try (final PreparedStatement ps = this.conn.prepareStatement(query)) {
            final String zone = zoneId.getId();
            ps.setString(1, zone);
            ps.setLong(2, this.guildID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Load the values this guildconfig stores
     */
    private void loadValues() {
        final String query = "SELECT commandPrefix,greetingTemplate,logChannel,locale,timeZone FROM Guilds WHERE id = ?;";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                //Check that database contains this guild
                if (rs.next()) {
                    //Load command prefix
                    try {
                        this.commandPrefix = rs.getString("commandPrefix");
                    } catch (SQLException ex) {
                        LOGGER.error("SQL error on fetching the command prefix: {}", ex.getMessage());
                        LOGGER.warn("Stack trace:", ex);
                    }
                    //Load greeting template
                    try {
                        this.greetingTemplate = Optional.ofNullable(rs.getString("greetingTemplate"));
                    } catch (SQLException ex) {
                        LOGGER.error("SQL error on fetching greeting template: {}", ex.getMessage());
                        LOGGER.warn("Stack trace:", ex);
                    }
                    //Load logchannel
                    try {
                        final long channelID = rs.getLong("logChannel");
                        if (channelID == 0) {
                            this.logChannelID = Optional.empty();
                        } else {
                            this.logChannelID = Optional.of(channelID);
                        }
                    } catch (SQLException ex) {
                        LOGGER.error("SQL error on fetching log channel: {}", ex.getMessage());
                        LOGGER.warn("Stack trace:", ex);
                    }
                    //Load language
                    try {
                        final Locale loadedLocale = Locale.forLanguageTag(rs.getString("locale"));
                        if (SUPPORTED_LOCALES.contains(loadedLocale)) {
                            this.locale = loadedLocale;
                        } else {
                            LOGGER.warn("Loaded unsupported locale: {} from database for guild: {}", loadedLocale.toString(), this.guildID);
                        }
                    } catch (SQLException ex) {
                        LOGGER.error("SQL error on fetching locale: {}", ex.getMessage());
                        LOGGER.warn("Stack trace:", ex);
                    }
                    //Load guild time zone
                    try {
                        this.timeZone = ZoneId.of(rs.getString("timeZone"));
                    } catch (DateTimeException ex) {
                        LOGGER.warn("Loaded malformed ZoneId for guild: {} error: {}", this.guildID, ex.getMessage());
                    } catch (SQLException ex) {
                        LOGGER.error("SQL error on fetching the time zone: {}", ex.getMessage());
                        LOGGER.warn("Stack trace:", ex);
                    }
                    return;
                }
                LOGGER.info("Tried to load guild that does not exist in database, adding to database");
                if (!addGuild()) {
                    LOGGER.error("Adding the guild to database failed");
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Failed to load guild config from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
        }
    }

    /**
     * Add this guild to database
     *
     * @return true if add was succesfull
     * @throws SQLException if database connection failed
     */
    private boolean addGuild() throws SQLException {
        LOGGER.debug("Adding guild to database: {}", this.guildID);
        final String query = "INSERT OR IGNORE INTO Guilds("
                + "id,commandPrefix,greetingTemplate,locale,logChannel,timeZone) VALUES (?,?,?,?,?,?);";
        try (final PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, "lemonbot#");
            ps.setString(3, this.greetingTemplate.orElse(null));
            ps.setString(4, DEFAULT_LOCALE.toLanguageTag());
            ps.setLong(5, 0);
            ps.setString(6, this.timeZone.getId());
            return ps.executeUpdate() > 0;
        }
    }

}
