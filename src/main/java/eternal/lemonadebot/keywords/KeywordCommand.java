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
package eternal.lemonadebot.keywords;

import eternal.lemonadebot.commands.AdminCommand;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.customcommands.TemplateProvider;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.PermissionUtilities;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.PatternSyntaxException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command used to edit keyword based commands
 *
 * @author Neutroni
 */
public class KeywordCommand extends AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(Locale locale) {
        return TranslationKey.COMMAND_KEYWORD.getTranslation(locale);
    }

    @Override
    public String getDescription(Locale locale) {
        return TranslationKey.DESCRIPTION_KEYWORD.getTranslation(locale);
    }

    @Override
    public String getHelpText(Locale locale) {
        final String template = TranslationKey.SYNTAX_KEYWORD.getTranslation(locale);
        final String keys = TemplateProvider.getHelp(locale);
        return String.format(template, keys);
    }

    @Override
    public void respond(CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildConf.getLocale();
        final String[] arguments = matcher.getArguments(1);
        if (arguments.length == 0) {
            textChannel.sendMessage(TranslationKey.ERROR_MISSING_OPERATION.getTranslation(locale)).queue();
            return;
        }

        final String actionName = arguments[0];
        final ActionKey action = translationCache.getActionKey(actionName);
        switch (action) {
            case CREATE: {
                createKeywords(matcher, guildData);
                break;
            }
            case DELETE: {
                deleteKeyword(arguments, matcher, guildData);
                break;
            }
            case LIST: {
                listKeywords(matcher, guildData);
                break;
            }
            default:
                textChannel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + actionName).queue();
                break;
        }
    }

    private static void createKeywords(CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = matcher.getLocale();
        
        //create name pattern action
        final List<String> arguments = matcher.parseArguments(4);
        if (arguments.size() < 2) {
            textChannel.sendMessage(TranslationKey.KEYWORD_CREATE_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        if (arguments.size() < 3) {
            textChannel.sendMessage(TranslationKey.KEYWORD_CREATE_MISSING_KEYWORD.getTranslation(locale)).queue();
            return;
        }
        if(arguments.size() < 4){
            textChannel.sendMessage(TranslationKey.KEYWORD_CREATE_MISSING_TEMPLATE.getTranslation(locale)).queue();
            return;
        }

        final String commandName = arguments.get(1);
        final String commandPattern = arguments.get(2);
        final String commandTemplate = arguments.get(3);
        final Member sender = matcher.getMember();
        final KeywordManager commands = guildData.getKeywordManager();
        try {
            final KeywordAction newAction = new KeywordAction(commandName, commandPattern, commandTemplate, sender.getIdLong());
            if (commands.addKeyword(newAction)) {
                textChannel.sendMessage(TranslationKey.KEYWORD_CREATE_SUCCESS.getTranslation(locale)).queue();
                return;
            }
            textChannel.sendMessage(TranslationKey.KEYWORD_ALREADY_EXISTS.getTranslation(locale)).queue();
        } catch (PatternSyntaxException e) {
            textChannel.sendMessage(TranslationKey.KEYWORD_PATTERN_SYNTAX_ERROR.getTranslation(locale)).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(TranslationKey.KEYWORD_SQL_ERROR_ON_CREATE.getTranslation(locale)).queue();
            LOGGER.error("Failure to add keyword command: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }

    }

    private static void deleteKeyword(String[] arguments, CommandMatcher matcher, GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final Locale locale = guildData.getConfigManager().getLocale();

        if (arguments.length < 2) {
            textChannel.sendMessage(TranslationKey.KEYWORD_DELETE_MISSING_NAME.getTranslation(locale)).queue();
            return;
        }
        final String commandName = arguments[1];
        final KeywordManager commands = guildData.getKeywordManager();
        final Optional<KeywordAction> optCommand = commands.getCommand(commandName);
        if (optCommand.isEmpty()) {
            final String template = TranslationKey.KEYWORD_DELETE_NOT_FOUND.getTranslation(locale);
            textChannel.sendMessageFormat(template, commandName).queue();
            return;
        }
        final KeywordAction command = optCommand.get();

        //Check if user has permission to remove the keyword
        final Member sender = matcher.getMember();
        textChannel.getGuild().retrieveMemberById(command.getAuthor()).submit().whenComplete((Member commandOwner, Throwable u) -> {
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, commandOwner);
            if (!hasPermission) {
                textChannel.sendMessage(TranslationKey.KEYWORD_DELETE_PERMISSION_DENIED.getTranslation(locale)).queue();
                return;
            }

            //Delete the command
            try {
                commands.removeKeyword(command);
                textChannel.sendMessage(TranslationKey.KEYWORD_DELETE_SUCCESS.getTranslation(locale)).queue();
            } catch (SQLException ex) {
                textChannel.sendMessage(TranslationKey.KEYWORD_SQL_ERROR_ON_DELETE.getTranslation(locale)).queue();
                LOGGER.error("Failure to delete keyword command: {}", ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });
    }

    private static void listKeywords(CommandMatcher matcher, GuildDataStore guildData) {
        final Locale locale = guildData.getConfigManager().getLocale();
        final TextChannel textChannel = matcher.getTextChannel();

        //Construct embed
        final String header = TranslationKey.HEADER_KEYWORDS.getTranslation(locale);
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(header);

        //Get the list of templates
        final Collection<KeywordAction> templates = guildData.getKeywordManager().getCommands();
        final ArrayList<CompletableFuture<String>> futures = new ArrayList<>(templates.size());
        templates.forEach((KeywordAction command) -> {
            futures.add(command.toListElement(locale, textChannel.getJDA()));
        });
        //After all the futures all initialized start waiting for results
        final StringBuilder contentBuilder = new StringBuilder();
        futures.forEach((CompletableFuture<String> desc) -> {
            contentBuilder.append(desc.join());
        });
        if (templates.isEmpty()) {
            contentBuilder.append(TranslationKey.KEYWORD_NO_KEYWORDS.getTranslation(locale));
        }
        eb.setDescription(contentBuilder);
        textChannel.sendMessage(eb.build()).queue();
    }
}
