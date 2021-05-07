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

import eternal.lemonadebot.commands.CommandList;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.radixtree.RadixTree;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javax.sql.DataSource;

/**
 *
 * @author Neutroni
 */
public class PermissionManagerCache extends PermissionManager {

    private volatile boolean permissionsLoaded = false;
    private final RadixTree<CommandPermission> permissions;

    /**
     * Constructor
     *
     * @param ds DataSource to get connection from
     * @param guildID ID of the guild to store permissions for
     * @param locale Locale to use for loading default permissions
     * @param commands Built in commands
     */
    public PermissionManagerCache(final DataSource ds, final long guildID, final ConfigManager locale, final CommandList commands) {
        super(ds, guildID, locale, commands);
        this.permissions = new RadixTree<>(null);
    }

    @Override
    public boolean setPermission(final CommandPermission perm) throws SQLException {
        final String action = perm.getAction();
        this.permissions.put(action, perm);
        return super.setPermission(perm);
    }

    @Override
    Collection<CommandPermission> getPermissions() throws SQLException {
        if (this.permissionsLoaded) {
            return Collections.unmodifiableCollection(this.permissions.getValues());
        }
        final Collection<CommandPermission> perms = super.getPermissions();
        perms.forEach((CommandPermission p) -> {
            this.permissions.put(p.getAction(), p);
        });
        this.permissionsLoaded = true;
        return perms;
    }

    @Override
    protected Optional<CommandPermission> getPermission(final String action) throws SQLException {
        if (!this.permissionsLoaded) {
            //Attempt to load permissions
            getPermissions();
        }
        return this.permissions.get(action);
    }

}
