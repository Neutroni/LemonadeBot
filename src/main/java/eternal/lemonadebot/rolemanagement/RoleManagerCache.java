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

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Role;

/**
 *
 * @author Neutroni
 */
public class RoleManagerCache extends RoleManager {

    private boolean rolesLoaded = false;
    private final Map<Long, AllowedRole> roles = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param ds database connection
     * @param guildID if of the guild to store list of allowed roles for
     */
    public RoleManagerCache(DataSource ds, long guildID) {
        super(ds, guildID);
    }

    @Override
    boolean allowRole(AllowedRole role) throws SQLException {
        this.roles.putIfAbsent(role.getRoleID(), role);
        return super.allowRole(role);
    }

    @Override
    boolean disallowRole(Role role) throws SQLException {
        this.roles.remove(role.getIdLong());
        return super.disallowRole(role);
    }

    @Override
    Collection<AllowedRole> getRoles() throws SQLException {
        if (rolesLoaded) {
            return Collections.unmodifiableCollection(this.roles.values());
        }
        final Collection<AllowedRole> roleCollection = super.getRoles();
        roleCollection.forEach((AllowedRole role) -> {
            this.roles.putIfAbsent(role.getRoleID(), role);
        });
        this.rolesLoaded = true;
        return roleCollection;
    }

    @Override
    boolean isAllowed(Role role) throws SQLException {
        if (rolesLoaded) {
            return this.roles.containsKey(role.getIdLong());
        }
        final AllowedRole r = this.roles.get(role.getIdLong());
        if (r == null) {
            //Cache did not have the role
            final Optional<AllowedRole> roleAllowed = super.getAllowedRole(role);
            if (roleAllowed.isPresent()) {
                this.roles.put(role.getIdLong(), roleAllowed.get());
                return true;
            }
            //Database did not have the role
            return false;
        }

        //Cache had the role but is not fully loaded
        return true;
    }

}
