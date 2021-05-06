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

import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.translation.TranslationCache;
import java.util.ResourceBundle;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Context for the command execution
 *
 * @author Neutroni
 */
public class CommandContext {

    private final CommandMatcher command;
    private final GuildDataStore guildData;
    private final TranslationCache translation;

    /**
     * Constructor
     *
     * @param matcher Message that initiated the command
     * @param guildData Data for the guild command was called in
     * @param translation Translation data for the locale of the guild
     */
    public CommandContext(final CommandMatcher matcher, final GuildDataStore guildData, final TranslationCache translation) {
        this.command = matcher;
        this.guildData = guildData;
        this.translation = translation;
    }

    /**
     * Getter for CommandMatcher
     *
     * @return Command that initiated the action
     */
    public CommandMatcher getMatcher() {
        return command;
    }

    /**
     * Getter for GuildDataStore
     *
     * @return Data for the guild
     */
    public GuildDataStore getGuildData() {
        return guildData;
    }

    /**
     * Getter for TranslationCache
     *
     * @return Locale specific data
     */
    public TranslationCache getTranslation() {
        return translation;
    }

    /**
     * Shortcut to get the channel command was initiated in
     *
     * @return TextChannel
     */
    public TextChannel getChannel() {
        return this.command.getTextChannel();
    }

    /**
     * Shortcut to get the resourcebundle for the locale
     *
     * @return ResourceBundle
     */
    public ResourceBundle getResource() {
        return this.translation.getResourceBundle();
    }

}
