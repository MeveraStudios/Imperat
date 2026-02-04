package studio.mevera.imperat;

import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;

import java.util.Set;

/**
 * The {@code FlagRegistrar} interface is responsible for managing the registration and retrieval
 * of free flags in the Imperat command framework. Free flags are flags that can be used anywhere
 * in the command syntax, without being tied to a specific position or index.
 *
 * @param <S> the type of source that extends the {@code Source} class, representing the origin
 *            or context of the command (e.g., a user, a system, etc.).
 */
public interface FlagRegistrar<S extends Source> {

    /**
     * Retrieves all registered flags in the system.
     *
     * @return a {@link Set} containing all {@link FlagData} objects that have been registered.
     */
    Set<FlagParameter<S>> getRegisteredFlags();

    default boolean isFlagRegistered(String flagName) {
        return getRegisteredFlags().stream()
                       .anyMatch(flag -> flag.name().equalsIgnoreCase(flagName) ||
                                                 flag.flagData().aliases().stream()
                                                         .anyMatch(alias -> alias.equalsIgnoreCase(flagName)));
    }

}
