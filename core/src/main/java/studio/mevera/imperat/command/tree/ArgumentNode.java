package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;

@ApiStatus.Internal
public final class ArgumentNode<S extends Source> extends CommandNode<S, Argument<S>> {

    private final Priority priority;

    ArgumentNode(@Nullable CommandNode<S, ?> parent, @NotNull Argument<S> data, int depth, @Nullable CommandPathway<S> usage) {
        super(parent, data, depth, usage);
        priority = Priority.of(loadPriority(data));
    }

    private static <S extends Source> int loadPriority(Argument<S> Argument) {
        int base = Argument.type().priority().getLevel();
        int res = 5;

        if (Argument.isFlag()) {
            res--;
        } else if (Argument.isOptional()) {
            res -= 2;
        }

        return base + res;
    }

    @Override
    public String format() {
        return data.format();
    }

    @Override
    public Priority priority() {
        return priority;
    }

    @Override
    public boolean isGreedyParam() {
        ArgumentType<S, ?> type = this.data.type();
        return type.isGreedy(data);
    }

}
