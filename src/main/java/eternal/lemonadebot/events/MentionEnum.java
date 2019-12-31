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
package eternal.lemonadebot.events;

/**
 * Enum for deciding who to mention in reminders
 *
 * @author Neutroni
 */
public enum MentionEnum {
    /**
     * Do not mention anyone
     */
    NONE("none"),
    /**
     * Mention event members
     */
    EVENT("event"),
    /**
     * Mention people online
     */
    HERE("here"),
    /**
     * Error parsing enum
     */
    ERROR("error");

    private final String enumName;

    /**
     * Constructor
     *
     * @param name name of the enum
     */
    MentionEnum(String name) {
        this.enumName = name;
    }

    /**
     * Get the friendly name of this enum
     *
     * @return String representation for this enum
     */
    public String getFriendlyName() {
        return this.enumName;
    }

    /**
     * Get enum by it's friendly name
     *
     * @param friendlyName Name to search for
     * @return Enum that matches the friendly name, ERROR if not found
     */
    public static MentionEnum getByName(String friendlyName) {
        for (MentionEnum e : MentionEnum.values()) {
            if (e.getFriendlyName().equals(friendlyName)) {
                return e;
            }
        }
        return ERROR;
    }
}
