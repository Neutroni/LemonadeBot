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

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import java.util.Objects;
import net.dv8tion.jda.api.entities.Guild;

/**
 * User defined commands that take template as input and when run return the
 * template with parts substituted according to SimpleActions defined in
 * ActionManager
 *
 * @author Neutroni
 */
public class CustomCommand implements ChatCommand {

    private final ConfigManager configManager;

    private final String commandName;
    private final String actionTemplate;
    private final long owner;

    /**
     * Constructor
     *
     * @param configManager ConfigManager to get command permissions from
     * @param commandName command this is activeted by
     * @param actionTemplate action template
     * @param owner who created this command
     */
    public CustomCommand(ConfigManager configManager, String commandName, String actionTemplate, long owner) {
        this.configManager = configManager;
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
    public void respond(CommandMatcher match) {
        final CharSequence response = ActionManager.processActions(match, actionTemplate);
        match.getMessage().getChannel().sendMessage(response).queue();
    }

    @Override
    public String getHelpText() {
        return "Custom command with template:\n "
                + this.actionTemplate
                + "\nSee \"help customcommand\" for details on custom commands.";
    }

    @Override
    public CommandPermission getPermission(Guild guild) {
        return this.configManager.getCommandRunPermission();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.commandName);
        return hash;
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
