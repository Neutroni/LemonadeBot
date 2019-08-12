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

import eternal.lemonadebot.customcommands.CustomCommand;
import eternal.lemonadebot.messages.CommandPermission;
import eternal.lemonadebot.stores.CommandStore;
import eternal.lemonadebot.stores.DataStore;
import eternal.lemonadebot.stores.Event;
import eternal.lemonadebot.stores.EventStore;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Member;
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
    private final String DB_LOCATION;
    private final SQLiteManager DB;

    // Caching database data
    private final String ownerID;
    private volatile Optional<String> commandPrefix;
    private volatile Optional<String> ruleChannelID;
    private volatile CommandPermission permissionManageCommands = CommandPermission.MEMBER;
    private volatile CommandPermission permissionUseCommands = CommandPermission.USER;

    //Stores
    private final EventStore eventStore;
    private final DataStore<String> adminStore;
    private final DataStore<String> channelStore;
    private final CommandStore commandStore;

    /**
     * Creates a connection to a database
     *
     * @param ownerID Id of the bot owner, if present used to initialize
     * database
     * @param databaseLocation location for the database
     * @throws DatabaseException if database connection failed
     */
    public DatabaseManager(Optional<String> ownerID, Optional<String> databaseLocation) throws DatabaseException {
        try {
            //Create database connection
            this.DB_LOCATION = databaseLocation.orElse("database.db");
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

            //Load permissions
            loadManagePerm();
            loadUsePerm();

            //Load admins
            this.adminStore = new DataStore<>();
            DB.loadAdmins(adminStore);

            //Load listening channels
            this.channelStore = new DataStore<>();
            DB.loadChannels(channelStore);

            //Load commands
            this.commandStore = new CommandStore(this);
            DB.loadCommands(this.commandStore);

            //Load events
            this.eventStore = new EventStore();
            DB.loadEvents(this.eventStore);

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
    public CommandStore getCommandBuilder() {
        return this.commandStore;
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
            LOGGER.error("Closing database connection failed");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
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
            LOGGER.error("Initializing database failed");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
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
            LOGGER.error("Failed to store command prefix");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
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
        boolean added = this.commandStore.add(command);
        try {
            synchronized (this.commandStore) {
                if (!DB.hasCommand(command.getCommand())) {
                    DB.addCommand(command.getCommand(), command.getAction(), command.getOwner());
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Adding command to database failed");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            throw new DatabaseException(ex);
        }
        return added;
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
        boolean removed = this.commandStore.remove(command);
        try {
            synchronized (this.commandStore) {
                removed = DB.removeCommand(command.getCommand()) > 0;
            }
        } catch (SQLException ex) {
            LOGGER.error("Failed to remove command from database");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            throw new DatabaseException(ex);
        }
        return removed;
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
        boolean added = this.channelStore.add(id);

        //Add channel to database
        try {
            synchronized (this.channelStore) {
                if (!DB.hasChannel(id)) {
                    DB.addChannel(id);
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Adding new channel failed");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            throw new DatabaseException(ex);
        }
        return added;
    }

    /**
     * Stop listening on a channel
     *
     * @param id channel to remove
     * @return true if channel was removed, false if channel was alredy removed
     * @throws DatabaseException if removing channel from database failed
     */
    public boolean removeChannel(String id) throws DatabaseException {
        boolean removed = this.channelStore.remove(id);
        try {
            synchronized (this.channelStore) {
                removed = DB.removeChannel(id) > 0;
            }
        } catch (SQLException ex) {
            LOGGER.error("Removing channel failed");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
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
        return this.channelStore.hasItem(channel.getId());
    }

    /**
     * Get the list of channels we listen on
     *
     * @return List of channels ids
     */
    public List<String> getChannels() {
        return this.channelStore.getItems();
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
        boolean added = this.adminStore.add(id);

        //Add admin to database
        try {
            synchronized (this.adminStore) {
                if (!DB.hasAdmin(id)) {
                    this.DB.addAdmin(id);
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Adding admin to database failed");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
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
        boolean removed = this.adminStore.remove(id);
        //Going to database even if user is not admin,
        //because database might be in different state if remove failed before
        try {
            synchronized (this.adminStore) {
                removed = DB.removeAdmin(id) > 0;
            }
        } catch (SQLException ex) {
            LOGGER.error("Removing admin from database failed");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
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
        return this.adminStore.hasItem(user.getId());
    }

    /**
     * Gets array of admin ids
     *
     * @return Array of admin ids
     */
    public List<String> getAdminIds() {
        return this.adminStore.getItems();
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

    /**
     * Add event to database
     *
     * @param event event to add
     * @return true if event exists
     * @throws DatabaseException if database connection failed
     */
    public boolean addEvent(Event event) throws DatabaseException {
        boolean added = this.eventStore.add(event);

        //Add event to database
        try {
            if (!DB.hasEvent(event.getName())) {
                this.DB.addEvent(event.getName(), event.getOwner());
            }
        } catch (SQLException ex) {
            LOGGER.error("Adding event to database failed");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            throw new DatabaseException(ex);
        }
        return added;
    }

    /**
     * Removes event from database
     *
     * @param event evet to remove
     * @return true if remove
     * @throws DatabaseException if database connection failed
     */
    public boolean removeEvent(Event event) throws DatabaseException {
        boolean removed = this.eventStore.remove(event);
        //Going to database even if user is not admin,
        //because database might be in different state if remove failed before
        try {
            removed = DB.removeEvent(event.getName()) > 0;
        } catch (SQLException ex) {
            LOGGER.error("Removing event from database failed");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            throw new DatabaseException(ex);
        }
        return removed;
    }

    /**
     * Join event
     *
     * @param member member to add to event
     * @param event event to join
     * @return true if join was succesfull, false otherwise
     * @throws DatabaseException if database connection failed
     */
    public boolean joinEvent(Member member, Event event) throws DatabaseException {
        boolean joined = event.join(member.getId());
        try {
            synchronized (this.eventStore) {
                if (!DB.hasAttended(event.getName(), member.getId())) {
                    DB.joinEvent(event.getName(), member.getId());
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Joining event failed");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            throw new DatabaseException(ex);
        }
        return joined;
    }

    /**
     * Leave event
     *
     * @param member member to remove from event
     * @param event event to add member to
     * @return true if left event succesfully
     * @throws DatabaseException if leaving failed
     */
    public boolean leaveEvent(Member member, Event event) throws DatabaseException {
        boolean left = event.leave(member.getId());
        try {
            synchronized (this.eventStore) {
                if (DB.hasAttended(event.getName(), member.getId())) {
                    left = DB.leaveEvent(event.getName(), member.getId()) > 0;
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Leaving event failed");
            LOGGER.warn(ex.getMessage());
            LOGGER.trace("Stack trace:", ex);
            throw new DatabaseException(ex);
        }
        return left;
    }

    /**
     * Get eventbuilder
     *
     * @return EventStore
     */
    public EventStore getEventStore() {
        return this.eventStore;
    }

    /**
     * Clears all members from event
     *
     * @param event event to remove all members from
     * @throws DatabaseException if database connection failed
     */
    public void clearEvent(Event event) throws DatabaseException {
        event.clear();
        synchronized (this.eventStore) {
            try {
                DB.clearEvent(event.getName());
            } catch (SQLException ex) {
                LOGGER.error("Clearing event failed");
                LOGGER.warn(ex.getMessage());
                LOGGER.trace("Stack trace:", ex);
                throw new DatabaseException(ex);
            }
        }

    }
}
