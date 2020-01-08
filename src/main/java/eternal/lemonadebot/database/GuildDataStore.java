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
package eternal.lemonadebot.database;

import java.sql.Connection;
import net.dv8tion.jda.api.JDA;

/**
 *
 * @author Neutroni
 */
public class GuildDataStore {

    private final ConfigManager config;
    private final ChannelManager channels;
    private final CustomCommandManager commands;
    private final EventManager events;
    private final RemainderManager remainders;

    /**
     * Constructor
     *
     * @param connection database connection to use
     * @param jda JDA to use when communicating with discord
     * @param guild Guild this config is for
     */
    GuildDataStore(Connection connection, JDA jda, long guild) {
        this.config = new ConfigManager(connection, guild);
        this.channels = new ChannelManager(connection, guild);
        this.commands = new CustomCommandManager(connection, this.config);
        this.events = new EventManager(connection, guild);
        this.remainders = new RemainderManager(connection, jda, this.events);
    }

    /**
     * Get the configmanager for this datastore
     *
     * @return ConfigManager
     */
    public ConfigManager getConfigManager() {
        return this.config;
    }

    /**
     * Get the channelmanager for this datastore
     *
     * @return ChannelManager
     */
    public ChannelManager getChannelManager() {
        return this.channels;
    }

    /**
     * Get tge customcommandmanager for this datastore
     *
     * @return CustomCommandManager
     */
    public CustomCommandManager getCustomCommands() {
        return this.commands;
    }

    /**
     * Get the eventmanager for this datastore
     *
     * @return EventManager
     */
    public EventManager getEventManager() {
        return this.events;
    }

    /**
     * Get the remaindermanager for this datastore
     *
     * @return RemainderManager
     */
    public RemainderManager getRemainderManager() {
        return this.remainders;
    }

}
