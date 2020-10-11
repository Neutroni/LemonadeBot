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
package eternal.lemonadebot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class InventoryManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DataSource dataSource;
    private final long guildID;
    //Map of UserID to map of item name to count of items
    private final Map<Long, Map<String, Long>> inventory = new ConcurrentHashMap<>();

    InventoryManager(DataSource ds, long guildID) {
        this.dataSource = ds;
        this.guildID = guildID;
        loadInventory();
    }

    public boolean addItem(Member member, String itemName, long count) throws SQLException {
        final Map<String, Long> userItems = this.inventory.computeIfAbsent(member.getIdLong(), (Long t) -> {
            //If user has no inventory add a empty one
            return new ConcurrentHashMap<>();
        });

        //Synchronize on users items so no concurrent modification
        synchronized (userItems) {
            final long itemCount = userItems.getOrDefault(itemName, 0l);
            final long newCount = itemCount + count;
            if (newCount < 0) {
                //User does not have enough items
                return false;
            }
            if (newCount == 0) {
                //User does not have any more of the item, remove from items
                userItems.remove(itemName);
                deleteItemFromUser(member.getIdLong(), itemName);
            } else {
                //Update users item count
                userItems.put(itemName, newCount);
                setItemCountForUser(member.getIdLong(), itemName, newCount);
            }
            return true;
        }
    }

    /**
     * Get view of users inventory
     *
     * @param member Member to get inventory for
     * @return Map of users items, with key as item name and value as count of
     * items
     */
    public Map<String, Long> getUserInventory(Member member) {
        return Collections.unmodifiableMap(this.inventory.computeIfAbsent(member.getIdLong(), (Long t) -> {
            return new ConcurrentHashMap<>();
        }));
    }

    private boolean deleteItemFromUser(long userID, String item) throws SQLException {
        final String query = "DELETE FROM Inventory WHERE guild = ? AND owner = ? AND item = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setLong(2, userID);
            ps.setString(3, item);
            return ps.executeUpdate() > 0;
        }
    }

    private boolean setItemCountForUser(long userID, String item, long count) throws SQLException {
        final String query = "INSERT OR  REPLACE INTO Inventory(guild,owner,item,count) VALUES(?,?,?,?);";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setLong(2, userID);
            ps.setString(3, item);
            ps.setLong(4, count);
            return ps.executeUpdate() > 0;
        }
    }

    private void loadInventory() {
        final String query = "SELECT owner,item,count FROM Inventory WHERE guild = ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final long itemOwner = rs.getLong("owner");
                    final String itemName = rs.getString("item");
                    final long itemCount = rs.getLong("count");
                    this.inventory.computeIfAbsent(itemOwner, (Long userID) -> {
                        return new ConcurrentHashMap<>();
                    }).put(itemName, itemCount);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Loading items from database failed: {}", e.getMessage());
            LOGGER.trace("Stack trace:", e);
        }
    }

}
