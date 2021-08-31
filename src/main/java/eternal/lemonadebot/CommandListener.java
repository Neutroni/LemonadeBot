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

import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.config.ConfigCache;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.database.StorageManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.messageparsing.MessageMatcher;
import eternal.lemonadebot.translation.TranslationCache;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * JDA MessageListener, responsible for reacting to messages discord sends
 *
 * @author Neutroni
 */
public class CommandListener extends ListenerAdapter {

    private final CommandProvider commands;
    private final ConfigCache configs;
    private final StorageManager storage;

    /**
     * Constructor
     *
     * @param storage StorageManager to get configuration and commands from
     */
    public CommandListener(final StorageManager storage) {
        this.commands = storage.getCommandProvider();
        this.configs = storage.getConfigCache();
        this.storage = storage;
    }

    /**
     * Received when someone sends a message
     *
     * @param event message info
     */
    @Override
    public void onGuildMessageReceived(final @Nonnull GuildMessageReceivedEvent event) {
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
        final ConfigManager configManager = this.configs.getConfigManager(eventGuild.getIdLong());
        final TranslationCache translation = configManager.getTranslationCache();
        final ResourceBundle resources = translation.getResourceBundle();
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
                responseBuilder.appendFormat(resources.getString("BOT_VERSION"), LemonadeBot.BOT_VERSION);
                responseBuilder.append('\n');
                responseBuilder.appendFormat(resources.getString("PREFIX_CURRENT_VALUE"), configManager.getCommandPrefix());
                textChannel.sendMessage(responseBuilder.build()).queue();
                return;
            }
        }

        //Check if message is a command
        final CommandMatcher cmdMatch = new MessageMatcher(configManager, message);
        final CommandContext context = new CommandContext(cmdMatch, this.storage);
        final Optional<ChatCommand> action = this.commands.getAction(cmdMatch, configManager);
        action.ifPresent((ChatCommand command) -> {
            //Run the command
            command.run(context);
        });
    }
}
