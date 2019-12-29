/*
 * The MIT License
 *
 * Copyright 2019 Neutroni.
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
import eternal.lemonadebot.messages.CommandManager;
import eternal.lemonadebot.messages.CommandMatcher;
import java.util.Optional;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author Neutroni
 */
class PrefixCommand extends OwnerCommand {

    private final CommandManager commandParser;

    /**
     * Constructor
     *
     * @param parser parser to parse commands with
     */
    PrefixCommand(CommandManager parser) {
        this.commandParser = parser;
    }

    @Override
    public String getCommand() {
        return "prefix";
    }

    @Override
    public String getHelp() {
        return "Syntax: prefix <prefix>\n"
                + "Set the command prefix used to call this bot to [prefix]";
    }

    @Override
    public void respond(CommandMatcher matcher) {
        //Verify we are on a discord server and not a private chat
        final Optional<TextChannel> optChannel = matcher.getTextChannel();
        if (optChannel.isEmpty()) {
            matcher.getMessageChannel().sendMessage("Command prefixes are specific to discord servers and must be edited on one").queue();
            return;
        }
        final TextChannel textChannel = optChannel.get();

        //Check if user provide prefix
        final String[] options = matcher.getArguments(1);
        if (options.length == 0) {
            textChannel.sendMessage("Provide a prefix to set commandprefix to.").queue();
            return;
        }

        //Update the prefix
        final String newPrefix = options[0];
        final boolean updateSuccess = commandParser.setPrefix(newPrefix, textChannel.getGuild());
        if (updateSuccess) {
            textChannel.sendMessage("Updated prefix succesfully to: " + newPrefix).queue();
        } else {
            textChannel.sendMessage("Storing prefix in DB failed, will still use new prefix until reboot, re-issue command once DB issue is fixed.").queue();
        }
    }

}
