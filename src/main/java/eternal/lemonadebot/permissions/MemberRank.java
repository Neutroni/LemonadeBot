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

import eternal.lemonadebot.translation.TranslationCache;
import java.text.Collator;
import java.util.ResourceBundle;
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
    USER("RANK_USER", "RANK_DESCRIPTION_USER"),
    /**
     * Members on discord have at least one role
     */
    MEMBER("RANK_MEMBER", "RANK_DESCRIPTION_MEMBER"),
    /**
     * Admins are the users that have permission manageServer
     */
    ADMIN("RANK_ADMIN", "RANK_DESCRIPTION_ADMIN"),
    /**
     * Owner of the server
     */
    SERVER_OWNER("RANK_SERVER_OWNER", "RANK_DESCRIPTION_SERVER_OWNER");

    /**
     * Get the names and descriptions of rank values in locale
     *
     * @param locale Locale to get description in
     * @return Description in locale
     */
    public static String getLevelDescriptions(final ResourceBundle locale) {
        final StringBuilder sb = new StringBuilder();
        for (final MemberRank p : values()) {
            sb.append(locale.getString(p.name));
            sb.append(" - ");
            sb.append(locale.getString(p.desc));
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Get rank by translated name
     *
     * @param rankName name of the rank to find
     * @param translationCache Cache to retrieve translated names from
     * @return MemberRank if found
     * @throws IllegalArgumentException if no matching rank could be found
     */
    public static MemberRank getByLocalizedName(final String rankName, TranslationCache translationCache) throws IllegalArgumentException {
        final Collator collator = translationCache.getCollator();
        final ResourceBundle locale = translationCache.getResourceBundle();
        for (final MemberRank rank : MemberRank.values()) {
            final String localRankName = locale.getString(rank.getNameKey());
            if (collator.equals(rankName, localRankName)) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Unknown rank name: " + rankName);
    }

    private final String name;
    private final String desc;

    MemberRank(final String name, final String description) {
        this.name = name;
        this.desc = description;
    }

    /**
     * Get the TranslationKey for the name of this rank
     *
     * @return TranslationKey
     */
    public String getNameKey() {
        return this.name;
    }

    /**
     * What rank user has
     *
     * @param member user to check
     * @return Rank of the member
     */
    public static MemberRank getRank(final Member member) {
        if (member.isOwner()) {
            return MemberRank.SERVER_OWNER;
        }
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            return MemberRank.ADMIN;
        }
        if (member.getRoles().size() > 0) {
            return MemberRank.MEMBER;
        }
        return MemberRank.USER;
    }
}
