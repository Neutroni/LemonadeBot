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
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.cooldowns.CooldownManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.translation.TranslationKey;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

/**
 * User defined commands that take template as input and when run return the
 * template with parts substituted according to ActionTemplateFunctions defined
 * in TemplateProvider
 *
 * @author Neutroni
 */
public class CustomCommand implements ChatCommand {

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
    public String getCommand(final Locale locale) {
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
    public String getDescription(final Locale locale) {
        return TranslationKey.DESCRIPTION_CUSTOMCOMMAND.getTranslation(locale);
    }

    @Override
    public String getHelpText(final Locale locale) {
        final String template = TranslationKey.SYNTAX_CUSTOMCOMMAND.getTranslation(locale);
        return String.format(template, this.actionTemplate);
    }

    @Override
    public Collection<CommandPermission> getDefaultRanks(final Locale locale, final long guildID, final PermissionManager permissions) {
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
    public void respond(final CommandMatcher message, final GuildDataStore guildData) {
        final TextChannel channel = message.getTextChannel();
        final CharSequence response = TemplateProvider.parseAction(message, guildData, this.actionTemplate);
        final Locale locale = guildData.getConfigManager().getLocale();

        //Check if message is empty
        final String commandString = response.toString();
        if (commandString.isBlank()) {
            channel.sendMessage(TranslationKey.ERROR_TEMPLATE_EMPTY.getTranslation(locale)).queue();
            return;
        }

        //Send response assuming it is not a command
        if (response.charAt(0) != '!') {
            channel.sendMessage(response).queue();
            return;
        }

        //If response begins with a exclamation point it should be treated as a command
        final CommandMatcher fakeMatcher = new FakeMessageMatcher(message, commandString);
        final CommandProvider commands = guildData.getCommandProvider();
        final Optional<ChatCommand> optCommand = commands.getAction(fakeMatcher);
        if (optCommand.isEmpty()) {
            channel.sendMessage(TranslationKey.ERROR_COMMAND_NOT_FOUND.getTranslation(locale) + commandString).queue();
            return;
        }

        //Check if command is custom command, do not allow recursion
        final ChatCommand command = optCommand.get();
        if (command instanceof CustomCommand) {
            channel.sendMessage(TranslationKey.ERROR_RECURSION_NOT_PERMITTED.getTranslation(locale)).queue();
            return;
        }

        //Check if user has permission to run the command
        final Member member = message.getMember();
        final PermissionManager permissions = guildData.getPermissionManager();
        if (!permissions.hasPermission(member, command, commandString)) {
            channel.sendMessage(TranslationKey.ERROR_INSUFFICIENT_PERMISSION.getTranslation(locale)).queue();
            return;
        }

        //Check if command is on cooldown
        final CooldownManager cdm = guildData.getCooldownManager();
        cdm.checkCooldown(member, commandString).ifPresentOrElse((Duration remainingCooldown) -> {
            final String currentCooldown = CooldownManager.formatDuration(remainingCooldown, locale);
            final String template = TranslationKey.ERROR_COMMAND_COOLDOWN_TIME.getTranslation(locale);
            channel.sendMessage(template + currentCooldown).queue();
        }, () -> {
            command.respond(fakeMatcher, guildData);
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
    public CompletableFuture<String> toListElement(final Locale locale, final JDA jda) {
        final CompletableFuture<String> result = new CompletableFuture<>();
        final String template = TranslationKey.TEMPLATE_COMMAND_LIST_ELEMENT.getTranslation(locale);
        jda.retrieveUserById(this.author).queue((User commandOwner) -> {
            //Found user
            final String creatorName = commandOwner.getAsMention();
            result.complete(String.format(template, this.commandName, creatorName));
        }, (Throwable t) -> {
            //User missing
            final String creatorName = TranslationKey.UNKNOWN_USER.getTranslation(locale);
            result.complete(String.format(template, this.commandName, creatorName));
        });
        return result;
    }

}
