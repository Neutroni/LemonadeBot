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
package eternal.lemonadebot;

import eternal.lemonadebot.database.DatabaseManager;
import java.sql.SQLException;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
        LOGGER.debug("Bot starting up");

        try {
            //Parse command line arguments
            final Options options = new Options();
            options.addRequiredOption("k", "key", true, "Discord api key");
            options.addOption("h", "help", false, "Prints this message");
            options.addOption("d", "database", true, "Database location");
            options.addOption("o", "owner", true, "Id of the bot owner");

            final HelpFormatter formatter = new HelpFormatter();
            final CommandLineParser parser = new DefaultParser();
            final CommandLine cmd = parser.parse(options, args);

            //Check if we should print help
            if (cmd.hasOption("h")) {
                formatter.printHelp("bot", options);
                System.exit(Returnvalue.SUCCESS.ordinal());
            }

            //Check that user provided api key
            if (!cmd.hasOption("k")) {
                LOGGER.fatal("No api key provided, quitting");
                System.exit(Returnvalue.MISSING_API_KEY.ordinal());
            }

            //Get database location
            final String databaseLocation;
            if (cmd.hasOption("d")) {
                databaseLocation = cmd.getOptionValue("d");
            } else {
                databaseLocation = "database.db";
            }

            //Check if database should be initialized
            if (cmd.hasOption("o")) {
                LOGGER.debug("Initializing database");
                final String ownerID = cmd.getOptionValue("o");
                DatabaseManager.initialize(databaseLocation, ownerID);
            }

            //Start loading JDA
            final JDA jda = new JDABuilder(cmd.getOptionValue("k")).build();

            //Connect to the database
            final DatabaseManager DB = new DatabaseManager(databaseLocation, jda);
            LOGGER.debug("Connected to database succefully");

            //Start listening for messages
            jda.addEventListener(new MessageListener(DB));
            LOGGER.debug("Startup succesfull");
        } catch (SQLException ex) {
            LOGGER.fatal("Failed to connect to database during startup");
            LOGGER.trace("Stack trace:", ex);
            System.exit(Returnvalue.DATABASE_FAILED.ordinal());
        } catch (LoginException ex) {
            LOGGER.fatal("Login failed");
            LOGGER.trace("Stack trace:", ex);
            System.exit(Returnvalue.LOGIN_FAILED.ordinal());
        } catch (ParseException ex) {
            LOGGER.fatal("Command line argument parsing failed");
            LOGGER.trace("Stack trace:", ex);
        } catch (InterruptedException ex) {
            LOGGER.fatal("JDA loading interrupted");
            LOGGER.trace("Stack trace:", ex);
            System.exit(Returnvalue.INTERRUPTED.ordinal());
        }
    }
}
