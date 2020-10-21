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
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
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
    public String getCommand(final Locale locale) {
        return TranslationKey.COMMAND_CONFIG.getTranslation(locale);
    }

    @Override
    public String getDescription(final Locale locale) {
        return TranslationKey.DESCRIPTION_CONFIG.getTranslation(locale);
    }

    @Override
    public String getHelpText(final Locale locale) {
        return TranslationKey.SYNTAX_CONFIG.getTranslation(locale);
    }

    @Override
    public void respond(final CommandMatcher message, final GuildDataStore guildData) {
        final TextChannel channel = message.getTextChannel();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildData.getConfigManager().getLocale();
        final String[] options = message.getArguments(2);
        if (options.length == 0) {
            channel.sendMessage(TranslationKey.ERROR_MISSING_OPERATION.getTranslation(locale)).queue();
            return;
        }

        final String action = options[0];
        final ActionKey key = translationCache.getActionKey(action);
        switch (key) {
            case SET: {
                if (options.length < 2) {
                    channel.sendMessage(TranslationKey.CONFIG_SET_MISSING_OPTION.getTranslation(locale)).queue();
                    return;
                }
                if (options.length < 3) {
                    channel.sendMessage(TranslationKey.CONFIG_MISSING_VALUE.getTranslation(locale)).queue();
                }
                setValue(options[1], options[2], guildData, message);
                break;
            }
            case GET: {
                if (options.length < 2) {
                    channel.sendMessage(TranslationKey.CONFIG_GET_MISSING_OPTION.getTranslation(locale)).queue();
                    return;
                }
                getValue(options[1], channel, guildData);
                break;
            }
            case DISABLE: {
                if (options.length < 2) {
                    channel.sendMessage(TranslationKey.CONFIG_DISABLE_MISSING_OPTION.getTranslation(locale)).queue();
                    return;
                }
                disableValue(options[1], channel, guildData);
                break;
            }
            default: {
                channel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + action).queue();
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
    private static void setValue(final String config, final String value, final GuildDataStore guildData, final CommandMatcher matcher) {
        final TextChannel channel = matcher.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildConf.getLocale();
        final ActionKey key = translationCache.getActionKey(config);
        switch (key) {
            case PREFIX: {
                try {
                    guildConf.setCommandPrefix(value);
                    channel.sendMessage(TranslationKey.CONFIG_PREFIX_UPDATE_SUCCESS.getTranslation(locale) + value).queue();
                } catch (SQLException ex) {
                    channel.sendMessage(TranslationKey.CONFIG_PREFIX_SQL_ERROR.getTranslation(locale)).queue();
                    LOGGER.error("Failure to update command prefix in database: {}", ex.getMessage());
                    LOGGER.trace("Stack Trace", ex);
                }
                break;
            }
            case GREETING: {
                try {
                    guildConf.setGreetingTemplate(value);
                    channel.sendMessage(TranslationKey.CONFIG_GREETING_UPDATE_SUCCESS.getTranslation(locale) + value).queue();
                } catch (SQLException ex) {
                    channel.sendMessage(TranslationKey.CONFIG_GREETING_SQL_ERROR.getTranslation(locale)).queue();
                    LOGGER.error("Failure to update greeting template in database: {}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case LOG_CHANNEL: {
                final List<TextChannel> channels = matcher.getMentionedChannels();
                if (channels.isEmpty()) {
                    channel.sendMessage(TranslationKey.CONFIG_LOG_CHANNEL_MISSING.getTranslation(locale)).queue();
                    return;
                }
                final TextChannel logChannel = channels.get(0);
                try {
                    guildConf.setLogChannel(logChannel);
                    channel.sendMessage(TranslationKey.CONFIG_LOG_CHANNEL_UPDATE_SUCCESS.getTranslation(locale) + logChannel.getName()).queue();
                } catch (SQLException ex) {
                    channel.sendMessage(TranslationKey.CONFIG_LOG_CHANNEL_SQL_ERROR.getTranslation(locale)).queue();
                    LOGGER.error("Failure to update log channel in database: {}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case LANGUAGE: {
                try {
                    final Locale newLocale = new Locale(value);
                    if (guildConf.setLocale(newLocale)) {
                        channel.sendMessage(TranslationKey.CONFIG_LANGUAGE_UPDATE_SUCCESS.getTranslation(newLocale) + newLocale.getDisplayLanguage(newLocale)).queue();
                    } else {
                        final String supportedLanguages = ConfigManager.SUPPORTED_LOCALES.stream().map((t) -> {
                            return t.getLanguage() + " - " + t.getDisplayLanguage(locale);
                        }).collect(Collectors.joining(","));
                        final String template = TranslationKey.CONFIG_UNSUPPORTED_LOCALE.getTranslation(locale);
                        channel.sendMessageFormat(template, supportedLanguages).queue();
                    }
                } catch (SQLException ex) {
                    channel.sendMessage(TranslationKey.CONFIG_LANGUAGE_SQL_ERROR.getTranslation(locale)).queue();
                    LOGGER.error("Failure to update language in database: {}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            default: {
                channel.sendMessageFormat(TranslationKey.CONFIG_ERROR_UNKNOWN_SETTING.getTranslation(locale), config).queue();
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
    private static void getValue(final String option, final TextChannel channel, final GuildDataStore guildData) {
        final ConfigManager guildConf = guildData.getConfigManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildConf.getLocale();
        final ActionKey key = translationCache.getActionKey(option);
        switch (key) {
            case PREFIX: {
                channel.sendMessageFormat(TranslationKey.CONFIG_CURRENT_PREFIX.getTranslation(locale), guildConf.getCommandPrefix()).queue();
                break;
            }
            case GREETING: {
                guildConf.getGreetingTemplate().ifPresentOrElse((String greeting) -> {
                    channel.sendMessageFormat(TranslationKey.CONFIG_CURRENT_GREETING.getTranslation(locale), greeting).queue();
                }, () -> {
                    channel.sendMessage(TranslationKey.CONFIG_GREETING_DISABLED_CURRENTLY.getTranslation(locale)).queue();
                });
                break;
            }
            case LOG_CHANNEL: {
                guildConf.getLogChannelID().ifPresentOrElse((Long channelID) -> {
                    final TextChannel logChannel = channel.getGuild().getTextChannelById(channelID);
                    if (logChannel == null) {
                        channel.sendMessage(TranslationKey.CONFIG_LOG_CHANNEL_UNKNOWN.getTranslation(locale)).queue();
                    } else {
                        channel.sendMessageFormat(TranslationKey.CONFIG_CURRENT_LOG_CHANNEL.getTranslation(locale), logChannel.getAsMention()).queue();
                    }
                }, () -> {
                    channel.sendMessage(TranslationKey.CONFIG_LOG_CHANNEL_DISABLED_CURRENTLY.getTranslation(locale)).queue();
                });
                break;
            }
            case LANGUAGE: {
                channel.sendMessageFormat(TranslationKey.CONFIG_CURRENT_LANGUAGE.getTranslation(locale), locale.getDisplayLanguage(locale)).queue();
                break;
            }
            default: {
                channel.sendMessageFormat(TranslationKey.CONFIG_ERROR_UNKNOWN_SETTING.getTranslation(locale), option).queue();
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
    private static void disableValue(final String option, final TextChannel channel, final GuildDataStore guildData) {
        final ConfigManager guildConf = guildData.getConfigManager();
        final TranslationCache translationCache = guildData.getTranslationCache();
        final Locale locale = guildConf.getLocale();
        final ActionKey key = translationCache.getActionKey(option);
        switch (key) {
            case PREFIX: {
                channel.sendMessage(TranslationKey.CONFIG_DISABLE_PREFIX.getTranslation(locale)).queue();
                break;
            }
            case GREETING: {
                try {
                    guildConf.setGreetingTemplate(null);
                    channel.sendMessage(TranslationKey.CONFIG_GREETING_DISABLED.getTranslation(locale)).queue();
                } catch (SQLException ex) {
                    channel.sendMessage(TranslationKey.CONFIG_SQL_ERROR_ON_GREETING_DISABLE.getTranslation(locale)).queue();
                    LOGGER.error("Failure to update greeting template in database: {}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case LOG_CHANNEL: {
                try {
                    guildConf.setLogChannel(null);
                    channel.sendMessage(TranslationKey.CONFIG_LOG_CHANNEL_DISABLED.getTranslation(locale)).queue();
                } catch (SQLException ex) {
                    channel.sendMessage(TranslationKey.CONFIG_SQL_ERROR_ON_LOG_CHANNEL_DISABLE.getTranslation(locale)).queue();
                    LOGGER.error("Failure to update log channel in database: {}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case LANGUAGE: {
                channel.sendMessage(TranslationKey.CONFIG_LANGUAGE_DISABLE.getTranslation(locale)).queue();
                break;
            }
            default: {
                channel.sendMessage(TranslationKey.CONFIG_ERROR_UNKNOWN_SETTING.getTranslation(locale) + option).queue();
                break;
            }
        }

    }
}
