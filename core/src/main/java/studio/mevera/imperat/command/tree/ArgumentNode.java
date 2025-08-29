package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.context.Source;

@ApiStatus.Internal
public final class ArgumentNode<S extends Source> extends ParameterNode<S, CommandParameter<S>> {

    private final int priority;
    ArgumentNode(@Nullable ParameterNode<S, ?> parent, @NotNull CommandParameter<S> data, int depth, @Nullable CommandUsage<S> usage) {
        super(parent, data, depth, usage);
        priority = loadPriority(data);
    }
    private static <S extends Source> int loadPriority(CommandParameter<S> commandParameter) {
        int res = 1;
        if(commandParameter.isOptional()) {
            res = commandParameter.isFlag() ? 2 : 3;
        }
        return res;
    }
    
    @Override
    public String format() {
        return data.format();
    }

    @Override
    public int priority() {
        return priority;
    }
    
    @Override
    public boolean isGreedyParam() {
        ParameterType<S, ?> type = this.getPrimaryType();
        if(type != null)
            System.out.println("type: " + type.type().getTypeName() + " isGreedy: " + type.isGreedy(data));
        return type != null && type.isGreedy(data);
    }
    
}
