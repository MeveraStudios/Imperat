package studio.mevera.imperat;

import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;

import java.util.Optional;
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
     * Registers a new free flag in the system.
     *
     * @param flagBuilder the {@link FlagData} object containing the data required to define
     *                    and register the flag. This includes the flag's name, aliases,
     *                    description, and any associated logic or behavior.
     */
    void registerFlag(FlagData<S> flagBuilder);

    /**
     * Retrieves a registered flag based on its raw input (e.g., a string provided by the user
     * in a command).
     *
     * @param raw the raw input string that may represent a flag (e.g., {@code --help}, {@code -h}).
     * @return an {@link Optional} containing the {@link FlagData} object if a matching flag is found.
     * If no match is found, the {@link Optional} will be empty.
     */
    Optional<FlagData<S>> getFlagFromRaw(String raw);

    /**
     * Retrieves all registered flags in the system.
     *
     * @return a {@link Set} containing all {@link FlagData} objects that have been registered.
     */
    Set<FlagData<S>> getRegisteredFlags();
}
