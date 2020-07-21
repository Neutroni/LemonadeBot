/*
 * The MIT License
 *
 * Copyright 2020 joonas.
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
package eternal.lemonadebot.permissions;

/**
 *
 * @author Neutroni
 */
public class ActionPermission {

    private final String action;
    private final MemberRank rank;

    /**
     * Constructor
     *
     * @param action Action string
     * @param rank MemberRank required to perform the action
     */
    public ActionPermission(String action, MemberRank rank) {
        this.action = action;
        this.rank = rank;
    }

    /**
     * Get the action string
     *
     * @return String of the action stored
     */
    public String getAction() {
        return this.action;
    }

    /**
     * Get the MemberRank that is required to perform the action
     *
     * @return Rank required
     */
    public MemberRank getRank() {
        return this.rank;
    }

}
