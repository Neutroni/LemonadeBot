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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main class of the bot, initializes database and connects to discord
 *
 * @author Neutroni
 */
public class LemonadeBot {

    public static final String BOT_VERSION = LemonadeBot.class.getPackage().getImplementationVersion();
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Main function
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        LOGGER.info("LemonadeBot starting up, version: " + BOT_VERSION);

        //Get property file location
        final String configLocation;
        if (args.length > 0) {
            configLocation = args[0];
        } else {
            configLocation = "lemonadebot.properties";
        }

        //Load properties
        final Properties properties = new Properties();
        try (final InputStream inputStream = new FileInputStream(configLocation)) {
            properties.load(inputStream);
        } catch (FileNotFoundException ex) {
            LOGGER.fatal("Could not find configuration file, {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
            System.exit(Returnvalue.MISSING_CONFIG.ordinal());
        } catch (IOException ex) {
            LOGGER.fatal("Loading configuration file failed, {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
            System.exit(Returnvalue.CONFIG_READ_ERROR.ordinal());
        }

        //Check that user provided api key
        final String discordKey = properties.getProperty("discord-api-key");
        if (discordKey == null) {
            LOGGER.fatal("No api key provided, quitting");
            System.exit(Returnvalue.MISSING_API_KEY.ordinal());
        }

        //Start loading JDA
        final List<GatewayIntent> intents = List.of(
                GatewayIntent.GUILD_MEMBERS, //User join
                GatewayIntent.GUILD_MESSAGES, //Messages
                GatewayIntent.GUILD_VOICE_STATES //Voice chat
        );
        final JDABuilder jdabuilder = JDABuilder.create(discordKey, intents);
        final List<CacheFlag> cacheFlagsToDisable = List.of(
                CacheFlag.ACTIVITY,
                CacheFlag.EMOTE,
                CacheFlag.CLIENT_STATUS
        );
        jdabuilder.disableCache(cacheFlagsToDisable);
        jdabuilder.setMemberCachePolicy(MemberCachePolicy.ALL);
        try {
            //Start loading JDA
            final JDA jda = jdabuilder.build();

            //Connect to the database
            final DatabaseManager DB = new DatabaseManager(properties, jda);
            LOGGER.debug("Connected to database succefully");

            //Start listening for messages
            jda.addEventListener(new MessageListener(DB));
            jda.addEventListener(new MessageLoggerListener(DB));
            jda.addEventListener(new JoinListener(DB));

            LOGGER.debug("Startup succesfull");
        } catch (SQLException ex) {
            LOGGER.fatal("Failed to connect to database during startup: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            System.exit(Returnvalue.DATABASE_FAILED.ordinal());
        } catch (LoginException ex) {
            LOGGER.fatal("Login failed: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            System.exit(Returnvalue.LOGIN_FAILED.ordinal());
        } catch (NumberFormatException ex) {
            LOGGER.fatal("Loading max messages value from configuration file failed: {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
            System.exit(Returnvalue.CONFIG_READ_ERROR.ordinal());
        }
    }
}
