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
package eternal.lemonadebot.messages;

/**
 * Permissions required to run a command
 *
 * @author Neutroni
 */
public enum CommandPermission {
    /**
     * Commands anyone can run
     */
    USER("User"),
    /**
     * Command members can run
     */
    MEMBER("Member on this server"),
    /**
     * Command admins can run
     */
    ADMIN("Admin on the server"),
    /**
     * Command only owner can run
     */
    OWNER("Owner of this bot");

    private final String desc;

    private CommandPermission(String description) {
        this.desc = description;
    }

    /**
     * Get the descriptiotn for this role
     *
     * @return description string
     */
    public String getDescription() {
        return this.desc;
    }
}
