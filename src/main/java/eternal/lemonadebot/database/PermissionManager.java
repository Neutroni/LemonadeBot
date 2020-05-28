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
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.permissions.CommandPermission;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class PermissionManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Connection conn;
    private final long guildID;
    private final Map<String, CommandPermission> permissions = Collections.synchronizedMap(new HashMap<>());
    private final CommandPermission adminPermission;

    public PermissionManager(Connection conn, long guildID) {
        this.conn = conn;
        this.guildID = guildID;
        this.adminPermission = new CommandPermission(MemberRank.ADMIN, guildID);
        loadPermissions();
    }

    /**
     * Check if user has required permission to perform action
     *
     * @param member User to check
     * @param action Action user is trying to perform
     * @return True if user can perform the action
     */
    public boolean hasPermission(Member member, String action) {
        long keyLength = 0;
        CommandPermission perm = this.adminPermission;

        for (Entry<String, CommandPermission> p : this.permissions.entrySet()) {
            final String key = p.getKey();
            if (!action.startsWith(key)) {
                continue;
            }
            final long newKeyLength = key.length();
            if (newKeyLength > keyLength) {
                perm = p.getValue();
                keyLength = newKeyLength;
            }
        }
        return perm.hashPermission(member);
    }

    /**
     * Get permission by action string
     *
     * @param action action to get permission for
     * @return Optional containing the permission
     */
    public Optional<CommandPermission> getPermission(String action) {
        return Optional.ofNullable(this.permissions.get(action));
    }

    public boolean setPermission(String action, CommandPermission perm) throws SQLException {
        this.permissions.put(action, perm);
        final String query = "INSERT OR REPLACE INTO Permissions(guild,action,requiredRank,requiredRole) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setString(2, action);
            ps.setString(3, perm.getRequiredRank().name());
            ps.setLong(4, perm.getRequiredRoleID());
            return (ps.executeUpdate() > 0);
        }
    }

    private void loadPermissions() {
        //Load default permissions
        for (final ChatCommand c : CommandProvider.getCommands()) {
            final CommandPermission perm = new CommandPermission(c.getDefaultRank(), guildID);
            this.permissions.put(c.getCommand(), perm);
        }

        //Load permissions from database
        final String query = "SELECT action,requiredRank,requiredRole FROM Permissions WHERE guild = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try (ResultSet rs = ps.executeQuery()) {
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
                    final CommandPermission perm = new CommandPermission(rank, requiredRole);
                    permissions.put(action, perm);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading permissions from database failed");
            LOGGER.warn(e.getMessage());
            LOGGER.trace(e);
        }
    }

}
