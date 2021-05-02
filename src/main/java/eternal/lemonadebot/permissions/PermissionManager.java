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
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.translation.LocaleUpdateListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class PermissionManager implements LocaleUpdateListener {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DataSource dataSource;
    private final long guildID;
    private final CommandPermission adminPermission;
    private volatile ResourceBundle locale;

    /**
     * Constructor
     *
     * @param ds DataSource to get connection from
     * @param guildID ID of the guild to store permissions for
     * @param locale Locale to use for loading default permissions
     */
    public PermissionManager(final DataSource ds, final long guildID, final Locale locale) {
        this.dataSource = ds;
        this.guildID = guildID;
        this.adminPermission = new CommandPermission("", MemberRank.ADMIN, guildID);
        this.locale = ResourceBundle.getBundle("Translation", locale);
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
            final CommandPermission perm = getPermission(command, action);
            return perm.hashPermission(member);
        } catch (SQLException ex) {
            return this.adminPermission.hashPermission(member);
        }
    }

    /**
     * Update permissions according to locale
     *
     * @param locale Translation to use
     */
    @Override
    public void updateLocale(final Locale locale) {
        this.locale = ResourceBundle.getBundle("Translation", locale);
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
            ps.setLong(1, this.guildID);
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
     * @return CommandPermission
     */
    public CommandPermission getTemplateRunPermission() {
        final String key = this.locale.getString("TEMPLATE_RUN_ACTION");
        try {
            final CommandPermission perm = getPermission(key).orElse(this.adminPermission);
            if (key.equals(perm.getAction())) {
                return perm;
            }
        } catch (SQLException ex) {
            LOGGER.error("Failure to retrieve permission for running custom commands from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
        }
        return this.adminPermission;
    }

    /**
     * Get permission by fuzzy action string
     *
     * @param command Command to get permission from if no other permission set
     * @param action action to get permission for
     * @return CommandPermission for action, if no permission has been set the
     * returned permission will have rank of ADMIN and any role
     */
    CommandPermission getPermission(final ChatCommand command, final String action) throws SQLException {
        final Optional<CommandPermission> optPerm = getPermission(action);
        CommandPermission builtInPerm = null;
        int keyLength = 0;
        for (final CommandPermission p : command.getDefaultRanks(this.locale, this.guildID, this)) {
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
            builtInPerm = this.adminPermission;
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
    Collection<CommandPermission> getPermissions() throws SQLException {
        final List<CommandPermission> permissions = new ArrayList<>();
        //Get default permissions for commands
        CommandProvider.COMMANDS.forEach(c -> {
            permissions.addAll(c.getDefaultRanks(this.locale, this.guildID, this));
        });

        //Load permissions from database
        final String query = "SELECT action,requiredRank,requiredRole FROM Permissions WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
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
                    permissions.add(new CommandPermission(action, rank, requiredRole));
                }
            }
        }
        return permissions;
    }

    /**
     * Get permission from database for given action
     *
     * @param command Action to get permission for
     * @return Optional containing permission if found
     * @throws SQLException if database connection failed
     */
    protected Optional<CommandPermission> getPermission(final String command) throws SQLException {
        final String query = "SELECT action,requiredRank,requiredRole FROM Permissions "
                + "WHERE guild = ? AND (action = ? OR ? LIKE action || ' %');";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, command);
            ps.setString(3, command);
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
                    return Optional.of(new CommandPermission(action, rank, requiredRole));
                }
            }
        }
        return Optional.empty();
    }

}
