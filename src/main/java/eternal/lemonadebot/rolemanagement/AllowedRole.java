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

import java.util.ResourceBundle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

/**
 *
 * @author Neutroni
 */
public class AllowedRole {

    private final long guildID;
    private final long roleID;
    private final String description;

    /**
     * Constructor
     *
     * @param role Role to allow
     * @param description Description for role
     */
    AllowedRole(final Role role, final String description) {
        this.guildID = role.getGuild().getIdLong();
        this.roleID = role.getIdLong();
        this.description = description;
    }

    /**
     * Constructor
     *
     * @param role role id
     * @param description Description for this role
     */
    AllowedRole(final long role, final String description, final long guildID) {
        this.roleID = role;
        this.description = description;
        this.guildID = guildID;
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
     * Get the id the role is from
     *
     * @return guild id
     */
    long getGuildID() {
        return this.guildID;
    }

    /**
     * Get string representation of the event for listing events
     *
     * @param locale Locale to return the list element in
     * @param guild guild to get the role name from
     * @return String
     */
    String toListElement(final ResourceBundle locale, final Guild guild) {
        final Role role = guild.getRoleById(this.roleID);
        final String template = locale.getString("ROLE_COMMAND_LIST_ELEMENT");

        //Get the role as mention
        final String roleName;
        if (role == null) {
            roleName = locale.getString("ROLE_MISSING");
        } else {
            roleName = role.getAsMention();
        }

        //Get the description for the role
        final String roleDescription;
        if (this.description == null) {
            roleDescription = locale.getString("ROLE_NO_DESCRIPTION");
        } else {
            roleDescription = this.description;
        }

        return String.format(template, roleName, roleDescription);
    }
}
