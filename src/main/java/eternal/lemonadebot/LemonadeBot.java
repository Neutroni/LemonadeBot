/*
 * The MIT License
 *
 * Copyright 2019 Neutroni.
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
package eternal.lemonadebot;

import eternal.lemonadebot.database.DatabaseException;
import eternal.lemonadebot.database.DatabaseManager;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main class of the bot, initializes database and connects to discord
 *
 * @author Neutroni
 */
public class LemonadeBot {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Main function
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        //Check that user provided api key
        if (args.length < 1) {
            LOGGER.error("No api key provided, quitting");
            System.exit(Returnvalue.MISSING_API_KEY.getValue());
        }
        try (final DatabaseManager DB = new DatabaseManager()) {
            //If owner id was provided initialize database
            if (args.length == 2) {
                DB.initializeDatabase(args[1]);
            }
            final JDA jda = new JDABuilder(args[0]).build();
            jda.addEventListener(new MessageListener(DB));
        } catch (DatabaseException ex) {
            LOGGER.error("Failed to connect to database");
            LOGGER.warn(ex);
            System.exit(Returnvalue.DATABASE_FAILED.getValue());
        } catch (LoginException ex) {
            LOGGER.error("Login failed");
            LOGGER.warn(ex.getMessage());
            System.exit(Returnvalue.LOGIN_FAILED.getValue());
        }
    }
}
