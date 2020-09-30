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

import eternal.lemonadebot.customcommands.FakeMessageMatcher;
import eternal.lemonadebot.customcommands.KeywordAction;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.CooldownManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.database.KeywordManager;
import eternal.lemonadebot.database.PermissionManager;
import eternal.lemonadebot.translation.TranslationKey;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 *
 * @author Neutroni
 */
public class MessageKeywordListener extends ListenerAdapter {

    private final DatabaseManager db;

    /**
     * Constructor
     *
     * @param database Database to use for operations
     */
    public MessageKeywordListener(DatabaseManager database) {
        this.db = database;
    }

    /**
     * Received when someone sends a message.
     *
     * @param event Message info
     */
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        final Guild eventGuild = event.getGuild();
        final Message message = event.getMessage();
        final String input = message.getContentRaw();
        final GuildDataStore guildData = this.db.getGuildData(eventGuild);
        final ConfigManager configManager = guildData.getConfigManager();
        final KeywordManager keywordManager = guildData.getKeywordManager();
        final CooldownManager cooldownManager = guildData.getCooldownManager();
        final CommandMatcher commandMatcher = new MessageMatcher(configManager, message);

        //Find if message contains any keyword
        for (KeywordAction com : keywordManager.getCommands()) {
            if (!com.matches(input)) {
                continue;
            }
            
            //Check cooldown
            final String commandName = com.getName();
            final Optional<Duration> cooldownTime = cooldownManager.updateActivationTime(commandName);
            if (cooldownTime.isPresent()) {
                return;
            }
            
            //Run the command
            final CommandMatcher fakeMatcher = new FakeMessageMatcher(commandMatcher, commandName);
            com.respond(fakeMatcher, guildData);

        }

    }
}
