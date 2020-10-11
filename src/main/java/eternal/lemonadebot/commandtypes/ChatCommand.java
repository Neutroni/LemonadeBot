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
package eternal.lemonadebot.commandtypes;

import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import java.util.Collection;
import java.util.Locale;

/**
 * Interface all commands must implement
 *
 * @author Neutroni
 */
public interface ChatCommand {

    /**
     * Every action has a command it is called with
     *
     * @param locale Locale to get the command name inF
     * @return the command to activate this action
     */
    public String getCommand(Locale locale);

    /**
     * Short description for command, used for command listings
     *
     * @param locale Locale to return the description in
     * @return Description for what this command does
     */
    public String getDescription(Locale locale);

    /**
     * Help text for command, usage info for the command
     *
     * @param locale Locale to return the help in
     * @return help text for the command
     */
    public String getHelpText(Locale locale);

    /**
     * What rank is needed to run this command by default
     *
     * @param locale Locale to get the ranks for
     * @param guildID ID of the guild used to create commandpermission
     * @return The default rank needed to run this command
     */
    public Collection<CommandPermission> getDefaultRanks(Locale locale, long guildID);
    
    /**
     * Responds to a message
     *
     * @param matcher Message contents
     * @param guildData data for the guild the message was sent in
     */
    void respond(CommandMatcher matcher, GuildDataStore guildData);

}
