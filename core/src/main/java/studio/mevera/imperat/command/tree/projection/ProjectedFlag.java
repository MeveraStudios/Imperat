package studio.mevera.imperat.command.tree.projection;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.context.CommandSource;

import java.util.List;

/**
 * Flat, platform-neutral description of a single flag that is reachable at a
 * given {@link ProjectedNode} scope. Unlike the runtime
 * {@link FlagArgument}, this projection exposes the flag's name plus all of
 * its aliases as a single ordered list, and remembers the pathway that owns
 * the flag so platform layers can re-enter the framework's permission /
 * suggestion machinery without re-deriving the scope.
 *
 * @param <S> the command-source type
 */
public final class ProjectedFlag<S extends CommandSource> {

    private final FlagArgument<S> flag;
    private final List<String> aliases; // includes the canonical name at index 0
    private final boolean isSwitch;
    private final CommandPathway<S> owningPathway;

    ProjectedFlag(@NotNull FlagArgument<S> flag,
            @NotNull List<String> aliases,
            boolean isSwitch,
            @NotNull CommandPathway<S> owningPathway) {
        this.flag = flag;
        this.aliases = List.copyOf(aliases);
        this.isSwitch = isSwitch;
        this.owningPathway = owningPathway;
    }

    public @NotNull FlagArgument<S> flag() {
        return flag;
    }

    /** All flag names — canonical at index 0, aliases following. */
    public @NotNull List<String> aliases() {
        return aliases;
    }

    /** Convenience: the canonical name (always {@code aliases().get(0)}). */
    public @NotNull String name() {
        return aliases.get(0);
    }

    public boolean isSwitch() {
        return isSwitch;
    }

    /**
     * The pathway that registered this flag — used by consumers to check
     * permission visibility against the same pathway the framework would.
     */
    public @NotNull CommandPathway<S> owningPathway() {
        return owningPathway;
    }
}
