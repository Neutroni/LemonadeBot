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
package eternal.lemonadebot.database;

import eternal.lemonadebot.customcommands.CommandBuilder;
import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.messages.CommandPermission;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Caches the database and controls the database on disc
 *
 * @author Neutroni
 */
public class DatabaseManager implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    //SQLite database
    private final String DB_LOCATION = "database.db";
    private final SQLiteManager DB;

    // Caching database data
    private final String ownerID;
    private volatile Optional<String> commandPrefix;
    private volatile Optional<String> ruleChannelID;
    private final List<String> channels;
    private final List<String> admins;

    //Custom commands
    private final CommandBuilder commandBuilder;
    private volatile CommandPermission permissionManageCommands = CommandPermission.MEMBER;
    private volatile CommandPermission permissionUseCommands = CommandPermission.USER;
    private final List<CustomCommand> customCommands = Collections.synchronizedList(new ArrayList<>());

    /**
     * Creates a connection to a database
     *
     * @param ownerID Id of the bot owner, if present used to initialize
     * database
     * @throws DatabaseException if database connection failed
     */
    public DatabaseManager(Optional<String> ownerID) throws DatabaseException {
        try {
            this.DB = new SQLiteManager(DB_LOCATION);

            //Check if DB needs to be initialized
            if (ownerID.isPresent()) {
                this.DB.initialize(ownerID.get());
            }

            //Load owner
            final Optional<String> owner = DB.loadSetting(ConfigKey.OWNER_ID.name());
            if (owner.isEmpty()) {
                throw new DatabaseException("No owner id defined, has the database been initialized?");
            }
            this.ownerID = owner.get();

            //Load settings
            this.commandPrefix = DB.loadSetting(ConfigKey.COMMAND_PREFIX.name());
            this.ruleChannelID = DB.loadSetting(ConfigKey.RULE_CHANNEL.name());

            //Load
            loadManagePerm();
            loadUsePerm();

            //Load listen channels and admins
            this.channels = DB.loadChannels();
            this.admins = DB.loadAdmins();

            //Load commands
            this.commandBuilder = new CommandBuilder(this);
            final List<String[]> commands = DB.loadCommands();
            for (String[] arr : commands) {
                if (arr.length != 3) {
                    throw new DatabaseException("Database provided command in wrong format");
                }
                final CustomCommand command = this.commandBuilder.build(arr[0], arr[1], arr[2]);
                this.customCommands.add(command);
            }
        } catch (SQLException ex) {
            LOGGER.error("Failure creating DatabaseManager", ex);
            throw new DatabaseException(ex);
        }
    }

    /**
     * Gets the actionmanager used to build custom commands
     *
     * @return CommandBuilder
     */
    public CommandBuilder getCommandBuilder() {
        return this.commandBuilder;
    }

    /**
     * Loads the permission to use custom commands from DB
     *
     * @throws SQLException if databaseconnection failed
     */
    private void loadManagePerm() throws SQLException {
        final Optional<String> opt = DB.loadSetting(ConfigKey.CUSTOM_COMMAND_MANAGE.name());
        if (opt.isEmpty()) {
            LOGGER.info("No permission for managing custom commands defined in DB.");
            LOGGER.info("Saving default value: " + this.permissionManageCommands.getDescription());
            DB.updateSetting(ConfigKey.CUSTOM_COMMAND_MANAGE.name(), this.permissionManageCommands.name());
            return;
        }
        final String managePerm = opt.get();
        try {
            this.permissionManageCommands = CommandPermission.valueOf(managePerm);
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Malformed command permission for custom commands: " + managePerm);
            LOGGER.info("Using default value: " + this.permissionManageCommands.name());
        }
    }

    /**
     * Loads the permission to use custom commands from DB
     *
     * @throws SQLException if database connection failed
     */
    private void loadUsePerm() throws SQLException {
        final Optional<String> opt = DB.loadSetting(ConfigKey.CUSTOM_COMMAND_USE.name());
        if (opt.isEmpty()) {
            LOGGER.info("No permission for using custom commands defined in DB.");
            LOGGER.info("Saving default value: " + this.permissionUseCommands.getDescription());
            DB.updateSetting(ConfigKey.CUSTOM_COMMAND_USE.name(), this.permissionUseCommands.name());
            return;
        }
        final String usePerm = opt.get();
        try {
            this.permissionUseCommands = CommandPermission.valueOf(opt.get());
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Malformed command permission for custom commands: " + usePerm);
            LOGGER.info("Using default value: " + this.permissionUseCommands.name());
        }
    }

    /**
     * Close the connection on shutdown
     *
     * @throws DatabaseException if databaseconnection failed
     */
    @Override
    public void close() throws DatabaseException {
        try {
            this.DB.close();
        } catch (SQLException ex) {
            LOGGER.error("Closing database connection failed", ex);
            throw new DatabaseException(ex);
        }
    }

    /**
     * Initializes the database structure
     *
     * @param ownerID Owner of the bot
     * @throws DatabaseException if database connection failed
     */
    public void initializeDatabase(String ownerID) throws DatabaseException {
        try {
            DB.initialize(ownerID);
        } catch (SQLException ex) {
            LOGGER.error("Initializing database failed", ex);
            throw new DatabaseException(ex);
        }
    }

    /**
     * Gets the prefix bot uses to identify commands
     *
     * @return command prefix
     */
    public Optional<String> getCommandPrefix() {
        return this.commandPrefix;
    }

    /**
     * Sets the command prefix
     *
     * @param prefix new prefix to store
     * @throws DatabaseException if Database connection failed
     */
    public void setCommandPrefix(String prefix) throws DatabaseException {
        this.commandPrefix = Optional.ofNullable(prefix);
        try {
            DB.updateSetting(ConfigKey.COMMAND_PREFIX.name(), prefix);
        } catch (SQLException ex) {
            LOGGER.error("Failed to store command prefix", ex);
            throw new DatabaseException(ex);
        }
    }

    /**
     * Adds new custom command
     *
     * @param command command to add
     * @return true if add was succesfull, false if command was alredy added
     * @throws DatabaseException if database connection failed
     */
    public boolean addCommand(CustomCommand command) throws DatabaseException {
        //Check if we alredy know this command
        boolean cached = this.customCommands.contains(command);
        //If command is new add to list of commands
        if (!cached) {
            cached = this.customCommands.add(command);
        }
        try {
            if (!DB.hasCommand(command.getCommand())) {
                DB.addCommand(command.getCommand(), command.getAction(), command.getOwner());
            }
        } catch (SQLException ex) {
            LOGGER.error("Adding command to database failed", ex);
            throw new DatabaseException(ex);
        }
        return cached;
    }

    /**
     * Removes custom command
     *
     * @param command command to remove
     * @return true if remove was succesfull, false if command was alredy
     * removed
     * @throws DatabaseException if database connection fails
     */
    public boolean removeCommand(CustomCommand command) throws DatabaseException {
        boolean removed = this.customCommands.remove(command);
        try {
            removed = DB.removeCommand(command.getCommand()) > 0;
        } catch (SQLException ex) {
            LOGGER.error("Failed to remove command from database", ex);
            throw new DatabaseException(ex);
        }
        return removed;
    }

    /**
     * Gets the current custom commands
     *
     * @return list of custom commands
     */
    public List<CustomCommand> getCommands() {
        return Collections.unmodifiableList(this.customCommands);
    }

    /**
     * Gets the ID of the channel we redirect new joins to
     *
     * @return Id of the rule channel
     */
    public Optional<String> getRuleChannelID() {
        return this.ruleChannelID;
    }

    /**
     * Start listening on channel
     *
     * @param channel channel to add
     * @return true if add was succesfull, false if channel was alredy added
     * @throws DatabaseException if database connection failed
     */
    public boolean addChannel(TextChannel channel) throws DatabaseException {
        final String id = channel.getId();
        //Store in cache
        boolean cached = this.channels.contains(id);
        if (!cached) {
            cached = this.channels.add(id);
        }
        //Add channel to database
        try {
            if (!DB.hasChannel(id)) {
                DB.addChannel(id);
            }
        } catch (SQLException ex) {
            LOGGER.error("Adding new channel failed", ex);
            throw new DatabaseException(ex);
        }
        return cached;
    }

    /**
     * Stop listening on a channel
     *
     * @param id channel to remove
     * @return true if channel was removed, false if channel was alredy removed
     * @throws DatabaseException if removing channel from database failed
     */
    public boolean removeChannel(String id) throws DatabaseException {
        boolean removed = this.channels.remove(id);
        try {
            removed = DB.removeChannel(id) > 0;
        } catch (SQLException ex) {
            LOGGER.error("Removing channel failed", ex);
            throw new DatabaseException(ex);
        }
        return removed;
    }

    /**
     * Check if we listen on channel
     *
     * @param channel Channel to check
     * @return true if we listen on channel, false otherwise
     */
    public boolean watchingChannel(TextChannel channel) {
        return this.channels.contains(channel.getId());
    }

    /**
     * Get the list of channels we listen on
     *
     * @return List of channels ids
     */
    public List<String> getChannelIds() {
        return Collections.unmodifiableList(this.channels);
    }

    /**
     * Adds user as admin
     *
     * @param user User too add
     * @return true if add was succefull, false if user was alredy admin
     * @throws DatabaseException if database connection failed
     */
    public boolean addAdmin(User user) throws DatabaseException {
        final String id = user.getId();
        boolean added = this.admins.contains(id);
        if (!added) {
            added = this.admins.add(id);
        }
        //Add admin to database
        try {
            if (!DB.hasAdmin(id)) {
                this.DB.addAdmin(id);
            }
        } catch (SQLException ex) {
            LOGGER.error("Adding admin to database failed", ex);
            throw new DatabaseException(ex);
        }
        return added;
    }

    /**
     * Revoke admin permissions from user
     *
     * @param id User to revoke permission from
     * @return true if admin status was succesfully revoked, false if user was
     * not admin
     * @throws DatabaseException if database connection fails
     */
    public boolean removeAdmin(String id) throws DatabaseException {

        boolean removed = this.admins.remove(id);
        //Going to database even if user is not admin,
        //because database might be in different state if remove failed before
        try {
            removed = DB.removeAdmin(id) > 0;
        } catch (SQLException ex) {
            LOGGER.error("Removing admin from database failed", ex);
            throw new DatabaseException(ex);
        }
        return removed;
    }

    /**
     * Check if user can use admin functionalities
     *
     * @param user User to check
     * @return is the user an admin
     */
    public boolean isAdmin(User user) {
        return this.admins.contains(user.getId());
    }

    /**
     * Gets the list of admins
     *
     * @return List of admin ids
     */
    public List<String> getAdminIds() {
        return Collections.unmodifiableList(this.admins);
    }

    /**
     * Check if user is the owner of this bot
     *
     * @param user User to check
     * @return true if user is the owner
     */
    public boolean isOwner(User user) {
        return this.ownerID.equals(user.getId());
    }

    /**
     * What permission is required to use custom commands
     *
     * @return CommandPermission needed to use commands
     */
    public CommandPermission getCommandUsePermission() {
        return this.permissionUseCommands;
    }

    /**
     * What permission is required to manage custom commands
     *
     * @return CommandPermission needed to edit commands
     */
    public CommandPermission getCommandManagePermission() {
        return this.permissionManageCommands;
    }
}
