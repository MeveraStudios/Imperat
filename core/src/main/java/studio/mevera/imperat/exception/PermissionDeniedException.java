package studio.mevera.imperat.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.tree.CommandPathSearch;
import studio.mevera.imperat.context.Source;

@SuppressWarnings("unchecked")
public final class PermissionDeniedException extends CommandException {

    private final CommandUsage<?> usage;
    private final @Nullable CommandParameter<?> targetParameter;

    public <S extends Source> PermissionDeniedException(
            @NotNull CommandUsage<S> usage,
            @Nullable CommandParameter<S> targetParameter
    ) {
        super("Insufficient permissions to execute this command" + (targetParameter != null ? " due to parameter: " + targetParameter.name() : ""));
        this.usage = usage;
        this.targetParameter = targetParameter;
    }

    public <S extends Source> PermissionDeniedException(CommandPathSearch<S> pathSearch) {
        this(
                pathSearch.getFoundUsage() != null ? pathSearch.getFoundUsage() : pathSearch.getLastCommandNode().getData().getDefaultUsage(),
                pathSearch.getLastNode().getData()
        );
    }

    public <S extends Source> @NotNull CommandUsage<S> getUsage() {
        return (CommandUsage<S>) usage;
    }

    /**
     * if the permission denial is caused by a parameter/root-command, this will return the parameter that caused it, otherwise its caused by the
     * usage's personal permission condition and this will return null.
     * @see studio.mevera.imperat.permissions.PermissionsData
     * @return the parameter that caused the permission denial, if any
     */
    public <S extends Source> @Nullable CommandParameter<S> getInAccessibleParameter() {
        return (CommandParameter<S>) targetParameter;
    }
}
