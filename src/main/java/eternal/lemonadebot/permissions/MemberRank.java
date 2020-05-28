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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

/**
 * Permissions required to run a command
 *
 * @author Neutroni
 */
public enum MemberRank {
    /**
     * User is anyone on the discord guild that has joined
     */
    USER("Account without roles"),
    /**
     * Members on discord have at least one role
     */
    MEMBER("Account with at least one role"),
    /**
     * Admins are the users that have permission manageserver
     */
    ADMIN("Account with permission ADMINSTRATOR");

    public static String getLevelDescriptions() {
        final StringBuilder sb = new StringBuilder();
        for (MemberRank p : values()) {
            sb.append(p.toString().toLowerCase());
            sb.append(" - ");
            sb.append(p.getDescription());
            sb.append('\n');
        }
        return sb.toString();
    }

    private final String desc;

    private MemberRank(String description) {
        this.desc = description;
    }

    /**
     * Get the descriptiotn for this role
     *
     * @return description string
     */
    public String getDescription() {
        return this.desc;
    }
    
    /**
     * What rank user has
     *
     * @param member user to check
     * @return Rank of the member
     */
    public static MemberRank getRank(Member member){
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            return MemberRank.ADMIN;
        }
        if (member.getRoles().size() > 0) {
            return MemberRank.MEMBER;
        }
        return MemberRank.USER;
    }
}
