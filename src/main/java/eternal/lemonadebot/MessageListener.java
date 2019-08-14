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

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.ChannelManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JDA MessageListener, responsible for reacting to messages discord sends
 *
 * @author Neutroni
 */
public class MessageListener extends ListenerAdapter {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DatabaseManager DATABASE;
    private final CommandManager commandManager;
    private final ChannelManager channelManager;

    /**
     * Constructor
     *
     * @param db Database to use for operations
     */
    public MessageListener(DatabaseManager db) {
        this.DATABASE = db;
        this.channelManager = db.getChannels();
        this.commandManager = new CommandManager(db);
    }

    /**
     * Received when someone sends a message
     *
     * @param event message info
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        //Only listen on textchannels for now
        if (!event.isFromType(ChannelType.TEXT)) {
            return;
        }
        //Check if we are listening on this channel
        final TextChannel textChannel = event.getTextChannel();
        if (!channelManager.hasChannel(textChannel)) {
            return;
        }

        //Check if message is a command
        final Message message = event.getMessage();
        final Optional<ChatCommand> action = commandManager.getAction(message);
        if (action.isEmpty()) {
            return;
        }

        //React to command
        final ChatCommand ca = action.get();
        final Member member = event.getMember();
        if (commandManager.hasPermission(member, ca)) {
            ca.respond(member, message, textChannel);
        }
    }

    private String getRuleChannelMessage(Guild g) {
        //Only this guild, direct to rule channel
        final Optional<String> rco = DATABASE.getConfig().getRuleChannelID();
        if (rco.isEmpty()) {
            return "";
        }

        final String rcsnowflake = rco.get();
        final TextChannel ruleChannel = g.getTextChannelById(rcsnowflake);
        if (ruleChannel == null) {
            return "";
        }
        final String rcmention = ruleChannel.getAsMention();
        return " please check the guild rules over at " + rcmention;

    }

    private void sendDefaultMessage(TextChannel textChannel, Member member) {
        textChannel.sendMessage("Welcome to our guild discord " + member.getEffectiveName()
                + getRuleChannelMessage(textChannel.getGuild())).queue();
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

        //Check if we can find the channel they were directed to
        if (textChannel == null) {
            LOGGER.error("Can't determine the default channel for guild: " + guild.getName());
            return;
        }

        //Check if we should react on this channel
        if (!channelManager.hasChannel(textChannel)) {
            return;
        }

        //Get the correct rule message based on wheter they come from another guild
        final Member member = event.getMember();
        final List<Guild> mutualGuilds = member.getUser().getMutualGuilds();
        switch (mutualGuilds.size()) {
            case 1: {
                sendDefaultMessage(textChannel, member);
                return;
            }
            case 2: {
                //This and another guild, try to get role for them based on other guild
                final List<Guild> mutableGuilds = new ArrayList<>(mutualGuilds);
                mutableGuilds.remove(guild);

                if (mutableGuilds.size() != 1) {
                    sendDefaultMessage(textChannel, member);
                    return;
                }

                final Guild otherGuild = mutableGuilds.get(0);
                final Member otherGuildmember = otherGuild.getMember(member.getUser());
                if (otherGuildmember == null) {
                    sendDefaultMessage(textChannel, member);
                    return;
                }
                final List<Role> otherGuildRoles = otherGuildmember.getRoles();
                if (otherGuildRoles.isEmpty()) {
                    sendDefaultMessage(textChannel, member);
                    return;
                }

                final String roleName = otherGuild.getName();
                final List<Role> roles = guild.getRolesByName(roleName, false);
                guild.modifyMemberRoles(member, roles, null).queue((t) -> {
                    //Success
                    textChannel.sendMessage("Welcome to our guild discord " + member.getEffectiveName()
                            + "You have been assigned role based on other guild you are also on, "
                            + "if you believe this is error please contact guild lead.").queue();
                }, (t) -> {
                    //Failure
                    sendDefaultMessage(textChannel, member);
                });
                break;
            }

            default: {
                //More guilds, ask them to use role command
                textChannel.sendMessage("Welcome to our guild discord " + member.getEffectiveName()+ "\n"
                        + "You apper to be on multiple guilds and as such I can't find a role for you, "
                        + "use command \"role\" to assing a role based on other guild you are also on.").queue();
                break;
            }
        }
    }

    /**
     * Received when we join a guild
     *
     * @param event info about the join
     */
    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        final ChannelManager chm = DATABASE.getChannels();
        if (chm.isEmpty()) {
            try {
                final TextChannel channel = event.getGuild().getDefaultChannel();
                if (channel == null) {
                    LOGGER.error("Cant find a default channel");
                    return;
                }
                chm.addChannel(channel);
                channel.sendMessage("Hello everyone I'm a new bot here, nice to meet you all").queue();
            } catch (SQLException ex) {
                LOGGER.error("Adding default listen channel failed", ex);
            }
        }
    }

    /**
     * Closes the database once JDA has shutdown
     *
     * @param event event from JDA
     */
    @Override
    public void onShutdown(ShutdownEvent event) {
        try {
            DATABASE.close();
        } catch (SQLException ex) {
            LOGGER.error("Shutting down database connection failed", ex);
        }
        LOGGER.info("Shutting down");
    }
}
