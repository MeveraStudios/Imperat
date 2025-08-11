package studio.mevera.imperat.command.tree.help;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Source;
import java.util.Objects;

/**
 * Standard help provider that queries the command tree.
 */
public class StandardHelpProvider<S extends Source> implements HelpProvider<S> {
    
    @Override
    public HelpEntryList<S> provide(Command<S> command, HelpQuery<S> query) {
        // Just retrieves data, no rendering logic
        if(command.parent() != null) {
            return HelpEntryList.empty();
        }
        return Objects.requireNonNull(command.tree()).queryHelp(query);
    }
}