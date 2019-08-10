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
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandPermission;
import java.util.Objects;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 * User defined commands that take template as input and when run return the
 * template with parts substituted according to SimpleActions defined in
 * ActionManager
 *
 * @author Neutroni
 */
public class CustomCommand implements ChatCommand {

    private final DatabaseManager DB;
    private final ActionManager actionManager;

    private final String command;
    private final String action;
    private final String owner;

    /**
     * Constructor
     *
     * @param db Database to fetch permission from
     * @param actions SimpleActions to replace action templates with
     * @param command command this is activeted by
     * @param action action template
     * @param owner who created this command
     */
    public CustomCommand(DatabaseManager db, ActionManager actions, String command, String action, String owner) {
        this.DB = db;
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
    public String getOwner() {
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
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.command);
        return hash;
    }

    @Override
    public void respond(Member member, Message message, TextChannel textChannel) {
        final String response = this.actionManager.processActions(message, action);
        textChannel.sendMessage(response).queue();
    }

    @Override
    public String getHelp() {
        return "Custom command";
    }

    @Override
    public CommandPermission getPermission() {
        return DB.getCommandUsePermission();
    }
}
