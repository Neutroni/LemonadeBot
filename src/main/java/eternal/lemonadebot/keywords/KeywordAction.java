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

import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.translation.TranslationKey;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author Neutroni
 */
public class KeywordAction extends CustomCommand {

    private final Pattern keywordPattern;

    /**
     * Constructor
     *
     * @param name Name of keyword
     * @param patternString pattern for keyword
     * @param actionTemplate template for action
     * @param owner owner of the command
     */
    public KeywordAction(final String name, final String patternString, final String actionTemplate, final long owner) throws PatternSyntaxException {
        super(name, actionTemplate, owner);
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
    public CompletableFuture<String> toListElement(final Locale locale, final JDA jda) {
        final CompletableFuture<String> result = new CompletableFuture<>();
        final String template = TranslationKey.KEYWORD_COMMAND_LIST_ELEMENT.getTranslation(locale);
        jda.retrieveUserById(getAuthor()).queue((User commandOwner) -> {
            //Found user
            final String creatorName = commandOwner.getAsMention();
            result.complete(String.format(template, getName(), getPatternString(), creatorName));
        }, (Throwable t) -> {
            //User missing
            final String creatorName = TranslationKey.UNKNOWN_USER.getTranslation(locale);
            result.complete(String.format(template, getName(), getPatternString(), creatorName));
        });
        return result;
    }

}
