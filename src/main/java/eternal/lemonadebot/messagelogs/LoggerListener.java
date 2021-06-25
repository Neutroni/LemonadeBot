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
package eternal.lemonadebot.messagelogs;

import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.RuntimeStorage;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.ResourceBundle;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.TimeUtil;

/**
 *
 * @author Neutroni
 */
public class LoggerListener extends ListenerAdapter {

    private final RuntimeStorage db;
    private final MessageManager messageManager;

    /**
     * Constructor
     *
     * @param rs RuntimeStorage to use for getting guild info
     * @param db Database to use for logging messages
     */
    public LoggerListener(final RuntimeStorage rs, final DatabaseManager db) {
        this.db = rs;
        this.messageManager = new MessageManager(db.getDataSource());
    }

    /**
     * Received when someone sends a message. Logs the message if logging is
     * enabled
     *
     * @param event Message info
     */
    @Override
    public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
        final Guild eventGuild = event.getGuild();
        final Message message = event.getMessage();
        final GuildDataStore guildData = this.db.getGuildData(eventGuild);
        final ConfigManager guildConf = guildData.getConfigManager();

        //Check if guild has logging enabled
        final Optional<Long> logID = guildConf.getLogChannelID();
        if (logID.isEmpty()) {
            return;
        }
        this.messageManager.logMessage(message);
    }

    /**
     * Received when message is updated
     *
     * @param event info about the message that was updated
     */
    @Override
    public void onGuildMessageUpdate(final GuildMessageUpdateEvent event) {
        final Guild guild = event.getGuild();
        final GuildDataStore guildData = this.db.getGuildData(guild);

        //Check if guild has logging enabled
        final ConfigManager guildConf = guildData.getConfigManager();
        final Optional<Long> logID = guildConf.getLogChannelID();
        if (logID.isEmpty()) {
            return;
        }
        //Get the log channel if it exists
        final TextChannel logChannel = guild.getTextChannelById(logID.get());
        if (logChannel == null) {
            return;
        }
        //Get the old content if stored
        final Message message = event.getMessage();
        final ResourceBundle locale = this.db.getTranslationCache(guild).getResourceBundle();
        final Optional<StoredMessage> oldContent = this.messageManager.getMessageContent(message.getIdLong());
        oldContent.ifPresent((StoredMessage t) -> {
            final User author = event.getAuthor();
            final EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(locale.getString("MESSAGE_UPDATE_HEADER"));
            eb.setTitle(locale.getString("MESSAGE_LOG_USER") + author.getAsMention());
            eb.addField(locale.getString("MESSAGE_CONTENT_BEFORE"), t.getContent(), false);
            eb.addField(locale.getString("MESSAGE_CONTENT_AFTER"), message.getContentRaw(), false);
            final OffsetDateTime dt = message.getTimeCreated();
            eb.setFooter(locale.getString("MESSAGE_CREATION_TIME") + dt.toString());
            logChannel.sendMessageEmbeds(eb.build()).queue();
        });

        //Log the message
        messageManager.logMessage(message);
    }

    /**
     * Received when a message is removed
     *
     * @param event info about the message that was removed
     */
    @Override
    public void onGuildMessageDelete(final GuildMessageDeleteEvent event) {
        final Guild guild = event.getGuild();
        final GuildDataStore guildData = this.db.getGuildData(guild);

        //Check if guild has logging enabled
        final ConfigManager guildConf = guildData.getConfigManager();
        final Optional<Long> logID = guildConf.getLogChannelID();
        if (logID.isEmpty()) {
            return;
        }
        //Get the log channel if it exists
        final TextChannel logChannel = guild.getTextChannelById(logID.get());
        if (logChannel == null) {
            return;
        }
        //Get the old content if stored
        final long messageID = event.getMessageIdLong();
        final ResourceBundle locale = this.db.getTranslationCache(guild).getResourceBundle();
        final Optional<StoredMessage> oldContent = this.messageManager.getMessageContent(messageID);
        oldContent.ifPresent((StoredMessage t) -> {
            final EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(locale.getString("MESSAGE_DELETE_HEADER"));
            event.getJDA().retrieveUserById(t.getAuthor()).submit().whenComplete((User user, Throwable error) -> {
                if (user == null) {
                    eb.setTitle(locale.getString("MESSAGE_LOG_USER_UNKNOWN"));
                } else {
                    eb.setTitle(locale.getString("MESSAGE_LOG_USER") + user.getAsMention());
                }
                eb.addField(locale.getString("MESSAGE_CONTENT"), t.getContent(), false);
                final OffsetDateTime dt = TimeUtil.getTimeCreated(messageID);
                eb.setFooter(locale.getString("MESSAGE_CREATION_TIME") + dt.toString());
                logChannel.sendMessageEmbeds(eb.build()).queue();
            });
        });
    }
}
