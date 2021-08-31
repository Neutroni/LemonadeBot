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
import eternal.lemonadebot.config.ConfigCache;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.radixtree.RadixTree;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Neutroni
 */
public class PermissionManagerCache extends PermissionManager {

    private final Map<Long, RadixTree<CommandPermission>> permissions;

    /**
     * Constructor
     *
     * @param db DataSource to get connection from
     * @param config Config cache to get configmanager from
     * @param commands Built in commands
     */
    public PermissionManagerCache(final DatabaseManager db, final ConfigCache config, final CommandList commands) {
        super(db, config, commands);
        this.permissions = new ConcurrentHashMap<>();
    }

    @Override
    public boolean setPermission(final CommandPermission perm) throws SQLException {
        final long guildID = perm.getGuildID();
        final RadixTree<CommandPermission> guildPermissions = getPermissionsForGuild(guildID);
        final String action = perm.getAction();
        guildPermissions.put(action, perm);
        return super.setPermission(perm);
    }

    @Override
    Collection<CommandPermission> getPermissions(final long guildID) throws SQLException {
        final RadixTree<CommandPermission> guildPermissions = getPermissionsForGuild(guildID);
        return guildPermissions.getValues();
    }

    @Override
    protected Optional<CommandPermission> getPermission(final String action, final long guildID) throws SQLException {
        final RadixTree<CommandPermission> guildPermissions = getPermissionsForGuild(guildID);
        return guildPermissions.get(action);
    }

    private RadixTree<CommandPermission> getPermissionsForGuild(final long guildID) throws SQLException {
        //Check if permissions for a guild are already cached
        final RadixTree<CommandPermission> guildPermissions = this.permissions.get(guildID);
        if (guildPermissions != null) {
            //Already in cache, return the cache
            return guildPermissions;
        }
        //Not in the cache, get permissions from database
        final Collection<CommandPermission> permissionList = super.getPermissions(guildID);
        final RadixTree<CommandPermission> loadedCooldowns = new RadixTree<>(null);
        permissionList.forEach(cd -> {
            loadedCooldowns.put(cd.getAction(), cd);
        });
        //Add loaded permissions to the cache
        this.permissions.put(guildID, loadedCooldowns);
        return loadedCooldowns;
    }

}
