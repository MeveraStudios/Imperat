package studio.mevera.imperat.command.tree.help.renderers.layouts;

import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.tree.ParameterNode;
import studio.mevera.imperat.context.Source;

import java.util.List;

record ListViewModel<S extends Source>(List<ListItem<S>> items) {
    
    record ListItem<S extends Source>(ParameterNode<S, ?> node, CommandUsage<S> usage) { }
    
}
