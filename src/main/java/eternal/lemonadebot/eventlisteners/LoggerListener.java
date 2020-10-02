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
package eternal.lemonadebot.eventlisteners;

import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.MessageManager;
import eternal.lemonadebot.dataobjects.StoredMessage;
import eternal.lemonadebot.translation.TranslationKey;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
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

    private final DatabaseManager db;

    /**
     * Constructor
     *
     * @param database Database to use for operations
     */
    public LoggerListener(DatabaseManager database) {
        this.db = database;
    }

    /**
     * Received when someone sends a message. Logs the message if loggin is
     * enabled
     *
     * @param event Message info
     */
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        final Guild eventGuild = event.getGuild();
        final Message message = event.getMessage();
        final GuildDataStore guildData = this.db.getGuildData(eventGuild);
        final ConfigManager guildConf = guildData.getConfigManager();
        guildData.getMessageManager().logMessage(message, guildConf);
    }

    /**
     * Received when message is updated
     *
     * @param event info about the message that was updated
     */
    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
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
        final MessageManager messageManager = guildData.getMessageManager();
        final Locale locale = guildConf.getLocale();
        final Optional<StoredMessage> oldContent = messageManager.getMessageContent(message.getIdLong());
        oldContent.ifPresent((StoredMessage t) -> {
            final User author = event.getAuthor();
            final EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(TranslationKey.MESSAGE_UPDATE_HEADER.getTranslation(locale));
            eb.setTitle(TranslationKey.MESSAGE_LOG_USER.getTranslation(locale) + author.getAsMention());
            eb.addField(TranslationKey.MESSAGE_CONTENT_BEFORE.getTranslation(locale), t.getContent(), false);
            eb.addField(TranslationKey.MESSAGE_CONTENT_AFTER.getTranslation(locale), message.getContentRaw(), false);
            final OffsetDateTime dt = message.getTimeCreated();
            eb.setFooter(TranslationKey.MESSAGE_CREATION_TIME.getTranslation(locale) + dt.toString());
            logChannel.sendMessage(eb.build()).queue();
        });
    }

    /**
     * Received when a message is removed
     *
     * @param event info about the message that was removed
     */
    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
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
        final MessageManager messageManager = guildData.getMessageManager();
        final Locale locale = guildConf.getLocale();
        final Optional<StoredMessage> oldContent = messageManager.getMessageContent(messageID);
        oldContent.ifPresent((StoredMessage t) -> {
            final EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(TranslationKey.MESSAGE_DELETE_HEADER.getTranslation(locale));
            event.getJDA().retrieveUserById(t.getAuthor()).submit().whenComplete((User user, Throwable error) -> {
                if (user == null) {
                    eb.setTitle(TranslationKey.MESSAGE_LOG_USER_UNKNOWN.getTranslation(locale));
                } else {
                    eb.setTitle(TranslationKey.MESSAGE_LOG_USER + user.getAsMention());
                }
                eb.addField(TranslationKey.MESSAGE_CONTENT.getTranslation(locale), t.getContent(), false);
                final OffsetDateTime dt = TimeUtil.getTimeCreated(messageID);
                eb.setFooter(TranslationKey.MESSAGE_CREATION_TIME.getTranslation(locale) + dt.toString());
                logChannel.sendMessage(eb.build()).queue();
            });
        });
    }
}
