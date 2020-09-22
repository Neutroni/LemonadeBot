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
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.customcommands.TemplateProvider;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.TemplateManager;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.permissions.PermissionUtilities;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command used to edit custom commands
 *
 * @author Neutroni
 */
public class TemplateCommand implements ChatCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(Locale locale) {
        return TranslationKey.COMMAND_TEMPLATE.getTranslation(locale);
    }

    @Override
    public String getDescription(Locale locale) {
        return TranslationKey.DESCRIPTION_TEMPLATE.getTranslation(locale);
    }

    @Override
    public String getHelpText(Locale locale) {
        final String template = TranslationKey.SYNTAX_TEMPLATE.getTranslation(locale);
        final String keys = TemplateProvider.getHelp(locale);
        return String.format(template, keys);
    }

    @Override
    public Map<String, CommandPermission> getDefaultRanks(Locale locale, long guildID) {
        return Map.of(getCommand(locale),
                new CommandPermission(MemberRank.MEMBER, guildID));
    }

    @Override
    public void respond(CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final Locale locale = guildConf.getLocale();
        final String[] arguments = matcher.getArguments(2);
        if (arguments.length == 0) {
            textChannel.sendMessage(TranslationKey.ERROR_MISSING_OPERATION.getTranslation(locale)).queue();
            return;
        }

        final String actionName = arguments[0];
        final ActionKey action = ActionKey.getAction(actionName, guildConf);
        switch (action) {
            case CREATE: {
                createCustomCommand(arguments, matcher, guildData);
                break;
            }
            case DELETE: {
                deleteCustomCommand(arguments, matcher, guildData);
                break;
            }
            case LIST: {
                listCustomCommands(matcher, guildData);
                break;
            }
            default:
                textChannel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + actionName).queue();
                break;
        }
    }

    private void createCustomCommand(String[] arguments, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();

        if (arguments.length < 2) {
            textChannel.sendMessage(TranslationKey.TEMPLATE_CREATE_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        if (arguments.length < 3) {
            textChannel.sendMessage(TranslationKey.TEMPLATE_CREATE_MISSING_TEMPLATE.getTranslation(locale)).queue();
            return;
        }

        //Check that there is no such built in command
        final String commandName = arguments[1];
        final Optional<ChatCommand> optCommand = CommandProvider.getBuiltInCommand(commandName, locale);
        if (optCommand.isPresent()) {
            textChannel.sendMessage(TranslationKey.TEMPLATE_NAME_RESERVED.getTranslation(locale)).queue();
            return;
        }

        final String commandTemplate = arguments[2];
        final Member sender = matcher.getMember();
        final TemplateManager commands = guildData.getCustomCommands();
        final CustomCommand newAction = new CustomCommand(commandName, commandTemplate, sender.getIdLong());
        try {
            if (commands.addCommand(newAction)) {
                textChannel.sendMessage(TranslationKey.TEMPLATE_CREATE_SUCCESS.getTranslation(locale)).queue();
                return;
            }
            textChannel.sendMessage(TranslationKey.TEMPLATE_ALREADY_EXISTS.getTranslation(locale)).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(TranslationKey.TEMPLATE_SQL_ERROR_ON_CREATE.getTranslation(locale)).queue();
            LOGGER.error("Failure to add custom command: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }

    }

    private void deleteCustomCommand(String[] arguments, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();

        if (arguments.length < 2) {
            textChannel.sendMessage(TranslationKey.TEMPLATE_DELETE_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String commandName = arguments[1];
        final TemplateManager commands = guildData.getCustomCommands();
        final Optional<CustomCommand> optCommand = commands.getCommand(commandName);
        if (optCommand.isEmpty()) {
            final String template = TranslationKey.TEMPLATE_DELETE_NOT_FOUND.getTranslation(locale);
            textChannel.sendMessageFormat(template, commandName).queue();
            return;
        }
        final CustomCommand command = optCommand.get();

        //Check if user has permission to remove the command
        final Member sender = matcher.getMember();
        final Member commandOwner = textChannel.getGuild().getMemberById(command.getOwner());
        final boolean hasPermission = PermissionUtilities.hasPermission(sender, commandOwner);
        if (!hasPermission) {
            textChannel.sendMessage(TranslationKey.TEMPLATE_DELETE_PERMISSION_DENIED.getTranslation(locale)).queue();
            return;
        }

        //Delete the command
        try {
            commands.removeCommand(command);
            textChannel.sendMessage(TranslationKey.TEMPLATE_DELETE_SUCCESS.getTranslation(locale)).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(TranslationKey.TEMPLATE_SQL_ERROR_ON_DELETE.getTranslation(locale)).queue();
            LOGGER.error("Failure to delete custom command: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }
    }

    private void listCustomCommands(CommandMatcher matcher, GuildDataStore guildData) {
        final Set<CustomCommand> coms = guildData.getCustomCommands().getCommands();
        final Locale locale = guildData.getConfigManager().getLocale();
        final Guild guild = matcher.getGuild();
        final TextChannel textChannel = matcher.getTextChannel();
        final MessageBuilder sb = new MessageBuilder(TranslationKey.HEADER_COMMANDS.getTranslation(locale));
        sb.append('\n');
        for (final CustomCommand c : coms) {
            final Member creator = guild.getMemberById(c.getOwner());
            final String creatorName;
            if (creator == null) {
                creatorName = TranslationKey.UNKNOWN_USER.getTranslation(locale);
            } else {
                creatorName = creator.getEffectiveName();
            }
            final String template = TranslationKey.TEMPLATE_COMMAND_LIST_ELEMENT.getTranslation(locale);
            sb.appendFormat(template, c.getCommand(locale), creatorName);
            sb.append('\n');
        }
        if (coms.isEmpty()) {
            sb.append(TranslationKey.TEMPLATE_NO_COMMANDS);
        }
        textChannel.sendMessage(sb.build()).queue();
    }
}
