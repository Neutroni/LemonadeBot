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
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandMatcher;
import java.sql.SQLException;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;

/**
 *
 * @author Neutroni
 */
class PrefixCommand extends OwnerCommand {

    private final DatabaseManager db;

    /**
     * Constructor
     *
     * @param database ConfigManager to handle prefix with
     */
    PrefixCommand(DatabaseManager database) {
        this.db = database;
    }

    @Override
    public String getCommand() {
        return "prefix";
    }

    @Override
    public String getHelp() {
        return "Syntax: prefix <prefix>\n"
                + "Set the command prefix used to call this bot to <prefix>";
    }

    @Override
    public void respond(CommandMatcher matcher) {
        final MessageChannel channel = matcher.getMessageChannel();

        //Verify we are on a discord server and not a private chat
        final Optional<Guild> optGuild = matcher.getGuild();
        if (optGuild.isEmpty()) {
            channel.sendMessage("Command prefixes are specific to discord servers and must be edited on one").queue();
            return;
        }
        final Guild guild = optGuild.get();

        //Check if user provide prefix
        final String[] options = matcher.getArguments(1);
        if (options.length == 0) {
            channel.sendMessage("Provide a prefix to set commandprefix to.").queue();
            return;
        }

        //Update the prefix
        final String newPrefix = options[0];
        final ConfigManager guildConf = this.db.getConfig(guild);
        try {
            guildConf.setCommandPrefix(newPrefix);
            channel.sendMessage("Updated prefix succesfully to: " + newPrefix).queue();
        } catch (SQLException ex) {
            channel.sendMessage("Storing prefix in DB failed, will still use new prefix until reboot, re-issue command once DB issue is fixed.").queue();
        }

    }

}
