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

import eternal.lemonadebot.database.DatabaseManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holds command pattern for parsing commands
 *
 * @author Neutroni
 */
class CommandPattern {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DatabaseManager DATABASE;
    private volatile Pattern PATTERN;

    /**
     * Constructor
     *
     * @param db Database to use
     */
    CommandPattern(DatabaseManager db) {
        this.DATABASE = db;
        final Optional<String> opt = DATABASE.getConfig().getCommandPrefix();
        if (opt.isPresent()) {
            final String prefix = opt.get();
            updatePattern(prefix);
        } else {
            updatePattern("!");
        }

    }

    /**
     * Gets a command matcher for given input
     *
     * @param msg
     * @return
     */
    CommandMatcher getCommandMatcher(String msg) {
        return new CommandMatcher(this.PATTERN.matcher(msg), msg);
    }

    /**
     * Updates command pattern
     *
     * @param prefix
     */
    private void updatePattern(String prefix) {
        //Start of match, optionally @numericID, prefix, match group 2 is command
        this.PATTERN = Pattern.compile("^(@\\d+ )?" + Pattern.quote(prefix) + "(\\w+) ?");
    }

    /**
     * Sets the command prefix
     *
     * @param prefix prefix to use
     * @return was prefix stored succesfully
     */
    boolean setPrefix(String prefix) {
        try {
            updatePattern(prefix);
            DATABASE.getConfig().setCommandPrefix(prefix);
            return true;
        } catch (SQLException ex) {
            LOGGER.error(ex);
        }
        return false;
    }
}
