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
package eternal.lemonadebot.customcommands;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

/**
 * Cache for custom commands
 *
 * @author Neutroni
 */
public class TemplateCache extends TemplateManager {

    private boolean templatesLoaded = false;
    private final Map<String, CustomCommand> commands = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param ds Database connection to use
     * @param guildID guild to store templates for
     */
    public TemplateCache(DataSource ds, long guildID) {
        super(ds, guildID);
    }

    @Override
    boolean addCommand(CustomCommand command) throws SQLException {
        this.commands.putIfAbsent(command.getName(), command);
        return super.addCommand(command);
    }

    @Override
    boolean removeCommand(CustomCommand command) throws SQLException {
        this.commands.remove(command.getName());
        return super.removeCommand(command);
    }

    @Override
    public Optional<CustomCommand> getCommand(String name) throws SQLException {
        final CustomCommand command = this.commands.get(name);
        if (templatesLoaded) {
            return Optional.ofNullable(command);
        }
        if (command == null) {
            final Optional<CustomCommand> optCommand = super.getCommand(name);
            optCommand.ifPresent((CustomCommand t) -> {
                this.commands.put(t.getName(), t);
            });
            return optCommand;
        }
        return Optional.of(command);
    }

    @Override
    Collection<CustomCommand> getCommands() throws SQLException {
        if (templatesLoaded) {
            return Collections.unmodifiableCollection(this.commands.values());
        }
        final Collection<CustomCommand> templates = super.getCommands();
        templates.forEach((CustomCommand t) -> {
            this.commands.putIfAbsent(t.getTemplate(), t);
        });
        templatesLoaded = true;
        return templates;
    }

}
