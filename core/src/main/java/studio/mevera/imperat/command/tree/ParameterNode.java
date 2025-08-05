package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Source;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Predicate;

public abstract class ParameterNode<S extends Source, T extends CommandParameter<S>> {

    protected final @NotNull T data;

    protected @Nullable CommandUsage<S> executableUsage;

    private final LinkedList<ParameterNode<S, ?>> nextNodes = new LinkedList<>();

    private final int depth;
    
    private final @Nullable ParameterNode<S, ?> parent;
    
    private String permission;
    
    protected ParameterNode(@Nullable ParameterNode<S, ?> parent, @NotNull T data, int depth, @Nullable CommandUsage<S> executableUsage) {
        this.parent = parent;
        this.data = data;
        this.depth = depth;
        this.executableUsage = executableUsage;
        this.permission = data.getSinglePermission();
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

    public abstract boolean matchesInput(String input);

    public abstract String format();

    public boolean isLast() {
        return nextNodes.isEmpty();
    }

    public abstract int priority();

    public boolean isGreedyParam() {
        return (this instanceof ArgumentNode<?> param) && param.data.isGreedy();
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
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParameterNode<?, ?> that)) return false;
        return Objects.equals(data.name(), that.data.name()) && this.depth == that.depth && Objects.equals(nextNodes, that.nextNodes);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(data.name(), this.depth,  nextNodes);
    }
    
    public @Nullable ParameterNode<S,?> getTopChild() {
        if(nextNodes.isEmpty())return null;
        return nextNodes.getFirst();
    }
   
}
