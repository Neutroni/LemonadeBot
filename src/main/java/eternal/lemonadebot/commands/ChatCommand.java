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
package eternal.lemonadebot.commands;

import eternal.lemonadebot.cooldowns.CooldownManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.RuntimeStorage;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.PermissionManager;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Interface all commands must implement
 *
 * @author Neutroni
 */
public abstract class ChatCommand {

    /**
     * Every action has a command it is called with
     *
     * @param locale Locale to get the command name inF
     * @return the command to activate this action
     */
    public abstract String getCommand(final ResourceBundle locale);

    /**
     * Short description for command, used for command listings
     *
     * @param locale Locale to return the description in
     * @return Description for what this command does
     */
    public abstract String getDescription(final ResourceBundle locale);

    /**
     * Help text for command, usage info for the command
     *
     * @param locale Locale to return the help in
     * @return help text for the command
     */
    public abstract String getHelpText(final ResourceBundle locale);

    /**
     * What rank is needed to run this command by default
     *
     * @param locale Locale to get the ranks for
     * @param guildID ID of the guild used to create commandPermission
     * @param permissions PermissionManager to use
     * @return The default rank needed to run this command
     */
    public abstract Collection<CommandPermission> getDefaultRanks(ResourceBundle locale, long guildID, PermissionManager permissions);

    /**
     * Run the command, will check if user has permission
     *
     * @param context Context to run the command in
     * @param silent If on error command should respond with a error message
     */
    public void run(final CommandContext context, final boolean silent) {
        final CommandMatcher cmdMatch = context.getMatcher();
        final Member member = cmdMatch.getMember();
        final String inputString = cmdMatch.getAction();
        final TextChannel textChannel = cmdMatch.getTextChannel();
        final GuildDataStore guildData = context.getGuildData();
        final ResourceBundle resources = context.getResource();

        //Check if user has permission
        final PermissionManager permissions = guildData.getPermissionManager();
        if (!permissions.hasPermission(member, this, inputString)) {
            //No message if silent
            if (silent) {
                return;
            }
            final String response = resources.getString("ERROR_INSUFFICIENT_PERMISSION");
            textChannel.sendMessage(response).queue();
            return;
        }

        //Check if command is on cooldown
        final CooldownManager cooldownManager = guildData.getCooldownManager();
        cooldownManager.checkCooldown(member, inputString).ifPresentOrElse((Duration t) -> {
            //No message if silent
            if (silent) {
                return;
            }
            //Command on cooldown
            final String template = resources.getString("ERROR_COMMAND_COOLDOWN_TIME");
            final String currentCooldown = CooldownManager.formatDuration(t, resources);
            textChannel.sendMessage(template + currentCooldown).queue();
        }, () -> {
            //Run the command
            respond(context);
        });
    }

    /**
     * Run the command if user has permission, if not user will see error
     *
     * @param context Context to run the command in
     */
    public void run(final CommandContext context) {
        run(context, false);
    }

    /**
     * Responds to a message
     *
     * @param context Context for the message
     */
    protected abstract void respond(final CommandContext context);

    /**
     * Initialize data that the command needs
     *
     * @param guilds List of guilds that need to be initialized
     * @param rs RuntimeStorage that commands can use in initialization
     */
    public void initialize(final List<Guild> guilds, final RuntimeStorage rs) {
        //No-op
    }

    /**
     * Close any resources command might use when shutting down the bot
     */
    public void close() {
        //No-op
    }

}
