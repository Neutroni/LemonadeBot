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
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.PermissionManager;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

/**
 * User defined commands that take template as input and when run return the
 * template with parts substituted according to ActionTemplateFunctions defined
 * in TemplateProvider
 *
 * @author Neutroni
 */
public class CustomCommand extends ChatCommand {

    private final String commandName;
    private final String actionTemplate;
    private final long author;

    /**
     * Constructor
     *
     * @param commandName command this is activated by
     * @param actionTemplate action template
     * @param owner who created this command
     */
    public CustomCommand(final String commandName, final String actionTemplate, final long owner) {
        this.commandName = commandName;
        this.actionTemplate = actionTemplate;
        this.author = owner;
    }

    /**
     * Get the name of the custom command
     *
     * @param locale Locale from the guild, ignored for custom commands
     * @return Command name
     */
    @Override
    public String getCommand(final ResourceBundle locale) {
        return this.commandName;
    }

    /**
     * Get the command name without needing to pass locale
     *
     * @return Command name
     */
    public String getName() {
        return this.commandName;
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_CUSTOMCOMMAND");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        final String template = locale.getString("SYNTAX_CUSTOMCOMMAND");
        return String.format(template, this.actionTemplate);
    }

    @Override
    public Collection<CommandPermission> getDefaultRanks(final ResourceBundle locale, final long guildID, final PermissionManager permissions) {
        final CommandPermission rankRole = permissions.getTemplateRunPermission();
        return List.of(new CommandPermission(getName(), rankRole.getRequiredRank(), rankRole.getRequiredRoleID()));
    }

    /**
     * Get the template for the action this command performs
     *
     * @return action String
     */
    public String getTemplate() {
        return this.actionTemplate;
    }

    /**
     * Get the owner of this command
     *
     * @return ID of the owner
     */
    public long getAuthor() {
        return this.author;
    }

    @Override
    protected void respond(final CommandContext context) {
        final TextChannel channel = context.getChannel();
        final CharSequence response = TemplateProvider.parseAction(context, this.actionTemplate);
        final ResourceBundle locale = context.getResource();

        //Check if message is empty
        final String commandString = response.toString();
        if (commandString.isBlank()) {
            channel.sendMessage(locale.getString("ERROR_TEMPLATE_EMPTY")).queue();
            return;
        }

        //Send response assuming it is not a command
        if (response.charAt(0) != '!') {
            channel.sendMessage(response).queue();
            return;
        }

        //If response begins with a exclamation point it should be treated as a command
        final GuildDataStore guildData = context.getGuildData();
        final CommandMatcher message = context.getMatcher();
        final CommandMatcher fakeMatcher = new FakeMessageMatcher(message, commandString);
        final CommandProvider commands = guildData.getCommandProvider();
        final Optional<ChatCommand> optCommand = commands.getAction(fakeMatcher);
        optCommand.ifPresentOrElse((ChatCommand command) -> {
            //Check if command is custom command, do not allow recursion
            if (command instanceof CustomCommand) {
                channel.sendMessage(locale.getString("ERROR_RECURSION_NOT_PERMITTED")).queue();
                return;
            }
            //Run the command
            command.run(context);
        }, () -> {
            //Did not find a command
            channel.sendMessage(locale.getString("ERROR_COMMAND_NOT_FOUND") + commandString).queue();
        });
    }

    @Override
    public int hashCode() {
        return this.commandName.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof CustomCommand) {
            final CustomCommand otherCommand = (CustomCommand) other;
            return this.commandName.equals(otherCommand.getName());
        }
        return false;
    }

    /**
     * Get string representing the commands for listing commands
     *
     * @param locale Locale to return the list element in
     * @param jda JDA to use to get command owner
     * @return String
     */
    public CompletableFuture<String> toListElement(final ResourceBundle locale, final JDA jda) {
        final CompletableFuture<String> result = new CompletableFuture<>();
        final String template = locale.getString("TEMPLATE_COMMAND_LIST_ELEMENT");
        jda.retrieveUserById(this.author).queue((User commandOwner) -> {
            //Found user
            final String creatorName = commandOwner.getAsMention();
            result.complete(String.format(template, this.commandName, creatorName));
        }, (Throwable t) -> {
            //User missing
            final String creatorName = locale.getString("UNKNOWN_USER");
            result.complete(String.format(template, this.commandName, creatorName));
        });
        return result;
    }

}
