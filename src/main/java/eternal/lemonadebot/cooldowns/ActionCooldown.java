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
package eternal.lemonadebot.cooldowns;

import java.time.Duration;
import java.time.Instant;
import java.util.ResourceBundle;

/**
 * Class that stores cooldown set for action
 *
 * @author Neutroni
 */
public class ActionCooldown {

    private final String command;
    private volatile Duration cooldownDuration;
    private volatile Instant activationTime;

    /**
     * Constructor
     *
     * @param action Action this cooldown is for
     * @param cooldownDuration Duration the cooldown is
     */
    ActionCooldown(final String action, final Duration cooldownDuration) {
        this.command = action;
        this.cooldownDuration = cooldownDuration;
        this.activationTime = Instant.EPOCH;
    }

    /**
     * Constructor
     *
     * @param action Action this cooldown is for
     * @param cooldownDurationSeconds Duration the cooldown is
     * @param activationTimeSeconds Last time of activation for action
     */
    ActionCooldown(final String action, final long cooldownDurationSeconds, final long activationTimeSeconds) {
        this.command = action;
        this.cooldownDuration = Duration.ofSeconds(cooldownDurationSeconds);
        this.activationTime = Instant.ofEpochSecond(activationTimeSeconds);
    }

    /**
     * Update the cooldown set for action
     *
     * @param duration duration to set
     */
    void updateCooldownDuration(final Duration duration) {
        this.cooldownDuration = duration;
    }

    /**
     * Update the time command was last activated
     *
     * @param activation time to set
     */
    void updateActivationTime(final Instant activation) {
        this.activationTime = activation;
    }

    /**
     * Get the start of action that this cooldown matches
     *
     * @return action start for this cooldown
     */
    String getAction() {
        return this.command;
    }

    /**
     * Get the duration set for the cooldown
     *
     * @return Cooldown total duration
     */
    Duration getDuration() {
        return this.cooldownDuration;
    }

    /**
     * Get the time remaining on cooldown.
     *
     * @return Duration
     */
    Duration getRemainingDuration() {
        final Instant now = Instant.now();
        return Duration.between(this.activationTime.plus(this.cooldownDuration), now);
    }

    /**
     * Format the actioncooldown for usage in a list.
     *
     * @param locale Locale to format to
     * @return String description for cooldown.
     */
    String toListElement(final ResourceBundle locale) {
        final String template = locale.getString("COOLDOWN_LIST_ELEMENT");
        return String.format(template, this.command, CooldownManager.formatDuration(this.cooldownDuration, locale));
    }
}
