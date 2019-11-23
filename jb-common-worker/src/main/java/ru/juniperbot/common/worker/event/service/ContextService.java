/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.worker.event.service;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.requests.RestAction;

import java.awt.*;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ContextService {

    void setLocale(Locale locale);

    void setColor(Color color);

    Locale getLocale();

    Color getColor();

    Color getDefaultColor();

    Locale getLocale(String localeName);

    Locale getLocale(Guild guild);

    Locale getLocale(long guildId);

    void initContext(GenericEvent event);

    void initContext(Guild guild);

    <T> T withContext(Long guildId, Supplier<T> action);

    <T> T withContext(Guild guildId, Supplier<T> action);

    void withContext(Long guildId, Runnable action);

    void withContext(Guild guild, Runnable action);

    void withContextAsync(Guild guild, Runnable action);

    void initContext(User user);

    void initContext(long guildId);

    void resetContext();

    <T> void queue(Guild guild, RestAction<T> action, Consumer<T> success);

    <T> void queue(Long guildId, RestAction<T> action, Consumer<T> success);
}
