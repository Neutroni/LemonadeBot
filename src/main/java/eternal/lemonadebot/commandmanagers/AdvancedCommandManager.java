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
package eternal.lemonadebot.commandmanagers;

import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.OwnerCommand;
import eternal.lemonadebot.database.DatabaseException;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandParser;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Neutroni
 */
public class AdvancedCommandManager {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();

    private final List<ChatCommand> COMMANDS = List.of(new Admin(), new Channel(), new Shutdown());
    private final DatabaseManager DATABASE;
    private final CommandParser COMMAND_PARSER;

    /**
     * Constructor
     *
     * @param db
     * @param cp
     */
    public AdvancedCommandManager(DatabaseManager db, CommandParser cp) {
        this.DATABASE = db;
        this.COMMAND_PARSER = cp;
    }

    /**
     * Get list of commands this manager provides
     *
     * @return Immutable list of commands
     */
    public List<ChatCommand> getCommands() {
        return COMMANDS;
    }

    private class Shutdown extends OwnerCommand {

        @Override
        public String getCommand() {
            return "shutdown";
        }

        @Override
        public String getHelp() {
            return "Shuts the bot down";
        }

        @Override
        public void respond(Member sender, Message message, TextChannel textChannel) {
            textChannel.sendMessage("Shutting down").queue();
            message.getJDA().shutdown();
        }

    }

    private class Admin extends OwnerCommand {

        @Override
        public String getCommand() {
            return "admin";
        }

        @Override
        public String getHelp() {
            return "add - adds admin by mention\n"
                    + "remove - removes admin by mention\n"
                    + "list - lists admins";
        }

        @Override
        public void respond(Member sender, Message message, TextChannel textChannel) {
            final CommandMatcher matcher = COMMAND_PARSER.getCommandMatcher(message);
            final String[] opts = matcher.getParameters(1);
            if (opts.length == 0) {
                textChannel.sendMessage("Provide operation to perform, check help custom for possible operations").queue();
                return;
            }
            final String action = opts[0];

            switch (action) {
                case "add": {
                    final StringBuilder sb = new StringBuilder();
                    final List<Member> mentioned = message.getMentionedMembers();
                    final Member self = textChannel.getGuild().getSelfMember();
                    final List<Member> mentions = new ArrayList<>(mentioned);
                    mentions.remove(self);
                    for (Member m : mentions) {
                        try {
                            final boolean added = DATABASE.addAdmin(m.getUser());
                            if (added) {
                                sb.append("Succesfully added admin ").append(m.getNickname()).append('\n');
                            } else {
                                sb.append("Admin was alredy added ").append(m.getNickname()).append('\n');
                            }
                        } catch (DatabaseException ex) {
                            sb.append("Database error adding admin ").append(m.getNickname());
                            sb.append(" has admin right until next reboot unlessa added succesfully to database\n");
                        }
                    }
                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                case "remove": {
                    final StringBuilder sb = new StringBuilder();
                    final List<Member> mentioned = message.getMentionedMembers();
                    final Member self = textChannel.getGuild().getSelfMember();
                    final List<Member> mentions = new ArrayList<>(mentioned);
                    mentions.remove(self);
                    for (Member m : mentions) {
                        try {
                            final boolean removed = DATABASE.removeAdmin(m.getUser().getId());
                            if (removed) {
                                sb.append("Succesfully removed admin ").append(m.getNickname()).append('\n');
                            } else {
                                sb.append("Admin was alredy removed ").append(m.getNickname()).append('\n');
                            }
                        } catch (DatabaseException ex) {
                            sb.append("Database error removing admin ").append(m.getNickname());
                            sb.append(" does not have admin rights until next reboot unless removed succesfully from database\n");
                        }
                    }
                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                case "list": {
                    final List<String> adminIds = DATABASE.getAdminIds();
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < adminIds.size(); i++) {
                        final User admin = textChannel.getJDA().getUserById(adminIds.get(i));
                        if (admin == null) {
                            sb.append("Found admin in database who could not be found, removing admin status\n");
                            try {
                                final boolean removed = DATABASE.removeAdmin(adminIds.get(i));
                                if (removed) {
                                    sb.append("Admin succesfully removed\n");
                                } else {
                                    sb.append("Admin alredy removed by someone else\n");
                                }
                            } catch (DatabaseException ex) {
                                sb.append("Database failure in removing the admin\n");
                            }
                            continue;
                        }
                        sb.append(admin.getName());
                        if (i < adminIds.size() - 1) {
                            sb.append('\n');
                        }

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

    private class Channel extends OwnerCommand {

        @Override
        public String getCommand() {
            return "channel";
        }

        @Override
        public String getHelp() {
            return "add - adds channel by mention\n"
                    + "remove - removes channel by mention\n"
                    + "list - lists channels";
        }

        @Override
        public void respond(Member sender, Message message, TextChannel textChannel) {
            final CommandMatcher matcher = COMMAND_PARSER.getCommandMatcher(message);
            final String[] opts = matcher.getParameters(1);
            if (opts.length == 0) {
                textChannel.sendMessage("Provide operation to perform, check help custom for possible operations").queue();
                return;
            }
            final String action = opts[0];

            switch (action) {
                case "add": {
                    final StringBuilder sb = new StringBuilder();
                    final List<TextChannel> mentioned = message.getMentionedChannels();
                    for (TextChannel channel : mentioned) {
                        try {
                            final boolean added = DATABASE.addChannel(channel);
                            if (added) {
                                sb.append("Succesfully started listening on channel ").append(channel.getName()).append('\n');
                            } else {
                                sb.append("Was alredy listening on channel ").append(channel.getName()).append('\n');
                            }
                        } catch (DatabaseException ex) {
                            sb.append("Database error adding channel ").append(channel.getName());
                            sb.append(" will listen on channel until next reboot unless added succesfully to database\n");
                        }
                    }
                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                case "remove": {
                    final StringBuilder sb = new StringBuilder();
                    final List<TextChannel> mentions = message.getMentionedChannels();
                    for (TextChannel channel : mentions) {
                        try {
                            final boolean removed = DATABASE.removeChannel(channel.getId());
                            if (removed) {
                                sb.append("Succesfully stopped listening on channel ").append(channel.getName()).append('\n');
                            } else {
                                sb.append("Was not listening on channel ").append(channel.getName()).append('\n');
                            }
                        } catch (DatabaseException ex) {
                            sb.append("Database error removing channel ").append(channel.getName());
                            sb.append(" Will not listen on channel until next reboot unless removed succesfully from database\n");
                        }

                    }
                    textChannel.sendMessage(sb.toString()).queue();
                    break;
                }
                case "list": {
                    final List<String> channelIds = DATABASE.getChannelIds();
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < channelIds.size(); i++) {
                        final TextChannel channel = textChannel.getGuild().getTextChannelById(channelIds.get(i));
                        if (channel == null) {
                            sb.append("Channel in database which could not be found, removing from listened channels\n");
                            try {
                                final boolean removed = DATABASE.removeChannel(channelIds.get(i));
                                if (removed) {
                                    sb.append("Stopped listening on channel succesfully\n");
                                } else {
                                    sb.append("Channel alredy removed by someone else\n");
                                }
                            } catch (DatabaseException ex) {
                                sb.append("Database failure in removing channel from database\n");
                            }
                            continue;
                        }
                        sb.append(channel.getName());
                        if (i < channelIds.size() - 1) {
                            sb.append('\n');
                        }
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
}
