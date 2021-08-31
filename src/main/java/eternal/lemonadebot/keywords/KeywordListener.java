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
import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.commands.CommandProvider;
import eternal.lemonadebot.config.ConfigCache;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.database.StorageManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.messageparsing.MessageMatcher;
import eternal.lemonadebot.messageparsing.SimpleMessageMatcher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.sqlite.Function;
import org.sqlite.SQLiteConnection;

/**
 *
 * @author Neutroni
 */
public class KeywordListener extends ListenerAdapter {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();

    private final CommandProvider commands;
    private final ConfigCache configs;
    private final DataSource dataSource;
    private final StorageManager storage;

    /**
     * Constructor
     *
     * @param storage StorageManager to pass to commands
     */
    public KeywordListener(final StorageManager storage) {
        this.storage = storage;
        this.commands = storage.getCommandProvider();
        this.configs = storage.getConfigCache();
        this.dataSource = storage.getDataSource();
    }

    /**
     * Received when someone sends a message.
     *
     * @param event Message info
     */
    @Override
    public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
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

        //Check to make sure we are not reacting to our own creation
        final ConfigManager guildConf = this.configs.getConfigManager(eventGuild);
        final MessageMatcher matcher = new MessageMatcher(guildConf, message);
        final Optional<ChatCommand> optCommand = commands.getAction(matcher, guildConf);
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

        //Get matching keywords
        final String input = message.getContentDisplay();
        for (final KeywordAction com : getMatchingKeywords(input, eventGuild)) {
            //Ignore modification to the keyword
            if (com.getName().equals(name)) {
                continue;
            }

            final CommandMatcher fakeMatcher = new SimpleMessageMatcher(event.getMember(), event.getChannel());
            final CommandContext fakeContext = new CommandContext(fakeMatcher, storage);
            com.run(fakeContext, true);
        }
    }

    /**
     * Used to load matching keywords from database
     *
     * @param text Text to match agains
     * @param guild Guild to get keywords for
     * @return Collection of the matching keywords
     */
    private Collection<KeywordAction> getMatchingKeywords(final String text, final Guild guild) {
        final String query = "SELECT name,pattern,template,owner,runasowner FROM Keywords WHERE guild = ? AND ;";
        final ArrayList<KeywordAction> keywords = new ArrayList<>();
        try (final Connection connection = this.dataSource.getConnection()) {
            //Add regexp function to the connection
            registerRegexpFunction(connection);
            //Get the matching keywords
            try (final PreparedStatement ps = connection.prepareStatement(query)) {
                final long guildID = guild.getIdLong();
                ps.setLong(1, guildID);
                ps.setString(2, text);
                try (final ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final String commandName = rs.getString("name");
                        final String commandPattern = rs.getString("pattern");
                        final String commandTemplate = rs.getString("template");
                        final long commandOwnerID = rs.getLong("owner");
                        final boolean runAsOwner = rs.getBoolean("runasowner");
                        keywords.add(new KeywordAction(commandName, commandPattern, commandTemplate, commandOwnerID, runAsOwner, guildID));
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Failed to fetch Keywords for a guild from database: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
        }
        return keywords;
    }

    /**
     * Used to register the SQLite regexp function to the connection
     *
     * @param connection Connection to register the function in
     * @throws SQLException If Function creation fails or connection is not
     * from SQLite
     */
    private void registerRegexpFunction(final Connection connection) throws SQLException {
        Function.create(connection.unwrap(SQLiteConnection.class), "REGEXP", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                final String expression = value_text(0);
                String value = value_text(1);
                if (value == null) {
                    value = "";
                }

                final Pattern pattern = Pattern.compile(expression);
                result(pattern.matcher(value).find() ? 1 : 0);
            }
        });
    }
}
