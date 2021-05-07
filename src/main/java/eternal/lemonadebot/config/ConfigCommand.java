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
package eternal.lemonadebot.config;

import eternal.lemonadebot.commands.AdminCommand;
import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command used to manage settings for a guild
 *
 * @author Neutroni
 */
public class ConfigCommand extends AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(final ResourceBundle locale) {
        return locale.getString("COMMAND_CONFIG");
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_CONFIG");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        return locale.getString("SYNTAX_CONFIG");
    }

    @Override
    public void respond(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TranslationCache translation = context.getTranslation();
        final TextChannel channel = matcher.getTextChannel();
        final ResourceBundle locale = translation.getResourceBundle();
        final String[] options = matcher.getArguments(2);
        if (options.length == 0) {
            channel.sendMessage(locale.getString("ERROR_MISSING_OPERATION")).queue();
            return;
        }

        final String action = options[0];
        final ActionKey key = translation.getActionKey(action);
        switch (key) {
            case SET: {
                if (options.length < 2) {
                    channel.sendMessage(locale.getString("CONFIG_SET_MISSING_OPTION")).queue();
                    return;
                }
                if (options.length < 3) {
                    channel.sendMessage(locale.getString("CONFIG_MISSING_VALUE")).queue();
                }
                setValue(options[1], options[2], context);
                break;
            }
            case GET: {
                if (options.length < 2) {
                    channel.sendMessage(locale.getString("CONFIG_GET_MISSING_OPTION")).queue();
                    return;
                }
                getValue(options[1], context);
                break;
            }
            case DISABLE: {
                if (options.length < 2) {
                    channel.sendMessage(locale.getString("CONFIG_DISABLE_MISSING_OPTION")).queue();
                    return;
                }
                disableValue(options[1], context);
                break;
            }
            default: {
                channel.sendMessage(locale.getString("ERROR_UNKNOWN_OPERATION") + action).queue();
                break;
            }
        }
    }

    /**
     * Set the config value according to the input
     *
     * @param config Name of config to update
     * @param value Value to set the config to
     * @param guildData GuildData to get config from
     * @param matcher message that made the request
     */
    private static void setValue(final String config, final String value, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TranslationCache translation = context.getTranslation();
        final GuildDataStore guildData = context.getGuildData();
        final TextChannel channel = matcher.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final ResourceBundle locale = translation.getResourceBundle();
        final ActionKey key = translation.getActionKey(config);
        switch (key) {
            case PREFIX: {
                try {
                    guildConf.setCommandPrefix(value);
                    channel.sendMessage(locale.getString("CONFIG_PREFIX_UPDATE_SUCCESS") + value).queue();
                } catch (SQLException ex) {
                    channel.sendMessage(locale.getString("CONFIG_PREFIX_SQL_ERROR")).queue();
                    LOGGER.error("Failure to update command prefix in database: {}", ex.getMessage());
                    LOGGER.trace("Stack Trace", ex);
                }
                break;
            }
            case GREETING: {
                try {
                    guildConf.setGreetingTemplate(value);
                    channel.sendMessage(locale.getString("CONFIG_GREETING_UPDATE_SUCCESS") + value).queue();
                } catch (SQLException ex) {
                    channel.sendMessage(locale.getString("CONFIG_GREETING_SQL_ERROR")).queue();
                    LOGGER.error("Failure to update greeting template in database: {}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case LOG_CHANNEL: {
                final List<TextChannel> channels = matcher.getMentionedChannels();
                if (channels.isEmpty()) {
                    channel.sendMessage(locale.getString("CONFIG_LOG_CHANNEL_MISSING")).queue();
                    return;
                }
                final TextChannel logChannel = channels.get(0);
                try {
                    guildConf.setLogChannel(logChannel);
                    channel.sendMessage(locale.getString("CONFIG_LOG_CHANNEL_UPDATE_SUCCESS") + logChannel.getName()).queue();
                } catch (SQLException ex) {
                    channel.sendMessage(locale.getString("CONFIG_LOG_CHANNEL_SQL_ERROR")).queue();
                    LOGGER.error("Failure to update log channel in database: {}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case LANGUAGE: {
                try {
                    final Locale newLocale = new Locale(value);
                    if (guildConf.setLocale(newLocale)) {
                        final ResourceBundle newRB = translation.getResourceBundle();
                        channel.sendMessage(newRB.getString("CONFIG_LANGUAGE_UPDATE_SUCCESS") + newLocale.getDisplayLanguage(newLocale)).queue();
                    } else {
                        final String supportedLanguages = ConfigManager.SUPPORTED_LOCALES.stream().map((t) -> {
                            return t.getLanguage() + " - " + t.getDisplayLanguage(locale.getLocale());
                        }).collect(Collectors.joining(","));
                        final String template = locale.getString("CONFIG_UNSUPPORTED_LOCALE");
                        channel.sendMessageFormat(template, supportedLanguages).queue();
                    }
                } catch (SQLException ex) {
                    channel.sendMessage(locale.getString("CONFIG_LANGUAGE_SQL_ERROR")).queue();
                    LOGGER.error("Failure to update language in database: {}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case TIMEZONE: {
                try {
                    final ZoneId zone = ZoneId.of(value);
                    guildConf.setZoneId(zone);
                    channel.sendMessage(locale.getString("CONFIG_TIMEZONE_UPDATE_SUCCESS") + value).queue();
                } catch (ZoneRulesException ex) {
                    //Zone could not be found
                    channel.sendMessage(locale.getString("CONFIG_TIMEZONE_ZONE_NOT_FOUND")).queue();
                } catch (DateTimeException ex) {
                    //Invalid format for timezone
                    channel.sendMessage(locale.getString("CONFIG_TIMEZONE_ZONE_MALFORMED")).queue();
                } catch (SQLException ex) {
                    channel.sendMessage(locale.getString("CONFIG_TIMEZONE_SQL_ERROR")).queue();
                    LOGGER.error("Failure to update command timezone in database: {}", ex.getMessage());
                    LOGGER.trace("Stack Trace", ex);
                }
                break;
            }
            default: {
                channel.sendMessageFormat(locale.getString("CONFIG_ERROR_UNKNOWN_SETTING"), config).queue();
                break;
            }
        }

    }

    /**
     * Reply with the current value for configuration option
     *
     * @param option String of option
     * @param channel TextChannel to reply on
     * @param guildData ConfigManager to get value from
     */
    private static void getValue(final String option, final CommandContext context) {
        final TextChannel channel = context.getMatcher().getTextChannel();
        final TranslationCache translationCache = context.getTranslation();
        final GuildDataStore guildData = context.getGuildData();
        final ConfigManager guildConf = guildData.getConfigManager();
        final ResourceBundle locale = translationCache.getResourceBundle();
        final ActionKey key = translationCache.getActionKey(option);
        switch (key) {
            case PREFIX: {
                channel.sendMessageFormat(locale.getString("CONFIG_CURRENT_PREFIX"), guildConf.getCommandPrefix()).queue();
                break;
            }
            case GREETING: {
                guildConf.getGreetingTemplate().ifPresentOrElse((String greeting) -> {
                    channel.sendMessageFormat(locale.getString("CONFIG_CURRENT_GREETING"), greeting).queue();
                }, () -> {
                    channel.sendMessage(locale.getString("CONFIG_GREETING_DISABLED_CURRENTLY")).queue();
                });
                break;
            }
            case LOG_CHANNEL: {
                guildConf.getLogChannelID().ifPresentOrElse((Long channelID) -> {
                    final TextChannel logChannel = channel.getGuild().getTextChannelById(channelID);
                    if (logChannel == null) {
                        channel.sendMessage(locale.getString("CONFIG_LOG_CHANNEL_UNKNOWN")).queue();
                    } else {
                        channel.sendMessageFormat(locale.getString("CONFIG_CURRENT_LOG_CHANNEL"), logChannel.getAsMention()).queue();
                    }
                }, () -> {
                    channel.sendMessage(locale.getString("CONFIG_LOG_CHANNEL_DISABLED_CURRENTLY")).queue();
                });
                break;
            }
            case LANGUAGE: {
                final String template = locale.getString("CONFIG_CURRENT_LANGUAGE");
                final Locale currentLocale = locale.getLocale();
                channel.sendMessageFormat(template, currentLocale.getDisplayLanguage(currentLocale)).queue();
                break;
            }
            case TIMEZONE: {
                final String template = locale.getString("CONFIG_CURRENT_TIMEZONE");
                channel.sendMessageFormat(template, guildConf.getZoneId().getDisplayName(TextStyle.FULL, locale.getLocale())).queue();
                break;
            }
            default: {
                channel.sendMessageFormat(locale.getString("CONFIG_ERROR_UNKNOWN_SETTING"), option).queue();
                break;
            }
        }
    }

    /**
     *
     * @param option String of option to disable
     * @param channel TextChannel to reply on
     * @param guildData ConfigManager in which to disable to value
     */
    private static void disableValue(final String option, final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel channel = matcher.getTextChannel();
        final TranslationCache translationCache = context.getTranslation();
        final GuildDataStore guildData = context.getGuildData();
        final ConfigManager guildConf = guildData.getConfigManager();
        final ResourceBundle locale = translationCache.getResourceBundle();
        final ActionKey key = translationCache.getActionKey(option);
        switch (key) {
            case PREFIX: {
                channel.sendMessage(locale.getString("CONFIG_DISABLE_PREFIX")).queue();
                break;
            }
            case GREETING: {
                try {
                    guildConf.setGreetingTemplate(null);
                    channel.sendMessage(locale.getString("CONFIG_GREETING_DISABLED")).queue();
                } catch (SQLException ex) {
                    channel.sendMessage(locale.getString("CONFIG_SQL_ERROR_ON_GREETING_DISABLE")).queue();
                    LOGGER.error("Failure to update greeting template in database: {}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case LOG_CHANNEL: {
                try {
                    guildConf.setLogChannel(null);
                    channel.sendMessage(locale.getString("CONFIG_LOG_CHANNEL_DISABLED")).queue();
                } catch (SQLException ex) {
                    channel.sendMessage(locale.getString("CONFIG_SQL_ERROR_ON_LOG_CHANNEL_DISABLE")).queue();
                    LOGGER.error("Failure to update log channel in database: {}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case LANGUAGE: {
                channel.sendMessage(locale.getString("CONFIG_LANGUAGE_DISABLE")).queue();
                break;
            }
            case TIMEZONE: {
                channel.sendMessage(locale.getString("CONFIG_TIMEZONE_DISABLE")).queue();
                break;
            }
            default: {
                channel.sendMessage(locale.getString("CONFIG_ERROR_UNKNOWN_SETTING") + option).queue();
                break;
            }
        }

    }
}
