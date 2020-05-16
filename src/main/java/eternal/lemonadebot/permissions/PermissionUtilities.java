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

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.ConfigManager;
import java.util.EnumSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

/**
 * Various utility functions for checking command permissions
 *
 * @author Neutroni
 */
public class PermissionUtilities {

    /**
     * What rank user has
     *
     * @param member user to check
     * @return Rank of the member
     */
    public static CommandPermission getRank(Member member) {
        final EnumSet<Permission> permissions = member.getPermissions();
        if (permissions.contains(Permission.ADMINISTRATOR)) {
            return CommandPermission.ADMIN;
        }
        if (member.getRoles().size() > 0) {
            return CommandPermission.MEMBER;
        }
        return CommandPermission.USER;
    }

    /**
     * Checks wheter given user has permission to run given command
     *
     * @param member User to check permission for
     * @param config Configmanager to get required permission from
     * @param command Command to check
     * @return Does the person have permission
     */
    public static boolean hasPermission(Member member, ConfigManager config, ChatCommand command) {
        return getRank(member).ordinal() >= command.getPermission(config).ordinal();
    }

    /**
     * Check if member has required permission
     *
     * @param member Member to check
     * @param requiredPermission permission needed
     * @return true if user has permission
     */
    public static boolean hasPermission(Member member, CommandPermission requiredPermission) {
        return getRank(member).ordinal() >= requiredPermission.ordinal();
    }

    /**
     * Check if user has permission to manage content owned by other member
     *
     * @param member User trying to do an action that modifies content
     * @param owner owner of the content, can be null
     * @return true if user has permission
     */
    public static boolean hasPermission(@Nonnull Member member, @Nullable Member owner) {
        //Same person
        if (member.equals(owner)) {
            return true;
        }
        final CommandPermission requesterRank = getRank(member);
        //No owner
        if (owner == null) {
            return (requesterRank.ordinal() >= CommandPermission.ADMIN.ordinal());
        }
        return getRank(owner).ordinal() < requesterRank.ordinal();
    }

}
