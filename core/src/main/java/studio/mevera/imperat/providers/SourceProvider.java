package studio.mevera.imperat.providers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

/**
 * Resolves a derived view of the canonical command source.
 *
 * <p>Used to customize how a method-parameter type is materialized from the
 * live {@code S} source instance during {@code @Execute} dispatch. Plugin
 * authors register a provider per derived type via
 * {@code ConfigBuilder.sourceProvider(Class, SourceProvider)}.</p>
 *
 * <p>Resolution precedence inside
 * {@code ExecutionContextImpl.provideSource(Type)}:</p>
 * <ol>
 *     <li>{@code clazz.isInstance(source)} — return the source itself
 *         (covers {@code S} and any of its supertypes up to
 *         {@code CommandSource})</li>
 *     <li>Registered {@code SourceProvider} — explicit user override</li>
 *     <li>{@code source.origin()} instance check — default platform-derived
 *         path (e.g. {@code Player}, {@code CommandSender})</li>
 *     <li>{@code ContextArgumentProvider} fallback — domain types</li>
 * </ol>
 *
 * <p>If a {@code SourceProvider} is registered and returns {@code null},
 * resolution falls through to the origin path.</p>
 *
 * @param <S> the canonical source type configured on the {@code Imperat}
 *            instance
 * @param <R> the derived view type this provider produces
 */
@FunctionalInterface
public interface SourceProvider<S extends CommandSource, R> {

    /**
     * Produces the derived view from the canonical source.
     *
     * @param source the live source instance for the current dispatch
     * @return the derived view, or {@code null} to fall through to the
     *         origin-based default path
     * @throws CommandException if resolution fails
     */
    @Nullable
    R provide(@NotNull S source) throws CommandException;
}
