package studio.mevera.imperat;

import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.FlagData;

import java.util.Set;

/**
 * The {@code FlagRegistrar} interface is responsible for managing the registration and retrieval
 * of free flags in the Imperat command framework. Free flags are flags that can be used anywhere
 * in the command syntax, without being tied to a specific position or index.
 *
 * @param <S> the type of source that extends the {@code CommandSource} class, representing the origin
 *            or context of the command (e.g., a user, a system, etc.).
 */
public interface FlagRegistrar<S extends CommandSource> {

    /**
     * Retrieves all registered flags in the system.
     *
     * @return a {@link Set} containing all {@link FlagData} objects that have been registered.
     */
    Set<FlagArgument<S>> getRegisteredFlags();

    default boolean isFlagRegistered(String flagName) {
        return getRegisteredFlags().stream()
                       .anyMatch(flag -> flag.getName().equalsIgnoreCase(flagName) ||
                                                 flag.flagData().aliases().stream()
                                                         .anyMatch(alias -> alias.equalsIgnoreCase(flagName)));
    }

}
