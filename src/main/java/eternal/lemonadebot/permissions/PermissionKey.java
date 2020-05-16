/*
 * The MIT License
 *
 * Copyright 2020 joonas.
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
package eternal.lemonadebot.permissions;

/**
 *
 * @author joonas
 */
public enum PermissionKey {
    /**
     * Name of permission for using custom commands
     */
    RunCommands(CommandPermission.MEMBER),
    /**
     * Name of permission for editing custom commands
     */
    EditCommands(CommandPermission.ADMIN),
    /**
     * Name of permission for editing events
     */
    EditEvents(CommandPermission.ADMIN),
    /**
     * Name of permission for editing remainders
     */
    EditRemainders(CommandPermission.ADMIN),
    /**
     * Name of permission for playing music
     */
    PlayMusic(CommandPermission.MEMBER),
    /**
     * Name of permission for editing aliases
     */
    EditAliases(CommandPermission.ADMIN);

    private final CommandPermission defaultPermission;

    private PermissionKey(CommandPermission defaultValue) {
        this.defaultPermission = defaultValue;
    }

    /**
     * Get the default permissionlevel
     *
     * @return CommandPermission
     */
    public CommandPermission getDefaultpermission() {
        return this.defaultPermission;
    }

}
