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
package eternal.lemonadebot.keywords;

import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.cooldowns.CooldownManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.messageparsing.MessageMatcher;
import eternal.lemonadebot.messageparsing.SimpleMessageMatcher;
import java.time.Duration;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 *
 * @author Neutroni
 */
public class KeywordListener extends ListenerAdapter {

    private final DatabaseManager db;

    /**
     * Constructor
     *
     * @param database Database to use for operations
     */
    public KeywordListener(DatabaseManager database) {
        this.db = database;
    }

    /**
     * Received when someone sends a message.
     *
     * @param event Message info
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

        final Guild eventGuild = event.getGuild();
        final Message message = event.getMessage();
        final GuildDataStore guildData = this.db.getGuildData(eventGuild);
        final KeywordManager keywordManager = guildData.getKeywordManager();
        final CooldownManager cooldownManager = guildData.getCooldownManager();

        //Check to make sure we are not reacting to our own creation
        final ConfigManager guildConf = guildData.getConfigManager();
        final CommandProvider commandProvider = guildData.getCommandProvider();
        final MessageMatcher matcher = new MessageMatcher(guildConf, message);
        final Optional<ChatCommand> optCommand = commandProvider.getAction(matcher);
        String name = null;
        if (optCommand.isPresent()) {
            final ChatCommand command = optCommand.get();
            if (command instanceof KeywordCommand) {
                final String[] args = matcher.getArguments(2);
                if (args.length > 2) {
                    name = args[1];
                }
            }
        }

        //Find if message contains any keyword
        final String input = message.getContentDisplay();
        for (KeywordAction com : keywordManager.getCommands()) {
            if (!com.matches(input)) {
                continue;
            }

            //Ignore modification to the keyword
            if (com.getName().equals(name)) {
                continue;
            }

            //Check cooldown
            final String commandName = com.getName();
            final Member member = matcher.getMember();
            Optional<Duration> cooldownTime = cooldownManager.checkCooldown(member, commandName);
            if (cooldownTime.isEmpty()) {
                //Run the command
                final CommandMatcher fakeMatcher = new SimpleMessageMatcher(event.getMember(), event.getChannel(), guildConf.getLocale());
                com.respond(fakeMatcher, guildData);
            }
        }
    }
}
