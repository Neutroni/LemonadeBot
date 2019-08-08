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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.regex.Matcher;

/**
 *
 * @author Neutroni
 */
public class SimpleActions {

    private final List<Function<String, String>> actions = new ArrayList<>();
    private final Random rng = new Random();

    /**
     * Constructor, initializes actions
     */
    public SimpleActions() {
        actions.add((String t) -> {
            final ActionEnum ae = ActionEnum.COIN;
            if (!t.contains(ae.getKey())) {
                return t;
            }
            final Matcher m = ae.getPattern().matcher(t);
            final StringBuilder sb = new StringBuilder();
            final String[] COIN_SIDES = new String[]{"Heads", "Tails"};
            while (m.find()) {
                final String side = COIN_SIDES[rng.nextInt(2)];
                m.appendReplacement(sb, side);
            }
            m.appendTail(sb);
            return sb.toString();
        });
        actions.add((String t) -> {
            final ActionEnum ae = ActionEnum.DICE;
            if (!t.contains(ae.getKey())) {
                return t;
            }
            final Matcher m = ae.getPattern().matcher(t);
            final StringBuilder sb = new StringBuilder();
            while (m.find()) {
                final int roll = rng.nextInt(6) + 1;
                m.appendReplacement(sb, "" + roll);
            }
            m.appendTail(sb);
            return sb.toString();
        });
        actions.add((String t) -> {
            final ActionEnum ae = ActionEnum.D20;
            if (!t.contains(ae.getKey())) {
                return t;
            }
            final Matcher m = ae.getPattern().matcher(t);
            final StringBuilder sb = new StringBuilder();
            while (m.find()) {
                final int roll = rng.nextInt(20) + 1;
                m.appendReplacement(sb, "" + roll);
            }
            m.appendTail(sb);
            return sb.toString();
        });
    }

    /**
     * Gets a random generator
     *
     * @return Random
     */
    public Random getRandom() {
        return this.rng;
    }

    /**
     * Get list of all simpleactions
     *
     * @return List of actions
     */
    public List<Function<String, String>> getActions() {
        return Collections.unmodifiableList(this.actions);
    }

}
