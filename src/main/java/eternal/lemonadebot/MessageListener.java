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
package eternal.lemonadebot;

import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.database.DatabaseException;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandParser;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class MessageListener extends ListenerAdapter {

    private static final Logger LOGGER = LogManager.getLogger();
    private final CommandParser COMMAND_PARSER;
    private final DatabaseManager DB;

    /**
     * Constructor
     *
     * @param db Database to use for operations
     */
    public MessageListener(DatabaseManager db) {
        this.DB = db;
        this.COMMAND_PARSER = new CommandParser(db);
    }

    /**
     * Received when someone sends a message
     *
     * @param event message info
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //Only listen on textchannels for now
        if (!event.isFromType(ChannelType.TEXT)) {
            return;
        }
        //Check if we are listening on this channel
        final TextChannel textChannel = event.getTextChannel();
        if (!DB.watchingChannel(textChannel)) {
            return;
        }

        //Check if message is a command
        final Message message = event.getMessage();
        final Optional<ChatCommand> action = COMMAND_PARSER.getAction(message);
        if (action.isEmpty()) {
            return;
        }

        //React to command
        final ChatCommand ca = action.get();
        final Member member = event.getMember();
        if (COMMAND_PARSER.hasPermission(member, ca)) {
            ca.respond(member, message, textChannel);
        }
    }

    /**
     * Received when someone joins a guild
     *
     * @param event info about the join
     */
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        final Guild guild = event.getGuild();
        final TextChannel textChannel = guild.getDefaultChannel();
        final Member member = event.getMember();
        if (textChannel == null) {
            LOGGER.error("Can't determine the default channel for guild: " + guild.getName());
            return;
        }
        //Check if we should react on this channel
        if (!DB.watchingChannel(textChannel)) {
            return;
        }
        //Get the rule channel
        String ruleMessage = "";
        final Optional<String> rco = this.DB.getRuleChannelID();
        if (rco.isPresent()) {
            final String rcsnowflake = rco.get();
            final String rcmention = guild.getTextChannelById(rcsnowflake).getAsMention();
            ruleMessage = " please check the guild rules over at " + rcmention;
        }

        //TODO: add command to get a role for ones primary guild
        /*
        //Check if they come from another guild we watch
        final List<Guild> mutualGuilds = user.getMutualGuilds();
        mutualGuilds.remove(guild);
        boolean roleAssigned = false;
        //Iterate over the other guilds
        for (final Guild g : mutualGuilds) {
            //Check if user has rule on the other guild
            if (g.getMember(user).getRoles().isEmpty()) {
                continue;
            }
            //Sidenote: if user is on multiple guilds we can't know what is their main guild
            final List<Role> roles = guild.getRolesByName(g.getName(), true);
            if (roles.isEmpty()) {
                continue;
            }
            guild.getController().addRolesToMember(member, roles).queue();
            roleAssigned = true;
        }
        if (roleAssigned) {
            ruleMessage = " it appears you are from an allied guild and as such you have been assigned role based on other guilds you belong to, if you believe this is error please contact guild lead.";
        }
         */
        textChannel.sendMessage("Welcome to guild discord " + member.getNickname()
                + ruleMessage).queue();
    }

    /**
     * Received when we join a guild
     *
     * @param event info about the join
     */
    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        if (DB.getChannelIds().isEmpty()) {
            try {
                final TextChannel channel = event.getGuild().getDefaultChannel();
                if (channel == null) {
                    LOGGER.error("Cant find a default channel");
                    return;
                }
                DB.addChannel(channel);
                channel.sendMessage("Hello everyone I'm a new bot here, nice to meet you all").queue();
            } catch (DatabaseException ex) {
                LOGGER.error(ex);
            }
        }

    }
}
