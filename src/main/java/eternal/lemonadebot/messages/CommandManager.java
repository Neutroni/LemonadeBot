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
package eternal.lemonadebot.messages;

import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.CustomCommandManager;
import eternal.lemonadebot.database.DatabaseManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Various utility functions for parsing command from messages
 *
 * @author Neutroni
 */
public class CommandManager {

    private static final Logger LOGGER = LogManager.getLogger();

    //Command matching
    private final ConfigManager configManager;
    private final HashMap<Long, Pattern> commandPattern = new HashMap<>();

    //Commands
    private final CommandProvider commandProvider;
    private final CustomCommandManager customCommands;

    /**
     * Constructor
     *
     * @param db database to use
     */
    public CommandManager(DatabaseManager db) {
        this.configManager = db.getConfig();

        //Load command patterns for all guilds
        final JDA jda = db.getJDA();
        for (Guild g : jda.getGuilds()) {
            final long guildID = g.getIdLong();
            final String prefix = this.configManager.getCommandPrefix(guildID);
            this.commandPattern.put(guildID, getCommandPattern(prefix));
        }

        //Load commands
        this.commandProvider = new CommandProvider(this, db);
        this.customCommands = db.getCustomCommands();
    }

    /**
     * Get the action for command
     *
     * @param cmdMatcher Matcher to find command for
     * @return CommandAction or Option.empty if command was not found
     */
    public Optional<ChatCommand> getAction(CommandMatcher cmdMatcher) {
        final Optional<String> name = cmdMatcher.getCommand();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        //get the command name
        final String commandName = name.get();
        //Check if we find command by that name
        final Optional<ChatCommand> command = commandProvider.getCommand(commandName);
        if (command.isPresent()) {
            return command;
        }

        //Log the message if debug is enabled
        LOGGER.debug(() -> {
            return "Found command: " + commandName + " in " + cmdMatcher.getMessage().getContentRaw();
        });

        //Check if we find custom command by that name
        final Optional<CustomCommand> custom = customCommands.getCommand(commandName);
        if (custom.isPresent()) {
            return Optional.of(custom.get());
        }
        return Optional.empty();
    }

    /**
     * Match of command elements
     *
     * @param guild Guild the message was sent in
     * @param msg Message to parse
     * @return Matcher for the message
     */
    public CommandMatcher getCommandMatcher(Guild guild, Message msg) {
        final long guildID = guild.getIdLong();
        if (this.commandPattern.containsKey(guildID)) {
            return new CommandMatcher(this.commandPattern.get(guildID), msg);
        }
        //New guild
        final Pattern newPattern = getCommandPattern(this.configManager.getCommandPrefix(guildID));
        this.commandPattern.put(guildID, newPattern);
        return new CommandMatcher(newPattern, msg);
    }

    /**
     * What rank user has
     *
     * @param member user to check
     * @return Rank of the member
     */
    public CommandPermission getRank(Member member) {
        if (configManager.isOwner(member)) {
            return CommandPermission.OWNER;
        }
        if (member.getPermissions().contains(Permission.MANAGE_SERVER)) {
            return CommandPermission.ADMIN;
        }
        if (member.getRoles().size() > 0) {
            return CommandPermission.MEMBER;
        }
        return CommandPermission.USER;
    }

    /**
     * Checks wheter given user has permission to run given command
     *
     * @param member Person to check permission for
     * @param command Command to check
     * @return Does the person have permission
     */
    public boolean hasPermission(Member member, ChatCommand command) {
        return getRank(member).ordinal() >= command.getPermission().ordinal();
    }

    /**
     * Check if user has permission to manage content owned by other member
     *
     * @param member User trying to do an action that modifies content
     * @param owner owner of the content, can be null
     * @return true if user has permission
     */
    public boolean hasPermission(@Nonnull Member member, @Nullable Member owner) {
        if (member.equals(owner)) {
            return true;
        }
        final CommandPermission senderRank = getRank(member);
        if (owner == null) {
            return (senderRank.ordinal() >= CommandPermission.ADMIN.ordinal());
        }
        if (senderRank == CommandPermission.ADMIN) {
            return getRank(owner).ordinal() < CommandPermission.ADMIN.ordinal();
        }
        return configManager.isOwner(member);
    }

    /**
     * Updates command pattern
     *
     * @param prefix
     */
    private Pattern getCommandPattern(String prefix) {
        //Start of match, optionally @numericID, prefix, match group 2 is command
        return Pattern.compile("^(@\\d+ )?" + Pattern.quote(prefix) + "(\\w+) ?");
    }

    /**
     * Sets the commands prefix
     *
     * @param prefix prefix to use
     * @param guild guild the prefix is for
     * @return was storing prefix in database succesfull
     */
    public boolean setPrefix(String prefix, Guild guild) {
        try {
            this.commandPattern.put(guild.getIdLong(), getCommandPattern(prefix));
            this.configManager.setCommandPrefix(guild, prefix);
            return true;
        } catch (SQLException ex) {
            LOGGER.error(ex);
        }
        return false;
    }

}
