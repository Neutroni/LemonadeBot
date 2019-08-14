/*
 * The MIT License
 *
 * Copyright 2019 joonas.
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

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.commandtypes.UserCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandParser;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author joonas
 */
class HelpCommand extends UserCommand {

    private final CommandParser commandParser;

    /**
     * Constructor
     *
     * @param parser parser to use
     */
    HelpCommand(CommandParser parser) {
        this.commandParser = parser;
    }

    @Override
    public String getCommand() {
        return "help";
    }

    @Override
    public void respond(Member member, Message message, TextChannel textChannel) {
        final CommandMatcher m = commandParser.getCommandMatcher(message);
        final String[] options = m.getArguments(1);
        if (options.length == 0) {
            textChannel.sendMessage("Provide command to search help for, use commands for list of commands.").queue();
            return;
        }
        final String name = options[0];
        final Optional<ChatCommand> opt = commandParser.getCommand(name);
        if (opt.isPresent()) {
            final ChatCommand com = opt.get();
            if (commandParser.hasPermission(member, com)) {
                textChannel.sendMessage(com.getCommand() + '\n' + com.getHelp()).queue();
            } else {
                textChannel.sendMessage("You do not have permission to run that command, as such no help was provided").queue();
            }
            return;
        }
        final Optional<CustomCommand> custom = commandParser.getCustomCommand(name);
        if (custom.isPresent()) {
            final CustomCommand com = custom.get();
            textChannel.sendMessage("User defined custom command, see command \"custom\" for details.").queue();
            return;
        }
        textChannel.sendMessage("No such command: " + name).queue();
    }

    @Override
    public String getHelp() {
        return "Prints help message for command.";
    }

}
