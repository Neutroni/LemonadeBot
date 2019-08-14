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

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.commandtypes.UserCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.CustomCommandManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandManager;
import eternal.lemonadebot.messages.CommandMatcher;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author Neutroni
 */
class HelpCommand extends UserCommand {

    private final CommandManager commandParser;
    private final CommandProvider commands;
    private final CustomCommandManager customCommands;

    /**
     * Constructor
     *
     * @param parser parser to use
     * @param provider CommandProvider to search commands in
     */
    HelpCommand(CommandManager parser, CommandProvider provider, DatabaseManager db) {
        this.commandParser = parser;
        this.commands = provider;
        this.customCommands = db.getCustomCommands();
    }

    @Override
    public String getCommand() {
        return "help";
    }

    @Override
    public void respond(Member sender, Message message, TextChannel textChannel) {
        final CommandMatcher m = commandParser.getCommandMatcher(message);
        final String[] options = m.getArguments(1);
        if (options.length == 0) {
            listCommands(sender, textChannel);
            return;
        }
        final String name = options[0];
        listHelp(sender, textChannel, name);
    }

    @Override
    public String getHelp() {
        return "Prints help message for command.";
    }

    private void listHelp(Member sender, TextChannel textChannel, String name) {
        final Optional<ChatCommand> opt = commands.getCommand(name);
        if (opt.isPresent()) {
            final ChatCommand com = opt.get();
            if (commandParser.hasPermission(sender, com)) {
                textChannel.sendMessage(com.getCommand() + '\n' + com.getHelp()).queue();
            } else {
                textChannel.sendMessage("You do not have permission to run that command, as such no help was provided").queue();
            }
            return;
        }
        final Optional<CustomCommand> custom = customCommands.getCommand(name);
        if (custom.isPresent()) {
            final CustomCommand com = custom.get();
            textChannel.sendMessage("User defined custom command, see command \"custom\" for details.").queue();
            return;
        }
        textChannel.sendMessage("No such command: " + name).queue();
    }

    private void listCommands(Member sender, TextChannel textChannel) {

        //Construct the list of commands
        final StringBuilder sb = new StringBuilder();

        sb.append("Commands:\n");
        for (ChatCommand c : commands.getCommands()) {
            if (commandParser.hasPermission(sender, c)) {
                sb.append(' ').append(c.getCommand()).append('\n');
            }
        }
        textChannel.sendMessage(sb.toString()).queue();
    }

}
