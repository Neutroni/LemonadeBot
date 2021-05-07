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

import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.translation.TranslationCache;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author Neutroni
 */
public class KeywordAction extends CustomCommand {

    private final Pattern keywordPattern;
    private final boolean runAsOwner;

    /**
     * Constructor
     *
     * @param name Name of keyword
     * @param patternString pattern for keyword
     * @param actionTemplate template for action
     * @param owner owner of the command
     * @param runAsOwner If true keyword runs as the creator of the keyword,
     * otherwise as the person who triggered it.
     */
    public KeywordAction(final String name, final String patternString, final String actionTemplate, final long owner, boolean runAsOwner) throws PatternSyntaxException {
        super(name, actionTemplate, owner);
        this.runAsOwner = runAsOwner;
        this.keywordPattern = Pattern.compile(patternString);
    }

    /**
     * Get the pattern for keyword
     *
     * @return String of regex
     */
    public String getPatternString() {
        return this.keywordPattern.pattern();
    }

    /**
     * Check if the input contains keyword for this command
     *
     * @param input input to check
     * @return true if input contains keyword
     */
    public boolean matches(final String input) {
        return this.keywordPattern.matcher(input).find();
    }

    @Override
    protected void respond(final CommandContext context) {
        final CommandMatcher message = context.getMatcher();
        final GuildDataStore guildData = context.getGuildData();
        final TranslationCache translation = context.getTranslation();
        if (this.runAsOwner) {
            //Command should be run as the creator of the keyword
            message.getGuild().retrieveMemberById(getAuthor()).queue((Member t) -> {
                final KeywordMatcher matcher = new KeywordMatcher(message, t);
                final CommandContext fakeContext = new CommandContext(matcher, guildData, translation);
                super.run(fakeContext, true);
            });
        } else {
            super.run(context, true);
        }
    }

    @Override
    public CompletableFuture<String> toListElement(final ResourceBundle locale, final JDA jda) {
        final CompletableFuture<String> result = new CompletableFuture<>();
        final String template = locale.getString("KEYWORD_COMMAND_LIST_ELEMENT");
        jda.retrieveUserById(getAuthor()).queue((User commandOwner) -> {
            //Found user
            final String creatorName = commandOwner.getAsMention();
            result.complete(String.format(template, getName(), getPatternString(), creatorName));
        }, (Throwable t) -> {
            //User missing
            final String creatorName = locale.getString("UNKNOWN_USER");
            result.complete(String.format(template, getName(), getPatternString(), creatorName));
        });
        return result;
    }

    boolean shouldRunAsOwner() {
        return this.runAsOwner;
    }

}
