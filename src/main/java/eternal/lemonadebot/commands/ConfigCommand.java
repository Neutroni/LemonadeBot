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
package eternal.lemonadebot.commands;

import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.commandtypes.AdminCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.GuildDataStore;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
class ConfigCommand extends AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand() {
        return "config";
    }

    @Override
    public String getDescription() {
        return "Set configuration values used by the bot";
    }

    @Override
    public String getHelpText() {
        return "Syntax: config <action> <config> [value]\n"
                + "<action> can be one of the following:\n"
                + " get - get the current value for config\n"
                + " set - set the config to [value]\n"
                + " disable - disable the option if possible\n"
                + " help - Show help for value format."
                + "<config> is the configuration option to edit, one of following:\n"
                + " prefix - The prefix used by commands.\n"
                + " greeting - Text used to greet new members with."
                + " logchannel - Channel used to send log messages to."
                + " language - Language for the bot to reply in."
                + "[value] - Value to set configuration to.";
    }

    @Override
    public void respond(CommandMatcher message, GuildDataStore guildData) {
        final TextChannel channel = message.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final String[] options = message.getArguments(2);
        if (options.length == 0) {
            channel.sendMessage("Provide operation to perform, check help for possible operations.").queue();
            return;
        }
        switch (options[0]) {
            case "set": {
                if (options.length < 2) {
                    channel.sendMessage("Provide the name of the setting and the value to set.").queue();
                    return;
                }
                if (options.length < 3) {
                    channel.sendMessage("Provide the value to set the setting to.").queue();
                }
                setValue(options[1], options[2], channel, guildConf, message);
                break;
            }
            case "get": {
                if (options.length < 2) {
                    channel.sendMessage("Provide the name of the setting to get the value for.").queue();
                    return;
                }
                getValue(options[1], channel, guildConf);
                break;
            }
            case "disable": {
                if (options.length < 2) {
                    channel.sendMessage("Provide the name of the setting to disable.").queue();
                    return;
                }
                disableValue(options[1], channel, guildConf);
                break;
            }
            case "help": {
                if (options.length < 2) {
                    channel.sendMessage("Provide the name of the setting to show help for.").queue();
                    return;
                }
                printHelp(options[1], channel, guildConf);
                break;
            }
            default: {
                channel.sendMessage("Unknown operation: " + options[0]).queue();
                break;
            }
        }
    }

    /**
     * Set the config value according to the input
     *
     * @param options String of options, action(set),config,value
     * @param channel TextChannel to reply on
     * @param guildConf ConfigManager to update
     */
    private void setValue(String config, String value, TextChannel channel, ConfigManager guildConf, CommandMatcher matcher) {
        switch (config) {
            case "prefix": {
                try {
                    guildConf.setCommandPrefix(value);
                    channel.sendMessage("Updated prefix succesfully to: " + value).queue();
                } catch (SQLException ex) {
                    channel.sendMessage("Storing prefix in DB failed, will still use new prefix until reboot,"
                            + " re-issue command once DB issue is fixed.").queue();
                    LOGGER.error("Failure to update command prefix in database:\n" + ex.getMessage());
                    LOGGER.trace("Stack Trace", ex);
                }
                break;
            }
            case "greeting": {
                try {
                    guildConf.setGreetingTemplate(value);
                    channel.sendMessage("Updated greeting template succesfully to: " + value).queue();
                } catch (SQLException ex) {
                    channel.sendMessage("Storing greeting in database failed, will still use until reboot,"
                            + " re-issue command once DB issue is fixed.").queue();
                    LOGGER.error("Failure to update greeting template in database:\n" + ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case "logchannel": {
                final List<TextChannel> channels = matcher.getMentionedChannels();
                if (channels.isEmpty()) {
                    channel.sendMessage("Mention the channel you want to set as the the log channel.").queue();
                    return;
                }
                final TextChannel logChannel = channels.get(0);
                try {
                    guildConf.setLogChannel(logChannel);
                    channel.sendMessage("Update log channel succesfully to: " + logChannel.getName()).queue();
                } catch (SQLException ex) {
                    channel.sendMessage("Storing log channel in database failed, will still use until reboot,"
                            + " re-issue command once DB issue is fixed.").queue();
                    LOGGER.error("Failure to update log channel in database:\n" + ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case "language": {
                try {
                    final Locale locale = new Locale(value);
                    if (guildConf.setLocale(locale)) {
                        channel.sendMessage("Updated language succesfully to: " + locale.getDisplayLanguage(locale)).queue();
                    } else {
                        final String languages = ConfigManager.SUPPORTED_LOCALES.stream().map((t) -> {
                            return t.getLanguage() + " - " + t.getDisplayLanguage(guildConf.getLocale());
                        }).collect(Collectors.joining(","));
                        channel.sendMessage("Unsupported language, currently supported languages are: " + languages).queue();
                    }
                } catch (SQLException ex) {
                    channel.sendMessage("Storing language setting in database failed, will still use until reboot,"
                            + " re-issue command once DB issue is fixed.").queue();
                    LOGGER.error("Failure to update language in database:\n{}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            default: {
                channel.sendMessage("Unknown configuration option: " + config).queue();
                break;
            }
        }

    }

    /**
     * Reply with the current value for confuguration option
     *
     * @param options String of option
     * @param channel TextChannel to reply on
     * @param guildConf ConfigManager to get value from
     */
    private void getValue(String option, TextChannel channel, ConfigManager guildConf) {
        switch (option) {
            case "prefix": {
                channel.sendMessage("Current command prefix: " + guildConf.getCommandPrefix()).queue();
                break;
            }
            case "greeting": {
                guildConf.getGreetingTemplate().ifPresentOrElse((String greeting) -> {
                    channel.sendMessage("Current greeting: " + greeting).queue();
                }, () -> {
                    channel.sendMessage("Greeting disabled.").queue();
                });
                break;
            }
            case "logchannel": {
                guildConf.getLogChannelID().ifPresentOrElse((Long channelID) -> {
                    final TextChannel logChannel = channel.getGuild().getTextChannelById(channelID);
                    if (logChannel == null) {
                        channel.sendMessage("Log channel could not be found, it has probably been deleted since setting it").queue();
                    } else {
                        channel.sendMessage("Current log channel: " + logChannel.getAsMention()).queue();
                    }
                }, () -> {
                    channel.sendMessage("Log channel disabled.").queue();
                });
                break;
            }
            case "language": {
                final Locale locale = guildConf.getLocale();
                channel.sendMessage("Current language: " + locale.getDisplayLanguage(locale)).queue();
                break;
            }
            default: {
                channel.sendMessage("Unknown configuration option: " + option).queue();
                break;
            }
        }
        final Optional<String> optTemplate = guildConf.getGreetingTemplate();
        if (optTemplate.isEmpty()) {
            channel.sendMessage("Greeting new members is currently disabled").queue();
            return;
        }
        final String template = optTemplate.get();
        channel.sendMessage("Current template: " + template).queue();
    }

    /**
     *
     * @param options String of option to disable
     * @param channel TextChannel to reply on
     * @param guildConf ConfigManager in which to disable to value
     */
    private void disableValue(String option, TextChannel channel, ConfigManager guildConf) {
        switch (option) {
            case "prefix": {
                channel.sendMessage("Cannot disable the configuration for the command prefix.").queue();
                break;
            }
            case "greeting": {
                try {
                    guildConf.setGreetingTemplate(null);
                    channel.sendMessage("Disabled greeting succesfully").queue();
                } catch (SQLException ex) {
                    channel.sendMessage("Disabling greeting in database failed,"
                            + " will not greet untill reboot, re-issue command once DB issue is fixed.").queue();
                    LOGGER.error("Failure to update greeting template in database " + ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case "logchannel": {
                try {
                    guildConf.setLogChannel(null);
                    channel.sendMessage("Disabled message log succesfully").queue();
                } catch (SQLException ex) {
                    channel.sendMessage("Disabling log channel in database failed,"
                            + " will not log messages until reboot, re-issue command once DB issue is fixed.").queue();
                    LOGGER.error("Failure to update log channel in database " + ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case "language": {
                channel.sendMessage("Cannot disable the configuration for the bot language").queue();
                break;
            }
            default: {
                channel.sendMessage("Unknown configuration option: " + option).queue();
                break;
            }
        }

    }

    /**
     *
     * @param options String of the option to print help for
     * @param channel TextChannel to reply on
     * @param guildConf ConfigManager for translation
     */
    private void printHelp(String option, TextChannel channel, ConfigManager guildConf) {
        switch (option) {
            case "prefix": {
                channel.sendMessage(guildConf.getResourceBundle().getString("prefix-argument-help")).queue();
                break;
            }
            case "greeting": {
                channel.sendMessage(guildConf.getResourceBundle().getString("greeting-argument-help")).queue();
                break;
            }
            case "logchannel": {
                channel.sendMessage(guildConf.getResourceBundle().getString("logchannel-argument-help")).queue();
                break;
            }
            case "language": {
                channel.sendMessage(guildConf.getResourceBundle().getString("language-argument-help")).queue();
                break;
            }
            default: {
                channel.sendMessage("Unkown configuration option: " + option).queue();
                break;
            }
        }

    }
}
