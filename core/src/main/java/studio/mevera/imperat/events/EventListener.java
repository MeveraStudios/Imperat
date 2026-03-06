package studio.mevera.imperat.events;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.util.Priority;

import java.util.UUID;

public interface EventListener {

    <E extends Event> void listen(
            @NotNull Class<E> eventType,
            @NotNull EventListenerConsumer<E> handler,
            @NotNull Priority priority,
            @NotNull ExecutionStrategy strategy
    );

    default <E extends Event> void listen(
            @NotNull Class<E> eventType,
            @NotNull EventListenerConsumer<E> handler,
            @NotNull Priority priority
    ) {
        listen(eventType, handler, priority, ExecutionStrategy.SYNC);
    }

    default <E extends Event> void listen(
            @NotNull Class<E> eventType,
            @NotNull EventListenerConsumer<E> handler
    ) {
        listen(eventType, handler, Priority.NORMAL, ExecutionStrategy.SYNC);
    }

    boolean removeListener(@NotNull UUID subscriptionId);

}
