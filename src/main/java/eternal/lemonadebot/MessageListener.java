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
package eternal.lemonadebot;

import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.ChannelManager;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandManager;
import eternal.lemonadebot.messages.CommandMatcher;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
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

    private final DatabaseManager db;
    private final CommandManager commandManager;

    /**
     * Constructor
     *
     * @param database Database to use for operations
     */
    public MessageListener(DatabaseManager database) {
        this.db = database;
        this.commandManager = new CommandManager(database);
    }

    /**
     * Received when someone sends a message
     *
     * @param event message info
     */
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        //Don't reply to bots
        if (event.getAuthor().isBot()) {
            return;
        }

        //Don't reply to webhook messages
        if (event.isWebhookMessage()) {
            return;
        }

        final Guild eventGuild = event.getGuild();
        final ChannelManager channels = this.db.getChannels(eventGuild);

        //Check if we are listening on this channel
        final TextChannel textChannel = event.getChannel();
        if (!channels.hasChannel(textChannel)) {
            return;
        }

        //Check if message is a command
        final Message message = event.getMessage();
        final CommandMatcher cmdmatch = commandManager.getCommandMatcher(eventGuild, message);
        final Optional<ChatCommand> action = commandManager.getAction(cmdmatch);
        if (action.isEmpty()) {
            return;
        }

        //React to command
        final ChatCommand ca = action.get();
        final Member member = event.getMember();
        if (commandManager.hasPermission(member, ca)) {
            ca.respond(cmdmatch);
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
        final Member member = event.getMember();
        final TextChannel textChannel = guild.getSystemChannel();
        final ChannelManager channels = this.db.getChannels(guild);

        LOGGER.debug("New member: " + member.getEffectiveName());

        //Check if we have a channel to greet them on
        if (textChannel == null) {
            LOGGER.debug("Not greeting because greeting is disabled in discord settings");
            return;
        }

        //Check if we should react on this channel
        if (channels.hasChannel(textChannel)) {
            LOGGER.debug("Not greeting because not listening on the channel");
            return;
        }

        //Get the correct rule message based on wheter they come from another guild
        final List<Guild> mutualGuilds = member.getUser().getMutualGuilds();
        if (mutualGuilds.size() < 2) {
            //Only this guild
            sendDefaultMessage(textChannel, member);
        } else if (mutualGuilds.size() == 2) {
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
                //Has left other server somehow
                sendDefaultMessage(textChannel, member);
                return;
            }
            final List<Role> otherGuildRoles = otherGuildmember.getRoles();
            if (otherGuildRoles.isEmpty()) {
                //Not a member on other server
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
        } else {
            //More guilds, ask them to use role command
            textChannel.sendMessage("Welcome to our guild discord " + member.getEffectiveName() + "\n"
                    + "You apper to be on multiple guilds and as such I can't find a role for you, "
                    + "use command \"role\" to assing a role based on other guild you are also on.").queue();
        }
    }

    /**
     * Received when we join a guild
     *
     * @param event info about the join
     */
    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        final Guild eventGuild = event.getGuild();

        //Start listening on the default channel for the guild
        final TextChannel channel = event.getGuild().getSystemChannel();
        if (channel == null) {
            LOGGER.error("Joined a guild for first time but cant find a channel to start listening on");
            return;
        }
        //Store the channel in database
        final ChannelManager channels = this.db.getChannels(eventGuild);
        try {
            channels.addChannel(channel);
            if (channel.canTalk()) {
                channel.sendMessage("Hello everyone I'm a new bot here, nice to meet you all").queue();
            } else {
                LOGGER.warn("Joined guild but can't chat, still listening");
            }
        } catch (SQLException ex) {
            LOGGER.error("Adding default listen channel failed", ex);
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
            db.close();
        } catch (SQLException ex) {
            LOGGER.error("Shutting down database connection failed", ex);
        }
        LOGGER.info("Shutting down");
    }

    /**
     * Send default greeting when new person joins
     *
     * @param textChannel TextChannel to send greeting on
     * @param member Member who joined
     */
    private void sendDefaultMessage(TextChannel textChannel, Member member) {
        final ConfigManager guildConf = this.db.getConfig(textChannel.getGuild());
        final Optional<String> optTemplate = guildConf.getGreetingTemplate();
        if (optTemplate.isEmpty()) {
            LOGGER.debug("Not greeting because greet template is not set");
            return;
        }
        final String greetTemplate = optTemplate.get();
        final MessageBuilder mb = new MessageBuilder(greetTemplate);
        mb.replace("{name}", member.getEffectiveName());
        mb.replace("{mention}", member.getAsMention());
        textChannel.sendMessage(mb.build()).queue();
    }
}
