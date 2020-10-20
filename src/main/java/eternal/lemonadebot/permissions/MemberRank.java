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

import eternal.lemonadebot.translation.TranslationKey;
import java.text.Collator;
import java.util.Locale;
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
    USER(TranslationKey.RANK_USER, TranslationKey.RANK_DESCRIPTION_USER),
    /**
     * Members on discord have at least one role
     */
    MEMBER(TranslationKey.RANK_MEMBER, TranslationKey.RANK_DESCRIPTION_MEMBER),
    /**
     * Admins are the users that have permission manageServer
     */
    ADMIN(TranslationKey.RANK_ADMIN, TranslationKey.RANK_DESCRIPTION_ADMIN),
    /**
     * Owner of the server
     */
    SERVER_OWNER(TranslationKey.RANK_SERVER_OWNER, TranslationKey.RANK_DESCRIPTION_SERVER_OWNER);

    /**
     * Get the names and descriptions of rank values in locale
     *
     * @param locale Locale to get description in
     * @return Description in locale
     */
    public static String getLevelDescriptions(final Locale locale) {
        final StringBuilder sb = new StringBuilder();
        for (final MemberRank p : values()) {
            sb.append(p.name.getTranslation(locale));
            sb.append(" - ");
            sb.append(p.desc.getTranslation(locale));
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Get rank by translated name
     *
     * @param rankName name of the rank to find
     * @param locale locale the name is in
     * @param collator Collator to use to compare rank names
     * @return MemberRank if found
     * @throws IllegalArgumentException if no matching rank could be found
     */
    public static MemberRank getByLocalizedName(final String rankName, final Locale locale, final Collator collator) throws IllegalArgumentException {
        for (final MemberRank rank : MemberRank.values()) {
            final String localRankName = rank.getNameKey().getTranslation(locale);
            if (collator.equals(rankName, localRankName)) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Unknown rank name: " + rankName);
    }

    private final TranslationKey name;
    private final TranslationKey desc;

    MemberRank(final TranslationKey name, final TranslationKey description) {
        this.name = name;
        this.desc = description;
    }

    /**
     * Get the TranslationKey for the name of this rank
     *
     * @return TranslationKey
     */
    public TranslationKey getNameKey() {
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
