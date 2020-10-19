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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import net.dv8tion.jda.api.entities.Member;

/**
 *
 * @author Neutroni
 */
public class InventoryManager {

    private final DataSource dataSource;
    private final long guildID;

    public InventoryManager(final DataSource ds, final long guildID) {
        this.dataSource = ds;
        this.guildID = guildID;
    }

    /**
     * Add or remove items from users inventory
     *
     * @param member member whose inventory to edit
     * @param itemName Name of items which count to change
     * @param change amount which to add or remove items
     * @return true if inventory modified
     * @throws SQLException If database connection failed
     */
    boolean updateCount(final Member member, final String itemName, final long change) throws SQLException {
        final long memberID = member.getIdLong();
        if (change > 0) {
            return addItemsToUser(memberID, itemName, change);
        }
        if (change < 0) {
            return removeItemsFromUser(memberID, itemName, change);
        }
        return false;
    }

    /**
     * Pay item from user to another
     *
     * @param sender item sender
     * @param receiver item receiver
     * @param itemName name of item
     * @param count amount of items
     * @return true if paid succesfully, false if not enough items on sender
     * @throws SQLException If database connection failed, payment will rollback
     */
    boolean payItem(final Member sender, final Member receiver, final String itemName, final long count) throws SQLException {
        try (final Connection connection = this.dataSource.getConnection()) {
            final String transactionBegin = "BEGIN TRANSACTION;";
            try (final Statement st = connection.createStatement()) {
                st.execute(transactionBegin);
            }

            try {
                //Update database
                if (!removeItemsFromUser(sender.getIdLong(), itemName, count)) {
                    //User does not have enough items
                    return false;
                }
                addItemsToUser(receiver.getIdLong(), itemName, count);

                //Commit transaction
                final String transactionCommit = "COMMIT TRANSACTION;";
                try (final Statement st = connection.createStatement()) {
                    st.execute(transactionCommit);
                }
            } catch (SQLException e) {
                //Datbase update failed, rollback the changes.
                final String transactionRollback = "ROLLBACK TRANSACTION;";
                try (final Statement st = connection.createStatement()) {
                    st.execute(transactionRollback);
                }
                //Throw the exception so we do not indicate success accidentally
                throw e;
            }
            return true;
        }
    }

    /**
     * Get view of users inventory
     *
     * @param member Member to get inventory for
     * @return Map of users items, with key as item name and value as item count
     * @throws SQLException if database connection failed
     */
    Map<String, Long> getUserInventory(final Member member) throws SQLException {
        final String query = "SELECT item,count FROM Inventory WHERE guild = ? AND owner = ?;";
        final Map<String, Long> items = new HashMap<>();
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setLong(2, member.getColorRaw());
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String itemName = rs.getString("item");
                    final long itemCount = rs.getLong("count");
                    items.put(itemName, itemCount);
                }
            }
        }
        return Collections.unmodifiableMap(items);
    }

    private boolean addItemsToUser(final long userID, final String item, final long count) throws SQLException {
        final String query = "INSERT INTO Items (guild,owner,item,count) VALUES(?,?,?,?) ON CONFLICT(item) DO UPDATE SET count = count + ?;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, this.guildID);
            ps.setLong(2, userID);
            ps.setString(3, item);
            ps.setLong(4, count);
            ps.setLong(5, count);
            return ps.executeUpdate() > 0;
        }
    }

    private boolean removeItemsFromUser(final long userID, final String item, final long count) throws SQLException {
        final String query = "UPDATE Inventory SET count = count - ? WHERE guild = ? AND owner = ? and item = ? AND count - ? >= 0;";
        try (final Connection connection = this.dataSource.getConnection();
                final PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, count);
            ps.setLong(2, this.guildID);
            ps.setLong(3, userID);
            ps.setString(4, item);
            ps.setLong(5, count);
            return ps.executeUpdate() > 0;
        }
    }

}
