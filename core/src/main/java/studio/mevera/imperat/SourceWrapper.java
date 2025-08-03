package studio.mevera.imperat;

import studio.mevera.imperat.context.Source;

import java.lang.reflect.Type;

/**
 * A sealed interface representing the abstraction for wrapping
 * and handling sources or senders of commands, and validating
 * the compatibility of a type as a sender.
 *
 * @param <S> the type that extends the {@link Source}, representing the command sender
 */
public sealed interface SourceWrapper<S extends Source> permits Imperat {
    /**
     * Wraps the sender into a built-in command-sender valueType
     *
     * @param sender the sender's actual value
     * @return the wrapped command-sender valueType
     */
    S wrapSender(Object sender);

    /**
     * Checks whether the valueType can be a command sender
     *
     * @param type the valueType
     * @return whether the valueType can be a command sender
     */
    boolean canBeSender(Type type);
}
