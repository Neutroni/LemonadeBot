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

/**
 * Matcher for commands
 *
 * @author Neutroni
 */
public class CommandMatcher {

    private final String message;
    private final boolean matches;
    private final Matcher MATCH;

    /**
     * Constructor
     *
     * @param matcher Matcher to use to match commands
     * @param msg command message
     */
    CommandMatcher(Matcher matcher, String msg) {
        this.MATCH = matcher;
        this.matches = this.MATCH.find();
        this.message = msg;
    }

    /**
     * Get the command from the match
     *
     * @return optional containing command string if found
     */
    public Optional<String> getCommand() {
        if (this.matches) {
            return Optional.of(MATCH.group(2));
        }
        return Optional.empty();
    }

    /**
     * Get parameters limited by whitespace and the rest of the message as last
     * entry in returned array
     *
     * @param count number of parameters to return
     * @return array of parameters
     */
    public String[] getArguments(int count) {
        int parameterStart = MATCH.end();

        //Check if message ends at match end
        if (parameterStart == this.message.length()) {
            return new String[0];
        }

        final String parameterString = this.message.substring(parameterStart);
        return parameterString.split(" ", count + 1);
    }
}
