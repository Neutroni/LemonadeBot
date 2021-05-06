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
package eternal.lemonadebot.customcommands;

import eternal.lemonadebot.commands.CommandContext;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that holds substitutions for custom command templates
 *
 * @author Neutroni
 */
public class ActionTemplate {

    private final String helpText;
    private final Pattern pattern;
    private final BiFunction<CommandContext,Matcher,String> function;

    /**
     * Constructor
     *
     * @param pattern pattern string for this action
     * @param help help text for this action
     * @param func function this action executes
     */
    ActionTemplate(final String pattern, final String help, final BiFunction<CommandContext,Matcher,String> func) {
        this.pattern = Pattern.compile(pattern);
        this.helpText = help;
        this.function = func;
    }

    /**
     * Gets the help text for this action
     *
     * @param locale Locale to return the help text in
     * @return help string
     */
    public String getHelp(final ResourceBundle locale) {
        return locale.getString(this.helpText);
    }

    /**
     * Get a matcher for input string
     *
     * @param input input to match against
     * @return matcher for input
     */
    public Matcher getMatcher(final CharSequence input) {
        return this.pattern.matcher(input);
    }

    /**
     * Gets the replacement for given input string
     *
     * @param context Context for the command
     * @param templateMatcher match to replace
     * @return replacement string
     */
    public String getValue(final CommandContext context, final Matcher templateMatcher) {
        return this.function.apply(context, templateMatcher);
    }
}
