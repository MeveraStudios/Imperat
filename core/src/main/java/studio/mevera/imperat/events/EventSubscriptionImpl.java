package studio.mevera.imperat.events;

import studio.mevera.imperat.util.Priority;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Package-private implementation of {@link EventSubscription}.
 * Instantiated exclusively by {@link EventBusImpl} during handler registration.
 *
 * @param <T> the exact type of {@link CommandEvent} this subscription handles
 */
record EventSubscriptionImpl<T extends Event>(
        UUID id,
        Consumer<T> handler,
        Priority priority,
        ExecutionStrategy strategy
) implements EventSubscription<T> {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EventSubscription<?> that)) {
            return false;
        }
        return id.equals(that.id());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}