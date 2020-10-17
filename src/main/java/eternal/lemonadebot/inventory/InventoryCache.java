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
package eternal.lemonadebot.inventory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Member;

/**
 *
 * @author Neutroni
 */
public class InventoryCache extends InventoryManager {

    //Map of UserID to map of item name to count of items
    private final Map<Long, Map<String, Long>> inventory = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param ds DataSource to pass to InventoryManager
     * @param guildID guild to store items for
     */
    public InventoryCache(DataSource ds, long guildID) {
        super(ds, guildID);
    }

    @Override
    boolean updateCount(Member member, String itemName, long change) throws SQLException {
        //Update database
        if (!super.updateCount(member, itemName, change)) {
            //User does not have enough items
            return false;
        }

        //Synchronize on users items so no concurrent modification
        final Map<String, Long> userItems = getInventory(member);
        synchronized (userItems) {
            final long itemCount = userItems.getOrDefault(itemName, 0l);
            final long newCount = itemCount + change;
            if (newCount < 0) {
                //User does not have enough items
                return false;
            }
            if (newCount == 0) {
                //User does not have any more of the item, remove from items
                userItems.remove(itemName);
            } else {
                //Update users item count
                userItems.put(itemName, newCount);
            }
            return true;
        }
    }

    @Override
    boolean payItem(Member sender, Member receiver, String itemName, long count) throws SQLException {
        //Update database
        if (!super.payItem(sender, receiver, itemName, count)) {
            //User does not have enough items
            return false;
        }

        //Update cache
        final Map<String, Long> userItems = getInventory(sender);
        final Map<String, Long> receiverItems = getInventory(receiver);

        //Update sender items
        synchronized (userItems) {
            final long itemCount = userItems.getOrDefault(itemName, 0l);
            final long newCount = itemCount - count;
            if (newCount < 0) {
                //User does not have enough items
                return false;
            }
            if (newCount == 0) {
                //User does not have any more of the item, remove from items
                userItems.remove(itemName);
            } else {
                //Update users item count
                userItems.put(itemName, newCount);
            }
        }

        //Update receiver items
        synchronized (receiverItems) {
            final long itemCount = receiverItems.getOrDefault(itemName, 0l);
            final long newCount = itemCount + count;
            receiverItems.put(itemName, newCount);
        }
        return true;
    }

    @Override
    Map<String, Long> getUserInventory(Member member) throws SQLException {
        return Collections.unmodifiableMap(getInventory(member));
    }

    /**
     * Get modifiable map of users inventory
     *
     * @param member member to get inventory for
     * @return Map containing users inventory
     * @throws SQLException if loading items for user from database failed
     */
    private Map<String, Long> getInventory(Member member) throws SQLException {
        final long memberId = member.getIdLong();
        final Map<String, Long> inv = this.inventory.get(memberId);
        if (inv == null) {
            final Map<String, Long> userInv = super.getUserInventory(member);
            this.inventory.put(memberId, new ConcurrentHashMap<>(userInv));
            return userInv;
        }
        return inv;
    }

}
