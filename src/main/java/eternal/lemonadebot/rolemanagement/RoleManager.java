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
package eternal.lemonadebot.rolemanagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

/**
 *
 * @author Neutroni
 */
public class RoleManager {

    private final DataSource dataSource;

    /**
     * Constructor
     *
     * @param ds database connection
     */
    public RoleManager(final DataSource ds) {
        this.dataSource = ds;
    }

    /**
     * Add role to list of allowed roles
     *
     * @param role Role to allow bot to assign
     * @return true if event was added
     * @throws SQLException If database connection failed
     */
    boolean allowRole(final AllowedRole role) throws SQLException {
        final String query = "INSERT OR IGNORE INTO Roles(guild,role,description) VALUES(?,?,?);";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, role.getGuildID());
            ps.setLong(2, role.getRoleID());
            ps.setString(3, role.getDescription());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Remove role from list of allowed roles
     *
     * @param role Role to remove from the list of allowed roles
     * @return true if event was removed successfully
     * @throws SQLException if database connection failed
     */
    boolean disallowRole(final Role role) throws SQLException {
        final String query = "DELETE FROM Roles Where guild = ? AND role = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, role.getGuild().getIdLong());
            ps.setLong(2, role.getIdLong());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get list of roles
     *
     * @return list of roles
     * @throws SQLException if database connection failed
     */
    Collection<AllowedRole> getRoles(final Guild guild) throws SQLException {
        final long guildID = guild.getIdLong();
        final List<AllowedRole> roles = new ArrayList<>();
        final String query = "SELECT role,description FROM Roles WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final long roleID = rs.getLong("role");
                    final String description = rs.getString("description");
                    final AllowedRole role = new AllowedRole(roleID, description, guildID);
                    roles.add(role);
                }
            }
        }
        return Collections.unmodifiableCollection(roles);
    }

    /**
     * Check if role is allowed to assign
     *
     * @param role Role to check
     * @return true if role can be assigned.
     * @throws SQLException if database connection failed
     */
    boolean isAllowed(final Role role) throws SQLException {
        final String query = "SELECT role FROM Roles WHERE guild = ? AND role = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, role.getGuild().getIdLong());
            ps.setLong(2, role.getIdLong());
            try (final ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Get allowedRole for role if role set as allowed, used in RoleManagerCache
     *
     * @param role Role to get allowed role for if exists
     * @return Optional containing AllowedRole if found
     * @throws SQLException If database connection failed
     */
    protected Optional<AllowedRole> getAllowedRole(final Role role) throws SQLException {
        final long guildID = role.getGuild().getIdLong();
        final String query = "SELECT role,description FROM Roles WHERE guild = ? AND role = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, guildID);
            ps.setLong(2, role.getIdLong());
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final long roleID = rs.getLong("role");
                    final String description = rs.getString("description");
                    return Optional.of(new AllowedRole(roleID, description, guildID));
                }
            }
        }
        return Optional.empty();
    }
}
