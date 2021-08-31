/*
 * The MIT License
 *
 * Copyright 2021 Neutroni.
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
package eternal.lemonadebot.reactions;

import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.database.StorageManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import java.util.Optional;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 *
 * @author Neutroni
 */
public class MessageReactionListener extends ListenerAdapter {

    private final StorageManager storage;
    private final ReactionManager reactionManager;

    /**
     * Constructor
     *
     * @param storage Storage to use
     */
    public MessageReactionListener(final StorageManager storage) {
        this.storage = storage;
        final DataSource ds = storage.getDataSource();
        this.reactionManager = new ReactionManager(ds);
    }

    @Override
    public void onGuildMessageReactionAdd​(GuildMessageReactionAddEvent event) {
        //Check if we can talk on this channel
        final TextChannel textChannel = event.getChannel();
        if (!textChannel.canTalk()) {
            return;
        }

        //Check if we have a command for reaction
        final MessageReaction.ReactionEmote reaction = event.getReactionEmote();
        final Optional<String> command = reactionManager.onReactionAdd(event.getMessageIdLong(), reaction);
        command.ifPresent((String t) -> {
            runCommand(t, event);
        });
    }

    private void runCommand(final String action, final GenericGuildMessageReactionEvent event) {
        final CommandProvider commandProvider = this.storage.getCommandProvider();
        final CommandMatcher matcher = new ReactionMatcher(action, event);
        final long guildID = event.getGuild().getIdLong();
        final ConfigManager config = this.storage.getConfigCache().getConfigManager(guildID);
        commandProvider.getAction(matcher, config).ifPresent((ChatCommand com) -> {
            //Run the command
            final CommandContext context = new CommandContext(matcher, this.storage);
            com.run(context);
        });
    }

    @Override
    public void onGuildMessageReactionRemove​(GuildMessageReactionRemoveEvent event) {
        //Check if we can talk on this channel
        final TextChannel textChannel = event.getChannel();
        if (!textChannel.canTalk()) {
            return;
        }

        //Check if we have a command for reaction
        final MessageReaction.ReactionEmote reaction = event.getReactionEmote();
        final Optional<String> command = reactionManager.onReactionRemove(event.getMessageIdLong(), reaction);
        command.ifPresent((String t) -> {
            runCommand(t, event);
        });
    }
}
