/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eternal.lemonadebot;

/**
 *
 * @author joonas
 */
public enum Returnvalue {
    MISSING_API_KEY(1),
    DATABASE_FAILED(2),
    LOGIN_FAILED(3);
    private final int VALUE;

    private Returnvalue(int value) {
        this.VALUE = value;
    }

    public int getValue() {
        return this.VALUE;
    }
}
