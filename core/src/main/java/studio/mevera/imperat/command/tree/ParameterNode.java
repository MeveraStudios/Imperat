package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.PriorityList;

import java.util.Objects;
import java.util.function.Predicate;

public abstract class ParameterNode<S extends Source, T extends CommandParameter<S>> implements Comparable<ParameterNode<S, ?>> {

    protected final @NotNull T data;
    private final PriorityList<ParameterNode<S, ?>> children = new PriorityList<>();
    private final int depth;
    private final @Nullable ParameterNode<S, ?> parent;
    protected @Nullable CommandUsage<S> executableUsage;

    protected ParameterNode(@Nullable ParameterNode<S, ?> parent, @NotNull T data, int depth, @Nullable CommandUsage<S> executableUsage) {
        this.parent = parent;
        this.data = data;
        this.depth = depth;
        this.executableUsage = executableUsage;
    }

    public static <S extends Source> CommandNode<S> createCommandNode(
            @Nullable ParameterNode<S, ?> parent,
            @NotNull Command<S> data,
            int depth,
            @Nullable CommandUsage<S> executableUsage
    ) {
        return new CommandNode<>(parent, data, depth, executableUsage);
    }

    public static <S extends Source> ArgumentNode<S> createArgumentNode(
            ParameterNode<S, ?> parent,
            CommandParameter<S> data,
            int depth,
            @Nullable CommandUsage<S> executableUsage
    ) {
        return new ArgumentNode<>(parent, data, depth, executableUsage);
    }

    public int getDepth() {
        return depth;
    }

    public @Nullable CommandUsage<S> getExecutableUsage() {
        return executableUsage;
    }

    public void setExecutableUsage(@Nullable CommandUsage<S> executableUsage) {
        this.executableUsage = executableUsage;
    }

    public boolean isExecutable() {
        return this.executableUsage != null;
    }

    @NotNull
    public T getData() {
        return data;
    }

    public void addChild(ParameterNode<S, ?> node) {
        if (children.contains(node)) {
            return;
        }
        children.add(node.priority(), node);
    }

    public PriorityList<ParameterNode<S, ?>> getChildren() {
        return children;
    }

    public boolean matchesInput(int depth, Context<S> ctx) {
        // Check supported types in LIFO order
        return matchesInput(depth, ctx, false);
    }

    public boolean matchesInput(int depth, Context<S> ctx, boolean strict) {
        var primaryType = data.type();
        boolean primaryMatches = matchesInput(primaryType, depth, ctx);

        if (strict || isCommand()) {
            return primaryMatches;
        }

        if (primaryMatches) {
            return true;
        }

        ParameterNode<S, ?> siblingMatchingInput = findNeighborOfType(depth, ctx);
        //System.out.println("SIBLING FOUND: " + siblingMatchingInput.data.name());
        return siblingMatchingInput == null;//if no sibling matches this, this one MUST match
    }

    private @Nullable ParameterNode<S, ?> findNeighborOfType(int depth, Context<S> context) {
        if (parent == null) {
            return null;
        }
        for (var sibling : parent.getChildren()) {
            if (sibling.equals(this)) {
                continue;
            }
            if (sibling.matchesInput(depth, context, true)) {
                return sibling;
            }
        }
        return null;
    }

    private boolean matchesInput(ParameterType<S, ?> type, int depth, Context<S> ctx) {
        return type.matchesInput(depth, ctx, data);
    }


    public abstract String format();

    public boolean isLast() {
        return children.isEmpty();
    }

    public abstract Priority priority();

    public boolean isGreedyParam() {
        return data.isGreedy();
    }

    public boolean isOptional() {
        return (this instanceof ArgumentNode<?> param) && param.data.isOptional();
    }

    public @Nullable ParameterNode<S, ?> getChild(Predicate<ParameterNode<S, ?>> predicate) {
        for (var child : getChildren()) {
            if (predicate.test(child)) {
                return child;
            }
        }
        return null;
    }

    public ParameterNode<S, ?> getNextCommandChild() {
        return getChild((child) -> child instanceof CommandNode<?>);
    }

    public ParameterNode<S, ?> getNextParameterChild() {
        return getChild((child) -> true);
    }

    public boolean isRequired() {
        return data.isRequired();
    }

    public boolean isCommand() {
        return this instanceof CommandNode || data.isCommand();
    }

    public boolean isTrueFlag() {
        return this.data.isFlag() && !this.data.asFlagParameter().isSwitch();
    }

    public boolean isFlag() {
        return this.data.isFlag();
    }

    public @Nullable ParameterNode<S, ?> getParent() {
        return parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public int getNumberOfParametersToConsume() {
        int incrementation = this.data.type().getNumberOfParametersToConsume();
        if (incrementation < 1) {
            incrementation = 1;
        }
        return incrementation;
    }

    public PermissionsData getPermissionsData() {
        return data.getPermissionsData();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParameterNode<?, ?> that)) {
            return false;
        }
        return Objects.equals(this.parent, that.parent) && Objects.equals(data.name(), that.data.name()) && this.depth == that.depth
                       && Objects.equals(
                children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.parent, data.name(), this.depth, children);
    }

    @Override
    public int compareTo(@NotNull ParameterNode<S, ?> o) {
        //the highest priority comes first
        return this.priority().compareTo(o.priority());
    }
}
