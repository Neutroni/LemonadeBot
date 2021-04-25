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

import eternal.lemonadebot.LemonadeBot;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.permissions.PermissionManager;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Command used to show help for commands and list available commands
 *
 * @author Neutroni
 */
class HelpCommand implements ChatCommand {

    @Override
    public String getCommand(final ResourceBundle locale) {
        return locale.getString("COMMAND_HELP");
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_HELP");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        return locale.getString("SYNTAX_HELP");
    }

    @Override
    public Collection<CommandPermission> getDefaultRanks(final ResourceBundle locale, final long guildID, final PermissionManager permissions) {
        return List.of(new CommandPermission(getCommand(locale), MemberRank.USER, guildID));
    }

    @Override
    public void respond(final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = guildData.getTranslationCache().getResourceBundle();

        final String[] options = matcher.getArguments(1);
        if (options.length == 0) {
            //Help for this command
            final EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(getCommand(locale) + " - " + getDescription(locale));
            final String helpText = locale.getString("SYNTAX_HELP");
            eb.setDescription(helpText);
            final String template = locale.getString("BOT_VERSION");
            final String footer = String.format(template, LemonadeBot.BOT_VERSION);
            eb.setFooter(footer);
            textChannel.sendMessage(eb.build()).queue();
            return;
        }
        final String name = options[0];
        if (locale.getString("ACTION_COMMANDS").equals(name)) {
            //Respond with list of commands available to the user
            listCommands(matcher, guildData.getPermissionManager(), locale);
            return;
        }

        //Respond with help for command with given name if found
        listHelp(matcher, guildData, name);
    }

    /**
     * *
     * Respond with help of command if available to the user
     *
     * @param matcher Request matcher
     * @param guildData GuildData of the guild request originated from
     * @param name Name of the command to get help for
     */
    private static void listHelp(final CommandMatcher matcher, final GuildDataStore guildData, final String name) {
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = guildData.getTranslationCache().getResourceBundle();
        final PermissionManager permissions = guildData.getPermissionManager();
        final CommandProvider commands = guildData.getCommandProvider();

        //Get the command with the name user provided
        final Optional<ChatCommand> opt = commands.getCommand(name);
        if (opt.isPresent()) {
            final ChatCommand com = opt.get();
            final Member member = matcher.getMember();

            if (permissions.hasPermission(member, com, name)) {
                final EmbedBuilder eb = new EmbedBuilder();
                final String description = com.getDescription(locale);
                eb.setTitle(name + " - " + description);
                final String helpText = com.getHelpText(locale);
                eb.setDescription(helpText);
                textChannel.sendMessage(eb.build()).queue();
            } else {
                textChannel.sendMessage(locale.getString("ERROR_PERMISSION_DENIED")).queue();
            }
            return;
        }
        //Did not find a command
        textChannel.sendMessage(locale.getString("ERROR_NO_SUCH_COMMAND") + name).queue();
    }

    /**
     * Respond with list of commands available to the user
     *
     * @param matcher Request matcher
     * @param permissions Used to check if user has permission to the commands
     * @param locale TranslationManager to get command names from
     */
    private static void listCommands(final CommandMatcher matcher, final PermissionManager permissions, final ResourceBundle locale) {
        //Construct the list of commands
        final StringBuilder sb = new StringBuilder();
        final Member member = matcher.getMember();

        for (final ChatCommand c : CommandProvider.COMMANDS) {
            if (permissions.hasPermission(member, c, c.getCommand(locale))) {
                sb.append(c.getCommand(locale)).append(" - ");
                final String description = c.getDescription(locale);
                sb.append(description).append('\n');
            }
        }

        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(locale.getString("HEADER_COMMANDS"));
        eb.setDescription(sb.toString());

        matcher.getTextChannel().sendMessage(eb.build()).queue();
    }

}
