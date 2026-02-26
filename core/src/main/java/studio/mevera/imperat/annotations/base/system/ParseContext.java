package studio.mevera.imperat.annotations.base.system;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.base.element.ClassElement;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Mutable context passed through the annotation parsing process.
 * Maintains state for the current command being built and its inheritance chain.
 */
public final class ParseContext<S extends Source> {

    private final Imperat<S> imperat;
    private final ImperatConfig<S> config;

    // Command hierarchy stack (root -> current)
    private final Stack<CommandContext<S>> commandStack = new Stack<>();
    // Collected subcommands to process after parent is complete
    private final List<PendingSubcommand<S>> pendingSubcommands = new ArrayList<>();
    // Error collection for fail-fast
    private final List<ParseError> errors = new ArrayList<>();
    // Current pathway being built
    private CommandPathway.Builder<S> currentPathwayBuilder;
    // Inheritance chain for current method
    private ParameterInheritanceChain<S> currentInheritanceChain = ParameterInheritanceChain.empty();

    public ParseContext(Imperat<S> imperat) {
        this.imperat = imperat;
        this.config = imperat.config();
    }

    // ==================== Command Stack Management ====================

    public void pushCommand(Command<S> command, ClassElement classElement) {
        commandStack.push(new CommandContext<>(command, classElement));
    }

    public void popCommand() {
        if (!commandStack.isEmpty()) {
            commandStack.pop();
        }
    }

    public @NotNull Command<S> currentCommand() {
        if (commandStack.isEmpty()) {
            throw new IllegalStateException("No command in context");
        }
        return commandStack.peek().command();
    }

    public @NotNull CommandContext<S> currentContext() {
        return commandStack.peek();
    }

    public @NotNull List<Command<S>> getAncestorCommands() {
        return new ArrayList<>(commandStack.stream()
                                       .map(CommandContext::command)
                                       .toList());
    }

    public @Nullable Command<S> getParentCommand() {
        if (commandStack.size() < 2) {
            return null;
        }
        return commandStack.get(commandStack.size() - 2).command();
    }

    // ==================== Pathway Building ====================

    public void startPathway(CommandPathway.Builder<S> builder) {
        this.currentPathwayBuilder = builder;
    }

    public CommandPathway.Builder<S> getCurrentPathwayBuilder() {
        return currentPathwayBuilder;
    }


    public CommandPathway<S> buildAndAddPathway() {
        if (currentPathwayBuilder == null) {
            throw new IllegalStateException("No pathway builder active");
        }

        Command<S> cmd = currentCommand();
        CommandPathway<S> pathway = currentPathwayBuilder.build(cmd);

        // Apply inheritance chain if present
        if (!currentInheritanceChain.isEmpty()) {
            // Store inheritance info for tree composition
            currentContext().registerInheritance(pathway, currentInheritanceChain);
        }

        cmd.addPathway(pathway);

        // Reset for next pathway
        currentPathwayBuilder = null;
        currentInheritanceChain = ParameterInheritanceChain.empty();

        return pathway;
    }

    // ==================== Inheritance ====================

    public ParameterInheritanceChain<S> getCurrentInheritanceChain() {
        return currentInheritanceChain;
    }

    public void setCurrentInheritanceChain(ParameterInheritanceChain<S> chain) {
        this.currentInheritanceChain = chain;
    }

    // ==================== Subcommand Management ====================

    public void queueSubcommand(PendingSubcommand<S> subcommand) {
        pendingSubcommands.add(subcommand);
    }

    public List<PendingSubcommand<S>> getPendingSubcommands() {
        return new ArrayList<>(pendingSubcommands);
    }

    public void clearPendingSubcommands() {
        pendingSubcommands.clear();
    }

    // ==================== Error Handling ====================

    public void addError(String message, ParseElement<?> element) {
        errors.add(new ParseError(message, element));
    }

    public void addError(String message, ParseElement<?> element, Throwable cause) {
        errors.add(new ParseError(message, element, cause));
    }

    public void failIfErrors() {
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Command parsing failed with errors:\n");
            for (ParseError error : errors) {
                sb.append("  - ").append(error.message());
                if (error.element() != null) {
                    sb.append(" at ").append(error.element().getName());
                }
                sb.append("\n");
            }
            throw new CommandParseException(sb.toString(), errors);
        }
    }

    // ==================== Accessors ====================

    public Imperat<S> getImperat() {
        return imperat;
    }

    public ImperatConfig<S> getConfig() {
        return config;
    }

    // ==================== Inner Classes ====================

    public static final class CommandContext<S extends Source> {

        private final Command<S> command;
        private final ClassElement classElement;
        private final Map<CommandPathway<S>, ParameterInheritanceChain<S>> inheritanceMap = new HashMap<>();

        public CommandContext(Command<S> command, ClassElement classElement) {
            this.command = command;
            this.classElement = classElement;
        }

        public Command<S> command() {
            return command;
        }

        public ClassElement classElement() {
            return classElement;
        }

        public void registerInheritance(CommandPathway<S> pathway, ParameterInheritanceChain<S> chain) {
            inheritanceMap.put(pathway, chain);
        }

        public ParameterInheritanceChain<S> getInheritance(CommandPathway<S> pathway) {
            return inheritanceMap.getOrDefault(pathway, ParameterInheritanceChain.empty());
        }
    }

    public record PendingSubcommand<S extends Source>(
            Command<S> subcommand,
            ClassElement classElement,
            Command<S> parentCommand
    ) {

    }

    public record ParseError(String message, ParseElement<?> element, Throwable cause) {

        public ParseError(String message, ParseElement<?> element) {
            this(message, element, null);
        }
    }

    public static class CommandParseException extends RuntimeException {

        private final List<ParseError> errors;

        public CommandParseException(String message, List<ParseError> errors) {
            super(message);
            this.errors = errors;
        }

        public List<ParseError> getErrors() {
            return errors;
        }
    }
}