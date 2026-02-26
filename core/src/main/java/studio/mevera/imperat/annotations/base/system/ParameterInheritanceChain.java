package studio.mevera.imperat.annotations.base.system;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a chain of inherited parameters from ancestor commands.
 * Immutable and computed once per pathway.
 */
public final class ParameterInheritanceChain<S extends Source> {

    private final List<InheritedParameter<S>> chain;
    private final CommandPathway<S> sourcePathway;
    private final Command<S> sourceCommand;

    private ParameterInheritanceChain(
            List<InheritedParameter<S>> chain,
            CommandPathway<S> sourcePathway,
            Command<S> sourceCommand
    ) {
        this.chain = List.copyOf(chain);
        this.sourcePathway = sourcePathway;
        this.sourceCommand = sourceCommand;
    }

    public static <S extends Source> ParameterInheritanceChain<S> empty() {
        return new ParameterInheritanceChain<>(List.of(), null, null);
    }

    public static <S extends Source> Builder<S> builder() {
        return new Builder<>();
    }

    public List<InheritedParameter<S>> getChain() {
        return chain;
    }

    public boolean isEmpty() {
        return chain.isEmpty();
    }

    public int size() {
        return chain.size();
    }

    public @Nullable CommandPathway<S> getSourcePathway() {
        return sourcePathway;
    }

    public @Nullable Command<S> getSourceCommand() {
        return sourceCommand;
    }

    /**
     * Gets the combined parameter list: inherited params + personal params
     */
    public List<Argument<S>> combineWithPersonalParameters(List<Argument<S>> personalParams) {
        List<Argument<S>> combined = new ArrayList<>(chain.size() + personalParams.size());
        for (InheritedParameter<S> inherited : chain) {
            combined.add(inherited.getArgument());
        }
        combined.addAll(personalParams);
        return combined;
    }

    /**
     * Validates that personal parameters don't conflict with inherited ones
     */
    public void validatePersonalParameters(List<Argument<S>> personalParams) {
        Map<String, Argument<S>> inheritedByName = new LinkedHashMap<>();
        for (InheritedParameter<S> inherited : chain) {
            inheritedByName.put(inherited.getArgument().getName(), inherited.getArgument());
        }

        for (Argument<S> personal : personalParams) {
            Argument<S> conflict = inheritedByName.get(personal.getName());
            if (conflict != null) {
                throw new IllegalStateException(
                        "Personal parameter '" + personal.getName() + "' conflicts with inherited parameter " +
                                "from '" + Objects.requireNonNull(conflict.getParent()).getName() + "'"
                );
            }
        }
    }

    public static final class InheritedParameter<S extends Source> {

        private final Argument<S> argument;
        private final Command<S> sourceCommand;
        private final int sourceIndex;

        public InheritedParameter(Argument<S> argument, Command<S> sourceCommand, int sourceIndex) {
            this.argument = argument;
            this.sourceCommand = sourceCommand;
            this.sourceIndex = sourceIndex;
        }

        public Argument<S> getArgument() {
            return argument;
        }

        public Command<S> getSourceCommand() {
            return sourceCommand;
        }

        public int getSourceIndex() {
            return sourceIndex;
        }
    }

    public static final class Builder<S extends Source> {

        private final List<InheritedParameter<S>> chain = new ArrayList<>();
        private CommandPathway<S> sourcePathway;
        private Command<S> sourceCommand;

        public Builder<S> addInherited(Argument<S> argument, Command<S> sourceCommand, int sourceIndex) {
            chain.add(new InheritedParameter<>(argument, sourceCommand, sourceIndex));
            return this;
        }

        public Builder<S> sourcePathway(CommandPathway<S> pathway) {
            this.sourcePathway = pathway;
            return this;
        }

        public Builder<S> sourceCommand(Command<S> command) {
            this.sourceCommand = command;
            return this;
        }

        public ParameterInheritanceChain<S> build() {
            return new ParameterInheritanceChain<>(chain, sourcePathway, sourceCommand);
        }
    }
}