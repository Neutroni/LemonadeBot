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
package eternal.lemonadebot.translation;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Enum for translation, stores default translation if locale does not have one
 *
 * @author Neutroni
 */
public enum TranslationKey {
    COMMAND_CONFIG("config"),
    COMMAND_COOLDOWN("cooldown"),
    COMMAND_EVENT("event"),
    COMMAND_HELP("help"),
    COMMAND_PERMISSION("permission"),
    COMMAND_REMAINDER("remainder"),
    COMMAND_ROLE("role"),
    COMMAND_TEMPLATE("template"),
    COMMAND_MUSIC("music"),
    DESCRIPTION_CONFIG("Set configuration values used by the bot."),
    DESCRIPTION_COOLDOWN("Set cooldown for commands."),
    DESCRIPTION_EVENT("Manage events that people can join."),
    DESCRIPTION_HELP("Help for bot usage."),
    DESCRIPTION_PERMISSION("Manage permissions required to run commands."),
    DESCRIPTION_REMAINDER("Manage remainders that can be sent out by the bot."),
    DESCRIPTION_ROLE("Command for getting a role from allied guilds."),
    DESCRIPTION_TEMPLATE("Manage custom commands."),
    DESCRIPTION_MUSIC("Play music."),
    DESCRIPTION_CUSTOMCOMMAND("Custom command: %s by %s"),
    ACTION_ADD("add"),
    ACTION_REMOVE("remove"),
    ACTION_CREATE("create"),
    ACTION_DELETE("delete"),
    ACTION_LIST("list"),
    ACTION_SET("set"),
    ACTION_GET("get"),
    ACTION_JOIN("join"),
    ACTION_LEAVE("leave"),
    ACTION_DISABLE("disable"),
    ACTION_MEMBERS("members"),
    ACTION_CLEAR("clear"),
    ACTION_PING("ping"),
    ACTION_RANDOM("random"),
    ACTION_PLAY("pause"),
    ACTION_STOP("stop"),
    ACTION_SKIP("skip"),
    ACTION_PAUSE("pause"),
    ACTION_PREFIX("prefix"),
    ACTION_GREETING("greeting"),
    ACTION_LOG_CHANNEL("logchannel"),
    ACTION_LANGUAGE("language"),
    ACTION_COMMANDS("commands"),
    ACTION_UNKNOWN("unknown"),
    TIME_SECOND("second"),
    TIME_MINUTE("minute"),
    TIME_HOUR("hour"),
    TIME_DAY("day"),
    TIME_WEEK("week"),
    TIME_MONTH("month"),
    TIME_YEAR("year"),
    TIME_SECONDS("seconds"),
    TIME_MINUTES("minutes"),
    TIME_HOURS("hours"),
    TIME_DAYS("days"),
    TIME_WEEKS("weeks"),
    TIME_MONTHS("months"),
    TIME_YEARS("years"),
    DAY_MONDAY("Monday"),
    DAY_TUESDAY("Tuesday"),
    DAY_WEDNESDAY("Wednesday"),
    DAY_THURSDAY("Thursday"),
    DAY_FRIDAY("Friday"),
    DAY_SATURDAY("Saturday"),
    DAY_SUNDAY("Sunday"),
    DAY_DAILY("Daily"),
    SYNTAX_CONFIG("Syntax: config <action> <config> [value]\n"
            + "<action> can be one of the following:\n"
            + " get - get the current value for config\n"
            + " set - set the config to [value]\n"
            + " disable - disable the option if possible\n"
            + " help - Show help for value format.\n"
            + "<config> is the configuration option to edit, one of following:\n"
            + " prefix - The prefix used by commands.\n"
            + " greeting - Text used to greet new members with.\n"
            + " logchannel - Channel used to send log messages to.\n"
            + " language - Language for the bot to reply in.\n"
            + "[value] - Value to set configuration to, valid value formats for settings are\n"
            + " prefix - Any string.\n"
            + " greeting - Template with additional keys {name} and {mention} to add name of the new person or mention them in the message.\n"
            + " logchannel - Mention the channel using #channel\n"
            + " language - Language name as two letter code."),
    SYNTAX_COOLDOWN("Syntax: cooldown <option> [time] [unit] <action>\n"
            + "<option> is one of the following:\n"
            + " get - to get current cooldown for action\n"
            + " set - to set cooldown for action\n"
            + "[time] is the cooldown time in [unit] time units, for example: 2 minutes\n"
            + "<action> is the action to get or set the cooldown time for\n"),
    SYNTAX_EVENT("Syntax: event <action> <name> [description]\n"
            + "<action> can be one of the following:\n"
            + " create - create new event, you will join the event automatically\n"
            + " delete - deletes an event\n"
            + " join - join an event\n"
            + " leave - leave an event\n"
            + " members - list members for event\n"
            + " clear - clears event member list\n"
            + " list - list active events\n"
            + " ping - ping event members\n"
            + " random - pick random member from event\n"
            + "<name> is the name of the event\n"
            + "[description] description for the event"),
    SYNTAX_HELP("Syntax: help [command]\n"
            + " help commands - prints list of commands\n"
            + " help [command] - prints help for command\n"
            + " help without arguments prints this message\n"
            + "[] indicates optional argument, <> nessessary one."),
    SYNTAX_PERMISSION("Syntax: permission <action> [rank] [role] <name>\n"
            + "<action> can be one of the following:\n"
            + " get to get current permission\n"
            + " set to update permission\n"
            + "<name> is the name of permission to update\n"
            + "[rank] rank required for permission can be one of following\n"
            + "%s\n"
            + "[role] is a name of role needed for permission, use 'anyone' to disable role check"),
    SYNTAX_REMAINDER("Syntax: remainder <action> <name> [day] [time] [message]\n"
            + "<action> can be one of the following:\n"
            + " create - create new remainder\n"
            + " delete - delete remainder\n"
            + "<name> name of remainder\n"
            + "<day> day the remainder activates on, use 'any' to create remainder that activates daily\n"
            + "<time> time of the remainder hh:mm\n"
            + "[message] message to be sent at the remainder activation\n"
            + "Remainder will be activated on the channel it was created in"),
    SYNTAX_ROLE("Syntax: role [role]\n"
            + "If no role is provided try to automatically assign role.\n"
            + "Otherwise assign <role> to yourself"),
    SYNTAX_TEMPLATE("Syntax: template <option> [name] [template]\n"
            + "<option> can be one of the following:\n"
            + " create - create new custom command\n"
            + " delete - delete custom command\n"
            + " keys - shows list of keys custom command can contain\n"
            + " list - show list of custom commands\n"
            + "[name] name for action\n"
            + "[template] template for custom command, see below for syntax\n"
            + "Syntax for custom commands:\n"
            + " Text in the template will mostly be shown as is,\n"
            + " but you can use {key} to modify parts of the message.\n"
            + " Valid keys are:\n%s"),
    SYNTAX_MUSIC("Syntax: music <action> [url]\n"
            + "<action> can be either play, skip, stop, pause or list\n"
            + " play - adds song to the song queue or resumes play if paused\n"
            + " skip - skips next song, songs by url, or songs in playlist provided\n"
            + " stop - clears the playlist and stops music playback\n"
            + " list - prints upcoming songs in playlist\n"
            + "[url] is the url of the music to play"),
    SYNTAX_CUSTOMCOMMAND("Custom command with template:\n %s\n"
            + "See \"help command\" for details on custom commands."),
    CONFIG_SET_MISSING_OPTION("Provide the name of the setting and the value to set."),
    CONFIG_MISSING_VALUE("Provide the value to set the setting to."),
    CONFIG_GET_MISSING_OPTION("Provide the name of the setting to get the value for."),
    CONFIG_DISABLE_MISSING_OPTION("Provide the name of the setting to disable."),
    CONFIG_HELP_MISSING_OPTION("Provide the name of the setting to show help for."),
    CONFIG_PREFIX_UPDATE_SUCCESS("Updated prefix succesfully to: "),
    CONFIG_PREFIX_SQL_ERROR("Storing prefix in database failed, prefix might revert to old value after reboot."),
    CONFIG_GREETING_UPDATE_SUCCESS("Updated greeting template succesfully to: "),
    CONFIG_GREETING_SQL_ERROR("Storing greeting in database failed, greeting might revert to old value after reboot."),
    CONFIG_LOG_CHANNEL_MISSING("Mention the channel you want to set as the the log channel."),
    CONFIG_LOG_CHANNEL_UPDATE_SUCCESS("Update log channel succesfully to: "),
    CONFIG_LOG_CHANNEL_SQL_ERROR("Storing logchannel in database failed, logchannel might revert to old value after reboot."),
    CONFIG_LANGUAGE_UPDATE_SUCCESS("Updated language succesfully to: "),
    CONFIG_UNSUPPPRTED_LOCALE("Unsupported language, currently supported languages are: %s"),
    CONFIG_LANGUAGE_SQL_ERROR("Storing language setting in database failed, language might revert to old value after reboot."),
    CONFIG_ERROR_UNKNOWN_SETTING("Unknown configuration option: %s"),
    CONFIG_CURRENT_PREFIX("Current command prefix: %s"),
    CONFIG_CURRENT_GREETING("Current greeting: %s"),
    CONFIG_GREETING_DISABLED_CURRENTLY("Greeting disabled."),
    CONFIG_LOG_CHANNEL_UNKNOWN("Log channel could not be found, it has probably been deleted since setting it."),
    CONFIG_CURRENT_LOG_CHANNEL("Current log channel: %s"),
    CONFIG_LOG_CHANNEL_DISABLED_CURRENTLY("Log channel disabled."),
    CONFIG_CURRENT_LANGUAGE("Current language: %s"),
    CONFIG_DISABLE_PREFIX("Cannot disable the configuration for the command prefix."),
    CONFIG_GREETING_DISABLED("Disabled greeting succesfully"),
    CONFIG_SQL_ERROR_ON_GREETING_DISABLE("Disabling greeting in database failed, greeting might revert to old value after reboot."),
    CONFIG_LOG_CHANNEL_DISABLED("Disabled message log succesfully"),
    CONFIG_SQL_ERROR_ON_LOG_CHANNEL_DISABLE("Disabling logchannel in database failed, logchannel might revert to old value after reboot."),
    CONFIG_LANGUAGE_DISABLE("Cannot disable the configuration for the bot language."),
    COOLDOWN_MISSING_ACTION("Provide name of the action to perform."),
    COOLDOWN_NO_COOLDOWN_SET("No cooldown set for action: "),
    COOLDOWN_CURRENT_COOLDOWN("Cooldown time: %s\nFor action %s"),
    COOLDOWN_MISSING_TIME("Provide the amount of cooldown to set for action."),
    COOLDOWN_UNKNOWN_TIME("Unknown time amount: "),
    COOLDOWN_MISSIGN_UNIT("Provide the unit in which the coold down is to apply a cooldown to command."),
    COOLDOWN_UNKNOWN_UNIT("Unknown time unit: "),
    COOLDOWN_NO_ACTION("Provide the name of the action to set cooldown for."),
    COOLDOWN_UPDATED_SUCCESFULLY("Action cooldown updated succesfully."),
    COOLDOWN_SQL_ERROR_ON_UPDATE("Error updating the cooldown in database, cooldown might revert to old value after reboot."),
    ERROR_COMMAND_NOT_FOUND("Could not find command with input: "),
    ERROR_NO_SUCH_COMMAND("No such command: "),
    ERROR_UNKNOWN_DATE("Unknown date: "),
    ERROR_RECURSION_NOT_PERMITTED("User created commands cannot run user created custom command."),
    ERROR_TEMPLATE_EMPTY("Error: Template produced empty message."),
    ERROR_INSUFFICIENT_PERMISSION("Insufficient permississions to run that command."),
    ERROR_COMMAND_COOLDOWN_TIME("Command on cooldown, time remaining: "),
    ERROR_MISSING_OPERATION("Provide operation to perform, check help for possible operations."),
    ERROR_UNKNOWN_OPERATION("Unknown operation: "),
    ERROR_PERMISSION_DENIED("Permission denied."),
    BOT_VERSION("LemonadeBot version: %s"),
    PREFIX_CURRENT_VALUE("Current command prefix: %s"),
    UNKNOWN_USER("unknown"),
    HEADER_COMMANDS("Commands:"),
    HEADER_EVENTS("Events:"),
    HEADER_PING("Ping!"),
    HEADER_GUILDS("Possible guilds:"),
    HEADER_REMAINDERS("Remainders:"),
    HEADER_EVENT_MEMBERS("Members for the event %s:"),
    RANK_DESCRIPTION_USER("Account without roles."),
    RANK_DESCRIPTION_MEMBER("Account with at least one role."),
    RANK_DESCRIPTION_ADMIN("Account with permission ADMINSTRATOR."),
    RANK_DESCRIPTION_SERVER_OWNER("Leader of the server."),
    RANK_USER("user"),
    RANK_MEMBER("member"),
    RANK_ADMIN("admin"),
    RANK_SERVER_OWNER("leader"),
    HELP_TEMPLATE_DAYS_SINCE("{daysSince <date>} - Days since date specified according to ISO-8601"),
    HELP_TEMPLATE_CHOICE("{choice a|b} - Selects value separated by | randomly"),
    HELP_TEMPLATE_RNG("{rng x,y} - Generate random number between the two inputs."),
    HELP_TEMPLATE_MESSAGE("{message} Use the input as part of the reply"),
    HELP_TEMPLATE_MESSAGE_TEXT("{messageText} - Use the input as part of the reply but remove mentions"),
    HELP_TEMPLATE_MENTIONS("{mentions} - Lists the mentioned users"),
    HELP_TEMPLATE_SENDER("{sender} - The name of the command sender"),
    HELP_TEMPLATE_RANDOM_EVENT_MEMBER("{randomEventMember <eventname>} - Pick random member from event"),
    MESSAGE_UPDATE_HEADER("Message Edited"),
    MESSAGE_LOG_USER("User: "),
    MESSAGE_CONTENT_BEFORE("Before:"),
    MESSAGE_CONTENT_AFTER("After:"),
    MESSAGE_CREATION_TIME("Created: "),
    MESSAGE_DELETE_HEADER("Message Deleted"),
    MESSAGE_LOG_USER_UNKNOWN("User: Unknown"),
    MESSAGE_CONTENT("Content:"),
    EVENT_NO_DESCRIPTION("No description"),
    EVENT_CREATE_MISSING_NAME("Provide name of the event to create."),
    EVENT_ALREADY_EXISTS("Event with that name alredy exists."),
    EVENT_CREATE_JOIN_FAILED("Event created but failed to join the event."),
    EVENT_CREATE_SUCCESS("Event created succesfully."),
    EVENT_SQL_ERROR_ON_CREATE("Error adding event to database, the event might disappear after reboot"),
    EVENT_DELETE_MISSING_NAME("Provide name of the event to delete."),
    EVENT_NOT_FOUND_WITH_NAME("Could not find event with name: %s"),
    EVENT_NO_EVENTS("No events found."),
    EVENT_REMOVE_PERMISSION_DENIED("You do not have permission to remove that event, "
            + "only the event owner and admins can remove events."),
    EVENT_REMOVED_SUCCESFULLY("Event succesfully removed."),
    EVENT_SQL_ERROR_ON_REMOVE("Error removing event from database, the event might reappear after reboot."),
    EVENT_JOIN_MISSING_NAME("Provide name of the event to join."),
    EVENT_JOIN_SUCCESS("Succesfully joined event."),
    EVENT_JOIN_ALREADY_JOINED("You have alredy joined that event."),
    EVENT_SQL_ERROR_ON_JOIN("Database error joining event, the join might be reverted after reboot."),
    EVENT_LEAVE_MISSING_NAME("Provide name of the event to leave."),
    EVENT_LEAVE_SUCCESS("Succesfully left event."),
    EVENT_LEAVE_ALREADY_LEFT("You have not joined that event."),
    EVENT_SQL_ERROR_ON_LEAVE("Database error leaving event, the leave might revert after reboot."),
    EVENT_SHOW_MEMBERS_MISSING_NAME("Provide name of the event to show members for."),
    EVENT_NO_MEMBERS("No one has joined the event yet."),
    EVENT_CLEAR_MISSING_NAME("Provide name of the event to clear."),
    EVENT_CLEAR_PERMISSION_DENIED("Only the owner of the event can clear it."),
    EVENT_CLEAR_SUCCESS("Succesfully cleared the event."),
    EVENT_SQL_ERROR_ON_CLEAR("Error clearing the event in database, clear might be undone after reboot"),
    EVENT_PING_MISSING_NAME("Provide name of the event to ping members for."),
    EVENT_PING_PERMISSION_DENIED("Only the owner of the event can ping event members."),
    EVENT_PICK_RANDOM_MISSING_NAME("Provide name of the event to pick random member from."),
    EVENT_SELECTED_MEMBER("Selected %s from the event."),
    EVENT_COMMAND_LIST_ELEMENT("%s - %s by %s\n"),
    PERMISSION_ROLE_ANYONE("anyone"),
    PERMISSION_GET_MISSING_NAME("Provide name of permission to get curret value for."),
    PERMISSION_NOT_FOUND("No permission with that name currently set."),
    PERMISSION_RANK_MISSING_ROLE("Required rank: %s required role could not be found, defaulting to anyone."),
    PERMISSION_REQUIRED_RANK_ROLE("Required rank: %s Required role: %s"),
    PERMISSION_SET_MISSING_RANK("Provide the rank to set for permission."),
    PERMISSION_SET_MISSING_ROLE("Provide role to set for permission."),
    PERMISSION_SET_MISSING_ACTION("Provide name of permission to update."),
    PERMISSION_UNKNOWN_RANK("Unknown rank: %s\nValid rank names are:\n%s"),
    PERMISSION_ROLE_NOT_FOUND_NAME("Could not find a role with the name: "),
    PERMISSION_SQL_ERROR_ON_SET("Error setting the permission in database, old value might be restored after reboot."),
    REMAINDER_MISSING_NAME("Remainder needs a name."),
    REMAINDER_MISSING_DAY("Remainder needs a day to activate on."),
    REMAINDER_DAY_DAILY("daily"),
    REMAINDER_LIST_ELEMENT_TEMPLATE("%s - %s %s on channel %s by %s\n"),
    REMAINDER_CHANNEL_MISSING("Remainder %s deleted due to missing channel"),
    REMAINDER_USER_MISSING("Remainder %s deleted due to missing user"),
    REMAINDER_MISSING_TIME("Remainder needs a time to activate on."),
    REMAINDER_UNKNOWN_TIME("Unknown time: %s provide time in format hh:mm"),
    REMAINDER_MISSING_MESSAGE("Remainder needs a message to send at scheduled time"),
    REMAINDER_ALREADY_EXISTS("Matching remainder already exists."),
    REMAINDER_CREATE_SUCCESS("Remainder succesfully created.\n First activation at: %s"),
    REMAINDER_SQL_ERROR_ON_CREATE("Error adding remainder to database, remainder might be disappear after reboot."),
    REMAINDER_DELETE_MISSING_NAME("Provide the name of the remainder you want to delete."),
    REMAINDER_NOT_FOUND_NAME("Could not find remainder with name: %s"),
    REMAINDER_DELETE_MISSING_PERMISSION("You do not have permission to remove that remainder, "
            + "only the remainder owner and admins can remove remainders."),
    REMAINDER_NO_REMAINDERS("No remainders found."),
    REMAINDER_DELETE_SUCCESS("Remainder succesfully removed."),
    REMAINDER_SQL_ERROR_ON_DELETE("Error removing remainder from database, remainder might reappear after reboot."),
    REMAINDER_ERROR_UNKNOWN_DAY("Day must be weekday written in full, for example, 'Sunday', or special day 'Any' for daily activation."),
    ROLE_BOT_NO_PERMISSION("It appears I can't assign roles here, if you think this to be a mistake contact server adminstrators."),
    ROLE_ALREADY_HAS_ROLE("You cannot assign role to yourself using this command if you alredy have a role."),
    ROLE_CURRENT_GUILD_NOT_ALLOWED("Can't assign role for current server, check rules if you want member status."),
    ROLE_OTHER_SERVER_MEMBER_NOT_FOUND("Did you leave the server while I wasn't looking? Could not find you on that server."),
    ROLE_OTHER_SEVER_NO_ROLES("You do not have any roles on that server, as such no role was given."),
    ROLE_NO_ROLE_FOR_SERVER("It appears we do not have a role for that server yet, please contact admin if you believe that to be an error."),
    ROLE_ASSING_SUCCESS("Role assigned succesfully."),
    ROLE_ASSIGN_FAILED("Role assignment failed, either I can't assign roles on this server or other error occured."),
    ROLE_DID_NOT_FIND_GUILD("Could not find a guild for role with name: %s\n"),
    ROLE_VALID_ROLE_NAMES("Valid role names are: %s"),
    ROLE_NO_AVAILABLE_ROLES("Current guild does not appear to have roles for any allied guilds you are on."),
    ROLE_GUILD_MISSING_ROLES("Follwoing guilds do not have a role here: %s"),
    ROLE_NO_MUTUAL_GUILDS("You do not appear to be on any guilds other than this one that I can see,"
            + " ask guild leaders if you belive this to be error."),
    ROLE_MEMBER_NOT_FOUND("Could not find a role to assign for you, are you on any other guilds I can see?"),
    ROLE_NO_ROLES_ON_MUTUAL_SERVER("Could not find any guild where you are member, "
            + "found one where you are but do not have any roles, as such no role was assigned."),
    ROLE_NO_ROLE_FOUND("Can not find role for the other server, "
            + "if you belive this to be error contact guild lead."),
    ROLE_AUTOMATIC_ASSIGN_SUCCESS("Succesfully assigned role: %s "
            + "if you believe this is wrong role for you please contact guild lead."),
    ROLE_AUTOMATIC_MULTIPLE_GUILDS("You apper to be on multiple guilds and as such I can't find a role for you, "
            + "please provide the the guild name you want role for in the role command."),
    TEMPLATE_NO_COMMANDS("No custom commands."),
    TEMPLATE_COMMAND_LIST_ELEMENT("%s by %s\n"),
    TEMPLATE_DELETE_MISSING_NAME("Deleting custom command requires a name of the the command to remove."),
    TEMPLATE_CREATE_MISSING_NAME("Creating custom command requires a name for the command."),
    TEMPLATE_CREATE_MISSING_TEMPLATE("Custom command must contain a template string for the response."),
    TEMPLATE_NAME_RESERVED("Custom command name reserved by command."),
    TEMPLATE_CREATE_SUCCESS("Custom command added succesfully."),
    TEMPLATE_ALREADY_EXISTS("Custom command with that name alredy exists."),
    TEMPLATE_SQL_ERROR_ON_CREATE("Adding custom command to database failed, command might disappear after reboot"),
    TEMPLATE_DELETE_NOT_FOUND("No such custom command as %s"),
    TEMPLATE_DELETE_PERMISSION_DENIED("You do not have permission to delete that custom command, "
            + "only the custom command owner and admins can delete custom commands."),
    TEMPLATE_DELETE_SUCCESS("Custom command deleted succesfully."),
    TEMPLATE_SQL_ERROR_ON_DELETE("Deleting custom command from database failed, command might reappear after reboot."),
    MUSIC_ADDED_SONG("Adding to queue: %s"),
    MUSIC_ADDED_PLAYLIST("Added playlist: %s"),
    MUSIC_NOT_FOUND("Nothing found by: %s"),
    MUSIC_LOAD_FAILED("Could not play: %s"),
    MUSIC_TRACK_SKIPPED("Skipped to next track."),
    MUSIC_SKIP_NO_TRACK_TO_SKIP("No currently playing music."),
    MUSIC_TRACK_IN_QUEUE_SKIPPED("Removed from queue %s"),
    MUSIC_SKIP_TRACK_NOT_IN_PLAYLIST("Song not in the playlist."),
    MUSIC_SKIPPED_PLAYLIST("Skipped songs in playlist %s"),
    MUSIC_SKIP_SONGS_NOT_FOUND("No songs to be skipped found."),
    MUSIC_SKIP_SONG("Skipped song %s"),
    MUSIC_PLAYBACK_PAUSED("Playback paused."),
    MUSIC_PLAYBACK_RESUMED("Playback resumed."),
    MUSIC_PLAYBACK_STOPPED("Playback stopped and playlist cleared."),
    MUSIC_SKIP_FAILED("Could not skip: "),
    MUSIC_PLAYLIST_EMPTY("No songs currently in playlist."),
    MUSIC_HELP_ADD_MUSIC("Add music using \"music play <url>\""),
    MUSIC_CURRENTLY_PLAYING("Currently playing:"),
    MUSIC_END_OF_PLAYLIST("No music in playlist."),
    MUSIC_UPCOMING_SONGS("Upcoming songs:"),
    MUSIC_DURATION_TEMPLATE(" %d:%02d:%02d remaining."),
    MUSIC_PLAYLIST_LENGTH("Playlist lenght:");

    private static final Logger LOGGER = LogManager.getLogger();
    private final String defaultText;

    private TranslationKey(String defaultText) {
        this.defaultText = defaultText;
    }

    /**
     * Get the default translation text
     *
     * @param locale Locale to get translation in
     * @return default translation for key
     */
    public String getTranslation(final Locale locale) {
        try {
            final ResourceBundle rb = ResourceBundle.getBundle("Translation", locale);
            return rb.getString(this.name());
        } catch (MissingResourceException ex) {
            LOGGER.warn("Missing translation for key {} in locale {}", this.name(), locale.toString());
            LOGGER.warn(ex.getMessage());
        }
        return this.defaultText;
    }

}