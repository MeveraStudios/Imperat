package studio.mevera.imperat.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.tree.CommandPathSearch;
import studio.mevera.imperat.context.Source;

import java.util.Objects;

@SuppressWarnings("unchecked")
public final class PermissionDeniedException extends ImperatException {
    
    private final String lackingPermission;
    private final CommandUsage<?> usage;
    private final @Nullable CommandParameter<?> targetParameter;
    
    public <S extends Source> PermissionDeniedException(
            @NotNull CommandUsage<S> usage,
            @NotNull String lackingPermission,
            @Nullable CommandParameter<S> targetParameter
    ) {
        super("Lacking permission '" + lackingPermission + "'");
        this.usage = usage;
        this.lackingPermission = lackingPermission;
        this.targetParameter = targetParameter;
    }
    
    public <S extends Source> PermissionDeniedException(CommandPathSearch<S> pathSearch) {
        this(
                pathSearch.getFoundUsage() != null ? pathSearch.getFoundUsage() : pathSearch.getLastCommandNode().getData().getDefaultUsage(),
                Objects.requireNonNull(pathSearch.getLastNode()).getPermission(),
                pathSearch.getLastNode().getData()
        );
    }
    
    public @NotNull String getLackingPermission() {
        return lackingPermission;
    }
    
    public <S extends Source> @NotNull CommandUsage<S> getUsage() {
        return (CommandUsage<S>) usage;
    }
    
    public <S extends Source> @Nullable CommandParameter<S> getInAccessibleParameter() {
        return (CommandParameter<S>) targetParameter;
    }
}
