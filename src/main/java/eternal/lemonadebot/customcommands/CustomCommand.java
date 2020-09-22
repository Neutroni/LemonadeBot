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

import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.CooldownManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.PermissionManager;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.translation.TranslationKey;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * User defined commands that take template as input and when run return the
 * template with parts substituted according to ActionTemplateFuctions defined
 * in TemplateProvider
 *
 * @author Neutroni
 */
public class CustomCommand implements ChatCommand {

    private final String commandName;
    private final String actionTemplate;
    private final long owner;

    /**
     * Constructor
     *
     * @param commandName command this is activeted by
     * @param actionTemplate action template
     * @param owner who created this command
     */
    public CustomCommand(String commandName, String actionTemplate, long owner) {
        this.commandName = commandName;
        this.actionTemplate = actionTemplate;
        this.owner = owner;
    }

    /**
     * Get the name of the custom command
     *
     * @param locale Locale from the guild, ignored for custom commands
     * @return Command name
     */
    @Override
    public String getCommand(Locale locale) {
        return this.commandName;
    }

    /**
     * Get the command name without needing to pass locale
     *
     * @return Command name
     */
    public String getCommandName() {
        return this.commandName;
    }

    @Override
    public String getDescription(Locale locale) {
        return TranslationKey.DESCRIPTION_CUSTOMCOMMAND.getTranslation(locale);
    }

    @Override
    public String getHelpText(Locale locale) {
        return TranslationKey.SYNTAX_CUSTOMCOMMAND.getTranslation(locale);
    }

    @Override
    public Map<String, CommandPermission> getDefaultRanks(Locale locale, long guildID) {
        return Map.of(getCommand(locale), new CommandPermission(MemberRank.USER, guildID));
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
    public long getOwner() {
        return this.owner;
    }

    @Override
    public void respond(CommandMatcher message, GuildDataStore guildData) {
        final TextChannel channel = message.getTextChannel();
        final CharSequence response = TemplateProvider.parseAction(message, guildData, actionTemplate);
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
        final Optional<? extends ChatCommand> optCommand = CommandProvider.getAction(fakeMatcher, guildData);
        if (optCommand.isEmpty()) {
            channel.sendMessage(TranslationKey.ERROR_COMMAND_NOT_FOUND.getTranslation(locale) + commandString).queue();
            return;
        }
        final ChatCommand command = optCommand.get();
        if (command instanceof CustomCommand) {
            channel.sendMessage(TranslationKey.ERROR_RECURSION_NOT_PERMITTED.getTranslation(locale)).queue();
            return;
        }
        final Member member = message.getMember();
        final PermissionManager permissions = guildData.getPermissionManager();
        if (permissions.hasPermission(member, commandString)) {
            channel.sendMessage(TranslationKey.ERROR_INSUFFICIENT_PERMISSION.getTranslation(locale)).queue();
        }

        final CooldownManager cdm = guildData.getCooldownManager();
        final Optional<String> optCooldown = cdm.updateActivationTime(commandString);
        if (optCooldown.isPresent()) {
            final String days = TranslationKey.TIME_DAYS.getTranslation(locale);
            final String hours = TranslationKey.TIME_HOURS.getTranslation(locale);
            final String minutes = TranslationKey.TIME_MINUTES.getTranslation(locale);
            final String seconds = TranslationKey.TIME_SECONDS.getTranslation(locale);
            final String day = TranslationKey.TIME_DAY.getTranslation(locale);
            final String hour = TranslationKey.TIME_HOUR.getTranslation(locale);
            final String minute = TranslationKey.TIME_MINUTE.getTranslation(locale);
            final String second = TranslationKey.TIME_SECOND.getTranslation(locale);
            final String currentCooldown = String.format(optCooldown.get(), days, hours, minutes, seconds, day, hour, minute, second);
            channel.sendMessage(TranslationKey.ERROR_COMMAND_COOLDOWN_TIME.getTranslation(locale) + currentCooldown).queue();
            return;
        }

        command.respond(fakeMatcher, guildData);
    }

    @Override
    public int hashCode() {
        return this.commandName.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof CustomCommand) {
            final CustomCommand otherCommand = (CustomCommand) other;
            return this.commandName.equals(otherCommand.commandName);
        }
        return false;
    }

}
