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
import eternal.lemonadebot.commandtypes.MemberCommand;
import eternal.lemonadebot.database.CooldownManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.PermissionManager;
import java.util.Objects;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * User defined commands that take template as input and when run return the
 * template with parts substituted according to SimpleActions defined in
 * ActionManager
 *
 * @author Neutroni
 */
public class CustomCommand extends MemberCommand {

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

    @Override
    public String getCommand() {
        return this.commandName;
    }

    @Override
    public String getDescription() {
        return "Custom command";
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
        
        //Check if message is empty
        final String commandString = response.toString();
        if (commandString.isBlank()) {
            channel.sendMessage("Error: Template produced empty message").queue();
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
            channel.sendMessage("Could not find command with input: " + commandString).queue();
            return;
        }
        final ChatCommand command = optCommand.get();
        if (command instanceof CustomCommand) {
            channel.sendMessage("Custom command cannot run another custom command").queue();
            return;
        }
        final Member member = message.getMember();
        final PermissionManager permissions = guildData.getPermissionManager();
        if (permissions.hasPermission(member, commandString)) {
            channel.sendMessage("Insufficient permississions to run that command").queue();
        }

        final CooldownManager cdm = guildData.getCooldownManager();
        final Optional<String> optCooldown = cdm.updateActivationTime(commandString);
        if (optCooldown.isPresent()) {
            final String currentCooldown = optCooldown.get();
            channel.sendMessage("Command on cooldown, time remaining: " + currentCooldown + '.').queue();
        }

        command.respond(fakeMatcher, guildData);
    }

    @Override
    public String getHelpText() {
        return "Custom command with template:\n "
                + this.actionTemplate
                + "\nSee \"help command\" for details on custom commands.";
    }

    @Override
    public int hashCode() {
        return this.commandName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CustomCommand other = (CustomCommand) obj;
        return Objects.equals(this.commandName, other.commandName);
    }

}
