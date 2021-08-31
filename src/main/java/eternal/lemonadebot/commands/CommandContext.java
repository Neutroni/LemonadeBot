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
package eternal.lemonadebot.commands;

import eternal.lemonadebot.config.ConfigManager;
import eternal.lemonadebot.cooldowns.CooldownManager;
import eternal.lemonadebot.database.StorageManager;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.translation.TranslationCache;
import java.util.ResourceBundle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Context for the command execution
 *
 * @author Neutroni
 */
public class CommandContext {

    private final CommandMatcher message;
    private final StorageManager storage;

    /**
     * Constructor
     *
     * @param matcher Message that initiated the command
     * @param storage StorageManager to pass to commands
     *
     */
    public CommandContext(final CommandMatcher matcher, final StorageManager storage) {
        this.message = matcher;
        this.storage = storage;
    }

    /**
     * Getter for CommandMatcher
     *
     * @return Command that initiated the action
     */
    public CommandMatcher getMatcher() {
        return this.message;
    }

    /**
     * Get StorageManager
     *
     * @return StorageManager
     */
    public StorageManager getStorageManager() {
        return this.storage;
    }

    /**
     * Get the guild this context is for
     *
     * @return Guild
     */
    public Guild getGuild() {
        return this.message.getGuild();
    }

    /**
     * Get configManager for this command
     *
     * @return ConfigManager
     */
    public ConfigManager getConfigManager() {
        final long guildID = this.message.getGuild().getIdLong();
        return this.storage.getConfigCache().getConfigManager(guildID);
    }

    /**
     * Getter for CommandProvider
     *
     * @return CommandProvider
     */
    public CommandProvider getCommandProvider() {
        return this.storage.getCommandProvider();
    }

    /**
     * Getter for TranslationCache
     *
     * @return Locale specific data
     */
    public TranslationCache getTranslation() {
        return getConfigManager().getTranslationCache();
    }

    /**
     * Shortcut to get the resourcebundle for the locale
     *
     * @return ResourceBundle
     */
    public ResourceBundle getResource() {
        return getTranslation().getResourceBundle();
    }

    /**
     * Shortcut to get the channel command was initiated in
     *
     * @return TextChannel
     */
    public TextChannel getChannel() {
        return this.message.getTextChannel();
    }

    /**
     * Shortcut to get permissionManager from storage
     *
     * @return PermissionManager
     */
    public PermissionManager getPermissionManager() {
        return this.storage.getPermissionManager();
    }

    /**
     * Shortcut to get cooldownManager from storage
     *
     * @return CooldownManager
     */
    public CooldownManager getCooldownManager() {
        return this.storage.getCooldownManager();
    }

}
