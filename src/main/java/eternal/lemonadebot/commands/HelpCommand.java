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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
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
     * @param db database to use for checking if command is custom command
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
    public void respond(CommandMatcher matcher) {
        final Optional<TextChannel> optChannel = matcher.getTextChannel();
        final Optional<Member> optMember = matcher.getMember();
        if (optChannel.isEmpty() || optMember.isEmpty()) {
            matcher.getMessageChannel().sendMessage("Commands are specific to discord servers and must be edited on one").queue();
            return;
        }
        final TextChannel textChannel = optChannel.get();
        final Member sender = optMember.get();

        final String[] options = matcher.getArguments(1);
        if (options.length == 0) {
            listCommands(sender, textChannel);
            return;
        }
        final String name = options[0];
        listHelp(sender, textChannel, name);
    }

    @Override
    public String getHelp() {
        return "Syntax: help [command]\n"
                + "help without arguments shows list of commands.\n"
                + "help [command] shows help for [command].\n"
                + "[] indicates optional arguments, <> nessessary one.";
    }

    private void listHelp(Member sender, TextChannel textChannel, String name) {
        final Optional<ChatCommand> opt = commands.getCommand(name);
        if (opt.isPresent()) {
            final ChatCommand com = opt.get();
            if (commandParser.hasPermission(sender, com)) {
                final EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Help for command: " + com.getCommand());
                eb.setDescription(com.getHelp());
                textChannel.sendMessage(eb.build()).queue();
            } else {
                textChannel.sendMessage("You do not have permission to run that command, as such no help was provided").queue();
            }
            return;
        }
        final Optional<CustomCommand> custom = customCommands.getCommand(name);
        if (custom.isPresent()) {
            textChannel.sendMessage("User defined custom command, see command \"help custom\" for details.").queue();
            return;
        }
        textChannel.sendMessage("No such command: " + name).queue();
    }

    private void listCommands(Member sender, TextChannel textChannel) {

        //Construct the list of commands
        final StringBuilder sb = new StringBuilder();

        for (ChatCommand c : commands.getCommands()) {
            if (commandParser.hasPermission(sender, c)) {
                sb.append(' ').append(c.getCommand()).append('\n');
            }
        }

        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Commands:");
        eb.setDescription(sb.toString());

        textChannel.sendMessage(eb.build()).queue();
    }

}
