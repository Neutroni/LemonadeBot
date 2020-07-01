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

import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.permissions.MemberRank;
import net.dv8tion.jda.api.MessageBuilder;

/**
 * Interface all commands must implement
 *
 * @author Neutroni
 */
public interface ChatCommand {

    /**
     * Every action has a command it is called with
     *
     * @return the command to activate this action
     */
    public String getCommand();

    /**
     * Short description for command
     *
     * @return Description for what this command does
     */
    public String getDescription();

    /**
     * Help text for command
     *
     * @return help
     */
    public String getHelpText();

    /**
     * What rank is needed to run this command by default
     *
     * @return The default rank needed to run this command
     */
    public MemberRank getDefaultRank();

    /**
     * Responds to a message
     *
     * @param message Message contents
     * @param guildData data for the guild the message was sent in
     */
    public void respond(CommandMatcher message, GuildDataStore guildData);
}
