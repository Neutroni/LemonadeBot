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
 * Class storing info for guilds
 *
 * @author Neutroni
 */
public class GuildDataStore implements AutoCloseable {

    private final long guildID;
    private final ConfigManager config;
    private final PermissionManager permissions;
    private final TemplateManager commands;
    private final EventManager events;
    private final RemainderManager remainders;
    private final CooldownManager cooldowns;
    private final MessageManager messages;

    /**
     * Constructor
     *
     * @param connection database connection to use
     * @param guild Guild this config is for
     * @param jda JDA to use for remainders
     */
    GuildDataStore(final Connection connection, final Long guildID, JDA jda, final int maxMessages) {
        this.guildID = guildID;
        this.config = new ConfigManager(connection, guildID);
        this.permissions = new PermissionManager(connection, guildID, this.config);
        this.events = new EventManager(connection, guildID);
        this.cooldowns = new CooldownManager(connection, guildID);
        this.commands = new TemplateManager(connection, this.cooldowns, guildID);
        this.remainders = new RemainderManager(connection, jda, this, guildID);
        this.messages = new MessageManager(connection, maxMessages);
    }

    /**
     * Get the ID of the guild this datastore is for
     *
     * @return guildID
     */
    public long getGuildID() {
        return this.guildID;
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
     * Get the permissionsmanager for this datastore
     *
     * @return PermissionsManager
     */
    public PermissionManager getPermissionManager() {
        return this.permissions;
    }

    /**
     * Get tge customcommandmanager for this datastore
     *
     * @return CustomCommandManager
     */
    public TemplateManager getCustomCommands() {
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

    /**
     * Get the cooldownmanager for this datastore
     *
     * @return CooldownManager
     */
    public CooldownManager getCooldownManager() {
        return this.cooldowns;
    }

    /**
     * Get the messagemanager for this guild
     *
     * @return MessageManager
     */
    public MessageManager getMessageManager() {
        return this.messages;
    }

    @Override
    public void close() {
        this.remainders.close();
    }
}
