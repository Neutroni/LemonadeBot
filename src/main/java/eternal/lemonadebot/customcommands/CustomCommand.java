/*
 * The MIT License
 *
 * Copyright 2019 Neutroni.
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
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandPermission;

/**
 * User defined commands that take template as input and when run return the
 * template with parts substituted according to SimpleActions defined in
 * ActionManager
 *
 * @author Neutroni
 */
public class CustomCommand implements ChatCommand {

    private final ConfigManager manager;
    private final ActionManager actionManager;

    private final String command;
    private final String action;
    private final long owner;

    /**
     * Constructor
     *
     * @param commands CommandManager to get command permissions from
     * @param actions SimpleActions to replace action templates with
     * @param commandParser parser to use for parsing commands
     * @param command command this is activeted by
     * @param action action template
     * @param owner who created this command
     */
    public CustomCommand(ConfigManager commands, ActionManager actions, String command, String action, long owner) {
        this.manager = commands;
        this.actionManager = actions;
        this.command = command;
        this.action = action;
        this.owner = owner;
    }

    @Override
    public String getCommand() {
        return this.command;
    }

    /**
     * Get the action this command performs
     *
     * @return action String
     */
    public String getAction() {
        return this.action;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        CustomCommand c = (CustomCommand) o;
        return this.command.equals(c.command);
    }

    @Override
    public int hashCode() {
        return this.command.hashCode();
    }

    @Override
    public void respond(CommandMatcher match) {
        final String response = this.actionManager.processActions(match, action);
        match.getMessage().getChannel().sendMessage(response).queue();
    }

    @Override
    public String getHelp() {
        return "Custom command";
    }

    @Override
    public CommandPermission getPermission() {
        return manager.getEditPermission();
    }
}
