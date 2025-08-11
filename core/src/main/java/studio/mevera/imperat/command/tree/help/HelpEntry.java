package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.tree.ParameterNode;
import studio.mevera.imperat.context.Source;

import java.util.Objects;

public final class HelpEntry<S extends Source> {
    
    private final ParameterNode<S, ?> node;
    private final @NotNull CommandUsage<S> pathway;
    
    HelpEntry(ParameterNode<S, ?> node) {
        if(!node.isExecutable()) {
            throw new IllegalArgumentException("Node '" + node.format() + "' is not executable");
        }
        this.node = node;
        
        assert node.getExecutableUsage() != null;
        this.pathway = node.getExecutableUsage();
    }
    
    public @NotNull CommandUsage<S> getPathway() {
        return pathway;
    }
    
    public ParameterNode<S, ?> getNode() {
        return node;
    }
    
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof HelpEntry<?> helpEntry)) return false;
        return Objects.equals(node, helpEntry.node);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(node);
    }
}
