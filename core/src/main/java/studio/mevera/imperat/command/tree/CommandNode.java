package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.Source;

@ApiStatus.Internal
public final class CommandNode<S extends Source> extends ParameterNode<S, Command<S>> {
    
    CommandNode(@Nullable ParameterNode<S, ?> parent, @NotNull Command<S> data, int depth, @Nullable CommandUsage<S> usage) {
        super(parent, data, depth, usage);
    }

    boolean isSubCommand() {
        return data.hasParent();
    }

    boolean isRoot() {
        return !isSubCommand();
    }

    @Override
    public boolean matchesInput(String raw) {
        return data.hasName(raw);
    }


    @Override
    public String format() {
        return data.format();
    }

    @Override
    public int priority() {
        return -1;
    }

}
