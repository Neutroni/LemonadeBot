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
import eternal.lemonadebot.messages.CommandManager;
import eternal.lemonadebot.messages.CommandMatcher;
import java.sql.SQLException;
import java.util.List;
import net.dv8tion.jda.api.entities.Member;
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

    private final CommandManager commandParser;
    private final ChannelManager channels;

    /**
     * Constructor
     *
     * @param parser parser to parse command arguments with
     * @param db database to store channels in
     */
    ChannelManagmentCommand(CommandManager parser, DatabaseManager db) {
        this.commandParser = parser;
        this.channels = db.getChannels();
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
    public void respond(Member sender, Message message, TextChannel textChannel) {
        final CommandMatcher matcher = commandParser.getCommandMatcher(message);
        final String[] opts = matcher.getArguments(1);
        if (opts.length == 0) {
            textChannel.sendMessage("Provide operation to perform, check help for possible operations").queue();
            return;
        }
        final String action = opts[0];
        switch (action) {
            case "add": {
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
                break;
            }
            case "remove": {
                final List<TextChannel> mentions = message.getMentionedChannels();
                if (mentions.isEmpty()) {
                    textChannel.sendMessage("Mention channels you want to stop listening on").queue();
                    return;
                }
                final StringBuilder sb = new StringBuilder();
                for (TextChannel channel : mentions) {
                    try {
                        if (channels.removeChannel(channel.getId())) {
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
                break;
            }
            case "list": {
                final List<String> channelIds = channels.getChannels();
                final StringBuilder sb = new StringBuilder("Channels:\n");
                for (String id : channelIds) {
                    final TextChannel channel = textChannel.getGuild().getTextChannelById(id);
                    if (channel == null) {
                        sb.append("Channel in database which could not be found, removing from listened channels\n");
                        try {
                            if (channels.removeChannel(id)) {
                                sb.append("Stopped listening on channel succesfully\n");
                            } else {
                                sb.append("Channel alredy removed by someone else\n");
                            }
                        } catch (SQLException ex) {
                            sb.append("Database failure in removing channel from database\n");

                            LOGGER.error("Failure to remove channel while listing channels");
                            LOGGER.warn(ex.getMessage());
                            LOGGER.trace("Stack trace", ex);
                        }
                        continue;
                    }
                    sb.append(channel.getName()).append('\n');
                }
                if (channelIds.isEmpty()) {
                    sb.append("Not listening on any channels");
                }
                textChannel.sendMessage(sb.toString()).queue();
                break;
            }
            default: {
                textChannel.sendMessage("Unkown action: " + action).queue();
            }
        }
    }

}
