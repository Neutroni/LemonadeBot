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
package eternal.lemonadebot.database.cache;

import eternal.lemonadebot.database.CooldownManager;
import eternal.lemonadebot.dataobjects.ActionCooldown;
import eternal.lemonadebot.radixtree.RadixTree;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Class that manages cooldown for actions
 *
 * @author Neutroni
 */
public class CooldownCache extends CooldownManager {

    private boolean cooldownsLoaded = false;
    private final RadixTree<ActionCooldown> cooldowns;

    /**
     * Constructor
     *
     * @param ds DataSource to get connection from
     * @param guildID id of the guild to store cooldowns for
     */
    CooldownCache(DataSource ds, long guildID) {
        super(ds, guildID);
        this.cooldowns = new RadixTree<>(new ActionCooldown("", Duration.ZERO, Instant.EPOCH));
    }

    @Override
    public Collection<ActionCooldown> getCooldowns() throws SQLException {
        if (cooldownsLoaded) {
            return this.cooldowns.getValues();
        }
        final Collection<ActionCooldown> cooldownList = super.getCooldowns();
        for (final ActionCooldown cd : cooldownList) {
            this.cooldowns.put(cd.getAction(), cd);
        }
        this.cooldownsLoaded = true;
        return cooldownList;
    }

    @Override
    public boolean removeCooldown(String action) throws SQLException {
        this.cooldowns.remove(action);
        return super.removeCooldown(action);
    }

    @Override
    public boolean setCooldown(String action, Duration duration) throws SQLException {
        final ActionCooldown cd = this.cooldowns.get(action);
        if (cd.getAction().equals(action)) {
            //Old cooldown set for action, update the duration
            cd.updateCooldownDuration(duration);
        } else {
            //No cooldown set for action, add cooldown
            this.cooldowns.put(action, new ActionCooldown(action, duration, Instant.EPOCH));
        }

        //Update database
        return super.setCooldown(action, duration);
    }

    @Override
    public Optional<ActionCooldown> getActionCooldown(String action) throws SQLException{
        final ActionCooldown cd = this.cooldowns.get(action);
        if (cooldownsLoaded) {
            return Optional.of(cd);
        }
        if (cd.getDuration().isZero()) {
            final Optional<ActionCooldown> optCooldown = super.getActionCooldown(action);
            optCooldown.ifPresent((ActionCooldown t) -> {
                this.cooldowns.put(t.getAction(), t);
            });
            return optCooldown;
        }
        return Optional.of(cd);
    }

    @Override
    protected boolean updateActivationTime(String action) throws SQLException {
        final Optional<ActionCooldown> cd = getActionCooldown(action);
        //Action does not have a cooldown
        if (cd.isEmpty()) {
            return false;
        }

        //Action has no cooldown
        final ActionCooldown cooldown = cd.get();
        if(cooldown.getDuration().isZero()){
            return false;
        }
        
        //Update activationTime
        cooldown.updateActivationTime(Instant.now());

        //Store in database
        return super.updateActivationTime(action);
    }

}
