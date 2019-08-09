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

import eternal.lemonadebot.database.DatabaseManager;

/**
 *
 * @author Neutroni
 */
public class CommandBuilder {

    private final ActionManager actionManager = new ActionManager();
    private final DatabaseManager DATABASE;

    /**
     * Constructor
     *
     * @param db database to use
     */
    public CommandBuilder(DatabaseManager db) {
        this.DATABASE = db;
    }

    /**
     * Builds a custom command
     *
     * @param key key for command
     * @param pattern pattern for command
     * @param owner owner fo the command
     * @return the new custom command
     */
    public CustomCommand build(String key, String pattern, String owner) {
        return new CustomCommand(DATABASE, actionManager, key, pattern, owner);
    }

    /**
     * Gets the action manager used to build commands
     *
     * @return ActionManager
     */
    public ActionManager getActionManager() {
        return this.actionManager;
    }

}
