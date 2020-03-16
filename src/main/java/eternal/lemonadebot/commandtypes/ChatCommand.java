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
import eternal.lemonadebot.permissions.CommandPermission;
import net.dv8tion.jda.api.entities.Guild;

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
     * Help text for command
     *
     * @return help
     */
    public String getHelp();

    /**
     * Which roles have permssion to run this command
     *
     * @param guild Guild to check permission in
     * @return CommandPermission needed to run this command
     */
    public CommandPermission getPermission(Guild guild);

    /**
     * Responds to a message
     *
     * @param message Message contents
     */
    public void respond(CommandMatcher message);
}
