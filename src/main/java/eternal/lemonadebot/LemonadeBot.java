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

import eternal.lemonadebot.database.StorageManager;
import eternal.lemonadebot.keywords.KeywordListener;
import eternal.lemonadebot.messagelogs.LoggerListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
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

    /**
     * Version for the bot
     */
    public static final String BOT_VERSION = LemonadeBot.class.getPackage().getImplementationVersion();
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Main function
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        LOGGER.info("LemonadeBot starting up, version: {}", BOT_VERSION);

        //Get property file location
        final String configLocation;
        if (args.length > 0) {
            configLocation = args[0];
        } else {
            configLocation = "lemonadebot.properties";
        }

        //Load properties
        final Properties properties = new Properties();
        try (final Reader inputReader = new FileReader(configLocation, StandardCharsets.UTF_8)) {
            properties.load(inputReader);
        } catch (FileNotFoundException ex) {
            LOGGER.fatal("Could not find configuration file, {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
            generateDefaultPropertiesFile(configLocation);
            System.exit(ReturnValue.MISSING_CONFIG.ordinal());
        } catch (IOException ex) {
            LOGGER.fatal("Loading configuration file failed, {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
            System.exit(ReturnValue.CONFIG_READ_ERROR.ordinal());
        }

        //Check that user provided api key
        final String discordKey = properties.getProperty("discord-api-key");
        if (discordKey == null) {
            LOGGER.fatal("No api key provided, quitting");
            System.exit(ReturnValue.MISSING_API_KEY.ordinal());
        }

        //Start loading JDA
        final List<GatewayIntent> intents = List.of(
                GatewayIntent.GUILD_MEMBERS, //User join
                GatewayIntent.GUILD_MESSAGES, //Messages
                //GatewayIntent.GUILD_MESSAGE_REACTIONS, //Reactions
                GatewayIntent.GUILD_VOICE_STATES //Voice chat
        );
        final JDABuilder jdabuilder = JDABuilder.create(discordKey, intents);
        final List<CacheFlag> cacheFlagsToDisable = List.of(
                CacheFlag.ACTIVITY,
                CacheFlag.EMOTE,
                CacheFlag.CLIENT_STATUS,
                CacheFlag.ONLINE_STATUS
        );
        jdabuilder.disableCache(cacheFlagsToDisable);
        jdabuilder.setMemberCachePolicy(MemberCachePolicy.ALL);
        jdabuilder.setEventPool(Executors.newCachedThreadPool(), true);
        try {
            //Start loading JDA
            final JDA jda = jdabuilder.build();

            //Connect to the database
            final StorageManager storageManager = new StorageManager(properties);
            LOGGER.debug("Connected to database successfully");

            //Start listening for messages
            jda.addEventListener(new JoinListener(storageManager));
            jda.addEventListener(new CommandListener(storageManager));
            jda.addEventListener(new LoggerListener(storageManager));
            jda.addEventListener(new KeywordListener(storageManager));
            jda.addEventListener(new ShutdownListener(storageManager));

            //Initialize connected guilds
            jda.awaitReady();
            storageManager.initialize(jda.getGuilds());

            LOGGER.debug("Startup successful");
        } catch (SQLException ex) {
            LOGGER.fatal("Failed to connect to database during startup: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            System.exit(ReturnValue.DATABASE_FAILED.ordinal());
        } catch (LoginException ex) {
            LOGGER.fatal("Login failed: {}", ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            System.exit(ReturnValue.LOGIN_FAILED.ordinal());
        } catch (NumberFormatException ex) {
            LOGGER.fatal("Loading max messages value from configuration file failed: {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
            System.exit(ReturnValue.CONFIG_READ_ERROR.ordinal());
        } catch (InterruptedException ex) {
            LOGGER.fatal("Waiting for JDA to load was interrupted: {}", ex.getMessage());
            LOGGER.trace("Stack trace: ", ex);
            System.exit(ReturnValue.LOADING_INTERRUPTED.ordinal());
        }
    }

    /**
     * If configuration file could not be found attempt to write it
     *
     * @param configLocation location of configuration file
     */
    private static void generateDefaultPropertiesFile(final String configLocation) {
        LOGGER.debug("Attempting to write .properties file to: {}", configLocation);
        final Properties properties = new Properties();
        properties.setProperty("discord-api-key", "<discord api key here>");
        properties.setProperty("database-location", "database.db");
        properties.setProperty("max-messages", "1000");
        try (final Writer f = new FileWriter(configLocation, StandardCharsets.UTF_8)) {
            properties.store(f, "Configuration file for LemonadeBot");
            LOGGER.debug("Configuration file successfully created.");
        } catch (IOException e) {
            LOGGER.error("Properties file location is not writable and does not contain properties file. {}", e.getMessage());
            LOGGER.trace("Stack trace:", e);
        }

    }
}
