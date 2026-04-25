package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.permissions.PermissionChecker;
import studio.mevera.imperat.util.priority.PriorityList;

import java.util.Map;
import java.util.Optional;

public final class ParsedNode<S extends CommandSource> extends Node<S> {

    private final Node<S> delegate;
    private final Map<String, ParseResult<S>> parseResults;

    ParsedNode(@NotNull Node<S> delegate, Map<String, ParseResult<S>> parseResults) {
        super(delegate.parent, delegate.originalPathway, delegate.main, delegate.optionals);
        this.delegate = delegate;
        this.parseResults = parseResults;
    }

    public Node<S> getDelegate() {
        return delegate;
    }

    public Map<String, ParseResult<S>> getParseResults() {
        return parseResults;
    }

    public int getTotalParseScore() {
        return getParseResults().values().stream().mapToInt(ParseResult::getParseScore)
                       .sum();
    }

    @Override
    public PriorityList<Node<S>> getChildren() {
        return delegate.getChildren();
    }

    @Override
    public ParsedNode<S> parseArgument(RawInputStream<S> inputStream) {
        //            throw new UnsupportedOperationException("Node '" + delegate.main.format() + "' is already parsed");
        return this;
    }

    public Optional<Argument<S>> findInAccessibleArgument(ImperatConfig<S> config, S source) {

        PermissionChecker<S> checker = config.getPermissionChecker();
        //first check the perm for the main argument
        return this.parseResults.values()
                       .stream()
                       .map(ParseResult::getArgument)
                       .filter((argument) -> !checker.hasPermission(source, argument))
                       .findFirst();
    }
}