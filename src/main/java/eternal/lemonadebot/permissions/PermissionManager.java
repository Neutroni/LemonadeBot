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
package eternal.lemonadebot.permissions;

import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.CommandList;
import eternal.lemonadebot.config.ConfigCache;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import javax.sql.DataSource;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class PermissionManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DataSource dataSource;
    private final ConfigCache configCache;
    private final CommandList commands;

    /**
     * Constructor
     *
     * @param db DataSource to get connection from
     * @param configs Configuration to get locale from
     * @param commands Built in commands
     */
    public PermissionManager(final DatabaseManager db, final ConfigCache configs, final CommandList commands) {
        this.dataSource = db.getDataSource();
        this.configCache = configs;
        this.commands = commands;
    }

    /**
     * Check if user has required permission to perform action
     *
     * @param member User to check
     * @param command Command user is trying to run
     * @param action Action user is trying to perform
     * @return True if user can perform the action
     */
    public boolean hasPermission(final Member member, final ChatCommand command, final String action) {
        try {
            final long guildID = member.getGuild().getIdLong();
            final CommandPermission perm = getPermission(command, action, guildID);
            return perm.hashPermission(member);
        } catch (SQLException ex) {
            return member.getPermissions().contains(Permission.ADMINISTRATOR);
        }
    }

    /**
     * Set the permission required for action
     *
     * @param perm Permission required for the action
     * @return true if update succeeded
     * @throws SQLException if database connection failed
     */
    public boolean setPermission(final CommandPermission perm) throws SQLException {
        final String query = "INSERT OR REPLACE INTO Permissions(guild,action,requiredRank,requiredRole) VALUES(?,?,?,?);";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, perm.getGuildID());
            ps.setString(3, perm.getAction());
            ps.setString(4, perm.getRequiredRank().name());
            ps.setLong(5, perm.getRequiredRoleID());
            return (ps.executeUpdate() > 0);
        }
    }

    /**
     * Get permission required to run custom commands that do not have any other
     * permission set
     *
     * @param guildID ID of the guild to get default template run permission
     * @return CommandPermission
     */
    public CommandPermission getTemplateRunPermission(final long guildID) {
        final ConfigManager config = this.configCache.getConfigManager(guildID);
        final ResourceBundle resource = config.getTranslationCache().getResourceBundle();
        final String key = resource.getString("TEMPLATE_RUN_ACTION");
        try {
            final CommandPermission perm = getPermission(key, guildID)
                    .orElse(new CommandPermission(key, MemberRank.ADMIN, guildID, guildID));
            if (key.equals(perm.getAction())) {
                return perm;
            }
        } catch (SQLException ex) {
            LOGGER.error("Failure to retrieve permission for running custom commands from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
        }
        return new CommandPermission(key, MemberRank.ADMIN, guildID, guildID);
    }

    /**
     * Get permission by fuzzy action string
     *
     * @param command Command to get permission from if no other permission set
     * @param action action to get permission for
     * @return CommandPermission for action, if no permission has been set the
     * returned permission will have rank of ADMIN and any role
     */
    CommandPermission getPermission(final ChatCommand command, final String action, final long guildID) throws SQLException {
        final Optional<CommandPermission> optPerm = getPermission(action, guildID);
        final Locale locale = this.configCache.getConfigManager(guildID).getLocale();
        final ResourceBundle resource = ResourceBundle.getBundle("Translation", locale);
        CommandPermission builtInPerm = null;
        int keyLength = 0;
        for (final CommandPermission p : command.getDefaultRanks(resource, guildID, this)) {
            final String key = p.getAction();
            if (!action.startsWith(key)) {
                continue;
            }
            final int newKeyLength = key.length();
            if (newKeyLength > keyLength) {
                builtInPerm = p;
                keyLength = newKeyLength;
            }
        }
        if (builtInPerm == null) {
            builtInPerm = new CommandPermission("", MemberRank.ADMIN, guildID, guildID);
        }
        if (optPerm.isEmpty()) {
            return builtInPerm;
        }

        //Get longer of the actions
        final CommandPermission p = optPerm.get();
        final int dbPermLen = p.getAction().length();
        final int builtInPermLen = builtInPerm.getAction().length();
        if (dbPermLen > builtInPermLen) {
            return p;
        }
        return builtInPerm;
    }

    /**
     * Get list of permissions set for commands
     *
     * @return Collection of permissions
     */
    Collection<CommandPermission> getPermissions(final long guildID) throws SQLException {
        final Set<CommandPermission> permissions = new HashSet<>();

        //Load permissions from database
        final String query = "SELECT action,requiredRank,requiredRole FROM Permissions WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String action = rs.getString("action");
                    final String rankName = rs.getString("requiredRank");
                    final MemberRank rank;
                    try {
                        rank = MemberRank.valueOf(rankName);
                    } catch (IllegalArgumentException ex) {
                        LOGGER.warn("Permission with malformed rank in database: {}", ex.getMessage());
                        continue;
                    }
                    final long requiredRole = rs.getLong("requiredRole");
                    permissions.add(new CommandPermission(action, rank, requiredRole, guildID));
                }
            }
        }

        //Get default permissions for commands
        final Locale locale = this.configCache.getConfigManager(guildID).getLocale();
        final ResourceBundle resource = ResourceBundle.getBundle("Translation", locale);
        this.commands.forEach((ChatCommand c) -> {
            //Set will ignore any that were also loaded from the database
            permissions.addAll(c.getDefaultRanks(resource, guildID, this));
        });

        return permissions;
    }

    /**
     * Get permission from database for given action
     *
     * @param command Action to get permission for
     * @param guildID ID of the guild to get permission from
     * @return Optional containing permission if found
     * @throws SQLException if database connection failed
     */
    protected Optional<CommandPermission> getPermission(final String command, final long guildID) throws SQLException {
        final String query = "SELECT action,requiredRank,requiredRole FROM Permissions "
                + "WHERE guild = ? AND (action = ? OR ? LIKE action || ' %')"
                + "ORDER BY length(action) DESC LIMIT 1;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            ps.setString(2, command);
            ps.setString(3, command);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String action = rs.getString("action");
                    final String rankName = rs.getString("requiredRank");
                    final MemberRank rank;
                    try {
                        rank = MemberRank.valueOf(rankName);
                    } catch (IllegalArgumentException ex) {
                        LOGGER.warn("Permission with malformed rank in database: {}", ex.getMessage());
                        return Optional.empty();
                    }
                    final long requiredRole = rs.getLong("requiredRole");
                    return Optional.of(new CommandPermission(action, rank, requiredRole, guildID));
                }
            }
        }
        return Optional.empty();
    }

}
