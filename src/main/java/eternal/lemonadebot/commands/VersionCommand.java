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
package eternal.lemonadebot.commands;

import eternal.lemonadebot.commandtypes.OwnerCommand;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.CommandMatcher;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Command to check bot version
 *
 * @author Neutroni
 */
public class VersionCommand extends OwnerCommand {

    private final DatabaseManager db;

    /**
     * Constructor
     *
     * @param database database to use
     */
    VersionCommand(DatabaseManager database) {
        this.db = database;
    }

    @Override
    public String getCommand() {
        return "version";
    }

    @Override
    public String getHelp() {
        return "Syntax: version\n+"
                + "Shows the current version of bot";
    }

    @Override
    public void respond(CommandMatcher message) {
        final TextChannel channel = message.getTextChannel();
        channel.sendMessage("Current bot version: " + this.db.getVersionString()).queue();
    }

}
