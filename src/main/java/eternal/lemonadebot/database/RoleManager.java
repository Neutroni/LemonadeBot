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

import eternal.lemonadebot.dataobjects.AllowedRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class RoleManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DataSource dataSource;
    private final long guildID;
    private final Map<Long, AllowedRole> roles = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param ds database connection
     * @param guildID
     */
    RoleManager(DataSource ds, long guildID) {
        this.dataSource = ds;
        this.guildID = guildID;
        loadRoles();
    }

    /**
     * Add event to database
     *
     * @param role Role to allow bot to grant
     * @return true if event was added
     * @throws SQLException If database connection failed
     */
    public boolean allowRole(AllowedRole role) throws SQLException {
        this.roles.putIfAbsent(role.getRoleID(), role);

        //Add to database
        final String query = "INSERT OR IGNORE INTO Roles(guild,role,description) VALUES(?,?,?);";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setLong(2, role.getRoleID());
            ps.setString(3, role.getDescription());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Remove event from database
     *
     * @param role Role to remove from list of allowed roles
     * @return true if event was removed succesfully
     * @throws SQLException if database connection failed
     */
    public boolean disallowRole(Role role) throws SQLException {
        this.roles.remove(role.getIdLong());

        //Remove from database
        final String query = "DELETE FROM Roles Where guild = ? AND role = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setLong(2, role.getIdLong());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Get list of roles
     *
     * @return list of roles
     */
    public Collection<AllowedRole> getRoles() {
        return Collections.unmodifiableCollection(this.roles.values());
    }

    /**
     * Check if role is allowed to assing
     *
     * @param role Role to check
     * @return true if role can be assigned.
     */
    public boolean isAllowed(Role role) {
        return this.roles.containsKey(role.getIdLong());
    }

    /**
     * Load event from database
     *
     * @return
     * @throws SQLException if database connection failed
     */
    private void loadRoles() {
        final String query = "SELECT role,description FROM Roles WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final long roleID = rs.getLong("role");
                    final String description = rs.getString("description");
                    final AllowedRole role = new AllowedRole(roleID, description);
                    roles.put(roleID, role);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading roles from database failed");
            LOGGER.warn(e.getMessage());
            LOGGER.trace(e);
        }
    }
}
