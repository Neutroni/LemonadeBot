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
import eternal.lemonadebot.customcommands.TemplateProvider;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.PermissionUtilities;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.regex.PatternSyntaxException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
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
    public String getCommand(final ResourceBundle locale) {
        return locale.getString("COMMAND_KEYWORD");
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_KEYWORD");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        final String template = locale.getString("SYNTAX_KEYWORD");
        final String keys = TemplateProvider.getHelp(locale);
        return String.format(template, keys);
    }

    @Override
    public void respond(final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final ResourceBundle locale = translationCache.getResourceBundle();
        final String[] arguments = matcher.getArguments(1);
        if (arguments.length == 0) {
            textChannel.sendMessage(locale.getString("ERROR_MISSING_OPERATION")).queue();
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
                textChannel.sendMessage(locale.getString("ERROR_UNKNOWN_OPERATION") + actionName).queue();
                break;
        }
    }

    private static void createKeywords(final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = guildData.getTranslationCache().getResourceBundle();

        //create name runas pattern action
        final List<String> arguments = matcher.parseArguments(5);
        if (arguments.size() < 2) {
            textChannel.sendMessage(locale.getString("KEYWORD_CREATE_MISSING_NAME")).queue();
            return;
        }
        if (arguments.size() < 3) {
            textChannel.sendMessage(locale.getString("KEYWORD_CREATE_MISSING_USER")).queue();
            return;
        }
        if (arguments.size() < 4) {
            textChannel.sendMessage(locale.getString("KEYWORD_CREATE_MISSING_KEYWORD")).queue();
            return;
        }
        if (arguments.size() < 5) {
            textChannel.sendMessage(locale.getString("KEYWORD_CREATE_MISSING_TEMPLATE")).queue();
            return;
        }

        final String commandName = arguments.get(1);
        final String runAs = arguments.get(2);
        final boolean runAsCreator;
        if (runAs.equals(locale.getString("KEYWORD_RUN_AS_USER"))) {
            runAsCreator = false;
        } else if (runAs.equals(locale.getString("KEYWORD_RUN_AS_CREATOR"))) {
            runAsCreator = true;
        } else {
            textChannel.sendMessage(locale.getString("KEYWORD_RUN_AS_UNKNOWN")).queue();
            return;
        }
        final String commandPattern = arguments.get(3);
        final String commandTemplate = arguments.get(4);
        final Member sender = matcher.getMember();
        final KeywordManager commands = guildData.getKeywordManager();
        try {
            final KeywordAction newAction = new KeywordAction(commandName, commandPattern, commandTemplate, sender.getIdLong(), runAsCreator);
            if (commands.addKeyword(newAction)) {
                textChannel.sendMessage(locale.getString("KEYWORD_CREATE_SUCCESS")).queue();
                return;
            }
            textChannel.sendMessage(locale.getString("KEYWORD_ALREADY_EXISTS")).queue();
        } catch (PatternSyntaxException e) {
            textChannel.sendMessage(locale.getString("KEYWORD_PATTERN_SYNTAX_ERROR")).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(locale.getString("KEYWORD_SQL_ERROR_ON_CREATE")).queue();
            LOGGER.error("Failure to add keyword command: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }

    }

    private static void deleteKeyword(final String[] arguments, final CommandMatcher matcher, final GuildDataStore guildData) {
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = guildData.getTranslationCache().getResourceBundle();

        if (arguments.length < 2) {
            textChannel.sendMessage(locale.getString("KEYWORD_DELETE_MISSING_NAME")).queue();
            return;
        }
        final String commandName = arguments[1];
        final KeywordManager commands = guildData.getKeywordManager();
        final Optional<KeywordAction> optCommand = commands.getCommand(commandName);
        if (optCommand.isEmpty()) {
            final String template = locale.getString("KEYWORD_DELETE_NOT_FOUND");
            textChannel.sendMessageFormat(template, commandName).queue();
            return;
        }
        final KeywordAction command = optCommand.get();

        //Check if user has permission to remove the keyword
        final Member sender = matcher.getMember();
        textChannel.getGuild().retrieveMemberById(command.getAuthor()).submit().whenComplete((Member commandOwner, Throwable u) -> {
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, commandOwner);
            if (!hasPermission) {
                textChannel.sendMessage(locale.getString("KEYWORD_DELETE_PERMISSION_DENIED")).queue();
                return;
            }

            //Delete the command
            try {
                commands.removeKeyword(command);
                textChannel.sendMessage(locale.getString("KEYWORD_DELETE_SUCCESS")).queue();
            } catch (SQLException ex) {
                textChannel.sendMessage(locale.getString("KEYWORD_SQL_ERROR_ON_DELETE")).queue();
                LOGGER.error("Failure to delete keyword command: {}", ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });
    }

    private static void listKeywords(final CommandMatcher matcher, final GuildDataStore guildData) {
        final ResourceBundle locale = guildData.getTranslationCache().getResourceBundle();
        final TextChannel textChannel = matcher.getTextChannel();

        //Construct embed
        final String header = locale.getString("HEADER_KEYWORDS");
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
            contentBuilder.append(locale.getString("KEYWORD_NO_KEYWORDS"));
        }
        eb.setDescription(contentBuilder);
        textChannel.sendMessage(eb.build()).queue();
    }
}
