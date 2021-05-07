/*
 * The MIT License
 *
 * Copyright 2020 joonas.
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

import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.database.RuntimeStorage;
import java.util.Optional;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class JoinListener extends ListenerAdapter {

    private static final Logger LOGGER = LogManager.getLogger();

    private final RuntimeStorage db;

    /**
     * Constructor
     *
     * @param database Database to use for operations
     */
    public JoinListener(final RuntimeStorage database) {
        this.db = database;
    }

    /**
     * Received when someone joins a guild
     *
     * @param event info about the join
     */
    @Override
    public void onGuildMemberJoin(final GuildMemberJoinEvent event) {
        final Guild guild = event.getGuild();
        final Member member = event.getMember();
        LOGGER.debug(() -> {
            return "New member: " + member.getUser().getAsTag()
                    + "\nMember ID: " + member.getId()
                    + "\nOn guild: " + guild.getName()
                    + "\nGuild ID: " + guild.getId();
        });

        //Check if we have a channel to greet them on
        final TextChannel textChannel = guild.getSystemChannel();
        if (textChannel == null) {
            LOGGER.debug("Not greeting because greeting is disabled in discord settings");
            return;
        }

        //Check if guild has message to send to new members
        final ConfigManager guildConf = this.db.getGuildData(guild).getConfigManager();
        final Optional<String> optTemplate = guildConf.getGreetingTemplate();
        if (optTemplate.isEmpty()) {
            LOGGER.debug("Not greeting because greet template is not set");
            return;
        }

        //Send the greeting message
        final String greetTemplate = optTemplate.get();
        final MessageBuilder mb = new MessageBuilder(greetTemplate);
        mb.replace("{name}", member.getEffectiveName());
        mb.replace("{mention}", member.getAsMention());
        textChannel.sendMessage(mb.build()).queue();
    }

    /**
     * Received when we join a guild
     *
     * @param event info about the join
     */
    @Override
    public void onGuildJoin(final GuildJoinEvent event) {
        final Guild eventGuild = event.getGuild();
        LOGGER.info("Joined guild: {}", eventGuild.getName());
        LOGGER.info("Guild id: {}", eventGuild.getId());

        //Send greeting if we can on system channel
        final TextChannel channel = eventGuild.getSystemChannel();
        if (channel == null) {
            return;
        }
        if (channel.canTalk()) {
            channel.sendMessage("Hello everyone I'm a new bot here, nice to meet you all").queue();
        }
    }

}
