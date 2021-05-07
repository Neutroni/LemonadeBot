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
package eternal.lemonadebot.database;

import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.CommandList;
import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.translation.TranslationCache;
import java.io.Closeable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.entities.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class RuntimeStorage implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DatabaseManager db;
    private final CommandList commands;
    private final Map<Long, GuildDataStore> guildDataStores = new ConcurrentHashMap<>();
    private final Map<Locale, TranslationCache> translationCaches = new ConcurrentHashMap<>();

    public RuntimeStorage(final DatabaseManager db) {
        this.db = db;
        //Initialize commands
        this.commands = new CommandList(db);
    }

    /**
     * Get the list of built in commands
     *
     * @return CommandList
     */
    public CommandList getCommands() {
        return this.commands;
    }

    /**
     * Get GuildDataStore for guild
     *
     * @param guild guild to get manager for
     * @return GuildDataStore
     */
    public GuildDataStore getGuildData(final Guild guild) {
        return this.guildDataStores.computeIfAbsent(guild.getIdLong(), (Long newGuildID) -> {
            return new GuildDataStore(this.db, newGuildID, guild.getJDA(), this);
        });
    }

    /**
     * Get translationCache for locale of a guild
     *
     * @param guild Guild to get transaltion cache for
     * @return TranslationCache
     */
    public TranslationCache getTranslationCache(final Guild guild) {
        final ConfigManager config = getGuildData(guild).getConfigManager();
        final Locale locale = config.getLocale();
        return this.translationCaches.computeIfAbsent(locale, (Locale t) -> {
            return new TranslationCache(locale);
        });
    }

    @Override
    public void close() {
        this.db.close();
        this.guildDataStores.forEach((Long t, GuildDataStore u) -> {
            u.close();
        });
    }

    public void initialize(final List<Guild> guilds) {
        LOGGER.debug("Loading guilds from database");
        this.commands.forEach((ChatCommand t) -> {
            t.initialize(guilds);
        });
        //Initialize guildData storage
        guilds.forEach(this::getGuildData);
    }

}
