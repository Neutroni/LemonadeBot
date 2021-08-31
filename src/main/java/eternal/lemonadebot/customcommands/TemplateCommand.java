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
package eternal.lemonadebot.customcommands;

import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.commands.CommandList;
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.permissions.PermissionManager;
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
import net.dv8tion.jda.api.EmbedBuilder;
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
public class TemplateCommand extends ChatCommand {

    private static final Logger LOGGER = LogManager.getLogger();
    private final TemplateManager templateManager;

    /**
     * Constructor
     *
     * @param db Database to store templates in
     */
    public TemplateCommand(final DatabaseManager db) {
        this.templateManager = new TemplateManager(db);
    }

    @Override
    public String getCommand(final ResourceBundle locale) {
        return locale.getString("COMMAND_TEMPLATE");
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_TEMPLATE");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        final String template = locale.getString("SYNTAX_TEMPLATE");
        final String keys = TemplateProvider.getHelp(locale);
        return String.format(template, keys);
    }

    @Override
    public Collection<CommandPermission> getDefaultRanks(final ResourceBundle locale, final long guildID, final PermissionManager permissions) {
        return List.of(new CommandPermission(getCommand(locale), MemberRank.MEMBER, guildID, guildID));
    }

    @Override
    protected void respond(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel textChannel = matcher.getTextChannel();
        final TranslationCache translationCache = context.getTranslation();
        final ResourceBundle locale = translationCache.getResourceBundle();
        final String[] arguments = matcher.getArguments(2);
        if (arguments.length == 0) {
            textChannel.sendMessage(locale.getString("ERROR_MISSING_OPERATION")).queue();
            return;
        }

        final String actionName = arguments[0];
        final ActionKey action = translationCache.getActionKey(actionName);
        switch (action) {
            case CREATE: {
                createCustomCommand(arguments, context);
                break;
            }
            case DELETE: {
                deleteCustomCommand(arguments, context);
                break;
            }
            case LIST: {
                listCustomCommands(context);
                break;
            }
            default:
                textChannel.sendMessage(locale.getString("ERROR_UNKNOWN_OPERATION") + actionName).queue();
                break;
        }
    }

    private static void createCustomCommand(final String[] arguments, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();

        if (arguments.length < 2) {
            textChannel.sendMessage(locale.getString("TEMPLATE_CREATE_MISSING_NAME")).queue();
            return;
        }
        if (arguments.length < 3) {
            textChannel.sendMessage(locale.getString("TEMPLATE_CREATE_MISSING_TEMPLATE")).queue();
            return;
        }

        //Check that there is no such built in command
        final String commandName = arguments[1];
        final Guild guild = textChannel.getGuild();
        final CommandProvider commandProvider = context.getCommandProvider();
        final CommandList builtInCommands = commandProvider.getBuiltInCommands();
        final Optional<ChatCommand> optCommand = builtInCommands.getBuiltInCommand(commandName, locale.getLocale());
        if (optCommand.isPresent()) {
            textChannel.sendMessage(locale.getString("TEMPLATE_NAME_RESERVED")).queue();
            return;
        }

        final String commandTemplate = arguments[2];
        final Member sender = matcher.getMember();
        final TemplateManager commands = commandProvider.getTemplateManager();
        final CustomCommand newAction = new CustomCommand(commandName, commandTemplate, sender.getIdLong(), guild.getIdLong());
        try {
            if (commands.addCommand(newAction)) {
                textChannel.sendMessage(locale.getString("TEMPLATE_CREATE_SUCCESS")).queue();
                return;
            }
            textChannel.sendMessage(locale.getString("TEMPLATE_ALREADY_EXISTS")).queue();
        } catch (SQLException ex) {
            textChannel.sendMessage(locale.getString("TEMPLATE_SQL_ERROR_ON_CREATE")).queue();
            LOGGER.error("Failure to add custom command: {}", ex.getMessage());
            LOGGER.trace("Stack trace", ex);
        }

    }

    private void deleteCustomCommand(final String[] arguments, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel textChannel = matcher.getTextChannel();
        final ResourceBundle locale = context.getResource();

        if (arguments.length < 2) {
            textChannel.sendMessage(locale.getString("TEMPLATE_DELETE_MISSING_NAME")).queue();
            return;
        }
        final String commandName = arguments[1];
        final Guild guild = matcher.getGuild();
        final Optional<CustomCommand> optCommand;
        try {
            optCommand = this.templateManager.getCommand(commandName, guild.getIdLong());
        } catch (SQLException e) {
            textChannel.sendMessage(locale.getString("TEMPLATE_SQL_ERROR_ON_FINDING_COMMAND")).queue();
            return;
        }
        if (optCommand.isEmpty()) {
            final String template = locale.getString("TEMPLATE_DELETE_NOT_FOUND");
            textChannel.sendMessageFormat(template, commandName).queue();
            return;
        }
        final CustomCommand command = optCommand.get();

        //Check if user has permission to remove the command
        final Member sender = matcher.getMember();
        textChannel.getGuild().retrieveMemberById(command.getAuthor()).submit().whenComplete((Member commandOwner, Throwable u) -> {
            final boolean hasPermission = PermissionUtilities.hasPermission(sender, commandOwner);
            if (!hasPermission) {
                textChannel.sendMessage(locale.getString("TEMPLATE_DELETE_PERMISSION_DENIED")).queue();
                return;
            }

            //Delete the command
            try {
                this.templateManager.removeCommand(command);
                textChannel.sendMessage(locale.getString("TEMPLATE_DELETE_SUCCESS")).queue();
            } catch (SQLException ex) {
                textChannel.sendMessage(locale.getString("TEMPLATE_SQL_ERROR_ON_DELETE")).queue();
                LOGGER.error("Failure to delete custom command: {}", ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        });
    }

    private void listCustomCommands(final CommandContext context) {
        final ResourceBundle locale = context.getResource();
        final TextChannel textChannel = context.getChannel();
        final Guild guild = textChannel.getGuild();

        //Construct embed
        final String header = locale.getString("HEADER_COMMANDS");
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(header);

        //Get the list of templates
        final Collection<CustomCommand> templates;
        try {
            templates = this.templateManager.getCommands(guild.getIdLong());
        } catch (SQLException e) {
            textChannel.sendMessage(locale.getString("TEMPLATE_SQL_ERROR_ON_LOADING_COMMANDS")).queue();
            return;
        }
        final ArrayList<CompletableFuture<String>> futures = new ArrayList<>(templates.size());
        templates.forEach((CustomCommand command) -> {
            futures.add(command.toListElement(locale, textChannel.getJDA()));
        });
        //After all the futures all initialized start waiting for results
        final StringBuilder contentBuilder = new StringBuilder();
        futures.forEach((CompletableFuture<String> desc) -> {
            contentBuilder.append(desc.join());
        });
        if (templates.isEmpty()) {
            contentBuilder.append(locale.getString("TEMPLATE_NO_COMMANDS"));
        }
        eb.setDescription(contentBuilder);
        textChannel.sendMessageEmbeds(eb.build()).queue();
    }
}
