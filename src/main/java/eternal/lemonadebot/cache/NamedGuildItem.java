/*
 * The MIT License
 *
 * Copyright 2021 Neutroni.
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
package eternal.lemonadebot.cache;

import java.util.Objects;

/**
 *
 * @author Neutroni
 */
public class NamedGuildItem {

    private final long guildID;
    private final String itemName;

    /**
     * Constructor
     *
     * @param guildID ID of the guild the item is from
     * @param itemName Name of the item
     */
    public NamedGuildItem(final long guildID, final String itemName) {
        this.guildID = guildID;
        this.itemName = itemName;
    }

    /**
     * Gettter for guild id
     *
     * @return guildID
     */
    public long getGuildID() {
        return this.guildID;
    }

    /**
     * Getter for item name
     *
     * @return name of the item
     */
    public String getItemName() {
        return this.itemName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.guildID, this.itemName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final NamedGuildItem other = (NamedGuildItem) obj;
        if (this.guildID != other.guildID) {
            return false;
        }

        return Objects.equals(this.itemName, other.itemName);
    }

}
