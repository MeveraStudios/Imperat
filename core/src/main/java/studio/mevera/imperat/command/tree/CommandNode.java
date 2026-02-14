package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.PriorityList;

import java.util.Objects;
import java.util.function.Predicate;

public abstract class CommandNode<S extends Source, T extends Argument<S>> implements Comparable<CommandNode<S, ?>> {

    protected final @NotNull T data;
    private final PriorityList<CommandNode<S, ?>> children = new PriorityList<>();
    private final int depth;
    private final @Nullable CommandNode<S, ?> parent;
    protected @Nullable CommandUsage<S> executableUsage;

    protected CommandNode(@Nullable CommandNode<S, ?> parent, @NotNull T data, int depth, @Nullable CommandUsage<S> executableUsage) {
        this.parent = parent;
        this.data = data;
        this.depth = depth;
        this.executableUsage = executableUsage;
    }

    public static <S extends Source> LiteralCommandNode<S> createCommandNode(
            @Nullable CommandNode<S, ?> parent,
            @NotNull Command<S> data,
            int depth,
            @Nullable CommandUsage<S> executableUsage
    ) {
        return new LiteralCommandNode<>(parent, data, depth, executableUsage);
    }

    public static <S extends Source> ArgumentNode<S> createArgumentNode(
            CommandNode<S, ?> parent,
            Argument<S> data,
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

    public void addChild(CommandNode<S, ?> node) {
        if (children.contains(node)) {
            return;
        }
        children.add(node.priority(), node);
    }

    public PriorityList<CommandNode<S, ?>> getChildren() {
        return children;
    }

    public boolean matchesInput(int depth, Context<S> ctx) {
        // Check supported types in LIFO order
        return matchesInput(depth, ctx, false);
    }

    public boolean matchesInput(int depth, Context<S> ctx, boolean strict) {
        System.out.println("Node= " + this.data.name() + ", strict= " + strict);
        var primaryType = data.type();
        boolean primaryMatches = matchesInput(primaryType, depth, ctx);

        if (strict || isLiteral()) {
            return primaryMatches;
        }

        if (primaryMatches) {
            return true;
        }

        CommandNode<S, ?> siblingMatchingInput = findNeighborOfType(depth, ctx);
        //System.out.println("SIBLING FOUND: " + siblingMatchingInput.data.name());
        return siblingMatchingInput == null;//if no sibling matches this, this one MUST match
    }

    private @Nullable CommandNode<S, ?> findNeighborOfType(int depth, Context<S> context) {
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

    private boolean matchesInput(ArgumentType<S, ?> type, int depth, Context<S> ctx) {
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

    public @Nullable CommandNode<S, ?> getChild(Predicate<CommandNode<S, ?>> predicate) {
        for (var child : getChildren()) {
            if (predicate.test(child)) {
                return child;
            }
        }
        return null;
    }

    public CommandNode<S, ?> getNextCommandChild() {
        return getChild((child) -> child instanceof LiteralCommandNode<?>);
    }

    public CommandNode<S, ?> getNextParameterChild() {
        return getChild((child) -> true);
    }

    public boolean isRequired() {
        return data.isRequired();
    }

    public boolean isLiteral() {
        return this instanceof LiteralCommandNode || data.isCommand();
    }

    public boolean isTrueFlag() {
        return this.data.isFlag() && !this.data.asFlagParameter().isSwitch();
    }

    public boolean isFlag() {
        return this.data.isFlag();
    }

    public @Nullable CommandNode<S, ?> getParent() {
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
        if (!(o instanceof CommandNode<?, ?> that)) {
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
    public int compareTo(@NotNull CommandNode<S, ?> o) {
        //the highest priority comes first
        return this.priority().compareTo(o.priority());
    }
}
