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

    @Override
    public String format() {
        return data.format();
    }

    @Override
    public int priority() {
        return -1;
    }
    
    public CommandNode<S> copy() {
        return new CommandNode<>(this.getParent(), this.data, getDepth(), executableUsage);
    }
}
