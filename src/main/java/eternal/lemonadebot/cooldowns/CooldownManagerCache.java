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

import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.radixtree.RadixTree;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that manages cooldown for actions
 *
 * @author Neutroni
 */
public class CooldownManagerCache extends CooldownManager {

    private final Map<Long, RadixTree<ActionCooldown>> cooldowns;

    /**
     * Constructor
     *
     * @param db DataSource to get connection from
     */
    public CooldownManagerCache(final DatabaseManager db) {
        super(db);
        this.cooldowns = new ConcurrentHashMap<>();
    }

    @Override
    Collection<ActionCooldown> getCooldowns(final long guildID) throws SQLException {
        final RadixTree<ActionCooldown> guildCooldowns = getCooldownForGuild(guildID);
        return guildCooldowns.getValues();
    }

    @Override
    boolean removeCooldown(final String action, final long guildID) throws SQLException {
        final RadixTree<ActionCooldown> guildCooldowns = this.cooldowns.get(guildID);
        if (guildCooldowns != null) {
            guildCooldowns.remove(action);
        }
        return super.removeCooldown(action, guildID);
    }

    @Override
    boolean setCooldown(final String action, final Duration duration, final long guildID) throws SQLException {
        final RadixTree<ActionCooldown> guildCooldowns = getCooldownForGuild(guildID);
        //Check if cooldown already exisits to preserve activation time
        final Optional<ActionCooldown> cd = guildCooldowns.get(action);
        cd.ifPresentOrElse((ActionCooldown t) -> {
            if (t.getAction().equals(action)) {
                //Old cooldown set for action, update the duration
                t.updateCooldownDuration(duration);
            } else {
                //Found wrong cooldown, add correct one
                guildCooldowns.put(action, new ActionCooldown(action, duration));
            }
        }, () -> {
            //No cooldown set for action, add a new one
            guildCooldowns.put(action, new ActionCooldown(action, duration));
        });

        //Update database
        return super.setCooldown(action, duration, guildID);
    }

    @Override
    Optional<ActionCooldown> getActionCooldown(final String action, final long guildID) throws SQLException {
        final RadixTree<ActionCooldown> guildCooldowns = getCooldownForGuild(guildID);
        return guildCooldowns.get(action);
    }

    @Override
    protected boolean updateActivationTime(final String action, final long guildID) throws SQLException {
        final Optional<ActionCooldown> cd = getActionCooldown(action, guildID);
        //Action does not have a cooldown
        if (cd.isEmpty()) {
            return false;
        }

        //Action has no cooldown
        final ActionCooldown cooldown = cd.get();
        if (cooldown.getDuration().isZero()) {
            return false;
        }

        //Update activationTime
        cooldown.updateActivationTime(Instant.now());

        //Store in database
        return super.updateActivationTime(action, guildID);
    }

    private RadixTree<ActionCooldown> getCooldownForGuild(final long guildID) throws SQLException {
        //Check if cooldowns for a guild are alreayd cached
        final RadixTree<ActionCooldown> guildCooldowns = this.cooldowns.get(guildID);
        if (guildCooldowns != null) {
            //Already in cache, return the cache
            return guildCooldowns;
        }
        //Not in the cache, get cooldowns from database
        final Collection<ActionCooldown> cooldownList = super.getCooldowns(guildID);
        final RadixTree<ActionCooldown> loadedCooldowns = new RadixTree<>(null);
        cooldownList.forEach(cd -> {
            loadedCooldowns.put(cd.getAction(), cd);
        });
        //Add loaded cooldowns to the cache
        this.cooldowns.put(guildID, loadedCooldowns);
        return loadedCooldowns;
    }

}
