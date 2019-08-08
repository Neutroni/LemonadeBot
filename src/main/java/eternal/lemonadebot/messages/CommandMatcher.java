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
package eternal.lemonadebot.messages;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Neutroni
 */
public class CommandMatcher {

    private static Pattern PATTERN;
    private final Matcher MATCH;
    private final String message;

    /**
     * Constructor
     *
     * @param msg
     */
    CommandMatcher(String msg) {
        this.message = msg;
        this.MATCH = PATTERN.matcher(msg);
    }

    /**
     *
     * @param prefix
     */
    static void updatePattern(String prefix) {
        //Start of match, optionally @numericID, prefix, match group 2 is command
        PATTERN = Pattern.compile("^(@\\d+ )?" + Pattern.quote(prefix) + "(\\w+) ?");
    }

    /**
     * Get the command from the match
     *
     * @return command string
     */
    public String getCommand() {
        return MATCH.group(2);
    }

    /**
     * Get parameters limited by whitespace
     *
     * @param limit number of parameters to return
     * @return array of parameters
     */
    public String[] getParameters(int limit) {
        final int parameterStart = MATCH.start(4);
        if (parameterStart == -1) {
            return new String[0];
        }
        final String parameterString = this.message.substring(parameterStart);
        return parameterString.split(" ", limit);
    }

    /**
     *
     * @param after Parameter to start after
     * @return Optional of the data, empty if command ends at or before
     * parameter
     */
    public Optional<String> getData(int after) {
        int parameterStart = MATCH.end();
        for (int i = 0; i < after; i++) {
            //Find next parameter
            parameterStart = this.message.indexOf(' ', parameterStart);
            //Did we find anything
            if (parameterStart == -1) {
                return Optional.empty();
            }
            //Check if after this is the end of the string
            parameterStart++;
            if (parameterStart == this.message.length()) {
                return Optional.empty();
            }
        }
        //Found N parameters, return substring after the parameters
        return Optional.of(this.message.substring(parameterStart));
    }

}
