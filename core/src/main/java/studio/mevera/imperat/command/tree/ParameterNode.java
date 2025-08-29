package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.command.parameters.type.ParameterTypes;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;

import java.util.*;
import java.util.function.Predicate;

public abstract class ParameterNode<S extends Source, T extends CommandParameter<S>> {

    protected final @NotNull T data;

    protected @Nullable CommandUsage<S> executableUsage;

    private final LinkedList<ParameterNode<S, ?>> nextNodes = new LinkedList<>();
    
    //we want a LIFO structure here
    private final Deque<ParameterType<S, ?>> supportedTypes = new ArrayDeque<>();
    
    private final int depth;
    
    private final @Nullable ParameterNode<S, ?> parent;
    
    private String permission;
    
    private final static ParameterType<?, ?> DEFAULT_PARAM_TYPE = ParameterTypes.string();
    
    protected ParameterNode(@Nullable ParameterNode<S, ?> parent, @NotNull T data, int depth, @Nullable CommandUsage<S> executableUsage) {
        this.parent = parent;
        this.data = data;
        this.depth = depth;
        this.executableUsage = executableUsage;
        this.permission = data.getSinglePermission();
        
        if(!isCommand() && !isFlag()) {
            supportedTypes.add((ParameterType<S, ?>) DEFAULT_PARAM_TYPE);
        }
        
        if(!data.type().equalsExactly(DEFAULT_PARAM_TYPE.type())) {
            supportedTypes.add(data.type());
        }
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
            ParameterNode<S,?> parent,
            CommandParameter<S> data,
            int depth,
            @Nullable CommandUsage<S> executableUsage
    ) {
        return new ArgumentNode<>(parent, data, depth, executableUsage);
    }
    
    public String getPermission() {
        return permission;
    }
    
    public void setPermission(String permission) {
        this.permission = permission;
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
        if (nextNodes.contains(node)) return;
        
        int newNodePriority = node.priority();
        
        // Fast path for empty list
        if (nextNodes.isEmpty()) {
            nextNodes.add(node);
            return;
        }
        
        // Fast path for highest priority (commands) - add to front
        if (newNodePriority < nextNodes.getFirst().priority()) {
            nextNodes.addFirst(node);
            return;
        }
        
        // Fast path for lowest priority - add to end
        if (newNodePriority >= nextNodes.getLast().priority()) {
            nextNodes.addLast(node);
            return;
        }
        
        // Find insertion point using ListIterator for efficient insertion
        ListIterator<ParameterNode<S, ?>> iterator = nextNodes.listIterator();
        while (iterator.hasNext()) {
            ParameterNode<S, ?> existingNode = iterator.next();
            if (newNodePriority < existingNode.priority()) {
                iterator.previous(); // Step back
                iterator.add(node);  // Insert before current position
                return;
            }
        }
    }

    public LinkedList<ParameterNode<S,?>> getChildren() {
        return nextNodes;
    }
    
    public boolean matchesInput(int depth, Context<S> ctx) {
        // Check supported types in LIFO order
        return matchesInput(depth, ctx, false);
    }
    
    public boolean matchesInput(int depth, Context<S> ctx, boolean strict) {
        if(strict) {
            //it must be strict in a specific scenario
            //REGARDING complex middle optional arguments resolvation
            //we get the primary param type.
            var primaryType = getPrimaryType();
            return primaryType != null && matchesInput(primaryType, depth, ctx);
        }
        // Check supported types in LIFO order
        var iterator = supportedTypes.descendingIterator();
        while (iterator.hasNext()) {
            var type = iterator.next();
            if (matchesInput(type, depth, ctx)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean matchesInput(ParameterType<S, ?> type, int depth, Context<S> ctx) {
        return type.matchesInput(depth, ctx, data);
    }
    
    public void addSupportedType(ParameterType<S, ?> type) {
        if (!supportedTypes.contains(type)) {
            supportedTypes.add(type);
        }
    }
    
    //remove
    public void removeSupportedType(ParameterType<S, ?> type) {
        supportedTypes.remove(type);
    }
    
    public Deque<ParameterType<S, ?>> getSupportedTypes() {
        return supportedTypes;
    }
    
    public @Nullable ParameterType<S, ?> getPrimaryType() {
        return supportedTypes.peekLast();
    }
    
    public abstract String format();

    public boolean isLast() {
        return nextNodes.isEmpty();
    }

    public abstract int priority();

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
    
    
    public @Nullable ParameterNode<S,?> getTopChild() {
        if(nextNodes.isEmpty())return null;
        return nextNodes.getFirst();
    }
    
    public boolean isRoot() {
        return parent == null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParameterNode<?, ?> that)) return false;
        return Objects.equals(this.parent, that.parent) && Objects.equals(data.name(), that.data.name()) && this.depth == that.depth && Objects.equals(nextNodes, that.nextNodes);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.parent, data.name(), this.depth,  nextNodes);
    }

}
