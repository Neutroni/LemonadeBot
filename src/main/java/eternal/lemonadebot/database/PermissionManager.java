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

import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.radixtree.RadixTree;
import eternal.lemonadebot.translation.LocaleUpdateListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;
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
        this.permissions = new RadixTree<>(new CommandPermission("", MemberRank.ADMIN, guildID));
        loadPermissions(locale);
    }

    /**
     * Check if user has required permission to perform action
     *
     * @param member User to check
     * @param action Action user is trying to perform
     * @return True if user can perform the action
     */
    public boolean hasPermission(Member member, String action) {
        final CommandPermission perm = this.permissions.get(action);
        return perm.hashPermission(member);
    }

    /**
     * Get permission by fuzzy action string
     *
     * @param action action to get permission for
     * @return CommandPermission for action, if no permission has been set the
     * returned permission will have rank of ADMIN and any role
     */
    public CommandPermission getPermission(String action) {
        return this.permissions.get(action);
    }
    
    public Collection<CommandPermission> getPermissions(){
        return this.permissions.getValues();
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
        this.permissions.add(action, perm);
        final String query = "INSERT OR REPLACE INTO Permissions(guild,action,requiredRank,requiredRole) VALUES(?,?,?,?);";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, action);
            ps.setString(3, perm.getRequiredRank().name());
            ps.setLong(4, perm.getRequiredRoleID());
            return (ps.executeUpdate() > 0);
        }
    }

    /**
     * Update permissions according to locale
     *
     * @param locale Translation to use
     */
    @Override
    public void updateLocale(Locale locale) {
        this.permissions.clear();
        loadPermissions(locale);
    }

    /**
     * Load permissions according to locale
     *
     * @param locale ResourceBundle to get translated commands from
     */
    private void loadPermissions(Locale locale) {
        //Load default permissions
        for (final ChatCommand c : CommandProvider.COMMANDS) {
            for (final CommandPermission p : c.getDefaultRanks(locale, this.guildID)) {
                this.permissions.add(p.getAction(), p);
            }
        }

        //Load permissions from database
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
                        LOGGER.warn("Ignored permission with malformed rank: " + rankName);
                        continue;
                    }
                    final long requiredRole = rs.getLong("requiredRole");
                    final CommandPermission perm = new CommandPermission(action, rank, requiredRole);
                    permissions.add(action, perm);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading permissions from database failed");
            LOGGER.warn(e.getMessage());
            LOGGER.trace(e);
        }
    }

}
