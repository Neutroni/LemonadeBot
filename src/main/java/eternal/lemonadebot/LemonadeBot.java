/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eternal.lemonadebot;

import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author joonas
 */
public class LemonadeBot extends ListenerAdapter {

    private static final Logger LOGGER = LogManager.getLogger(LemonadeBot.class);

    public static void main(String[] args) {

        //Check that user provided api key
        if (args.length < 1) {
            LOGGER.error("No api key provided, quitting");
            System.exit(Returnvalue.MISSING_API_KEY.getValue());
        }
    }
}
