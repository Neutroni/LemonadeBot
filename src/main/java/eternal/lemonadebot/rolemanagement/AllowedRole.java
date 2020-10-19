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

import eternal.lemonadebot.translation.TranslationKey;
import java.util.Locale;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

/**
 *
 * @author Neutroni
 */
public class AllowedRole {

    private final long roleID;
    private final String description;

    /**
     * Constructor
     *
     * @param role Role to allow
     * @param description Description for role
     */
    AllowedRole(final Role role, final String description) {
        this.roleID = role.getIdLong();
        this.description = description;
    }

    /**
     * Constructor
     *
     * @param role role id
     * @param description Description for this role
     */
    AllowedRole(final long role, final String description) {
        this.roleID = role;
        this.description = description;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.roleID);
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof AllowedRole) {
            final AllowedRole otherEvent = (AllowedRole) other;
            return (this.roleID == otherEvent.getRoleID());
        }
        return false;
    }

    /**
     * Get the id of role
     *
     * @return RoleID
     */
    long getRoleID() {
        return this.roleID;
    }

    /**
     * Get the description of this event
     *
     * @return description for this event
     */
    String getDescription() {
        return this.description;
    }

    /**
     * Get string representation of the event for listing events
     *
     * @param locale Locale to return the list element in
     * @param guild guild to get the role name from
     * @return String
     */
    String toListElement(final Locale locale, final Guild guild) {
        final Role role = guild.getRoleById(this.roleID);
        final String template = TranslationKey.ROLE_COMMAND_LIST_ELEMENT.getTranslation(locale);

        //Get the role as mention
        final String roleName;
        if (role == null) {
            roleName = TranslationKey.ROLE_MISSING.getTranslation(locale);
        } else {
            roleName = role.getAsMention();
        }

        //Get the description for the role
        final String roleDescription;
        if (this.description == null) {
            roleDescription = TranslationKey.ROLE_NO_DESCRIPTION.getTranslation(locale);
        } else {
            roleDescription = this.description;
        }

        return String.format(template, roleName, roleDescription);
    }

}
