package studio.mevera.imperat.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.CommandSource;

import java.util.Collection;

/**
 * Storage contract for {@link Command} instances owned by an {@code Imperat}.
 * Default implementation is in-memory and keyed by lowercase name + alias;
 * embedders may swap it (persistent/distributed/audit-logging) by passing a
 * custom registry to {@code BaseImperat}.
 *
 * <p>The registry is intentionally minimal: it is a value store, not a
 * decision point. Pre/post-registration events, ambiguity checking, and
 * permission scoping live on the {@code Imperat} layer above. Implementations
 * should be thread-safe iff the embedder calls register/unregister from
 * multiple threads — the default is not.</p>
 *
 * @param <S> the command-source type
 */
public interface CommandRegistry<S extends CommandSource> {

    /**
     * Inserts {@code command} (and all of its aliases + shortcuts) into the
     * registry, replacing any existing entries with the same key.
     */
    void register(@NotNull Command<S> command);

    /**
     * Removes the command bound to {@code name} (and its aliases) from the
     * registry. No-op if absent.
     */
    void unregister(@NotNull String name);

    /**
     * Removes every command from the registry.
     */
    void clear();

    /**
     * Returns the command bound to {@code name} (case-insensitive), or
     * {@code null} if no such command exists. Implementations may search
     * aliases and shortcuts in addition to canonical names.
     */
    @Nullable
    Command<S> get(@NotNull String name);

    /**
     * Returns a snapshot view of every registered command. Multiple keys
     * pointing to the same command (canonical + aliases) collapse to one
     * entry.
     */
    @NotNull
    Collection<? extends Command<S>> values();
}
