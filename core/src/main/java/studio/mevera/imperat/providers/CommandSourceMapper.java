package studio.mevera.imperat.providers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandSource;

import java.util.function.Function;

/**
 * Bidirectional mapper between a platform's native {@link CommandSource}
 * type {@code P} and a user-supplied subtype {@code S extends P} that the
 * framework treats as canonical.
 *
 * <p>Imperat is parameterized over a single source type {@code S} that
 * flows through every framework site — argument types, suggestion
 * providers, context resolvers, dummy senders. When a plugin author
 * wants their domain-specific source class to be that canonical type
 * (instead of the default platform source), they supply a mapper at
 * config time so the framework can lift platform sources to {@code S}
 * at the wrapping seam.</p>
 *
 * <h2>Bound: {@code S extends P}</h2>
 *
 * <p>The user's source MUST extend the platform source. This makes
 * unwrapping a static cast (no work needed at the call site) and lets
 * every framework internal access platform fields/methods on {@code S}
 * directly via inheritance — no per-site {@code mapper.unwrap(s)} calls
 * for things like {@code s.asPlayer()}, {@code s.origin()}, etc.</p>
 *
 * <h2>Default path (no custom source)</h2>
 *
 * <p>When the user doesn't declare a custom source ({@code S = P}), the
 * builder installs {@link #identity()} which returns its input. Zero
 * runtime cost, no behaviour change.</p>
 *
 * @param <P> the platform's native command-source type
 * @param <S> the user's canonical source type, bound by {@code extends P}
 *
 * @since 4.0.0
 */
public interface CommandSourceMapper<P extends CommandSource, S extends P> {

    /**
     * Identity mapper for the no-custom-source path. Used when the user
     * declares {@code BukkitImperat<BukkitCommandSource>} (or similar)
     * without a {@code .source(...)} call.
     *
     * <p>The unchecked cast is safe because the implementation only ever
     * returns its input; the bound {@code S extends P} guarantees both
     * directions are valid at the type system level when {@code S = P}.</p>
     */
    @SuppressWarnings("unchecked")
    static <P extends CommandSource> @NotNull CommandSourceMapper<P, P> identity() {
        return (CommandSourceMapper<P, P>) IdentityHolder.INSTANCE;
    }

    /**
     * Builds a mapper from a pair of functions. Most plugin authors
     * will use this — the {@code wrap} function constructs the custom
     * source from the platform one, and {@code unwrap} extracts the
     * platform view back (typically just {@code s -> s} since
     * {@code S extends P} makes downcasting unnecessary).
     */
    static <P extends CommandSource, S extends P> @NotNull CommandSourceMapper<P, S> of(
            @NotNull Function<P, S> wrap,
            @NotNull Function<S, P> unwrap
    ) {
        return new CommandSourceMapper<>() {
            @Override
            public @NotNull S wrap(@NotNull P platformSource) {
                return wrap.apply(platformSource);
            }

            @Override
            public @NotNull P unwrap(@NotNull S source) {
                return unwrap.apply(source);
            }
        };
    }

    /**
     * Convenience: builds a mapper given only the wrap direction. The
     * unwrap direction defaults to the inherited identity cast (valid
     * because {@code S extends P}).
     */
    static <P extends CommandSource, S extends P> @NotNull CommandSourceMapper<P, S> wrapping(
            @NotNull Function<P, S> wrap
    ) {
        return wrap::apply;
    }

    /**
     * Lifts a platform-native source into the canonical {@code S} type.
     * Invoked once per dispatch at the framework's wrapping seam.
     */
    @NotNull S wrap(@NotNull P platformSource);

    /**
     * Reduces {@code S} back to its platform-native view. Default
     * implementation is the identity cast since {@code S extends P} is
     * an enforced bound — override only if a wrapping subclass needs
     * to surface a DIFFERENT platform instance than the one held.
     */
    default @NotNull P unwrap(@NotNull S source) {
        return source;
    }

    /**
     * Internal holder so the identity instance is created once and
     * reused across all generic invocations. The raw type lives behind
     * {@link #identity()}'s typed factory.
     */
    @SuppressWarnings("rawtypes")
    final class IdentityHolder {

        static final CommandSourceMapper INSTANCE = new CommandSourceMapper<>() {
            @Override
            public @NotNull CommandSource wrap(@NotNull CommandSource platformSource) {
                return platformSource;
            }
        };

        private IdentityHolder() {
        }
    }
}
