package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.Source;

import java.util.LinkedList;
import java.util.Queue;

public final class CommandPathSearch<S extends Source> {
    
    private final CommandNode<S> root;
    
    private @NotNull ParameterNode<S, ?> lastNode;
    private @NotNull ParameterNode<S, ?> lastCommandNode;
    
    private CommandUsage<S> directUsage, closestUsage;

    private Result result;
    
    private CommandPathSearch(@NotNull CommandNode<S> root, Result result) {
        this.root = root;
        this.lastCommandNode = root;
        this.lastNode = lastCommandNode;
        this.result = result;
    }
    
    private CommandPathSearch(Result result, @NotNull CommandNode<S> root, @NotNull ParameterNode<S, ?> lastNode, CommandUsage<S> directUsage) {
        this.root = root;
        this.result = result;
        this.lastNode = lastNode;
        this.directUsage = directUsage;
        this.lastCommandNode = root;
    }
    
    public static <S extends Source> CommandPathSearch<S> of(CommandNode<S> root, final Result result) {
        return new CommandPathSearch<>(root, result);
    }

    public static <S extends Source> CommandPathSearch<S> unknown(CommandNode<S> root) {
        return of(root, Result.UNKNOWN);
    }
    
    public static <S extends Source> CommandPathSearch<S> freshlyNew(Command<S> command) {
        CommandPathSearch<S> dispatch = of(command.tree().rootNode(), Result.UNKNOWN);
        dispatch.append(command.tree().rootNode());
        dispatch.setDirectUsage(command.getDefaultUsage());
        return dispatch;
    }
    
    public static <S extends Source> CommandPathSearch<S> paused(Command<S> command) {
        CommandPathSearch<S> dispatch = of(command.tree().rootNode(), Result.PAUSE);
        Command<S> target = null;
        if(command.tree() != null) {
            target = command;
        }else {
            Command<S> root = command;
            while ( root != null) {
                
                if(root.parent() == null) {
                    break;
                }
                root = root.parent();
            }
            if(root != null && root != command) {
                target = root;
            }
        }
        if(target != null) {
            dispatch.append(target.tree().rootNode());
            dispatch.setDirectUsage(target.getDefaultUsage());
        }

        return dispatch;
    }
    
    public void append(ParameterNode<S, ?> node) {
        if (node == null) return;
        if(node.isCommand()) {
            this.lastCommandNode = node;
        }
        this.lastNode = node;
    }

    public @NotNull ParameterNode<S, ?> getLastNode() {
        return lastNode;
    }
    
    public @Nullable CommandUsage<S> getFoundUsage() {
        return directUsage;
    }

    public Result getResult() {
        return result;
    }
    
    public CommandUsage<S> getClosestUsage() {
        if(closestUsage == null) {
            //lazy computation, only when demanded for better performance :D
            closestUsage = computeClosestUsage();
        }
        
        return closestUsage;
    }
    
    public void setResult(Result result) {
        this.result = result;
    }
    
    public void setDirectUsage(CommandUsage<S> directUsage) {
        this.directUsage = directUsage;
    }
    
    private CommandUsage<S> computeClosestUsage() {
        if(directUsage != null && lastNode.isLast()) {
            return directUsage;
        }
        
        return (closestUsage = closestUsageLookup());
    }
    
    private CommandUsage<S> closestUsageLookup() {
        CommandUsage<S> closestUsage = null;

        Queue<ParameterNode<S, ?>> nodes = new LinkedList<>();
        nodes.add(lastNode);

        ParameterNode<S, ?> curr;
        while (!nodes.isEmpty()) {
            curr = nodes.poll();
            if(!curr.isCommand() && curr.isExecutable()) {
                closestUsage = curr.getExecutableUsage();
                nodes.clear();
                break;
            }

            for(ParameterNode<S, ?> child : curr.getChildren()) {
                nodes.add(child);
            }
        }
        
        if(closestUsage == null) {
            //if its still null, then let's go back to the last cmd node
            if(lastCommandNode.isExecutable()) {
                closestUsage = lastCommandNode.getExecutableUsage();
            }else {
                closestUsage = ((CommandNode<S>)lastCommandNode).getData().getDefaultUsage();
            }
        }
        
        return closestUsage;
    }
    
    public CommandPathSearch<S> copy() {
        return new CommandPathSearch<>(result, root, lastNode, directUsage);
    }
    
    public @NotNull CommandNode<S> getLastCommandNode() {
        return (CommandNode<S>) lastCommandNode;
    }
    
    /**
     * Defines a setResult from dispatching the command execution.
     */
    public enum Result {
        
        /**
         * The tree stopped midway for some reason,
         * most probably would be that the source doesn't have access
         * to a {@link ParameterNode} that matches his corresponding input.
         */
        PAUSE,
        
        /**
         * Defines a complete dispatch of the command,
         * {@link CommandUsage} cannot be null unless the {@link StandardCommandTree} has issues
         */
        COMPLETE,
        
        /**
         * Defines an unknown execution/command, it's the default setResult
         */
        UNKNOWN,
        
        /**
         * Defines an execution that ended up with throwing an exception that had no handler
         */
        FAILURE;
        
        public boolean isStoppable() {
            return this == COMPLETE || this == PAUSE;
        }
    }
}
