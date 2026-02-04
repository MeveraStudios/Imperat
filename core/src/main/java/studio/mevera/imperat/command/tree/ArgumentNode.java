package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.Priority;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.context.Source;

@ApiStatus.Internal
public final class ArgumentNode<S extends Source> extends ParameterNode<S, CommandParameter<S>> {

    private final Priority priority;
    ArgumentNode(@Nullable ParameterNode<S, ?> parent, @NotNull CommandParameter<S> data, int depth, @Nullable CommandUsage<S> usage) {
        super(parent, data, depth, usage);
        priority = Priority.of(loadPriority(data));
    }

    private static <S extends Source> int loadPriority(CommandParameter<S> commandParameter) {
        int base = commandParameter.type().getPriority().getLevel();
        int res = 5;

        if(commandParameter.isFlag()) {
            res--;
        }else if(commandParameter.isOptional()) {
            res-=2;
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
        ParameterType<S, ?> type = this.data.type();
        return type.isGreedy(data);
    }
    
}
