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
package eternal.lemonadebot.commands;

import eternal.lemonadebot.commandtypes.OwnerCommand;
import eternal.lemonadebot.database.ChannelManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandMatcher;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
class ChannelManagmentCommand extends OwnerCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ChannelManager channels;
    private final JDA jda;

    /**
     * Constructor
     *
     * @param db database to store channels in
     */
    ChannelManagmentCommand(DatabaseManager db) {
        this.channels = db.getChannels();
        this.jda = db.getJDA();
    }

    @Override
    public String getCommand() {
        return "channel";
    }

    @Override
    public String getHelp() {
        return "Syntax: channel <action>\n"
                + "<action> can be one of the following:\n"
                + "  add - start listening on mentioned channels\n"
                + "  remove - stop listening on mentioned channels\n"
                + "  list - lists channels we listen on";
    }

    @Override
    public void respond(CommandMatcher matcher) {
        final Optional<TextChannel> optChannel = matcher.getTextChannel();
        if (optChannel.isEmpty()) {
            matcher.getMessageChannel().sendMessage("Channels are specific to discord servers and must be edited on one").queue();
            return;
        }
        final TextChannel textChannel = optChannel.get();
        final Message message = matcher.getMessage();

        final String[] arguments = matcher.getArguments(1);
        if (arguments.length == 0) {
            matcher.getMessage().getChannel().sendMessage("Provide operation to perform, check help for possible operations").queue();
            return;
        }
        final String action = arguments[0];
        switch (action) {
            case "add": {
                addChannel(textChannel, message);
                break;
            }
            case "remove": {
                removeChannel(textChannel, message);
                break;
            }
            case "list": {
                listChannels(textChannel);
                break;
            }
            default: {
                textChannel.sendMessage("Unkown action: " + action).queue();
            }
        }
    }

    private void addChannel(TextChannel textChannel, Message message) {
        final List<TextChannel> mentioned = message.getMentionedChannels();
        //Check if any channels were mentioned
        if (mentioned.isEmpty()) {
            textChannel.sendMessage("Mention channels you want to add").queue();
            return;
        }
        final StringBuilder sb = new StringBuilder();
        for (TextChannel channel : mentioned) {
            try {
                if (channels.addChannel(channel)) {
                    sb.append("Succesfully started listening on channel ").append(channel.getName()).append('\n');
                } else {
                    sb.append("Was alredy listening on channel ").append(channel.getName()).append('\n');
                }
            } catch (SQLException ex) {
                sb.append("Database error adding channel ").append(channel.getName());
                sb.append(" will listen on channel until next reboot unless added succesfully to database\n");

                LOGGER.error("Failure to add channel");
                LOGGER.warn(ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        }
        textChannel.sendMessage(sb.toString()).queue();
    }

    private void removeChannel(TextChannel textChannel, Message message) {
        final List<TextChannel> mentions = message.getMentionedChannels();
        if (mentions.isEmpty()) {
            textChannel.sendMessage("Mention channels you want to stop listening on").queue();
            return;
        }
        final StringBuilder sb = new StringBuilder();
        for (TextChannel channel : mentions) {
            try {
                if (channels.removeChannel(channel.getIdLong())) {
                    sb.append("Succesfully stopped listening on channel ").append(channel.getName()).append('\n');
                } else {
                    sb.append("Was not listening on channel ").append(channel.getName()).append('\n');
                }
            } catch (SQLException ex) {
                sb.append("Database error removing channel ").append(channel.getName());
                sb.append(" Will not listen on channel until next reboot unless removed succesfully from database\n");

                LOGGER.error("Failure to remove channel");
                LOGGER.warn(ex.getMessage());
                LOGGER.trace("Stack trace", ex);
            }
        }
        textChannel.sendMessage(sb.toString()).queue();
    }

    private void listChannels(TextChannel textChannel) {
        final List<Long> channelIds = channels.getChannels();
        final StringBuilder sb = new StringBuilder("Channels:\n");
        for (Long id : channelIds) {
            final TextChannel listeningChannel = this.jda.getTextChannelById(id);
            if (listeningChannel == null) {
                LOGGER.warn("Channel in database which could not be found, removing from listened channels\n");
                try {
                    final boolean removed = channels.removeChannel(id);
                    LOGGER.info("Status for removing channel " + id + ": " + removed);
                } catch (SQLException ex) {
                    LOGGER.error("Failure to remove channel while listing channels");
                    LOGGER.warn(ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                continue;
            }
            //Only print channels for current server
            if (textChannel.getGuild().equals(listeningChannel.getGuild())) {
                sb.append(listeningChannel.getName()).append('\n');
            }
        }
        if (channelIds.isEmpty()) {
            sb.append("Not listening on any channels");
        }
        textChannel.sendMessage(sb.toString()).queue();
    }

}
