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

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.commandtypes.UserCommand;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.CommandMatcher;
import java.util.Optional;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author Neutroni
 */
class HelpCommand extends UserCommand {

    private final PermissionManager permissions;
    private final CommandProvider commands;

    /**
     * Constructor
     *
     * @param permissionManager manager to check user permissions with
     * @param commandProvider CommandProvider to search commands in
     */
    HelpCommand(PermissionManager permissionManager, CommandProvider commandProvider) {
        this.permissions = permissionManager;
        this.commands = commandProvider;
    }

    @Override
    public String getCommand() {
        return "help";
    }

    @Override
    public void respond(CommandMatcher matcher) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Member sender = matcher.getMember();

        final String[] options = matcher.getArguments(1);
        if (options.length == 0) {
            listCommands(sender, textChannel);
            return;
        }
        final String name = options[0];
        listHelp(matcher, name);
    }

    @Override
    public String getHelp() {
        return "Syntax: help [command]\n"
                + "help without arguments shows list of commands.\n"
                + "help [command] shows help for [command].\n"
                + "[] indicates optional arguments, <> nessessary one.";
    }

    private void listHelp(CommandMatcher matcher, String name) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Member sender = matcher.getMember();
        final Optional<? extends ChatCommand> opt = commands.getCommand(name, textChannel.getGuild());
        if (opt.isPresent()) {
            final ChatCommand com = opt.get();
            if (permissions.hasPermission(sender, com)) {
                final EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Help for command: " + com.getCommand());
                eb.setDescription(com.getHelp());
                textChannel.sendMessage(eb.build()).queue();
            } else {
                textChannel.sendMessage("You do not have permission to run that command, as such no help was provided").queue();
            }
            return;
        }
        textChannel.sendMessage("No such command: " + name).queue();
    }

    private void listCommands(Member sender, TextChannel textChannel) {
        //Construct the list of commands
        final StringBuilder sb = new StringBuilder();
        sb.append("Syntax: help <name>\n"
                + "where 'name' is on of following:\n");

        for (ChatCommand c : commands.getCommands()) {
            if (permissions.hasPermission(sender, c)) {
                sb.append(' ').append(c.getCommand()).append('\n');
            }
        }

        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Commands:");
        eb.setDescription(sb.toString());

        textChannel.sendMessage(eb.build()).queue();
    }

}
