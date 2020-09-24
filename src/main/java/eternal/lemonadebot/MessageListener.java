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

import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.CooldownManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.PermissionManager;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ShutdownEvent;
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

    /**
     * Constructor
     *
     * @param database Database to use for operations
     */
    public MessageListener(DatabaseManager database) {
        this.db = database;
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

        //Check if we can talk on this channel
        final TextChannel textChannel = event.getChannel();
        if (!textChannel.canTalk()) {
            return;
        }

        //Check if message is just ping to us
        final Guild eventGuild = event.getGuild();
        final Member selfMember = eventGuild.getSelfMember();
        final Message message = event.getMessage();
        final GuildDataStore guildData = this.db.getGuildData(eventGuild);
        final ConfigManager configManager = guildData.getConfigManager();
        final Locale locale = configManager.getLocale();
        final List<Member> mentionedMembers = message.getMentionedMembers();
        if (mentionedMembers.size() == 1 && mentionedMembers.contains(selfMember)) {
            //Check that the message is just the mention and possibly whitespace
            final String textContent = message.getContentRaw();
            final Pattern mentionPattern = Message.MentionType.USER.getPattern();
            final Matcher mentionMatcher = mentionPattern.matcher(textContent);
            final StringBuilder sb = new StringBuilder();
            while (mentionMatcher.find()) {
                mentionMatcher.appendReplacement(sb, "");
            }
            mentionMatcher.appendTail(sb);
            if (sb.toString().isBlank()) {
                final MessageBuilder responseBuilder = new MessageBuilder();
                responseBuilder.appendFormat(TranslationKey.BOT_VERSION.getTranslation(locale), LemonadeBot.BOT_VERSION);
                responseBuilder.append('\n');
                responseBuilder.appendFormat(TranslationKey.PREFIX_CURRENT_VALUE.getTranslation(locale), configManager.getCommandPrefix());
                textChannel.sendMessage(responseBuilder.build()).queue();
                return;
            }
        }

        //Check if message is a command
        final MessageMatcher cmdMatch = new MessageMatcher(configManager, message);
        final CommandProvider commandProvider = guildData.getCommandProvider();
        final Optional<ChatCommand> action = commandProvider.getAction(cmdMatch);
        if (action.isEmpty()) {
            return;
        }
        final ChatCommand command = action.get();

        //Log the message if debug is enabled
        LOGGER.debug(() -> {
            return "Found command: " + cmdMatch.getAction() + " in:\n" + cmdMatch.getMessageText();
        });

        //Check if user has permission
        final Member member = cmdMatch.getMember();
        final PermissionManager permissions = guildData.getPermissionManager();
        final String inputString = cmdMatch.getAction();

        if (!permissions.hasPermission(member, inputString)) {
            return;
        }

        //Check if command is on cooldown
        if (!MemberRank.getRank(member).equals(MemberRank.ADMIN)) {
            final CooldownManager cdm = guildData.getCooldownManager();
            final Optional<Duration> optCooldown = cdm.updateActivationTime(cmdMatch.getAction());
            if (optCooldown.isPresent()) {
                final String template = TranslationKey.ERROR_COMMAND_COOLDOWN_TIME.getTranslation(locale);
                final String currentCooldown = CooldownManager.formatDuration(optCooldown.get(), locale);
                textChannel.sendMessage(template + currentCooldown).queue();
                return;
            }
        }

        //Run the command
        command.respond(cmdMatch, guildData);

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
            LOGGER.error("Shutting down database connection failed: {}", ex);
            LOGGER.trace("Stack trace", ex);
        }
        LOGGER.info("Shutting down");
    }

}
