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

import eternal.lemonadebot.commandtypes.OwnerCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.CommandMatcher;
import java.sql.SQLException;
import java.util.Optional;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class GreetCommand extends OwnerCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand() {
        return "greeting";
    }

    @Override
    public String getHelp() {
        return "Syntax: greeting <action> [template]\n"
                + "<action> can be one of the following:\n"
                + "  get - get the current greeting template\n"
                + "  set - set the greeting template to [template]\n"
                + "  disable - disable greeting new members\n"
                + "[template] is the message to be sent whenever new members joins,\n"
                + "it can contain the following keys to replace when shown\n"
                + "  {name} - name of the newly joined member.\n"
                + "  {mention} - mention the newly joined member.";
    }

    @Override
    public void respond(CommandMatcher message) {
        final TextChannel channel = message.getTextChannel();
        final ConfigManager guildConf = message.getGuildData().getConfigManager();
        final String[] options = message.getArguments(1);
        if (options.length == 0) {
            channel.sendMessage("Provide operation to perform, check help for possible operations").queue();
            return;
        }
        switch (options[0]) {
            case "set": {
                if (options.length < 2) {
                    channel.sendMessage("Provide the template to set greeting to").queue();
                    return;
                }
                final String newTemplate = options[1];
                try {
                    guildConf.setGreetingTemplate(newTemplate);
                    channel.sendMessage("Updated greeting template succesfully").queue();
                } catch (SQLException ex) {
                    channel.sendMessage("Updating greeting in database failed, will still use untill reboot, re-issue command once DB issue is fixed.").queue();
                    LOGGER.error("Failure to update greeting template in database");
                    LOGGER.warn(ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            case "get": {
                final Optional<String> optTemplate = guildConf.getGreetingTemplate();
                if (optTemplate.isEmpty()) {
                    channel.sendMessage("Greeting new members is currently disabled").queue();
                    return;
                }
                final String template = optTemplate.get();
                channel.sendMessage("Current template: " + template).queue();
                break;
            }
            case "disable": {
                try {
                    guildConf.setGreetingTemplate(null);
                    channel.sendMessage("Disabled greeting succesfully").queue();
                } catch (SQLException ex) {
                    channel.sendMessage("Disabling greeting in database failed, will not greet untill reboot, re-issue command once DB issue is fixed.").queue();
                    LOGGER.error("Failure to update greeting template in database");
                    LOGGER.warn(ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
                break;
            }
            default: {
                channel.sendMessage("Unkown operation: " + options[0]).queue();
                break;
            }
        }
    }
}
