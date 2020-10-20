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
    ADD(TranslationKey.ACTION_ADD),
    REMOVE(TranslationKey.ACTION_REMOVE),
    CREATE(TranslationKey.ACTION_CREATE),
    DELETE(TranslationKey.ACTION_DELETE),
    LIST(TranslationKey.ACTION_LIST),
    SET(TranslationKey.ACTION_SET),
    GET(TranslationKey.ACTION_GET),
    JOIN(TranslationKey.ACTION_JOIN),
    LEAVE(TranslationKey.ACTION_LEAVE),
    DISABLE(TranslationKey.ACTION_DISABLE),
    LOCK(TranslationKey.ACTION_LOCK),
    UNLOCK(TranslationKey.ACTION_UNLOCK),
    LIST_MEMBERS(TranslationKey.ACTION_MEMBERS),
    CLEAR(TranslationKey.ACTION_CLEAR),
    RANDOM(TranslationKey.ACTION_RANDOM),
    PLAY(TranslationKey.ACTION_PLAY),
    SKIP(TranslationKey.ACTION_SKIP),
    PAUSE(TranslationKey.ACTION_PAUSE),
    STOP(TranslationKey.ACTION_STOP),
    PREFIX(TranslationKey.ACTION_PREFIX),
    GREETING(TranslationKey.ACTION_GREETING),
    LOG_CHANNEL(TranslationKey.ACTION_LOG_CHANNEL),
    LANGUAGE(TranslationKey.ACTION_LANGUAGE),
    COMMANDS(TranslationKey.ACTION_COMMANDS),
    ALLOW(TranslationKey.ACTION_ALLOW),
    DISALLOW(TranslationKey.ACTION_DISALLOW),
    GUILD(TranslationKey.ACTION_GUILD),
    PAY(TranslationKey.ACTION_PAY),
    UNKNOWN(null);

    private final TranslationKey translationKey;

    ActionKey(final TranslationKey translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Get the TranslationKey for this action
     *
     * @return TranslationKey if action has one, null otherwise
     */
    @Nullable
    TranslationKey getTranslationKey() {
        return this.translationKey;
    }
}
