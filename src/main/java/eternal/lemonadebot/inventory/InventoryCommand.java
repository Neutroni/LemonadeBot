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
package eternal.lemonadebot.inventory;

import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import eternal.lemonadebot.translation.TranslationKey;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
public class InventoryCommand implements ChatCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommand(final Locale locale) {
        return TranslationKey.COMMAND_INVENTORY.getTranslation(locale);
    }

    @Override
    public String getDescription(final Locale locale) {
        return TranslationKey.DESCRIPTION_INVENTORY.getTranslation(locale);
    }

    @Override
    public String getHelpText(final Locale locale) {
        return TranslationKey.SYNTAX_INVENTORY.getTranslation(locale);
    }

    @Override
    public Collection<CommandPermission> getDefaultRanks(final Locale locale, final long guildID, final PermissionManager permissions) {
        final String commandName = getCommand(locale);
        final String actionCreate = TranslationKey.ACTION_ADD.getTranslation(locale);
        return List.of(new CommandPermission(commandName, MemberRank.USER, guildID),
                new CommandPermission(commandName + ' ' + actionCreate, MemberRank.ADMIN, guildID));
    }

    @Override
    public void respond(final CommandMatcher message, final GuildDataStore guildData) {
        final Locale locale = guildData.getConfigManager().getLocale();
        final TextChannel channel = message.getTextChannel();
        final TranslationCache translationCache = guildData.getTranslationCache();

        final String[] opts = message.getArguments(1);
        if (opts.length == 0) {
            channel.sendMessage(TranslationKey.ERROR_MISSING_OPERATION.getTranslation(locale)).queue();
            return;
        }

        final String action = opts[0];
        final ActionKey key = translationCache.getActionKey(action);
        switch (key) {
            case LIST: {
                showInventory(opts, message, guildData);
                break;
            }
            case ADD: {
                addItemToInventory(message, guildData);
                break;
            }
            case PAY: {
                payItemToUser(message, guildData);
                break;
            }
            default: {
                channel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + action).queue();
            }
        }
    }

    private static void showInventory(final String[] opts, final CommandMatcher message, final GuildDataStore guildData) {
        final TextChannel channel = message.getTextChannel();
        final InventoryManager inventoryManager = guildData.getInventoryManager();
        final Guild guild = message.getGuild();
        final Locale locale = guildData.getConfigManager().getLocale();
        final Member requester = message.getMember();
        if (opts.length < 2) {
            //No user specified, show inventory of requester
            showInventoryForUser(requester, inventoryManager, locale, channel);
            return;
        }

        //Member name specified, get member with the name.
        final String targetName = opts[1];
        guild.retrieveMembersByPrefix(targetName, 2).onSuccess((List<Member> members) -> {
            if (members.isEmpty()) {
                final String template = TranslationKey.INVENTORY_NO_USER_WITH_NAME.getTranslation(locale);
                channel.sendMessageFormat(template, targetName).queue();
                return;
            }
            if (members.size() > 1) {
                final String template = TranslationKey.INVENTORY_MULTIPLE_USERS_WITH_NAME.getTranslation(locale);
                channel.sendMessageFormat(template, targetName).queue();
                return;
            }
            final Member target = members.get(0);
            showInventoryForUser(target, inventoryManager, locale, channel);
        }).onError((Throwable t) -> {
            channel.sendMessage(TranslationKey.INVENTORY_BOT_NO_PERMISSION.getTranslation(locale)).queue();
        });
    }

    /**
     * Construct and send a message containing users inventory items.
     *
     * @param member Member to get inventory for
     * @param inventoryManager InventoryManager to get users inventory from
     * @param locale Locale to send the message in.
     * @param channel Channel to send the message on.
     */
    private static void showInventoryForUser(final Member member, final InventoryManager inventoryManager, final Locale locale, final TextChannel channel) {
        final EmbedBuilder eb = new EmbedBuilder();
        final String titleTemplate = TranslationKey.INVENTORY_FOR_USER.getTranslation(locale);
        final String userName = member.getEffectiveName();
        final String title = String.format(titleTemplate, userName);
        eb.setTitle(title);
        final StringBuilder sb = new StringBuilder();
        final Map<String, Long> inv;
        try {
            inv = inventoryManager.getUserInventory(member);
        } catch (SQLException e) {
            channel.sendMessage(TranslationKey.INVENTORY_SQL_ERROR_ON_FETCHING_INVENTORY.getTranslation(locale)).queue();
            return;
        }
        final String listElementTemplate = TranslationKey.INVENTORY_ITEM_ELEMENT.getTranslation(locale);
        inv.forEach((String item, Long count) -> {
            sb.append(String.format(listElementTemplate, count, item));
        });
        //If user has no items in inventory add message about it to description.
        if (inv.isEmpty()) {
            sb.append(TranslationKey.INVENTORY_NO_ITEMS_IN_INVENTORY.getTranslation(locale));
        }
        eb.setDescription(sb);
        channel.sendMessage(eb.build()).queue();
    }

    private static void addItemToInventory(final CommandMatcher message, final GuildDataStore guildData) {
        final TextChannel channel = message.getTextChannel();
        final Guild guild = message.getGuild();
        final Locale locale = guildData.getConfigManager().getLocale();
        final Member requester = message.getMember();
        //add item count user
        final List<String> args = message.parseArguments(5);
        if (args.size() < 2) {
            channel.sendMessage(TranslationKey.INVENTORY_ADD_MISSING_ITEM_NAME.getTranslation(locale)).queue();
            return;
        }
        final String itemName = args.get(1);
        if (args.size() < 3) {
            channel.sendMessage(TranslationKey.INVENTORY_ADD_MISSING_ITEM_COUNT.getTranslation(locale)).queue();
            return;
        }
        final String countString = args.get(2);
        final long itemCount;
        try {
            itemCount = Long.parseLong(countString);
        } catch (NumberFormatException e) {
            channel.sendMessage(TranslationKey.INVENTORY_COUNT_NOT_NUMBER.getTranslation(locale)).queue();
            return;
        }
        if (itemCount == 0) {
            channel.sendMessage(TranslationKey.INVENTORY_COUNT_ZERO.getTranslation(locale)).queue();
            return;
        }

        final InventoryManager inventoryManager = guildData.getInventoryManager();
        final String targetName;
        if (args.size() < 4) {
            //Add item to users own inventory
            try {
                if (inventoryManager.updateCount(requester, itemName, itemCount)) {
                    final String template;
                    if (itemCount > 0) {
                        template = TranslationKey.INVENTORY_USER_ITEM_ADDED_SUCCESS.getTranslation(locale);
                    } else {
                        template = TranslationKey.INVENTORY_USER_ITEM_REMOVED_SUCCESS.getTranslation(locale);
                    }

                    channel.sendMessageFormat(template, Math.abs(itemCount), itemName).queue();
                    return;
                }
                channel.sendMessage(TranslationKey.INVENTORY_USER_NOT_ENOUGH_ITEMS.getTranslation(locale)).queue();
            } catch (SQLException e) {
                channel.sendMessage(TranslationKey.INVENTORY_SQL_ERROR_ON_ADD.getTranslation(locale)).queue();
            }
            return;
        }
        targetName = args.get(3);

        final String modeName;
        if (args.size() < 5) {
            modeName = null;
        } else {
            modeName = args.get(4);
        }

        final String modeUser = TranslationKey.INVENTORY_MODE_USER.getTranslation(locale);
        final String modeRole = TranslationKey.INVENTORY_MODE_ROLE.getTranslation(locale);
        if (modeName == null || modeUser.equals(modeName)) {
            //Get user by name
            guild.retrieveMembersByPrefix(targetName, 2).onSuccess((List<Member> members) -> {
                if (members.isEmpty()) {
                    final String template = TranslationKey.INVENTORY_NO_USER_WITH_NAME.getTranslation(locale);
                    channel.sendMessageFormat(template, targetName).queue();
                    return;
                }
                if (members.size() > 1) {
                    final String template = TranslationKey.INVENTORY_MULTIPLE_USERS_WITH_NAME.getTranslation(locale);
                    channel.sendMessage(template).queue();
                    return;
                }

                //Add items to targets inventory
                final Member target = members.get(0);
                try {
                    if (inventoryManager.updateCount(target, itemName, itemCount)) {
                        if (itemCount > 0) {
                            final String template = TranslationKey.INVENTORY_ITEM_ADDED_SUCCESS.getTranslation(locale);
                            channel.sendMessageFormat(template, itemCount, itemName, target.getEffectiveName()).queue();
                            return;
                        }
                        final String template = TranslationKey.INVENTORY_ITEM_REMOVED_SUCCESS.getTranslation(locale);
                        channel.sendMessageFormat(template, itemCount, itemName, target.getEffectiveName()).queue();
                        return;
                    }
                    channel.sendMessage(TranslationKey.INVENTORY_NOT_ENOUGH_ITEMS.getTranslation(locale)).queue();
                } catch (SQLException e) {
                    channel.sendMessage(TranslationKey.INVENTORY_SQL_ERROR_ON_ADD.getTranslation(locale)).queue();
                }

            }).onError((t) -> {
                channel.sendMessage(TranslationKey.INVENTORY_BOT_NO_PERMISSION.getTranslation(locale)).queue();
            });
            return;
        }
        if (modeRole.equals(modeName)) {
            final List<Role> roles = guild.getRolesByName(targetName, false);
            if (roles.isEmpty()) {
                final String template = TranslationKey.ROLE_NO_ROLE_WITH_NAME.getTranslation(locale);
                channel.sendMessageFormat(template, targetName).queue();
                return;
            }
            if (roles.size() > 1) {
                final String template = TranslationKey.ROLE_MULTIPLE_ROLES_WITH_NAME.getTranslation(locale);
                channel.sendMessage(template).queue();
                return;
            }
            final Role role = roles.get(0);
            guild.findMembers((Member member) -> {
                return member.getRoles().contains(role);
            }).onSuccess((List<Member> members) -> {
                boolean anyFailed = false;
                boolean anyPrevented = false;
                for (final Member member : members) {
                    try {
                        if (!inventoryManager.updateCount(member, itemName, itemCount)) {
                            anyPrevented = true;
                        }
                    } catch (SQLException ex) {
                        anyFailed = true;
                        LOGGER.error("Failure add item to users inventory: {}", ex.getMessage());
                        LOGGER.trace("Stack trace", ex);
                    }
                }
                if (anyFailed) {
                    channel.sendMessage(TranslationKey.INVENTORY_SQL_ERROR_ON_ADD.getTranslation(locale)).queue();
                } else {
                    if (anyPrevented) {
                        channel.sendMessage(TranslationKey.INVENTORY_REMOVE_ROLE_SUCCESS_SOME_NOT_MODIFIED.getTranslation(locale)).queue();
                    } else {
                        final String template = TranslationKey.INVENTORY_ADD_ROLE_SUCCESS.getTranslation(locale);
                        channel.sendMessageFormat(template, itemCount, itemName, members.size()).queue();
                    }
                }
            }).onError((Throwable t) -> {
                channel.sendMessage(TranslationKey.INVENTORY_BOT_NO_PERMISSION.getTranslation(locale)).queue();
            });
            return;
        }
        //Unknown mode
        final String template = TranslationKey.INVENTORY_UNKNOWN_MODE.getTranslation(locale);
        channel.sendMessageFormat(template, modeName).queue();
    }

    private static void payItemToUser(final CommandMatcher message, final GuildDataStore guildData) {
        final TextChannel channel = message.getTextChannel();
        final Guild guild = message.getGuild();
        final Locale locale = guildData.getConfigManager().getLocale();
        final Member requester = message.getMember();
        //pay item count user
        final List<String> args = message.parseArguments(5);
        if (args.size() < 2) {
            channel.sendMessage(TranslationKey.INVENTORY_PAY_MISSING_ITEM_NAME.getTranslation(locale)).queue();
            return;
        }
        final String itemName = args.get(1);
        if (args.size() < 3) {
            channel.sendMessage(TranslationKey.INVENTORY_PAY_MISSING_ITEM_COUNT.getTranslation(locale)).queue();
            return;
        }
        final String countString = args.get(2);
        final long itemCount;
        try {
            itemCount = Long.parseLong(countString);
        } catch (NumberFormatException e) {
            channel.sendMessage(TranslationKey.INVENTORY_COUNT_NOT_NUMBER.getTranslation(locale)).queue();
            return;
        }
        if (itemCount == 0) {
            channel.sendMessage(TranslationKey.INVENTORY_COUNT_ZERO.getTranslation(locale)).queue();
            return;
        }
        if (itemCount < 0) {
            channel.sendMessage(TranslationKey.INVENTORY_COUNT_NEGATIVE.getTranslation(locale)).queue();
            return;
        }

        final InventoryManager inventoryManager = guildData.getInventoryManager();
        final String targetName;
        if (args.size() < 4) {
            channel.sendMessage(TranslationKey.INVENTORY_PAY_USER_MISSING.getTranslation(locale)).queue();
            return;
        }
        targetName = args.get(3);

        final String modeName;
        if (args.size() < 5) {
            modeName = null;
        } else {
            modeName = args.get(4);
        }

        final String modeUser = TranslationKey.INVENTORY_MODE_USER.getTranslation(locale);
        final String modeRole = TranslationKey.INVENTORY_MODE_ROLE.getTranslation(locale);
        if (modeName == null || modeUser.equals(modeName)) {
            //Get user by name
            guild.retrieveMembersByPrefix(targetName, 2).onSuccess((List<Member> members) -> {
                if (members.isEmpty()) {
                    final String template = TranslationKey.INVENTORY_NO_USER_WITH_NAME.getTranslation(locale);
                    channel.sendMessageFormat(template, targetName).queue();
                    return;
                }
                if (members.size() > 1) {
                    final String template = TranslationKey.INVENTORY_MULTIPLE_USERS_WITH_NAME.getTranslation(locale);
                    channel.sendMessage(template).queue();
                    return;
                }

                //Check to make sure target is different from requester
                final Member target = members.get(0);
                if (requester.equals(target)) {
                    channel.sendMessage(TranslationKey.INVENTORY_PAY_TARGET_SELF.getTranslation(locale)).queue();
                    return;
                }

                //Add items to targets inventory
                try {
                    if (!inventoryManager.payItem(requester, target, itemName, itemCount)) {
                        channel.sendMessage(TranslationKey.INVENTORY_USER_NOT_ENOUGH_ITEMS.getTranslation(locale)).queue();
                        return;
                    }
                    final String template = TranslationKey.INVENTORY_ITEM_PAID_SUCCESS.getTranslation(locale);
                    channel.sendMessageFormat(template, itemCount, itemName, target.getEffectiveName()).queue();
                } catch (SQLException e) {
                    channel.sendMessage(TranslationKey.INVENTORY_SQL_ERROR_ON_PAY.getTranslation(locale)).queue();
                }
            }).onError((Throwable t) -> {
                channel.sendMessage(TranslationKey.INVENTORY_BOT_NO_PERMISSION.getTranslation(locale)).queue();
            });
            return;
        }
        if (modeRole.equals(modeName)) {
            final List<Role> roles = guild.getRolesByName(targetName, false);
            if (roles.isEmpty()) {
                final String template = TranslationKey.ROLE_NO_ROLE_WITH_NAME.getTranslation(locale);
                channel.sendMessageFormat(template, targetName).queue();
                return;
            }
            if (roles.size() > 1) {
                final String template = TranslationKey.ROLE_MULTIPLE_ROLES_WITH_NAME.getTranslation(locale);
                channel.sendMessage(template).queue();
                return;
            }
            final Role role = roles.get(0);
            guild.findMembers((Member member) -> {
                return member.getRoles().contains(role);
            }).onSuccess((List<Member> members) -> {
                //Remove items from users inventory
                final long requiredCount = members.size() * itemCount;
                int paidPeople = 0;
                try {
                    final Map<String, Long> userInv = inventoryManager.getUserInventory(requester);
                    if (userInv.getOrDefault(itemName, 0L) < requiredCount) {
                        channel.sendMessage(TranslationKey.INVENTORY_PAY_USER_NOT_ENOUGH_ITEMS_FOR_EVERYONE.getTranslation(locale)).queue();
                        return;
                    }
                    for (int i = 0; i < members.size(); i++) {
                        final Member member = members.get(i);
                        if (inventoryManager.payItem(requester, member, itemName, itemCount)) {
                            paidPeople++;
                            continue;
                        }
                        break;
                    }
                    if (paidPeople < members.size()) {
                        final String template = TranslationKey.INVENTORY_PAY_INTERRUPTED_NOT_ENOUGH_FOR_EVERYONE.getTranslation(locale);
                        final List<Member> unpaid = members.subList(paidPeople, members.size());
                        final String names = unpaid.stream().map(Member::getEffectiveName).collect(Collectors.joining(","));
                        channel.sendMessageFormat(template, names).queue();
                        return;
                    }
                    final String template = TranslationKey.INVENTORY_PAY_ROLE_SUCCESS.getTranslation(locale);
                    channel.sendMessageFormat(template, itemCount, itemName, members.size()).queue();
                } catch (SQLException ex) {
                    final List<Member> unpaid = members.subList(paidPeople, members.size());
                    final String names = unpaid.stream().map(Member::getEffectiveName).collect(Collectors.joining(","));
                    final String template = TranslationKey.INVENTORY_ROLE_SQL_ERROR_ON_PAY.getTranslation(locale);
                    channel.sendMessageFormat(template, names).queue();
                    LOGGER.error("Failure for user to pay items to another: {}", ex.getMessage());
                    LOGGER.trace("Stack trace", ex);
                }
            }).onError((Throwable t) -> {
                channel.sendMessage(TranslationKey.INVENTORY_BOT_NO_PERMISSION.getTranslation(locale)).queue();
            });
            return;
        }
        //Unknown mode
        final String template = TranslationKey.INVENTORY_UNKNOWN_MODE.getTranslation(locale);
        channel.sendMessageFormat(template, modeName).queue();
    }

}
