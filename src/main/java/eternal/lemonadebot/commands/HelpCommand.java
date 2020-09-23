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

import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.LemonadeBot;
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.PermissionManager;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.translation.TranslationKey;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
    public String getCommand(Locale locale) {
        return TranslationKey.COMMAND_HELP.getTranslation(locale);
    }

    @Override
    public String getDescription(Locale locale) {
        return TranslationKey.DESCRIPTION_HELP.getTranslation(locale);
    }

    @Override
    public String getHelpText(Locale locale) {
        return TranslationKey.SYNTAX_HELP.getTranslation(locale);
    }

    @Override
    public Map<String, CommandPermission> getDefaultRanks(Locale locale, long guildID) {
        return Map.of(getCommand(locale),
                new CommandPermission(MemberRank.USER, guildID));
    }

    @Override
    public void respond(CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();

        final String[] options = matcher.getArguments(1);
        if (options.length == 0) {
            //Help for this command
            final EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(getCommand(locale) + " - " + getDescription(locale));
            final String helpText = TranslationKey.SYNTAX_HELP.getTranslation(locale);
            eb.setDescription(helpText);
            eb.setFooter(TranslationKey.BOT_VERSION.getTranslation(locale) + LemonadeBot.BOT_VERSION);
            textChannel.sendMessage(eb.build()).queue();
            return;
        }
        final String name = options[0];
        if (TranslationKey.ACTION_COMMANDS.getTranslation(locale).equals(name)) {
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
     * @param guildData Guildata of the guild request originated from
     * @param name Name of the command to get help for
     */
    private void listHelp(CommandMatcher matcher, GuildDataStore guildData, String name) {
        final TextChannel textChannel = matcher.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final Locale locale = guildConf.getLocale();
        final PermissionManager permissions = guildData.getPermissionManager();
        final CommandProvider commands = guildData.getCommandProvider();

        //Get the command with the name user provided
        final Optional<ChatCommand> opt = commands.getCommand(name);
        if (opt.isPresent()) {
            final ChatCommand com = opt.get();
            final Member member = matcher.getMember();

            if (permissions.hasPermission(member, name)) {
                final EmbedBuilder eb = new EmbedBuilder();
                final String description = com.getDescription(locale);
                eb.setTitle(name + " - " + description);
                final String helpText = com.getHelpText(locale);
                eb.setDescription(helpText);
                textChannel.sendMessage(eb.build()).queue();
            } else {
                textChannel.sendMessage(TranslationKey.ERROR_PERMISSION_DENIED.getTranslation(locale)).queue();
            }
            return;
        }
        //Did not find a command
        textChannel.sendMessage(TranslationKey.ERROR_NO_SUCH_COMMAND.getTranslation(locale) + name).queue();
    }

    /**
     * Respond with list of commands available to the user
     *
     * @param matcher Request matcher
     * @param permissions Used to check if user has permission to the commands
     * @param locale TranslationManager to get command names from
     */
    private void listCommands(CommandMatcher matcher, PermissionManager permissions, Locale locale) {
        //Construct the list of commands
        final StringBuilder sb = new StringBuilder();
        final Member member = matcher.getMember();

        for (ChatCommand c : CommandProvider.COMMANDS) {
            final String commandName = c.getCommand(locale);
            if (permissions.hasPermission(member, commandName)) {
                sb.append(c.getCommand(locale)).append(" - ");
                final String description = c.getDescription(locale);
                sb.append(description).append('\n');
            }
        }

        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(TranslationKey.HEADER_COMMANDS.getTranslation(locale));
        eb.setDescription(sb.toString());

        matcher.getTextChannel().sendMessage(eb.build()).queue();
    }

}
