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
import eternal.lemonadebot.radixtree.RadixTree;
import eternal.lemonadebot.translation.LocaleUpdateListener;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
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
    private final RadixTree<CommandPermission> permissions;
    private final CommandPermission adminPermission;
    private volatile Locale locale;

    /**
     * Constructor
     *
     * @param ds DataSource to get connection from
     * @param guildID ID of the guild to store permissions for
     * @param locale Locale to use for loading default permissions
     */
    public PermissionManager(DataSource ds, long guildID, Locale locale) {
        this.dataSource = ds;
        this.guildID = guildID;
        this.permissions = new RadixTree<>(null);
        this.adminPermission = new CommandPermission("", MemberRank.ADMIN, guildID);
        this.locale = locale;
        loadPermissions();
    }

    /**
     * Check if user has required permission to perform action
     *
     * @param member User to check
     * @param command Command user is trying to run
     * @param action Action user is trying to perform
     * @return True if user can perform the action
     */
    public boolean hasPermission(Member member, ChatCommand command, String action) {
        final CommandPermission perm = getPermission(command, action);
        return perm.hashPermission(member);
    }

    /**
     * Update permissions according to locale
     *
     * @param locale Translation to use
     */
    @Override
    public void updateLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Set the permission required for action
     *
     * @param perm Permission required for the action
     * @return true if update succeeded
     * @throws SQLException if database connetion failed
     */
    public boolean setPermission(CommandPermission perm) throws SQLException {
        final String action = perm.getAction();
        this.permissions.put(action, perm);
        final String query = "INSERT OR REPLACE INTO Permissions(guild,action,requiredRank,requiredRole) VALUES(?,?,?,?);";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(3, action);
            ps.setString(4, perm.getRequiredRank().name());
            ps.setLong(5, perm.getRequiredRoleID());
            return (ps.executeUpdate() > 0);
        }
    }

    /**
     * Get permission required to run custom commands that do not have
     * any other permission set
     *
     * @return CommandPermission
     */
    public CommandPermission getTemplateRunPermission() {
        final String key = TranslationKey.TEMPLATE_RUN_ACTION.getTranslation(this.locale);
        return this.permissions.get(key).orElse(this.adminPermission);
    }

    /**
     * Get permission by fuzzy action string
     *
     * @param command Command to get permission from if no other permission set
     * @param action action to get permission for
     * @return CommandPermission for action, if no permission has been set the
     * returned permission will have rank of ADMIN and any role
     */
    CommandPermission getPermission(ChatCommand command, String action) {
        final Optional<CommandPermission> optPerm = this.permissions.get(action);
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
        //Get logner of the actions
        final CommandPermission perm = optPerm.orElse(this.adminPermission);
        if (perm.getAction().length() > builtInPerm.getAction().length()) {
            return perm;
        }
        return builtInPerm;
    }

    /**
     * Get list of permissions set for commands
     *
     * @return Collection of permissions
     */
    Collection<CommandPermission> getPermissions() {
        return Collections.unmodifiableCollection(this.permissions.getValues());
    }

    /**
     * Load permissions according to locale
     *
     * @param locale ResourceBundle to get translated commands from
     */
    private void loadPermissions() {
        final String query = "SELECT action,requiredRank,requiredRole FROM Permissions WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try ( ResultSet rs = ps.executeQuery()) {
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
                    this.permissions.put(action, new CommandPermission(action, rank, requiredRole));
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Loading permissions from database failed: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
        }
    }

}
