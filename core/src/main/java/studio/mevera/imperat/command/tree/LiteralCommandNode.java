package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;

@ApiStatus.Internal
public final class LiteralCommandNode<S extends Source> extends CommandNode<S, Command<S>> {

    LiteralCommandNode(@Nullable CommandNode<S, ?> parent, @NotNull Command<S> data, int depth, @Nullable CommandPathway<S> usage) {
        super(parent, data, depth, usage);
    }

    @Override
    public String format() {
        return data.format();
    }

    @Override
    public Priority priority() {
        return Priority.MAXIMUM;
    }

    public LiteralCommandNode<S> copy() {
        return new LiteralCommandNode<>(this.getParent(), this.data, getDepth(), executableUsage);
    }
}
