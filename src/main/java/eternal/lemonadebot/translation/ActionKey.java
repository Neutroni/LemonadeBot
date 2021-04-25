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
package eternal.lemonadebot.translation;

import javax.annotation.Nullable;

/**
 * Used help selecting action from translated input, see TranslationCache
 *
 * @author Neutroni
 */
public enum ActionKey {
    ADD("ACTION_ADD"),
    REMOVE("ACTION_REMOVE"),
    CREATE("ACTION_CREATE"),
    DELETE("ACTION_DELETE"),
    LIST("ACTION_LIST"),
    SET("ACTION_SET"),
    GET("ACTION_GET"),
    JOIN("ACTION_JOIN"),
    LEAVE("ACTION_LEAVE"),
    DISABLE("ACTION_DISABLE"),
    LOCK("ACTION_LOCK"),
    UNLOCK("ACTION_UNLOCK"),
    LIST_MEMBERS("ACTION_MEMBERS"),
    CLEAR("ACTION_CLEAR"),
    RANDOM("ACTION_RANDOM"),
    PLAY("ACTION_PLAY"),
    SEARCH("ACTION_SEARCH"),
    SKIP("ACTION_SKIP"),
    PAUSE("ACTION_PAUSE"),
    STOP("ACTION_STOP"),
    PREFIX("ACTION_PREFIX"),
    GREETING("ACTION_GREETING"),
    LOG_CHANNEL("ACTION_LOG_CHANNEL"),
    LANGUAGE("ACTION_LANGUAGE"),
    TIMEZONE("ACTION_TIMEZONE"),
    COMMANDS("ACTION_COMMANDS"),
    ALLOW("ACTION_ALLOW"),
    DISALLOW("ACTION_DISALLOW"),
    GUILD("ACTION_GUILD"),
    PAY("ACTION_PAY"),
    UNKNOWN(null);

    private final String translationKey;

    /**
     * Constructor
     *
     * @param translationKey key
     */
    ActionKey(final String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Get the TranslationKey for this action
     *
     * @return TranslationKey if action has one, null otherwise
     */
    @Nullable
    String getTranslationKey() {
        return this.translationKey;
    }
}
