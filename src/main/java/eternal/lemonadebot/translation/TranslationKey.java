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
    COMMAND_REMINDER("reminder"),
    COMMAND_ROLE("role"),
    COMMAND_TEMPLATE("template"),
    COMMAND_MUSIC("music"),
    COMMAND_KEYWORD("keyword"),
    COMMAND_INVENTORY("inventory"),
    DESCRIPTION_CONFIG("Set configuration values used by the bot."),
    DESCRIPTION_COOLDOWN("Set cooldown for commands."),
    DESCRIPTION_EVENT("Manage events that people can join."),
    DESCRIPTION_HELP("Help for bot usage."),
    DESCRIPTION_PERMISSION("Manage permissions required to run commands."),
    DESCRIPTION_REMINDER("Manage reminders that can be sent out by the bot."),
    DESCRIPTION_ROLE("Command for getting a role from allied guilds."),
    DESCRIPTION_TEMPLATE("Manage custom commands."),
    DESCRIPTION_MUSIC("Play music."),
    DESCRIPTION_CUSTOMCOMMAND("User created custom command"),
    DESCRIPTION_KEYWORD("Manage keywords that trigger actions when seen in a message."),
    DESCRIPTION_INVENTORY("Command used to manage inventory."),
    ACTION_ADD("add"),
    ACTION_REMOVE("remove"),
    ACTION_CREATE("create"),
    ACTION_DELETE("delete"),
    ACTION_LIST("list"),
    ACTION_SET("set"),
    ACTION_GET("get"),
    ACTION_JOIN("join"),
    ACTION_LEAVE("leave"),
    ACTION_LOCK("lock"),
    ACTION_UNLOCK("unlock"),
    ACTION_DISABLE("disable"),
    ACTION_MEMBERS("members"),
    ACTION_CLEAR("clear"),
    ACTION_RANDOM("random"),
    ACTION_PLAY("play"),
    ACTION_STOP("stop"),
    ACTION_SKIP("skip"),
    ACTION_PAUSE("pause"),
    ACTION_PREFIX("prefix"),
    ACTION_GREETING("greeting"),
    ACTION_LOG_CHANNEL("logchannel"),
    ACTION_LANGUAGE("language"),
    ACTION_TIMEZONE("timezone"),
    ACTION_COMMANDS("commands"),
    ACTION_ALLOW("allow"),
    ACTION_DISALLOW("disallow"),
    ACTION_GUILD("guild"),
    ACTION_PAY("pay"),
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
            + "[value] Value to set configuration to, valid value formats for settings are\n"
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
            + " lock - lock event such that no one can join event\n"
            + " unlock - unlock locked event\n"
            + "<name> is the name of the event\n"
            + "[description] description for the event"),
    SYNTAX_HELP("Syntax: help [command]\n"
            + " help commands - prints list of commands\n"
            + " help [command] - prints help for command\n"
            + " help without arguments prints this message\n"
            + "[] indicates optional argument, <> required one."),
    SYNTAX_PERMISSION("Syntax: permission <action> [rank] [role] <name>\n"
            + "<action> can be one of the following:\n"
            + " get to get current permission\n"
            + " set to update permission\n"
            + "[rank] rank required for permission can be one of following\n"
            + "%s\n"
            + "[role] is a name of role needed for permission, use 'anyone' to disable role check,\n"
            + "if rolename contains space use double quotes around role name.\n"
            + "if rolename contains double quote use \\ before the quote to escape it.\n"
            + "if rolename contains \\ use \\\\ to include it in the role name.\n"
            + "<name> is the name of permission to update."),
    SYNTAX_REMINDER("Syntax: reminder <action> <name> [time] [day] [month] [day of week] [message]\n"
            + "<action> can be one of the following:\n"
            + " create - create new reminder\n"
            + " delete - delete reminder\n"
            + "<name> name of reminder\n"
            + "<time> time of the reminder hh:mm\n"
            + "<day> day the reminder activates on, 1-31 or *\n"
            + "<month> month the reminder activates on 1-12 or *\n"
            + "<day of week] day of week reminder activates on, either name of day written in full or *\n"
            + "[message] message to be sent at the reminder activation\n"
            + "Reminder will be activated on the channel it was created in"),
    SYNTAX_ROLE("Syntax: role <action> [name for role] [description]\n"
            + "<action> can be one of the following:\n"
            + " guild - Get role for another guild you are also on that is allied with current guild.\n"
            + " get - get role if role has been marked as obtainable.\n"
            + " remove - remove role from yourself.\n"
            + " allow - allow role to be assigned using this command.\n"
            + " disallow - disallow role from being assigned with this command, default.\n"
            + "[name for role] name of role you want to obtain if action is guild and no role is provided bot will try to automatically assign role.\n"
            + "if rolename contains space use double quotes around role name.\n"
            + "if rolename contains double quote use \\ before the quote to escape it.\n"
            + "if rolename contains \\ use \\\\ to include it in the role name.\n"
            + "[description] When allowing role to be assigned a description can be added for role."),
    SYNTAX_TEMPLATE("Syntax: template <option> [name] [template]\n"
            + "<option> can be one of the following:\n"
            + " create - create new custom command\n"
            + " delete - delete custom command\n"
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
    SYNTAX_KEYWORD("Syntax: keyword <option> [name] [pattern] [template]\n"
            + "<option> can be one of the following:\n"
            + " create - create new keyword action\n"
            + " delete - delete keyword action\n"
            + " list - show list of defined keywords\n"
            + "[name] name for keyword, used to edit keywords.\n"
            + "[pattern] pattern that activates the keyword, a java regular expression.\n"
            + "[template] template for the response to keyword"),
    SYNTAX_INVENTORY("Syntax: inventory <action> [item] [amount] [user] [type]\n"
            + "<action> can be one of the following:\n"
            + " list - list users inventory contents\n"
            + " pay - give item from your inventory to another user\n"
            + " add - add item to users inventory\n"
            + "[item] The name of the item to add or give\n"
            + "[amount] The amount of items to add or give\n"
            + "[user] Name of user who to list inventory for, or add or give items to\n"
            + "[type] Defines how [user] is interpreted, can be one of following:\n"
            + " user - Name of user, default\n"
            + " role - Name of role\n"
            + "If name of item or user/role contains space surround the name with quotes\n"
            + "If name contains quote escape it with backslash, escape backslashes with another backshlash."),
    SYNTAX_CUSTOMCOMMAND("Template based custom command with template:\n %s\n"
            + "See \"help template\" for details on custom commands."),
    CONFIG_SET_MISSING_OPTION("Provide the name of the setting and the value to set."),
    CONFIG_MISSING_VALUE("Provide the value to set the setting to."),
    CONFIG_GET_MISSING_OPTION("Provide the name of the setting to get the value for."),
    CONFIG_DISABLE_MISSING_OPTION("Provide the name of the setting to disable."),
    CONFIG_PREFIX_UPDATE_SUCCESS("Updated prefix successfully to: "),
    CONFIG_PREFIX_SQL_ERROR("Storing prefix in database failed, prefix might revert to old value after reboot."),
    CONFIG_GREETING_UPDATE_SUCCESS("Updated greeting template successfully to: "),
    CONFIG_GREETING_SQL_ERROR("Storing greeting in database failed, greeting might revert to old value after reboot."),
    CONFIG_LOG_CHANNEL_MISSING("Mention the channel you want to set as the the log channel."),
    CONFIG_LOG_CHANNEL_UPDATE_SUCCESS("Update log channel successfully to: "),
    CONFIG_LOG_CHANNEL_SQL_ERROR("Storing logchannel in database failed, logchannel might revert to old value after reboot."),
    CONFIG_LANGUAGE_UPDATE_SUCCESS("Updated language successfully to: "),
    CONFIG_UNSUPPORTED_LOCALE("Unsupported language, currently supported languages are: %s"),
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
    CONFIG_GREETING_DISABLED("Disabled greeting successfully"),
    CONFIG_SQL_ERROR_ON_GREETING_DISABLE("Disabling greeting in database failed, greeting might revert to old value after reboot."),
    CONFIG_LOG_CHANNEL_DISABLED("Disabled message log successfully"),
    CONFIG_SQL_ERROR_ON_LOG_CHANNEL_DISABLE("Disabling logchannel in database failed, logchannel might revert to old value after reboot."),
    CONFIG_LANGUAGE_DISABLE("Cannot disable the configuration for the bot language."),
    CONFIG_TIMEZONE_ZONE_NOT_FOUND("Could not find a timezone with the input, unknown timezone."),
    CONFIG_TIMEZONE_ZONE_MALFORMED("Not a valid format for timezone, valid timezones are either of format UTC+0, GMT+0 or Europe/London."),
    CONFIG_TIMEZONE_SQL_ERROR("Database error updating timezone, timezone might rever to old value after reboot."),
    CONFIG_TIMEZONE_UPDATE_SUCCESS("Timezone updated succesfully."),
    CONFIG_CURRENT_TIMEZONE("Current timezone: %s"), 
    CONFIG_TIMEZONE_DISABLE("Disabling time zone is not possible."),
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
    COOLDOWN_DISABLE_SUCCESS("Cooldown disabled succesfully."),
    COOLDOWN_SQL_ERROR_ON_DISABLE("Error disable the cooldown in database, cooldown might be restored on reboot."),
    COOLDOWN_LIST_ELEMENT("Action: %s Cooldown: %s\n"),
    COOLDOWN_NO_COOLDOWNS("No cooldowns defined."),
    COOLDOWN_SQL_ERROR_ON_RETRIEVE("Database error retrieving cooldown, can not show cooldown details."),
    COOLDOWN_SQL_ERROR_ON_LOADING("Database error retrieving coolodwns, can not show list of cooldowns."),
    ERROR_COMMAND_NOT_FOUND("Could not find command with input: "),
    ERROR_NO_SUCH_COMMAND("No such command: "),
    ERROR_UNKNOWN_DATE("Unknown date: "),
    ERROR_RECURSION_NOT_PERMITTED("User created commands cannot run user created custom command."),
    ERROR_TEMPLATE_EMPTY("Error: Template produced empty message."),
    ERROR_INSUFFICIENT_PERMISSION("Insufficient permissions to run that command."),
    ERROR_COMMAND_COOLDOWN_TIME("Command on cooldown, time remaining: "),
    ERROR_MISSING_OPERATION("Provide operation to perform, check help for possible operations."),
    ERROR_UNKNOWN_OPERATION("Unknown operation: "),
    ERROR_PERMISSION_DENIED("Permission denied."),
    BOT_VERSION("LemonadeBot version: %s"),
    PREFIX_CURRENT_VALUE("Current command prefix: %s"),
    UNKNOWN_USER("unknown"),
    HEADER_COMMANDS("Commands:"),
    HEADER_EVENTS("Events:"),
    HEADER_REMINDERS("Reminders:"),
    HEADER_KEYWORDS("Keywords:"),
    HEADER_COOLDOWNS("Cooldowns:"),
    HEADER_EVENT_MEMBERS("Members for the event %s:"),
    HEADER_REQUIRED_PERMISSION("Permission required:"),
    HEADER_ALLOWED_ROLES("Available roles:"),
    HEADER_ACTION("Action:"),
    HEADER_PERMISSIONS("Permissions:"),
    RANK_DESCRIPTION_USER("Account without roles."),
    RANK_DESCRIPTION_MEMBER("Account with at least one role."),
    RANK_DESCRIPTION_ADMIN("Account with permission ADMINISTRATOR."),
    RANK_DESCRIPTION_SERVER_OWNER("Leader of the server."),
    RANK_USER("User"),
    RANK_MEMBER("Member"),
    RANK_ADMIN("Admin"),
    RANK_SERVER_OWNER("Leader"),
    HELP_TEMPLATE_DAYS_SINCE("{daysSince <date>} - Days since date specified according to ISO-8601"),
    HELP_TEMPLATE_CHOICE("{choice a|b} - Selects value separated by | randomly"),
    HELP_TEMPLATE_RNG("{rng x,y} - Generate random number between the two inputs."),
    HELP_TEMPLATE_MESSAGE("{message} Use the input as part of the reply"),
    HELP_TEMPLATE_MESSAGE_TEXT("{messageText} - Use the input as part of the reply but remove mentions"),
    HELP_TEMPLATE_MENTIONS("{mentions} - Lists the mentioned users"),
    HELP_TEMPLATE_SENDER("{sender} - The name of the command sender"),
    HELP_TEMPLATE_RANDOM_EVENT_MEMBER("{randomEventMember <eventname>} - Pick random member from event"),
    HELP_TEMPLATE_ARGUMENT("{argument split,n} - Part of the input, split message into n elements and return n:th element"),
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
    EVENT_ALREADY_EXISTS("Event with that name already exists."),
    EVENT_CREATE_JOIN_FAILED("Event created but failed to join the event."),
    EVENT_CREATE_SUCCESS("Event created successfully."),
    EVENT_SQL_ERROR_ON_CREATE("Error adding event to database, the event might disappear after reboot"),
    EVENT_DELETE_MISSING_NAME("Provide name of the event to delete."),
    EVENT_NOT_FOUND_WITH_NAME("Could not find event with name: %s"),
    EVENT_NO_EVENTS("No events found."),
    EVENT_REMOVE_PERMISSION_DENIED("You do not have permission to remove that event, "
            + "only the event owner and admins can remove events."),
    EVENT_REMOVED_SUCCESFULLY("Event successfully removed."),
    EVENT_SQL_ERROR_ON_REMOVE("Error removing event from database, the event might reappear after reboot."),
    EVENT_JOIN_MISSING_NAME("Provide name of the event to join."),
    EVENT_JOIN_SUCCESS("Successfully joined event."),
    EVENT_JOIN_ALREADY_JOINED("You have already joined that event."),
    EVENT_SQL_ERROR_ON_JOIN("Database error joining event, the join might be reverted after reboot."),
    EVENT_LEAVE_MISSING_NAME("Provide name of the event to leave."),
    EVENT_LEAVE_SUCCESS("Successfully left event."),
    EVENT_LEAVE_ALREADY_LEFT("You have not joined that event."),
    EVENT_SQL_ERROR_ON_LEAVE("Database error leaving event, the leave might revert after reboot."),
    EVENT_SHOW_MEMBERS_MISSING_NAME("Provide name of the event to show members for."),
    EVENT_NO_MEMBERS("No one has joined the event yet."),
    EVENT_CLEAR_MISSING_NAME("Provide name of the event to clear."),
    EVENT_CLEAR_PERMISSION_DENIED("Only the owner of the event can clear it."),
    EVENT_CLEAR_SUCCESS("Successfully cleared the event."),
    EVENT_SQL_ERROR_ON_CLEAR("Error clearing the event in database, clear might be undone after reboot"),
    EVENT_PICK_RANDOM_MISSING_NAME("Provide name of the event to pick random member from."),
    EVENT_SELECTED_MEMBER("Selected %s from the event."),
    EVENT_LOCK_MISSING_NAME("Locking event requires the name of the event to lock."),
    EVENT_LOCK_PERMISSION_DENIED("You do not have permission to lock the event, only event owner and admins can lock events."),
    EVENT_LOCKED_SUCCESFULLY("Event locked successfully, no one can join the event and current event members cannot leave."),
    EVENT_SQL_ERROR_ON_LOCK("Error locking the event in database, event might become unlocked on reboot."),
    EVENT_UNLOCK_MISSING_NAME("Unlocking event required the name of the event to unlock."),
    EVENT_UNLOCK_PERMISSION_DENIED("You do not have permission to unlock the event, only event owner and admins can unlock events."),
    EVENT_UNLOCKED_SUCCESFULLY("Event unlocked successfully, people can join and leave the event again."),
    EVENT_ALREADY_LOCKED("Event already locked."),
    EVENT_ALREADY_UNLOCKED("Event already unlocked."),
    EVENT_SQL_ERROR_ON_UNLOCK("Unlocking event in database failed, event might become locked again after reboot."),
    EVENT_JOIN_LOCKED("Cannot join the event, event is locked."),
    EVENT_LEAVE_LOCKED("Cannot leave the event, event is locked."),
    EVENT_COMMAND_LIST_ELEMENT("%s - %s by %s\n"),
    EVENT_SQL_ERROR_ON_FINDING_EVENT("Error retrieving event from database, finding event failed."),
    EVENT_SQL_ERROR_LOADING_MEMBERS("Error retrieving members for event from database, loading members failed."),
    EVENT_SQL_ERROR_LOADING_EVENTS("Error retrieving events from database, loading events failed."),
    PERMISSION_ROLE_ANYONE("anyone"),
    PERMISSION_GET_MISSING_NAME("Provide name of permission to get current value for."),
    PERMISSION_NO_COMMAND("Could not find a command for that action."),
    PERMISSION_NO_PERMISSION("No permission set for action."),
    PERMISSION_NO_PERMISSIONS("No command has permissions set."),
    PERMISSION_DEFAULTING_TO("Using default permissions."),
    PERMISSION_LIST_ELEMENT("%s - Rank: %s Role: %s\n"),
    PERMISSION_RANK_MISSING_ROLE("Action: %s requires rank: %s required role could not be found, defaulting to anyone."),
    PERMISSION_REQUIRED_RANK_ROLE("Required rank: %s Required role: %s"),
    PERMISSION_SET_MISSING_RANK("Provide the rank to set for permission."),
    PERMISSION_SET_MISSING_ROLE("Provide role to set for permission."),
    PERMISSION_SET_MISSING_ACTION("Provide name of permission to update."),
    PERMISSION_UNKNOWN_RANK("Unknown rank: %s\nValid rank names are:\n%s"),
    PERMISSION_ROLE_NOT_FOUND_NAME("Could not find a role with the name: "),
    PERMISSION_UPDATE_SUCCESS("Permission updated successfully."),
    PERMISSION_SQL_ERROR_ON_SET("Error setting the permission in database, old value might be restored after reboot."),
    PERMISSION_SQL_ERROR_ON_LOAD("Database error retrieving permissions, can not show list of permissions."),
    PERMISSION_SQL_ERROR_ON_FIND("Database error finding permission, can not show details for permission."),
    REMINDER_MISSING_NAME("Reminder needs a name."),
    REMINDER_MISSING_DAY("Reminder needs a day to activate on, use * to denote any day."),
    REMINDER_INVALID_DATE("Day out of valid range, such date would never occur."),
    REMINDER_DAY_OF_MONTH_NOT_NUMBER("Reminder day of month must either be a number in range 1 to 31 or * to denote any day."),
    REMINDER_DAY_OF_MONTH_OUT_OF_RANGE("Day must be in range of 1 to 31 use * to denote any day."),
    REMINDER_MISSING_MONTH("Reminder needs a month to activate on, either a number in range 1 to 12 or * denote any month."),
    REMINDER_MONTH_NOT_NUMBER("Month must either a number in range 1 to 12 or * to denote any month."),
    REMINDER_MONTH_OUT_OF_RANGE("Month out of valid range, must either be a number in range 1 to 12 or * to denote any month."),
    REMINDER_MISSING_DAY_OF_WEEK("Remainder needs a day of week to activate, either name of day written in full or * to denote any day."),
    REMINDER_LIST_ELEMENT_TEMPLATE("%s - Template: %s\n Activates at: %s on channel %s by %s\n"),
    REMINDER_CHANNEL_MISSING("Reminder %s deleted due to missing channel"),
    REMINDER_USER_MISSING("Reminder %s deleted due to missing user"),
    REMINDER_MISSING_TIME("Reminder needs a time to activate on."),
    REMINDER_TIME_FORMAT("HH:mm"),
    REMINDER_UNKNOWN_TIME("Unknown time: %s provide time in format hh:mm"),
    REMINDER_MISSING_MESSAGE("Reminder needs a message to send at scheduled time"),
    REMINDER_ALREADY_EXISTS("Matching reminder already exists."),
    REMINDER_CREATE_SUCCESS("Reminder created successfully."),
    REMINDER_SQL_ERROR_ON_CREATE("Error adding reminder to database, reminder might be disappear after reboot."),
    REMINDER_DELETE_MISSING_NAME("Provide the name of the reminder you want to delete."),
    REMINDER_NOT_FOUND_NAME("Could not find reminder with name: %s"),
    REMINDER_DELETE_MISSING_PERMISSION("You do not have permission to remove that reminder, "
            + "only the reminder owner and admins can remove reminders."),
    REMINDER_NO_REMINDERS("No reminders found."),
    REMINDER_DELETE_SUCCESS("Reminder successfully removed."),
    REMINDER_SQL_ERROR_ON_DELETE("Error removing reminder from database, reminder might reappear after reboot."),
    REMINDER_ERROR_UNKNOWN_DAY("Day must be weekday written in full, for example, 'Sunday', or special day 'Any' for daily activation."),
    ROLE_BOT_NO_PERMISSION("It appears I can't assign roles here, if you think this to be a mistake contact server administrators."),
    ROLE_ALREADY_HAS_ROLE("You cannot assign role to yourself using this command if you already have a role."),
    ROLE_CURRENT_GUILD_NOT_ALLOWED("Can't assign role for current server, check rules if you want member status."),
    ROLE_OTHER_SERVER_MEMBER_NOT_FOUND("Did you leave the server while I wasn't looking? Could not find you on that server."),
    ROLE_OTHER_SEVER_NO_ROLES("You do not have any roles on that server, as such no role was given."),
    ROLE_NO_ROLE_FOR_SERVER("It appears we do not have a role for that server yet, please contact admin if you believe that to be an error."),
    ROLE_ASSING_SUCCESS("Role assigned successfully."),
    ROLE_ASSIGN_FAILED("Role assignment failed, either I can't assign roles on this server or other error occurred."),
    ROLE_DID_NOT_FIND_GUILD("Could not find a guild for role with name: %s\n"),
    ROLE_VALID_ROLE_NAMES("Valid role names are: %s"),
    ROLE_NO_AVAILABLE_ROLES("Current guild does not appear to have roles for any allied guilds you are on."),
    ROLE_GUILD_MISSING_ROLES("Following guilds do not have a role here: %s"),
    ROLE_NO_MUTUAL_GUILDS("You do not appear to be on any guilds other than this one that I can see,"
            + " ask guild leaders if you believe this to be error."),
    ROLE_NO_ROLES_ON_MUTUAL_SERVER("Could not find any guild where you are member, "
            + "found one where you are but do not have any roles, as such no role was assigned."),
    ROLE_NO_ROLE_FOUND("Can not find role for the other server, "
            + "if you belive this to be error contact guild lead."),
    ROLE_AUTOMATIC_ASSIGN_SUCCESS("Successfully assigned role: %s "
            + "if you believe this is wrong role for you please contact guild lead."),
    ROLE_AUTOMATIC_MULTIPLE_GUILDS("You apper to be on multiple guilds and as such I can't find a role for you, "
            + "please provide the the guild name you want role for in the role command."),
    ROLE_USER_NO_PERMISSION("Permission denied. You do not have permission to manage roles."),
    ROLE_ALLOW_MISSING_ROLE_NAME("Adding a role to list of allowed roles required the name of the role to add."),
    ROLE_NO_ROLE_WITH_NAME("Could not find a role with name: %s"),
    ROLE_DISALLOW_MISSING_ROLE_NAME("Disallowing role requires the name of the role to disallow."),
    ROLE_REMOVED_SUCCESFULLY("Role removed successfully."),
    ROLE_MULTIPLE_ROLES_WITH_NAME("Found multiple roles with name: %s at the moment bot does not have functionality to select correct one."),
    ROLE_NO_MEMBERS("No member has the role: %s"),
    ROLE_BOT_PRIVILIGE_MISSING("It appears I do not have permission to view all the members of the guild, as such finding random member with role failed."),
    ROLE_SELECTED_USER("Selected %s"),
    ROLE_RANDOM_MISSING_ROLE_NAME("Selecting a random member with role requires the name of the role."),
    ROLE_ROLE_NOT_ALLOWED("Role %s is not currently selected as a role bot can assign, see 'roles list' for list of allowed roles."),
    ROLE_DISALLOW_SUCCESS("Role successfully removed from the list of allowed roles."),
    ROLE_DISALLOW_ALREADY_DISALLOWED("Role was already not allowed."),
    ROLE_SQL_ERROR_ON_DISALLOW("Removing the role from the list of allowed roles in the database failed, role might become allowed again after reboot."),
    ROLE_ALLOW_SUCCESS("Role added to list of roles bot can assign successfully."),
    ROLE_ALLOW_ALREADY_ALLOWED("Role was already allowed."),
    ROLE_SQL_ERROR_ON_ALLOW("Adding role to list of allowed roles in database failed, bot might not allow assigning the role after reboot."),
    ROLE_GET_MISSING_ROLE_NAME("Obtaining a role requires the name of the role to obtain."),
    ROLE_ASSIGNED_SUCCESFULLY("Assigned role: %s successfully"),
    ROLE_REMOVE_MISSING_ROLE_NAME("Removing a role requires the name of the role for bot to remove from you."),
    ROLE_COMMAND_LIST_ELEMENT("%s - %s\n"),
    ROLE_MISSING("Role could not be found."),
    ROLE_NO_DESCRIPTION("No description for role."),
    ROLE_SQL_ERROR_ON_LIST("Database error retrieving the list of allowed roles, can not show role list."),
    ROLE_SQL_ERROR_ON_CHECK("Database error while checking if role is allowed. Can not modify your roles."),
    TEMPLATE_NO_COMMANDS("No custom commands."),
    TEMPLATE_COMMAND_LIST_ELEMENT("%s by %s\n"),
    TEMPLATE_DELETE_MISSING_NAME("Deleting custom command requires a name of the the command to remove."),
    TEMPLATE_CREATE_MISSING_NAME("Creating custom command requires a name for the command."),
    TEMPLATE_CREATE_MISSING_TEMPLATE("Custom command must contain a template string for the response."),
    TEMPLATE_NAME_RESERVED("Custom command name reserved by command."),
    TEMPLATE_CREATE_SUCCESS("Custom command added successfully."),
    TEMPLATE_ALREADY_EXISTS("Custom command with that name already exists."),
    TEMPLATE_SQL_ERROR_ON_CREATE("Adding custom command to database failed, command might disappear after reboot"),
    TEMPLATE_DELETE_NOT_FOUND("No such custom command as %s"),
    TEMPLATE_DELETE_PERMISSION_DENIED("You do not have permission to delete that custom command, "
            + "only the custom command owner and admins can delete custom commands."),
    TEMPLATE_DELETE_SUCCESS("Custom command deleted successfully."),
    TEMPLATE_SQL_ERROR_ON_DELETE("Deleting custom command from database failed, command might reappear after reboot."),
    TEMPLATE_SQL_ERROR_ON_FINDING_COMMAND("Database error retrieving command, can not find template."),
    TEMPLATE_SQL_ERROR_ON_LOADING_COMMANDS("Database error retrieving commands from database, can not list available commands."),
    TEMPLATE_RUN_ACTION("template run"),
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
    MUSIC_PLAYLIST_LENGTH("Playlist length:"),
    MUSIC_SKIP_PLAYLIST_END("Track skipped and end of playlist reached."),
    KEYWORD_CREATE_MISSING_KEYWORD("Creating a keyword requires a pattern the keywords activates on."),
    KEYWORD_CREATE_MISSING_TEMPLATE("Creating a keywords required a template for the response to keyword."),
    KEYWORD_CREATE_MISSING_NAME("Creating a keyword requires a name for the keyword."),
    KEYWORD_CREATE_SUCCESS("Keyword created successfully."),
    KEYWORD_PATTERN_SYNTAX_ERROR("Not a valid keyword pattern,"
            + " Some characters are reserved in pattern creation and must be escaped with '\\'"
            + " useful site for figuring out regex rules is https://regex101.com/"),
    KEYWORD_ALREADY_EXISTS("Keyword with that template already exists."),
    KEYWORD_DELETE_MISSING_NAME("Deleting a keyword requires the name of the keyword to delete"),
    KEYWORD_DELETE_NOT_FOUND("No such keyword as: %s"),
    KEYWORD_DELETE_PERMISSION_DENIED("You do not have permission to delete the keyword, only owner of the keyword and admins can delete it."),
    KEYWORD_DELETE_SUCCESS("Keyword deleted successfully."),
    KEYWORD_SQL_ERROR_ON_DELETE("Deleting keyword from database failed, keyword might reappear after reboot."),
    KEYWORD_NO_KEYWORDS("No keywords defined."),
    KEYWORD_SQL_ERROR_ON_CREATE("Adding keyword to database failed, keyword might disappear after reboot."),
    KEYWORD_COMMAND_LIST_ELEMENT("%s - Pattern: %s by: %s"),
    KEYWORD_CREATE_MISSING_USER("Creating a keyword requires the user as which to run."), 
    KEYWORD_RUN_AS_USER("user"), 
    KEYWORD_RUN_AS_CREATOR("me"), 
    KEYWORD_RUN_AS_UNKNOWN("Unknown value for user which to run the keyword as, valid values are: 'user' and 'me'"),
    INVENTORY_NO_USER_WITH_NAME("Could not find user with name: %s"),
    INVENTORY_MULTIPLE_USERS_WITH_NAME("Found multiple users with name: %s You can use users full discord name including the tag to specify users accurately."),
    INVENTORY_BOT_NO_PERMISSION("It appears I do not have permission to view members of the guild, can not find member with the provided name."),
    INVENTORY_FOR_USER("Inventory for %s"),
    INVENTORY_ITEM_ELEMENT("%sx %s\n"),
    INVENTORY_ADD_MISSING_ITEM_NAME("Adding item requires the name of the item to add."),
    INVENTORY_ADD_MISSING_ITEM_COUNT("Adding items requires the amount of items to add."),
    INVENTORY_COUNT_NOT_NUMBER("Amount of items does not appear to be a number."),
    INVENTORY_ITEM_ADDED_SUCCESS("Successfully added %sx %s to inventory of %s."),
    INVENTORY_ITEM_REMOVED_SUCCESS("Successfully removed %sx %s from inventory of %s."),
    INVENTORY_COUNT_ZERO("Amount of items to add can no be zero."),
    INVENTORY_NO_ITEMS_IN_INVENTORY("No items."),
    INVENTORY_USER_NOT_ENOUGH_ITEMS("Could not remove items, not enough items in inventory."),
    INVENTORY_SQL_ERROR_ON_ADD("Database error adding items, amount of items might revert after reboot."),
    INVENTORY_MODE_USER("user"),
    INVENTORY_MODE_ROLE("role"),
    INVENTORY_NOT_ENOUGH_ITEMS("Could not remove items, not enough items in inventory."),
    INVENTORY_USER_ITEM_ADDED_SUCCESS("Successfully added %sx %s to your inventory."),
    INVENTORY_USER_ITEM_REMOVED_SUCCESS("Successfully removed %sx %s from your inventory."),
    INVENTORY_UNKNOWN_MODE("Unknown mode for the target name specified: %s valid modes are 'user' and 'role'"),
    INVENTORY_PAY_MISSING_ITEM_NAME("Paying items to another user requires the name of the item to pay."),
    INVENTORY_PAY_MISSING_ITEM_COUNT("Paying items to another user requires the amount of items to pay."),
    INVENTORY_REMOVE_ROLE_SUCCESS_SOME_NOT_MODIFIED("Removed items from users inventory,"
            + " some of the users did not have enough items and no items were removed from their inventory."),
    INVENTORY_ADD_ROLE_SUCCESS("Successfully added %sx %s to inventory of %s users."),
    INVENTORY_COUNT_NEGATIVE("Paid item amount can not be negative, that's not a payment."),
    INVENTORY_PAY_USER_MISSING("Paying items to another user requires the name of the user to pay items to."),
    INVENTORY_PAY_TARGET_SELF("Can not pay items to self, that's not a payment."),
    INVENTORY_ITEM_PAID_SUCCESS("Successfully paid %sx %s to %s"),
    INVENTORY_SQL_ERROR_ON_PAY("Database error processing payment, payment cancelled."),
    INVENTORY_PAY_USER_NOT_ENOUGH_ITEMS_FOR_EVERYONE("You do not have enough items to pay everyone, payment cancelled."),
    INVENTORY_PAY_ROLE_SUCCESS("Successfully paid %sx %s to %s users."),
    INVENTORY_PAY_INTERRUPTED_NOT_ENOUGH_FOR_EVERYONE("Could not pay everyone due to not having enough items, the following people were not paid:\n%s"),
    INVENTORY_ROLE_SQL_ERROR_ON_PAY("Database error processing payment, following people did not receive payment:\n%s"),
    INVENTORY_SQL_ERROR_ON_FETCHING_INVENTORY("Database error retrieving list of items, can not show inventory contents.");

    private static final Logger LOGGER = LogManager.getLogger();
    private final String defaultText;

    TranslationKey(final String defaultText) {
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
