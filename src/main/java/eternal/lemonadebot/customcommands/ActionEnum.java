/*
 * The MIT License
 *
 * Copyright 2019 Neutroni.
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

import java.util.regex.Pattern;

/**
 *
 * @author Neutroni
 */
public enum ActionEnum {

    /**
     * Flips a coin
     */
    COIN("{coin}", "Flips a coin"),
    /**
     * Rolls a dice
     */
    DICE("{dice}", "Rolls a dice"),
    /**
     * Rolls a d20
     */
    D20("{d20}", "Rolls a d20"),
    /**
     * Adds names of mentioned users to message
     */
    MENTION("{mention}", "Mentioned users");

    private final String key;
    private final String help;
    private final Pattern pattern;

    /**
     * Constructor
     *
     * @param key key to call this action with
     * @param help help message for this action
     */
    private ActionEnum(String key, String help) {
        this.key = key;
        this.help = help;
        this.pattern = Pattern.compile(Pattern.quote(key));
    }

    /**
     * Key for this action
     *
     * @return string presentation of the key
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Get the pattern for this action
     *
     * @return Pattern
     */
    public Pattern getPattern() {
        return this.pattern;
    }

    /**
     * Gets the help text for this action
     *
     * @return help text string
     */
    public String getHelp() {
        return this.help;
    }
}
