package studio.mevera.imperat.exception;

import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.tree.CommandNode;
import studio.mevera.imperat.context.Source;

import java.util.Collection;
import java.util.stream.Collectors;

public final class AmbiguousCommandException extends RuntimeException {

    /**
     * Thrown when a root command has two or more command usages with the same syntax at any parameter.
     * @param rootCommand the root command that has ambiguous syntax
     * @param node the command node that represents the parameter with ambiguous syntax
     * @param <S> the command source type
     */
    public <S extends Source> AmbiguousCommandException(
            final Command<S> rootCommand,
            final CommandNode<S, ?> node,
            final Collection<CommandNode<S, ?>> filteredChildren
    ) {
        super(
                getAmbiguousParameterMsg(rootCommand, node, filteredChildren)
        );
    }

    private static <S extends Source> String getAmbiguousParameterMsg(
            Command<S> rootCmd,
            CommandNode<S, ?> node,
            Collection<CommandNode<S, ?>> filteredChildren
    ) {

        return String.format(
                "Root-RootCommand '%s' has node '%s' with ambiguous arguments '%s' \n Tree >>\n %s !",
                rootCmd,
                node.format(),
                filteredChildren.stream().map(CommandNode::format).collect(Collectors.joining(",")),
                rootCmd.getVisualizer().getVisualizationString()
        );
    }

    /**
     * Thrown when a root command with the same name/alias already exists in the command mapping.
     * @param rootCmd the root command that is being registered and has a name that already exists in the command mapping.
     * @param <S> the command source type
     */
    public <S extends Source> AmbiguousCommandException(
            final Command<S> rootCmd,
            final Command<S> otherCmd
    ) {
        super(
                getDuplicateCmdMsg(rootCmd, otherCmd)
        );
    }
    private static <S extends Source> String getDuplicateCmdMsg(Command<S> cmd, Command<S> otherCmd) {
        StringBuilder builder = new StringBuilder();
        builder.append("RootCommand with name '").append(cmd.format()).append("' already exists !\n");
        if(otherCmd.isAnnotated()) {
            var e = otherCmd.getAnnotatedElement();
            if(e instanceof MethodElement methodElement) {
                builder.append("From method: ").append(methodElement.getName()).append("() from class: '").append(methodElement.getParent().getName()).append("\n");
            }else {
                assert e != null;
                builder.append("From class: ")
                    .append(e.getName());
            }
        }
        return builder.toString();

    }

    public AmbiguousCommandException(String msg) {
        super(msg);
    }


}
