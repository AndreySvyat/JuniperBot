/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.worker.event.service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import ru.juniperbot.common.worker.configuration.WorkerProperties;
import ru.juniperbot.common.worker.event.DiscordEvent;
import ru.juniperbot.common.worker.event.intercept.EventFilterFactory;
import ru.juniperbot.common.worker.event.intercept.FilterChain;
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ContextEventManagerImpl implements JbEventManager {

    private final List<EventListener> listeners = new ArrayList<>();

    private final Map<Class<?>, EventFilterFactory<?>> filterFactoryMap = new ConcurrentHashMap<>();

    @Autowired
    private WorkerProperties workerProperties;

    @Autowired
    private ContextService contextService;

    @Autowired
    @Qualifier("eventManagerExecutor")
    private TaskExecutor taskExecutor;

    @Override
    public void handle(GenericEvent event) {
        if (workerProperties.getEvents().isAsyncExecution()) {
            try {
                taskExecutor.execute(() -> handleEvent(event));
            } catch (TaskRejectedException e) {
                log.debug("Event rejected: {}", event);
            }
        } else {
            handleEvent(event);
        }
    }

    private void handleEvent(GenericEvent event) {
        try {
            contextService.initContext(event);
            loopListeners(event);
        } catch (Exception e) {
            log.error("Event manager caused an uncaught exception", e);
        } finally {
            contextService.resetContext();
        }
    }

    private void loopListeners(GenericEvent event) {
        if (event instanceof GuildMessageReceivedEvent) {
            dispatchChain(GuildMessageReceivedEvent.class, (GuildMessageReceivedEvent) event);
        }
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Throwable throwable) {
                log.error("One of the EventListeners had an uncaught exception", throwable);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void dispatchChain(Class<T> type, T event) {
        EventFilterFactory<T> factory = (EventFilterFactory<T>) filterFactoryMap.get(type);
        if (factory == null) {
            return;
        }
        FilterChain<T> chain = factory.createChain(event);
        if (chain == null) {
            return;
        }

        try {
            chain.doFilter(event);
        } catch (Exception e) {
            log.error("Could not process filter chain", e);
        }
    }

    private int compareListeners(EventListener first, EventListener second) {
        return getPriority(first) - getPriority(second);
    }

    private int getPriority(EventListener eventListener) {
        return eventListener != null && eventListener.getClass().isAnnotationPresent(DiscordEvent.class)
                ? eventListener.getClass().getAnnotation(DiscordEvent.class).priority()
                : Integer.MAX_VALUE;
    }

    @Override
    public void unregister(Object listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    public void register(Object listener) {
        if (!(listener instanceof EventListener)) {
            throw new IllegalArgumentException("Listener must implement EventListener");
        }
        registerListeners(Collections.singletonList((EventListener) listener));
    }

    @Autowired
    public void registerContext(List<DiscordEventListener> listeners) {
        registerListeners(listeners);
    }

    @Autowired
    public void registerFilterFactories(List<EventFilterFactory> factories) {
        if (CollectionUtils.isNotEmpty(factories)) {
            factories.forEach(e -> filterFactoryMap.putIfAbsent(e.getType(), e));
        }
    }

    private void registerListeners(List<? extends EventListener> listeners) {
        synchronized (this.listeners) {
            Set<EventListener> listenerSet = new HashSet<>(this.listeners);
            listenerSet.addAll(listeners);
            this.listeners.clear();
            this.listeners.addAll(listenerSet);
            this.listeners.sort(this::compareListeners);
        }
    }

    @Override
    public List<Object> getRegisteredListeners() {
        return Collections.unmodifiableList(listeners);
    }
}
