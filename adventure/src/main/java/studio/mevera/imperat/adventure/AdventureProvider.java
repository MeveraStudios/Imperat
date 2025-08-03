package studio.mevera.imperat.adventure;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.ComponentLike;
import studio.mevera.imperat.context.Source;

/**
 * The {@code AdventureProvider} interface defines a mechanism for obtaining an
 * {@link Audience} based on a given source or context. It is designed to
 * facilitate communication or message broadcasting to a specific audience
 * within the application, typically in a context like a game server or
 * chat system where audiences are dynamically determined.
 *
 * @param <S> the valueType of the source from which the {@link Audience} can be derived
 */
@SuppressWarnings("unchecked")
public interface AdventureProvider<S> {

    /**
     * Obtains an {@link Audience} for the specified source.
     *
     * @param s the source from which the {@link Audience} is derived
     * @return the {@link Audience} corresponding to the given source
     */
    Audience audience(final S s);

    /**
     * Obtains an {@link Audience} for a given {@link Source} instance.
     * This method delegates to {@link #audience(Object)} by retrieving the origin
     * from the provided {@link Source}.
     *
     * @param source the {@link Source} instance from which the {@link Audience} is derived
     * @return the {@link Audience} corresponding to the origin of the provided {@link Source}
     */
    default Audience audience(final Source source) {
        return this.audience((S) source.origin());
    }

    /**
     * Sends a message to the {@link Audience} derived from the specified source.
     *
     * @param sender    the source from which the {@link Audience} is derived
     * @param component the message to be sent, represented by a {@link ComponentLike} instance
     */
    default void send(final S sender, final ComponentLike component) {
        this.audience(sender).sendMessage(component);
    }

    /**
     * Sends a message to the {@link Audience} derived from the specified {@link Source} instance.
     * This method delegates to {@link #send(Object, ComponentLike)} by retrieving the origin
     * from the provided {@link Source}.
     *
     * @param source    the {@link Source} instance from which the {@link Audience} is derived
     * @param component the message to be sent, represented by a {@link ComponentLike} instance
     */
    default void send(final Source source, final ComponentLike component) {
        this.send((S) source.origin(), component);
    }

    /**
     * Performs any necessary cleanup or resource management. By default, this method
     * does nothing, but it can be overridden to provide specific close operations.
     */
    default void close() {
    }

}
