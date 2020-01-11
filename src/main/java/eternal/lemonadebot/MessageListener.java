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
import java.util.Optional;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
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
        LOGGER.debug("New member: " + member.getEffectiveName());

        //Check if we have a channel to greet them on
        final TextChannel textChannel = guild.getSystemChannel();
        if (textChannel == null) {
            LOGGER.debug("Not greeting because greeting is disabled in discord settings");
            return;
        }

        //Check if we should react on this channel
        final ChannelManager channels = this.db.getChannels(guild);
        if (channels.hasChannel(textChannel)) {
            LOGGER.debug("Not greeting because not listening on the channel");
            return;
        }

        //Check if guild has message to send to new members
        final ConfigManager guildConf = this.db.getConfig(guild);
        final Optional<String> optTemplate = guildConf.getGreetingTemplate();
        if (optTemplate.isEmpty()) {
            LOGGER.debug("Not greeting because greet template is not set");
            return;
        }

        //Send the greeting message
        final String greetTemplate = optTemplate.get();
        final MessageBuilder mb = new MessageBuilder(greetTemplate);
        mb.replace("{name}", member.getEffectiveName());
        mb.replace("{mention}", member.getAsMention());
        textChannel.sendMessage(mb.build()).queue();
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

}
