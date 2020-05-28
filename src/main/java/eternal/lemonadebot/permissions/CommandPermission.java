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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class CommandPermission {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MemberRank rank;
    private final long roleID;

    /**
     * Constructor
     *
     * @param rank Rank required for this permission
     * @param role Role required for this permission
     */
    public CommandPermission(final MemberRank rank, final long role) {
        this.rank = rank;
        this.roleID = role;
    }

    /**
     * Check if member has permission that is required
     *
     * @param member Member to check
     * @return true if member has the permissions requiredF
     */
    public boolean hashPermission(final Member member) {
        //Check that user has required rank
        if (MemberRank.getRank(member).ordinal() < this.rank.ordinal()) {
            return false;
        }

        //Check if anyone with needed rank can run the command
        final Guild guild = member.getGuild();
        if (this.roleID == guild.getIdLong()) {
            return true;
        }

        //Check if user has needed role
        final Role requiredRole = guild.getRoleById(this.roleID);
        if (requiredRole == null) {
            //User has rank but role could not be found
            LOGGER.warn("Could not find a role needed for permission "
                    + "Role: " + this.roleID + " in Guild: " + guild.getId());
            return true;
        }
        return member.getRoles().contains(requiredRole);
    }

    /**
     * Get the rank that is needed for this permission
     *
     * @return MemberRank
     */
    public MemberRank getRequiredRank() {
        return this.rank;
    }

    /**
     * Get the ID of the role that is required for this permission
     *
     * @return ID of the rolef
     */
    public long getRequiredRoleID() {
        return this.roleID;
    }

    @Override
    public String toString() {
        return "Required rank: " + this.rank.name() + "required role id: " + this.roleID;
    }

}
