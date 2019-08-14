/*
 * The MIT License
 *
 * Copyright 2019 joonas.
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
import eternal.lemonadebot.database.DatabaseException;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandParser;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author joonas
 */
class AdminManagmentCommand extends OwnerCommand {

    private final CommandParser commandParser;
    private final DatabaseManager DATABASE;

    /**
     * Constructor
     *
     * @param parser commandparser to use for parsing commands
     * @param db database to store admins in
     */
    AdminManagmentCommand(CommandParser parser, DatabaseManager db) {
        this.commandParser = parser;
        this.DATABASE = db;
    }

    @Override
    public String getCommand() {
        return "admin";
    }

    @Override
    public String getHelp() {
        return "add - adds admin by mention\n" + "remove - removes admin by mention\n" + "list - lists admins";
    }

    @Override
    public synchronized void respond(Member sender, Message message, TextChannel textChannel) {
        final CommandMatcher matcher = commandParser.getCommandMatcher(message);
        final String[] opts = matcher.getArguments(1);
        if (opts.length == 0) {
            textChannel.sendMessage("Provide operation to perform, check help for possible operations").queue();
            return;
        }
        final String action = opts[0];
        switch (action) {
            case "add": {
                final List<Member> mentions = new ArrayList<>(message.getMentionedMembers());
                final Member self = textChannel.getGuild().getSelfMember();
                mentions.remove(self);
                //Check if any member was mentioned
                if (mentions.isEmpty()) {
                    textChannel.sendMessage("Mention users you want to add as admin").queue();
                    return;
                }
                final StringBuilder sb = new StringBuilder();
                for (Member m : mentions) {
                    try {
                        if (DATABASE.addAdmin(m.getUser())) {
                            sb.append("Succesfully added admin ").append(m.getNickname()).append('\n');
                        } else {
                            sb.append("Admin was alredy added ").append(m.getNickname()).append('\n');
                        }
                    } catch (DatabaseException ex) {
                        sb.append("Database error adding admin ").append(m.getNickname());
                        sb.append(" add again once database issue is fixed to make add persist after reboot\n");
                    }
                }
                textChannel.sendMessage(sb.toString()).queue();
                break;
            }
            case "remove": {
                final List<Member> mentions = new ArrayList<>(message.getMentionedMembers());
                final Member self = textChannel.getGuild().getSelfMember();
                mentions.remove(self);
                //Check if any members was mentioned
                if (mentions.isEmpty()) {
                    textChannel.sendMessage("Mention the users you want to remove admin status from").queue();
                    return;
                }
                final StringBuilder sb = new StringBuilder();
                for (Member m : mentions) {
                    try {
                        if (DATABASE.removeAdmin(m.getUser().getId())) {
                            sb.append("Succesfully removed admin ").append(m.getNickname()).append('\n');
                        } else {
                            sb.append("Admin was alredy removed ").append(m.getNickname()).append('\n');
                        }
                    } catch (DatabaseException ex) {
                        sb.append("Database error removing admin ").append(m.getNickname());
                        sb.append(" remove again once database issue is fixed to make remove persist after reboot\n");
                    }
                }
                textChannel.sendMessage(sb.toString()).queue();
                break;
            }
            case "list": {
                final List<String> adminIds = DATABASE.getAdminIds();
                final StringBuilder sb = new StringBuilder();
                for (String adminId : adminIds) {
                    final User admin = textChannel.getJDA().getUserById(adminId);
                    if (admin == null) {
                        sb.append("Found admin in database who could not be found, removing admin status\n");
                        try {
                            if (DATABASE.removeAdmin(adminId)) {
                                sb.append("Admin succesfully removed\n");
                            } else {
                                sb.append("Admin alredy removed by someone else\n");
                            }
                        } catch (DatabaseException ex) {
                            sb.append("Database failure in removing the admin\n");
                        }
                        continue;
                    }
                    sb.append(admin.getName()).append('\n');
                }
                if (adminIds.isEmpty()) {
                    sb.append("No admins added.");
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
